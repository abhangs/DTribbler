package src;


import include.KVStoreSimulator.KVStoreServerInfo;
import include.KVStoreSimulator.KVStoreSimulator;
import include.Tribbler.Tribble;
import include.Tribbler.Tribbler;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.*;

public class TribblerServer {

    public static void main(String[] args)
    {
        String _tribblerServerHost = "localhost";
        int _tribblerServerPort = 8585;

        //Get the KVStoreSeverInfo

        try
        {
            TSocket socket = new TSocket("localhost",7070);
            TTransport transport = socket;
            TProtocol protocol = new TBinaryProtocol(transport);

            KVStoreSimulator.Client kvStoreSimulatorClient = new KVStoreSimulator.Client(protocol);

            KVStoreServerInfo kvStoreServerInfo = kvStoreSimulatorClient.GetKVStoreServerInfo();

            //Next, start a TribblerServer using the KVStoreInfo

            TribblerServerHandler tribblerServerHandler = new TribblerServerHandler(kvStoreServerInfo.serverName,kvStoreServerInfo.getServerAddress(),Integer.parseInt(kvStoreServerInfo.getServerPort()));

            TProcessor processor = new Tribbler.Processor<Tribbler.Iface>(tribblerServerHandler);
            TServerTransport serverTransport = new TServerSocket(_tribblerServerPort);
            TTransportFactory transportFactory = new TTransportFactory();
            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

            TSimpleServer simpleServer = new TSimpleServer(new TSimpleServer.Args(serverTransport).processor(processor).inputProtocolFactory(protocolFactory).transportFactory(transportFactory));
            simpleServer.serve();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


}
