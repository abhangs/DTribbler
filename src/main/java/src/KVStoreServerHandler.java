package src;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import include.KeyValueStore.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import redis.clients.jedis.Jedis;


import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class KVStoreServerHandler implements KeyValueStore.Iface
{
    private String remoteServerRequestPrefix = "remote:";
    //variable for clock
    private long _atLeast;
    private String _serverName;
    private KVServerStatus _serverStatus;
    private HashMap<String,String> _backendServersQueue;


    private String _storageServerAddress;
    private String _storageServerPort;

    private String _redisServerAddress;
    private int _redisServerPort;

    private Jedis _jedis;

    private ArrayList<String> _serverCommandPrefixes;

    private static final Logger LOGGER = Logger.getLogger(KVStoreServerHandler.class.getName());

    private ExecutorService _executorService;

    private String _remoteServerRequestID;

    public KVStoreServerHandler(String serverID, String storageServerAddress,String storageServerPort,String redisServerAddress, String redisServerPort,HashMap<String, String> backEndServersQueue,  long clockSeedTime)
            throws Exception
    {
        this._serverName = serverID;
        this._storageServerAddress = storageServerAddress;
        this._storageServerPort = storageServerPort;
        this._redisServerAddress = redisServerAddress;
        this._redisServerPort = Integer.parseInt(redisServerPort);
        this._atLeast = clockSeedTime;

        this._backendServersQueue = backEndServersQueue;


        this._jedis = new Jedis(_redisServerAddress,_redisServerPort);


        _serverCommandPrefixes = new ArrayList<String>();
        _serverCommandPrefixes.add(Utilities.getServerStatusPrefix());
        _serverCommandPrefixes.add(Utilities.getServerAllKeysPrefix());
        _remoteServerRequestID = remoteServerRequestPrefix+ _serverName;

        _executorService = Executors.newCachedThreadPool();

        if(!Initialize())
                throw new Exception();

        _serverStatus = KVServerStatus.Running;
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
                Put(keyValue[0],keyValue[1],this._serverName);
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
            if(RemoteGet(serverInfo[0],Integer.parseInt(serverInfo[1]),Utilities.getServerStatusPrefix()).getValue().contentEquals(String.valueOf(KVServerStatus.Running)))
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
        if (_serverStatus == KVServerStatus.Running) {

           if(key.contains(remoteServerRequestPrefix))
           {
               if(_serverCommandPrefixes.contains(key.split(":")[1]))
               {
                  GetResponse processCommandResponse = (GetResponse) ProcessCommand(key);
                  return processCommandResponse;
               }

               else
               {
                   //process a normal remote get request
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
           }

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

                    //value not found in this server, continue searching in all other servers
                    //and return the value

                    ExecutorService executorService = Executors.newSingleThreadExecutor();

                    Set<Callable<GetResponse>> responseCallables = new HashSet<Callable<GetResponse>>();

                    Collection<String> keys = _backendServersQueue.keySet();

                    final String remoteGetKey = key;

                    for(String item:keys)
                    {

                       if(item==_serverName)
                           continue;

                       final String[] serverInfo = _backendServersQueue.get(item).split(":");

                        responseCallables.add(new Callable<GetResponse>() {
                            @Override
                            public GetResponse call() throws Exception {
                                GetResponse response;
                                try
                                {
                                    TSocket socket = new TSocket(serverInfo[0],Integer.parseInt(serverInfo[1]));
                                    TTransport transport = socket;

                                    TProtocol protocol = new TBinaryProtocol(transport);
                                    KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                    transport.open();

                                    response = client.Get(remoteServerRequestPrefix + remoteGetKey);

                                    transport.close();

                                    return response;
                                }
                                catch (TException e)
                                {
                                    e.printStackTrace();
                                    response = new GetResponse();
                                    response.setStatus(KVStoreStatus.EPUTFAILED);
                                    response.setValue(null);
                                    return response;
                                }

                            }
                        });
                    }


                    try
                    {
                        List<Future<GetResponse>> futures = executorService.invokeAll(responseCallables);

                        response.setStatus(KVStoreStatus.EITEMNOTFOUND);
                        response.setValue(null);

                        for(Future<GetResponse> item:futures)
                        {
                            if(item.get().status==KVStoreStatus.OK)
                            {
                                response.setStatus(KVStoreStatus.OK);
                                response.setValue(item.get().getValue());
                                break;
                            }
                        }

                        return response;
                    }
                    catch (Exception e) {
                        response.setStatus(KVStoreStatus.EPUTFAILED);
                        response.setValue(null);
                        return response;
                    }



                }

                response.setStatus(KVStoreStatus.EPUTFAILED);
                response.setValue(null);
                return  response;
            }
        }

        return new GetResponse(KVStoreStatus.EPUTFAILED,"");
    }

    @Override
    public GetListResponse GetList(String key) throws TException {
        if(this._serverStatus==KVServerStatus.Running)
        {
            if (key.contains(remoteServerRequestPrefix))
            {
                if(_serverCommandPrefixes.contains(key.split(":")[1]))
                {
                    return GetListResponse.class.cast(ProcessCommand(key));
                }

                else
                {
                    //process a normal remote get list request
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

                    //list not found, search in the all the remaining servers for the list
                    //and return if found

                    ExecutorService executorService = Executors.newSingleThreadExecutor();

                    Set<Callable<GetListResponse>> responseCallables = new HashSet<Callable<GetListResponse>>();

                    Collection<String> keys = _backendServersQueue.keySet();

                    for(String item:keys)
                    {
                        if(item==_serverName)
                            continue;

                        final String[] serverInfo = _backendServersQueue.get(item).split(":");
                        final String remoteGetRequestKey = key;

                        responseCallables.add(new Callable<GetListResponse>() {
                            @Override
                            public GetListResponse call() throws Exception {
                                GetListResponse listResponse;
                                try
                                {
                                    TSocket socket = new TSocket(serverInfo[0],Integer.parseInt(serverInfo[1]));
                                    TTransport transport = socket;

                                    TProtocol protocol = new TBinaryProtocol(transport);
                                    KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                    transport.open();

                                    listResponse = client.GetList(remoteServerRequestPrefix + remoteGetRequestKey);

                                    transport.close();

                                    return listResponse;
                                }
                                catch (TException e)
                                {
                                    e.printStackTrace();
                                    listResponse = new GetListResponse();
                                    listResponse.setStatus(KVStoreStatus.EPUTFAILED);
                                    listResponse.setValues(null);
                                    return listResponse;
                                }

                            }
                        });
                    }

                    try {
                        List<Future<GetListResponse>> futures = executorService.invokeAll(responseCallables);

                        listResponse.setValues(null);
                        listResponse.setStatus(KVStoreStatus.EITEMNOTFOUND);

                        for(Future<GetListResponse> item:futures)
                        {
                             if(item.get().status==KVStoreStatus.OK)
                             {
                                 listResponse.setStatus(KVStoreStatus.OK);
                                 listResponse.setValues(item.get().getValues());
                                 break;
                             }
                        }

                        return listResponse;
                    } catch (Exception e)
                    {
                        listResponse.setValues(null);
                        listResponse.setStatus(KVStoreStatus.EPUTFAILED);
                        return listResponse;
                    }


                }

                listResponse.setValues(null);
                listResponse.setStatus(KVStoreStatus.EPUTFAILED);
                return listResponse;
            }
        }

        return new GetListResponse(KVStoreStatus.EPUTFAILED,null);
    }


    @Override
    public KVStoreStatus Put(String key, String value, String clientid) throws TException {

        if (this._serverStatus==KVServerStatus.Running) {
            //Remote put request, we do not need to propagate this the other back-end servers
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

            //Original request, need to propagate this to other servers
            if(clientid.contentEquals(_serverName))
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

                                    //check if key was removed during UpdateServerList
                                    if(!_backendServersQueue.containsKey(item))
                                        continue;

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

            if(clientid.contentEquals(_serverName))
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

                                    if(!_backendServersQueue.containsKey(key))
                                          continue;

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

            if(clientid.contentEquals(_serverName))
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

        if(atLeast>_atLeast)
            _atLeast = atLeast;

        return new ClockResponse("",_atLeast,true);
    }

    @Override
    public boolean UpdateServerList(String jsonServerList) throws TException {

        {
            try
            {
                Gson gson = new Gson();

                _backendServersQueue = gson.fromJson(jsonServerList,_backendServersQueue.getClass());

                return true;
            }
            catch (JsonSyntaxException e)
            {
               return false;
            }
        }
    }


    public  GetResponse RemoteGet(String serverAddress,int serverPort, String key)
    {
        GetResponse response;
        try
        {
            TSocket socket = new TSocket(serverAddress,serverPort);
            TTransport transport = socket;

            TProtocol protocol = new TBinaryProtocol(transport);
            KeyValueStore.Client client = new KeyValueStore.Client(protocol);

            transport.open();

            response = client.Get(remoteServerRequestPrefix + key);

            transport.close();

            return response;
        }
        catch (TException e)
        {
            e.printStackTrace();
            response = new GetResponse();
            response.setStatus(KVStoreStatus.EPUTFAILED);
            response.setValue(null);
            return response;
        }

    }

    public  GetListResponse RemoteGetList(String serverAddress, int serverPort, String key) {
        GetListResponse listResponse;
        try
        {
            TSocket socket = new TSocket(serverAddress,serverPort);
            TTransport transport = socket;

            TProtocol protocol = new TBinaryProtocol(transport);
            KeyValueStore.Client client = new KeyValueStore.Client(protocol);

            transport.open();

            listResponse = client.GetList(remoteServerRequestPrefix + key);

            transport.close();

            return listResponse;
        }
        catch (TException e)
        {
            e.printStackTrace();
            listResponse = new GetListResponse();
            listResponse.setStatus(KVStoreStatus.EPUTFAILED);
            listResponse.setValues(null);
            return listResponse;
        }

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

    private GetResponse GetServerStatus() {
        return new GetResponse(KVStoreStatus.OK,String.valueOf(_serverStatus));
    }

    private Object ProcessCommand(String key) {

        if(key==Utilities.getServerAllKeysPrefix())
        {
            return GetAllKeys();
        }

        if(key==Utilities.getServerStatusPrefix())
        {
            return GetServerStatus();
        }

       return null;
    }



}


