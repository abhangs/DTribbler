package src;

import com.sun.org.apache.xpath.internal.operations.Bool;
import include.KeyValueStore.*;
import org.apache.thrift.TException;

import javax.management.monitor.StringMonitor;
import javax.swing.text.html.HTMLDocument;
import java.util.*;
import java.util.logging.Logger;

public class KVStoreServerHandler implements KeyValueStore.Iface
{
    //variable for clock
    private long _atLeast;
    private String _serverID;
    private KVServerStatus _serverStatus;
    private ArrayDeque<Map<String,Integer>> _backendServersQueue;
    private HashSet<String> _runningServerCollection;
    private HashSet<String> _deadServerCollection;

    private String _storageServerAddress;
    private String _storageServerPort;

    private String _redisServerAddress;
    private String _redisServerPort;

    private static final Logger LOGGER = Logger.getLogger(KVStoreServerHandler.class.getName());

    public KVStoreServerHandler(String serverID, String storageServerAddress,String storageServerPort,String redisServerAddress, String redisServerPort,ArrayDeque<Map<String, Integer>> backEndServersQueue,  long clockSeedTime)
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
    }

    private static Boolean Initialize()
    {
        return false;
    }

    private Map<String,Integer> GetRunningServer()
    {
        Map<String,Integer> serverInfo = null;
        Map<String,Integer>[] mapArray = new Map[_backendServersQueue.size()];
        _backendServersQueue.toArray(mapArray);


        for(Map<String,Integer> item:mapArray)
        {

        }

        return null;
    }


    @Override
    public GetResponse Get(String key) throws TException {
        return null;

    }

    @Override
    public GetListResponse GetList(String key) throws TException {
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
}


