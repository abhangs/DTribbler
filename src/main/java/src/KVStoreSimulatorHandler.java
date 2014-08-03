package src;

import com.google.gson.Gson;
import include.KVStoreSimulator.*;
import include.KVStoreSimulator.ClockResponse;
import include.KVStoreSimulator.GetListResponse;
import include.KVStoreSimulator.GetResponse;
import include.KVStoreSimulator.KVStoreStatus;
import include.KeyValueStore.*;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;


public class KVStoreSimulatorHandler implements KVStoreSimulator.Iface {

    private static HashMap<String,String> _backendServersQueue;
    private static HashMap<String,String> _redisServersQueue;
    private static long _atLeast;
    private static boolean _kvStoresReady = false;
    private static boolean _redisServersReady = false;
    private static String _respondedServerValue;

    public KVStoreSimulatorHandler()
    {
        _backendServersQueue = new HashMap<String, String>();
        _redisServersQueue = new HashMap<String, String>();
        _atLeast = 0;
        _respondedServerValue = "";

        CreateRedisServers();

        //busy waiting for all redis severs to be initialized
        //by the executor service
        while(!_redisServersReady);

        CreateKVStoreServers();

        //busy waiting for all KVStore severs to be initialized
        //by the executor service
        while(!_kvStoresReady);

        //setup the heartbeat and clock synchronization for each KVStore server
        HeartBeat();

    }

    @Override
    public ClockResponse Clock(long atLeast) throws TException {
        return new ClockResponse("",_atLeast,true);
    }

    @Override
    public boolean UpdateServerList(String jsonServerList) throws TException {

        Gson gson = new Gson();
        _backendServersQueue = gson.fromJson(jsonServerList,_backendServersQueue.getClass());
        return true;
    }

    @Override
    public KVStoreServerInfo GetKVStoreServerInfo() throws TException {

      //Circulate between responded server names for load balancing

      KVStoreServerInfo kvStoreServerInfo = new KVStoreServerInfo();

      Collection<String> keys = _backendServersQueue.keySet();

      for(String item:keys)
      {
          String value = _backendServersQueue.get(item);

          if(value==_respondedServerValue)
              continue;

          String serverAddress = value.split(":")[0];
          String serverPort = value.split(":")[1];

          kvStoreServerInfo.serverName = item;
          kvStoreServerInfo.serverAddress = serverAddress;
          kvStoreServerInfo.serverPort = serverPort;

          _respondedServerValue = value;
      }

      return kvStoreServerInfo;

    }


    public static void CreateKVStoreServers()
    {
        Collection<String> allRedisServers = _redisServersQueue.keySet();

        int serverPortSeed = 9090;

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(String item: allRedisServers)
        {
            try {
                String serverName = UUID.randomUUID().toString();
                String redisServerAddress = _redisServersQueue.get(item).split(":")[0];
                String redisServerPort = _redisServersQueue.get(item).split(":")[1];
                KVStoreServerCallable kvStoreWorker = new KVStoreServerCallable(serverName,"localhost",String.valueOf(serverPortSeed+1),redisServerAddress,redisServerPort,_backendServersQueue,_atLeast);
                FutureTask<ServerMessage> futureTask = new FutureTask<ServerMessage>(kvStoreWorker);
                executorService.submit(futureTask);

                while(!futureTask.isDone());

                if(futureTask.get().is_serverStarted())
                    _backendServersQueue.put(serverName,"localhost:"+String.valueOf(serverPortSeed+1));

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
            _kvStoresReady = true;
        } catch (InterruptedException e) {
            _kvStoresReady = false;
        }
    }

    public static void HeartBeat()
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.submit(new Callable<ServerMessage>() {

            @Override
            public ServerMessage call() throws Exception {
                _atLeast++;

                Thread.sleep(3000);

                Collection<String> allServersIDs = _backendServersQueue.keySet();

                for (String item : allServersIDs) {
                    try {


                        //check if backend sever information was removed
                        if(!_backendServersQueue.containsKey(item))
                            continue;

                        String[]serverInfo = _backendServersQueue.get(item).split(":");
                        TSocket socket = new TSocket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                        TTransport transport = socket;

                        TProtocol protocol = new TBinaryProtocol(transport);
                        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                        transport.open();

                        include.KeyValueStore.ClockResponse clockResponse = client.Clock(_atLeast );

                        if(!clockResponse.isSuccess())
                        {
                            throw new Exception();
                        }

                        transport.close();


                    }
                    catch (Exception ex)
                    {
                        //backend server failed to respond, remove from backend servers queue
                         _backendServersQueue.remove(item);

                    }
                    finally
                    {
                        //check if backend server was removed from servers queue and propagate
                        //this information to all other backend servers
                        if(!_backendServersQueue.containsKey(item))
                        {
                            ExecutorService service = Executors.newSingleThreadExecutor();
                            service.execute(new Runnable() {

                                @Override
                                public void run() {
                                    Collection<String> keys = _backendServersQueue.keySet();

                                    for(String item:keys)
                                    {
                                        try
                                        {
                                            String[]serverInfo = _backendServersQueue.get(item).split(":");
                                            TSocket socket = new TSocket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                                            TTransport transport = socket;

                                            TProtocol protocol = new TBinaryProtocol(transport);
                                            KeyValueStore.Client client = new KeyValueStore.Client(protocol);

                                            Gson gson = new Gson();

                                            //Here propagate the value and forget, we do not handle exceptions
                                            client.UpdateServerList(gson.toJson(_backendServersQueue));

                                            transport.open();

                                        }
                                        catch (Exception ex)
                                        {

                                        }
                                    }
                                }
                            });
                        }
                    }

                }

                return null;
            }
        });
    }

