package src;


import include.KeyValueStore.*;
import include.Tribbler.SubscriptionResponse;
import include.Tribbler.TribbleResponse;
import include.Tribbler.TribbleStatus;
import include.Tribbler.Tribbler;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;

public class TribblerServerHandler implements Tribbler.Iface, KeyValueStore.Iface
{

    //TRIBBLER SERVER: Implements the application logic, each client request results in
    //an RPC call by the Tribbler Server to the key-value storage back-end server and
    //retrieve the required data from the back-end from the data store.

    private String _storageServer;
    private int _storageServerPort;
    private final String UserPrefix = "TribbleUser";
    private final String SubscriptionPrefix = "TribbleSubscription";
    private final String TribblePrefix = "UserTribble";

    private final int MAXRetrievalLimit = 100;


    public TribblerServerHandler(String storageServer, int storageServerPort)
    {
        _storageServer = storageServer;
        _storageServerPort = storageServerPort;
    }

    //Note that there is no interface to delete users. A userid can never be re-used.
    @Override
    public TribbleStatus CreateUser(String userid) throws TException {

        //key = UserPrefix+userid
        //Value = TribbleUser.class in json format

        return null;
    }


    //server should not allow a user to subscribe to a nonexistent user ID, nor allow a nonexistent user ID to subscribe to anyone.
    @Override
    public TribbleStatus AddSubscription(String userid, String subscribeto) throws TException {
        return null;
    }


    @Override
    public TribbleStatus RemoveSubscription(String userid, String subscribeto) throws TException {
        return null;
    }


    //Client interface to posting a tribble provides only the contents, server is responsible for timestampping the entry and creating a Tribble struct
    //Non-existing user IDs should not be allowed to post or read tribbles
    @Override
    public TribbleStatus PostTribble(String userid, String tribbleContents) throws TException {
        return null;
    }

    //Basic function, retrieves a list of most recent tribbles by a particular user,
    //retrieved in reverse chronological order with most recent first, up to 100 max
    @Override
    public TribbleResponse GetTribbles(String userid) throws TException {
        return null;
    }


    //Retrieve a max of 100 most recent tribbles in reverse chronological order
    //from all the users a particular user has subscribed to
    @Override
    public TribbleResponse GetTribblesBySubscription(String userid) throws TException {
        return null;
    }

    //function lists the users to whom the target user subscribes
    //make sure you not to report subscriptions for nonexistent userID.
    @Override
    public SubscriptionResponse GetSubscriptions(String userid) throws TException {
        return null;
    }





    //Back-end key-value storage server calls.
    //KVStoreStatus is an enumerated data type that denotes the return status of the RPC.

    //GetResponse --> {KVStoreStatus , Values}
    //GetListResponse --> {KVStoreStatus, Values[]}

    //Each RPC request should validate the userid by checking return value of RPC request
    //made to the storage server and return appropriate message to the client.

    //A good implementation will not store a gigantic list of all tribbles for a user in a single key-value entry
    //system should be able to handle users with 1000s of tribbles without excessive bloat or slowdown

    //SUGGESTION: Store a list of tribbles IDs in some way, store each tribble as a separate key-value store item stored on same partition
    // as the user ID.

    // key and value are both of data type strings, need to serialize and de-serialize the stored value
    // using JSON

    @Override
    public GetResponse Get(String key) throws TException {
        TSocket socket = new TSocket(_storageServer,_storageServerPort);
        TTransport transport = socket;

        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        transport.open();

        GetResponse response = client.Get(key);

        transport.close();

        return response;
    }

    @Override
    public GetListResponse GetList(String key) throws TException {
        TSocket socket = new TSocket(_storageServer,_storageServerPort);
        TTransport transport = socket;

        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        transport.open();

        GetListResponse response = client.GetList(key);

        transport.close();

        return response;
    }

    @Override
    public KVStoreStatus Put(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServer,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);

        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        KVStoreStatus storeStatus;

        transport.open();

        storeStatus = client.Put(key,value,clientid);

        transport.close();

        return storeStatus;
    }

    @Override
    public KVStoreStatus AddToList(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServer,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);

        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        KVStoreStatus storeStatus ;

        transport.open();

        storeStatus = client.AddToList(key,value,clientid);

        transport.close();

        return storeStatus;
    }

    @Override
    public KVStoreStatus RemoveFromList(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServer,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);
        KVStoreStatus storeStatus;

        transport.open();

        storeStatus = client.RemoveFromList(key,value,clientid);

        transport.close();

        return  storeStatus;
    }


    //Remove this, instead Get a proxy object (client) and perform all the operations using that client
    @Override
    public ClockResponse Clock(long atLeast) throws TException {
        return null;
    }


}