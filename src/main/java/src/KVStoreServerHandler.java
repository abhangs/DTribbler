package src;

import include.KeyValueStore.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import redis.clients.jedis.Jedis;


import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class KVStoreServerHandler implements KeyValueStore.Iface
{
    private String remoteServerRequestPrefix = "remote:";
    //variable for clock
    private long _atLeast;
    private String _serverID;
    private KVServerStatus _serverStatus;
    private HashMap<String,String> _backendServersQueue;
    private HashSet<String> _runningServerCollection;
    private HashSet<String> _deadServerCollection;

    private String _storageServerAddress;
    private String _storageServerPort;

    private String _redisServerAddress;
    private int _redisServerPort;

    private Jedis _jedis;

    private ArrayList<String> _serverCommandPrefixes;

    private static final Logger LOGGER = Logger.getLogger(KVStoreServerHandler.class.getName());

    private ExecutorService _executorService;

    private String _remoteServerRequestID;

    public KVStoreServerHandler(String serverID, String storageServerAddress,String storageServerPort,String redisServerAddress, int redisServerPort,HashMap<String, String> backEndServersQueue,  long clockSeedTime)
            throws Exception
    {
        this._serverID = serverID;
        this._storageServerAddress = storageServerAddress;
        this._storageServerPort = storageServerPort;
        this._redisServerAddress = redisServerAddress;
        this._redisServerPort = redisServerPort;
        this._atLeast = clockSeedTime;

        this._backendServersQueue = backEndServersQueue;
        this._runningServerCollection = new HashSet<String>();
        this._deadServerCollection = new HashSet<String>();
        this._jedis = new Jedis(_redisServerAddress,_redisServerPort);


        _serverCommandPrefixes = new ArrayList<String>();
        _serverCommandPrefixes.add(Utilities.getServerStatusPrefix());
        _serverCommandPrefixes.add(Utilities.getServerAllKeysPrefix());
        _remoteServerRequestID = remoteServerRequestPrefix+_serverID;

        _executorService = Executors.newCachedThreadPool();

        if(!Initialize())
                throw new Exception();

        //check initialize value and call the keeper joincluster with appropriate message
        //then change server status to running if everything goes well  , should be a separate call?

    }


    public boolean Initialize()
    {
        _serverStatus = KVServerStatus.Syncing;
        if(!SyncServer())
        {
           return _backendServersQueue.size()>1?false:true;
        }
        return true;
    }

    private KVStoreStatus SyncDataFromServer(String serverAddress, int serverPort) {

        GetListResponse listResponse = RemoteGetList(serverAddress,serverPort,Utilities.getServerAllKeysPrefix());

        //need to check if this will correctly handle sets

        try{
            for(String item:listResponse.getValues())
            {
                String[] keyValue = item.split(":");
                Put(keyValue[0],keyValue[1],this._serverID);
            }

            return KVStoreStatus.OK;

        }catch(Exception ex)
        {
            return KVStoreStatus.EPUTFAILED;
        }


    }

    private boolean SyncServer()
    {
        Collection<String> allServers = _backendServersQueue.values();

        for(String item:allServers)
        {
            String[] serverInfo = item.split(":");
            if(RemoteGet(serverInfo[0],Integer.parseInt(serverInfo[1]),Utilities.getServerStatusPrefix()).getValue().equals(KVServerStatus.Running))
            {
               KVStoreStatus storeStatus = SyncDataFromServer(serverInfo[0],Integer.parseInt(serverInfo[1]));
               if(KVStoreStatus.OK==storeStatus)
               {
                   return true;
               }
            }

        }

        return false;
    }


    @Override
    public GetResponse Get(String key) throws TException {

        GetResponse response = new GetResponse();
        try
        {
            String value = _jedis.get(key);

            if(value.isEmpty())
            {
                throw new NullPointerException();
            }

            response.setValue(value);
            response.setStatus(KVStoreStatus.OK);
            return response;
        }
        catch(Exception ex)
        {
            if(ex.getClass()== NullPointerException.class)
            {
                response.setStatus(KVStoreStatus.EITEMNOTFOUND);
                response.setValue(null);
                return response;
            }

            response.setStatus(KVStoreStatus.EPUTFAILED);
            response.setValue(null);
            return  response;
        }

    }

    @Override
    public GetListResponse GetList(String key) throws TException {
        if(this._serverStatus==KVServerStatus.Running)
        {
            if(_serverCommandPrefixes.contains(key))
            {
                return GetListResponse.class.cast(ProcessCommand(key));
            }
        }

        GetListResponse listResponse = new GetListResponse();
        try
        {

            List<String> values = _jedis.lrange(key,0,-1);

            if(values.isEmpty())
            {
                throw new NullPointerException();
            }

            listResponse.setValues(values);
            listResponse.setStatus(KVStoreStatus.OK);
            return listResponse;
        }
        catch(Exception ex)
        {

            if(ex.getClass()==NullPointerException.class)
            {
                listResponse.setValues(null);
                listResponse.setStatus(KVStoreStatus.EITEMNOTFOUND);
                return listResponse;
            }

            listResponse.setValues(null);
            listResponse.setStatus(KVStoreStatus.EPUTFAILED);
            return listResponse;
        }

    }


    @Override
    public KVStoreStatus Put(String key, String value, String clientid) throws TException {

        if (this._serverStatus==KVServerStatus.Running) {
            //Remote put request, we do not need to domino this the other back-end servers
            if(clientid.contains("remote"))
            {
               if(clientid.contentEquals(_remoteServerRequestID))
               {
                   try
                   {
                       if(!_jedis.set(key,value).equals("OK"))
                       {
                           throw new Exception();
                       }
                      return KVStoreStatus.OK;
                   }
                   catch (Exception ex)
                   {
                       return KVStoreStatus.EPUTFAILED;
                   }
               }
            }

            //Original request, need to domino this to other servers
            if(clientid.contentEquals(_serverID))
            {
                final String fKey = key;
                final String fValue = value;
                try
                {
                    if(!_jedis.set(key,value).equals("OK"))
                    {
                        throw new Exception();
                    }
                    _executorService.execute(new Runnable() {

                        String key = fKey;
                        String value = fValue;

                        @Override
                        public void run() {
                            Collection<String> allServersIDs = _backendServersQueue.keySet();

                            for (String item : allServersIDs) {
                                try {

                                    String[]serverInfo = _backendServersQueue.get(item).split(":");
                                    TSocket socket = new TSocket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                                    TTransport transport = socket;

                                    TProtocol protocol = new TBinaryProtocol(transport);
                                    KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                    transport.open();

                                    KVStoreStatus storeStatus = client.Put(key,value,remoteServerRequestPrefix+item );

                                    transport.close();


                                } catch (Exception ex)
                                {

                                }
                            }
                        }
                    });
                }
                catch(Exception ex)
                {
                    return KVStoreStatus.EPUTFAILED;
                }
            }
        }

        return KVStoreStatus.EPUTFAILED;

    }

    @Override
    public KVStoreStatus AddToList(String key, String value, String clientid) throws TException {

        if (this._serverStatus==KVServerStatus.Running) {
            //a remote request
            if(clientid.contains("remote"))
            {
               if(clientid.contentEquals(_remoteServerRequestID))
               {
                   try
                   {
                       if(!(_jedis.lpush(key,value)>0))
                       {
                           throw new Exception();
                       }
                       return KVStoreStatus.OK;
                   }
                   catch (Exception ex)
                   {
                       return KVStoreStatus.EPUTFAILED;
                   }
               }
            }

            if(clientid.contentEquals(_serverID))
            {
                try
                {
                    final String fKey = key;
                    final String fValue = value;
                    if(!(_jedis.lpush(key,value)>0))
                    {
                        throw new Exception();
                    }

                    _executorService.execute(new Runnable() {
                        String key = fKey;
                        String value = fValue;
                        @Override
                        public void run() {
                            Collection<String> allServersIDs = _backendServersQueue.keySet();

                            for (String item : allServersIDs) {
                                try {

                                    String[]serverInfo = _backendServersQueue.get(item).split(":");
                                    TSocket socket = new TSocket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                                    TTransport transport = socket;

                                    TProtocol protocol = new TBinaryProtocol(transport);
                                    KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                    transport.open();

                                    KVStoreStatus storeStatus = client.AddToList(key, value, remoteServerRequestPrefix + item);

                                    transport.close();


                                } catch (Exception ex)
                                {

                                }
                            }
                        }
                    });

                    return KVStoreStatus.OK;
                }
                catch (Exception ex)
                {
                    return KVStoreStatus.EPUTFAILED;
                }
            }
        }

        return KVStoreStatus.EPUTFAILED;
    }

    @Override
    public KVStoreStatus RemoveFromList(String key, String value, String clientid) throws TException {

        if (this._serverStatus==KVServerStatus.Running) {
            if(clientid.contains("remote"))
            {
                if(clientid.contentEquals(_remoteServerRequestID))
                {
                    try
                    {
                        if(!(_jedis.lrem(key,1,value)>0))
                        {
                            throw new Exception();
                        }

                        return KVStoreStatus.OK;
                    }
                    catch(Exception ex)
                    {
                        return KVStoreStatus.EPUTFAILED;
                    }
                }
            }

            if(clientid.contentEquals(_serverID))
            {
                try
                {
                    final String fKey = key;
                    final String fValue = value;
                    if(!(_jedis.lrem(key,1,value)>0))
                    {
                        throw new Exception();
                    }

                    _executorService.execute(new Runnable() {
                        String key = fKey;
                        String value = fValue;
                        @Override
                        public void run() {
                            Collection<String> allServersIDs = _backendServersQueue.keySet();

                            for (String item : allServersIDs) {
                                try {

                                    String[]serverInfo = _backendServersQueue.get(item).split(":");
                                    TSocket socket = new TSocket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                                    TTransport transport = socket;

                                    TProtocol protocol = new TBinaryProtocol(transport);
                                    KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                    transport.open();

                                    KVStoreStatus storeStatus = client.RemoveFromList(key, value, remoteServerRequestPrefix + item);

                                    transport.close();


                                } catch (Exception ex)
                                {

                                }
                            }

                        }
                    });

                    return KVStoreStatus.OK;
                }
                catch(Exception ex)
                {
                    return KVStoreStatus.EPUTFAILED;
                }
            }
        }

        return KVStoreStatus.EPUTFAILED;
    }

    @Override
    public ClockResponse Clock(long atLeast) throws TException {
        return null;
    }

    //In RPC calls if a server is dead then remove it from the runningServersQueue

    public  GetResponse RemoteGet(String serverAddress,int serverPort, String key)
    {
        return null;
    }



    public  GetListResponse RemoteGetList(String serverAddress, int serverPort, String key) {
        return null;
    }

    //functions to perform various Server processes such synching, returning server status
    private GetListResponse GetAllKeys()
    {
        _serverStatus = KVServerStatus.Busy;
        Set<String> allKeys = _jedis.keys("*");
        GetListResponse response = new GetListResponse();
        ArrayList<String> keyValues = new ArrayList<String>();

        try{

            for(String key:allKeys)
            {
                String value = _jedis.get(key);
                keyValues.add(key+":"+value);
            }

            response.setValues(keyValues);
            response.setStatus(KVStoreStatus.OK);

        }catch (Exception ex)
        {
            response.setValues(null);
            response.setStatus(KVStoreStatus.INTERNAL_FAILURE);
        }
        _serverStatus = KVServerStatus.Running;
        return response;
    }

    private Object ProcessCommand(String key) {

        if(key==Utilities.getServerAllKeysPrefix())
        {
            return GetAllKeys();
        }

        
    
       return null;
    }

}


