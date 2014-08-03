package src;


import include.KVStoreSimulator.KVStoreSimulator;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;

public class KVStoreSimulatorServer{

    public static void main(String args[])
    {
        String simulatorHost = "localhost";
        int simulatorPort = 7070;

        try
        {
            KVStoreSimulatorHandler storeSimulatorHandler = new KVStoreSimulatorHandler();

            TProcessor processor = new KVStoreSimulator.Processor<KVStoreSimulator.Iface>(storeSimulatorHandler);
            TServerTransport serverTransport = new TServerSocket(simulatorPort);
            TTransportFactory transportFactory = new TTransportFactory();
            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

            TSimpleServer simpleServer = new TSimpleServer(new TSimpleServer.Args(serverTransport).processor(processor).inputProtocolFactory(protocolFactory).transportFactory(transportFactory));
            simpleServer.serve();
        }
        catch (TTransportException e) {
            e.printStackTrace();
        }


    }

}