    public static void CreateRedisServers()
    {
        int exeArgumentsCount = 4;

        String exePath = "E:\\projects\\redis-2.8\\redis-2.8\\bin\\release\\redis-2.8.12\\redis-server.exe";

        String exeArgument1 = "E:\\projects\\redis-2.8\\redis-2.8\\bin\\release\\redis-2.8.12\\redis6382.windows.conf";
        String exeArgument2 = "E:\\projects\\redis-2.8\\redis-2.8\\bin\\release\\redis-2.8.12\\redis6381.windows.conf";
        String exeArgument3 = "E:\\projects\\redis-2.8\\redis-2.8\\bin\\release\\redis-2.8.12\\redis6380.windows.conf";
        String exeArgument4 = "E:\\projects\\redis-2.8\\redis-2.8\\bin\\release\\redis-2.8.12\\redis6379.windows.conf";

        int[] exePorts = new int[]{6379,6380,6381,6382};
        String[] exeArguments = new String[]{exeArgument1,exeArgument2,exeArgument3,exeArgument4};

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(int i=0;i<exeArgumentsCount;i++)
        {
            try {
                String severName = UUID.randomUUID().toString();
                RedisCallable redisWorker = new RedisCallable(exePath,exeArguments[i],severName);
                FutureTask<ServerMessage> futureTask = new FutureTask<ServerMessage>(redisWorker);
                executorService.submit(futureTask);

                while(!futureTask.isDone());
                if(futureTask.get().is_serverStarted())
                _redisServersQueue.put(severName,"localhost:"+exePorts[i]);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        executorService.shutdown();

        try
        {
            executorService.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS);
            _redisServersReady = true;
        }
        catch (InterruptedException e) {
            _redisServersReady = false;
        }

    }


    public static class RedisCallable implements Callable<ServerMessage>
    {
        private String _serverEXEPath;
        private String _serverEXEArguments;
        private String _serverName;

        RedisCallable(String serverEXEPath,String serverEXEArguments, String serverName)
        {
            _serverEXEPath = serverEXEPath;
            _serverEXEArguments = serverEXEArguments;
            _serverName = serverName;
        }

        @Override
        public ServerMessage call() throws Exception {
            try
            {
                Process process = new ProcessBuilder(_serverEXEPath,_serverEXEArguments).start();
                ServerMessage serverMessage = new ServerMessage();
                serverMessage.set_serverStarted(true);
                serverMessage.set_exceptionMessage("");
                serverMessage.set_serverName(_serverName);
                return serverMessage;
            }
            catch (Exception e)
            {
                ServerMessage serverMessage = new ServerMessage();
                serverMessage.set_serverName(_serverName);
                serverMessage.set_exceptionMessage(e.getMessage());
                serverMessage.set_serverStarted(false);
                return serverMessage;
            }

        }
    }


    public static class KVStoreServerCallable implements Callable<ServerMessage>
    {
        private String _serverName;
        private String _serverAddress;
        private String _serverPort;
        private String _redisServerAddress;
        private String _redisServerPort;
        private HashMap<String,String> _backendServersQueue;
        private long _atLeast;

        KVStoreServerCallable(String serverName, String serverAddress, String serverPort,String redisServerAddress,String redisServerPort, HashMap<String,String> backendServersQueue, long atLeast)
        {
            _serverName = serverName;
            _serverAddress = serverAddress;
            _serverPort = serverPort;
            _redisServerAddress = redisServerAddress;
            _redisServerPort = redisServerPort;
            _backendServersQueue = backendServersQueue;
            _atLeast = atLeast;
        }


        @Override
        public ServerMessage call() throws Exception {
            try
            {
                KVStoreServerHandler kvStoreServerHandler = new KVStoreServerHandler(_serverName,_serverAddress,_serverPort,_redisServerAddress,_redisServerPort,_backendServersQueue,_atLeast);
                TProcessor processor = new KeyValueStore.Processor<KeyValueStore.Iface>(kvStoreServerHandler);
                TServerTransport transport = new TServerSocket(Integer.parseInt(_serverPort));
                TTransportFactory transportFactory = new TTransportFactory();
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

                TSimpleServer simpleServer = new TSimpleServer(new TSimpleServer.Args(transport).processor(processor).inputProtocolFactory(protocolFactory).transportFactory(transportFactory));
                simpleServer.serve();

                ServerMessage serverMessage = new ServerMessage();
                serverMessage.set_serverName(_serverName);
                serverMessage.set_exceptionMessage("");
                serverMessage.set_serverStarted(true);

                return serverMessage;
            }
            catch (Exception ex)
            {
                ServerMessage serverMessage = new ServerMessage();
                serverMessage.set_serverName(_serverName);
                serverMessage.set_exceptionMessage(ex.getMessage());
                serverMessage.set_serverStarted(false);
                return serverMessage;
            }

        }
    }


    public static class ServerMessage
    {
        private String _serverName;
        private boolean _serverStarted;
        private String _exceptionMessage;

        public String get_serverName() {
            return _serverName;
        }

        public void set_serverName(String _serverName) {
            this._serverName = _serverName;
        }

        public boolean is_serverStarted() {
            return _serverStarted;
        }

        public void set_serverStarted(boolean _serverStarted) {
            this._serverStarted = _serverStarted;
        }

        public String get_exceptionMessage() {
            return _exceptionMessage;
        }

        public void set_exceptionMessage(String _exceptionMessage) {
            this._exceptionMessage = _exceptionMessage;
        }
    }

    //============================= non required methods and classes (may be essential in future)=======================

    //Unused
    public static class KVStoreServerRunnable implements Runnable
    {
        private String _serverName;
        private String _serverAddress;
        private String _serverPort;
        private String _redisServerAddress;
        private String _redisServerPort;
        private HashMap<String,String> _backendServersQueue;
        private long _atLeast;

        KVStoreServerRunnable(String serverName, String serverAddress, String serverPort,String redisServerAddress,String redisServerPort, HashMap<String,String> backendServersQueue, long atLeast)
        {
            _serverName = serverName;
            _serverAddress = serverAddress;
            _serverPort = serverPort;
            _redisServerAddress = redisServerAddress;
            _redisServerPort = redisServerPort;
            _backendServersQueue = backendServersQueue;
            _atLeast = atLeast;
        }

        @Override
        public void run() {
            try
            {
                KVStoreServerHandler kvStoreServerHandler = new KVStoreServerHandler(_serverName,_serverAddress,_serverPort,_redisServerAddress,_redisServerPort,_backendServersQueue,_atLeast);
                TProcessor processor = new KeyValueStore.Processor<KeyValueStore.Iface>(kvStoreServerHandler);
                TServerTransport transport = new TServerSocket(Integer.parseInt(_serverPort));
                TTransportFactory transportFactory = new TTransportFactory();
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

                TSimpleServer simpleServer = new TSimpleServer(new TSimpleServer.Args(transport).processor(processor).inputProtocolFactory(protocolFactory).transportFactory(transportFactory));
                simpleServer.serve();

            }
            catch (Exception ex)
            {

            }
        }
    }

    //Unused
    public static class RedisRunnable implements Runnable
    {
        private String _serverEXEPath;
        private String _serverEXEArguments;
        private String _serverName;

        RedisRunnable(String serverEXEPath,String serverEXEArguments, String serverName)
        {
            _serverEXEPath = serverEXEPath;
            _serverEXEArguments = serverEXEArguments;
            _serverName = serverName;
        }

        @Override
        public void run() {
            try {
                Process process = new ProcessBuilder(_serverEXEPath,_serverEXEArguments).start();
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();

            }

        }
    }

    //Unused
    @Override
    public GetResponse Get(String key) throws TException {
        return null;
    }

    //Unused
    @Override
    public GetListResponse GetList(String key) throws TException {
        return null;
    }

    //Unused
    @Override
    public KVStoreStatus Put(String key, String value, String clientid) throws TException {
        return null;
    }

    //Unused
    @Override
    public KVStoreStatus AddToList(String key, String value, String clientid) throws TException {
        return null;
    }

    //Unused
    @Override
    public KVStoreStatus RemoveFromList(String key, String value, String clientid) throws TException {
        return null;
    }


}
