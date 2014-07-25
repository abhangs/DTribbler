package src;

import include.KeyValueStore.*;
import org.apache.thrift.TException;
import redis.clients.jedis.Jedis;


import java.util.*;
import java.util.logging.Logger;

public class KVStoreServerHandler implements KeyValueStore.Iface
{
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

    private static Jedis _jedis;

    private ArrayList<String> _serverCommandPrefixes;

    private static final Logger LOGGER = Logger.getLogger(KVStoreServerHandler.class.getName());

    public KVStoreServerHandler(String serverID, String storageServerAddress,String storageServerPort,String redisServerAddress, int redisServerPort,HashMap<String, String> backEndServersQueue,  long clockSeedTime)
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

        Initialize();
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
        return null;

    }

    @Override
    public GetListResponse GetList(String key) throws TException {
        if(this._serverStatus==KVServerStatus.Running)
        {
            if(_serverCommandPrefixes.contains(key))
            {
                return ProcessCommand(key);
            }
        }


        return null;
    }


    @Override
    public KVStoreStatus Put(String key, String value, String clientid) throws TException {
        return null;
    }

    @Override
    public KVStoreStatus AddToList(String key, String value, String clientid) throws TException {
        return null;
    }

    @Override
    public KVStoreStatus RemoveFromList(String key, String value, String clientid) throws TException {
        return null;
    }

    @Override
    public ClockResponse Clock(long atLeast) throws TException {
        return null;
    }

    //In RPC calls if a server is dead then remove it from the runningServersQueue

    public synchronized GetResponse RemoteGet(String serverAddress,int serverPort, String key)
    {
        return null;
    }

    public synchronized GetListResponse RemoteGetList(String serverAddress, int serverPort, String key) {
        return null;
    }

    //functions to perform various Server processes such synching, returning server status
    private synchronized GetListResponse GetAllKeys()
    {
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

        return response;
    }


    private GetListResponse ProcessCommand(String key) {

        if(key==Utilities.getServerAllKeysPrefix())
        {
            return GetAllKeys();
        }

        
    
       return null;
    }

}


