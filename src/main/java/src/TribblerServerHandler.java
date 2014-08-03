package src;


import com.google.gson.Gson;
import include.KeyValueStore.*;
import include.Tribbler.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.*;

public class TribblerServerHandler implements Tribbler.Iface
{

    //TRIBBLER SERVER: Implements the application logic, each client request results in
    //an RPC call by the Tribbler Server to the key-value storage back-end server and
    //retrieve the required data from the back-end from the data store.

    private static String _storageServerName;
    private static String _storageServerAddress;
    private static int _storageServerPort;
    private static long _atLeast;
    private final String UserPrefix = "TribbleUser";
    private final String SubscriptionPrefix = "TribbleSubscription";
    private final String TribblePrefix = "UserTribble";

    private final int MAXRetrievalLimit = 100;


    public TribblerServerHandler(String storageServerName,  String storageServer, int storageServerPort)
    {
        _storageServerName = storageServerName;
        _storageServerAddress = storageServer;
        _storageServerPort = storageServerPort;
        _atLeast = 0;
    }

    //Note that there is no interface to delete users. A userid can never be re-used.
    @Override
    public TribbleStatus CreateUser(String userid) throws TException {

        //key = UserPrefix+userid
        //Value = TribbleUser.class in json format

        try
        {
            Gson gson = new Gson();
            GetResponse response = Get(UserPrefix+userid);

            if(response.status==KVStoreStatus.EPUTFAILED)
            {
                return TribbleStatus.STORE_FAILED;
            }

            if(response.status==KVStoreStatus.EITEMNOTFOUND)
            {
                TribbleUser newUser = new TribbleUser(userid,new Date());

                String newUserJson = gson.toJson(newUser);

                Put(UserPrefix+userid,newUserJson,_storageServerName);

                return TribbleStatus.OK;
            }

            TribbleUser tribbleUser =  gson.fromJson(response.getValue(),TribbleUser.class);

            if(tribbleUser.userId.equals(userid))
            {
                return TribbleStatus.EEXISTS;
            }

            throw new Exception();
        }
        catch (Exception ex)
        {
            return TribbleStatus.STORE_FAILED;
        }
    }


    //server should not allow a user to subscribe to a nonexistent user ID, nor allow a nonexistent user ID to subscribe to anyone.
    @Override
    public TribbleStatus AddSubscription(String userid, String subscribeto) throws TException {
        try{

            GetResponse userResponse = Get(UserPrefix+userid);

            if(userResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return  TribbleStatus.INVALID_USER;
            }

            GetResponse subscriberResponse = Get(UserPrefix+subscribeto);

            if(subscriberResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return  TribbleStatus.INVALID_SUBSCRIBETO;
            }

            GetListResponse listResponse = GetList(SubscriptionPrefix+userid);

            if(listResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                if(AddToList(SubscriptionPrefix+userid,UserPrefix+subscribeto,_storageServerName)!=KVStoreStatus.OK)
                {
                    return  TribbleStatus.STORE_FAILED;
                }

                return TribbleStatus.OK;
            }

            for(String value:listResponse.getValues())
            {
                if(value.equals(UserPrefix + userid))
                {
                    return TribbleStatus.EEXISTS;
                }
            }

            if(AddToList(SubscriptionPrefix+userid,UserPrefix+subscribeto,_storageServerName)!=KVStoreStatus.OK)
            {
                return  TribbleStatus.STORE_FAILED;
            }

            return TribbleStatus.OK;
        }
        catch (Exception ex)
        {
            return TribbleStatus.STORE_FAILED;
        }
    }


    @Override
    public TribbleStatus RemoveSubscription(String userid, String subscribeto) throws TException {
        try{
            // Gson gson = new Gson();
            GetResponse userResponse = Get(UserPrefix+userid);

            if(userResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return  TribbleStatus.INVALID_USER;
            }

            GetResponse subscriberResponse = Get(UserPrefix+subscribeto);

            if(subscriberResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return  TribbleStatus.INVALID_SUBSCRIBETO;
            }

            GetListResponse listResponse = GetList(SubscriptionPrefix+userid);

            if(listResponse.status!=KVStoreStatus.OK)
            {
                return TribbleStatus.INVALID_SUBSCRIBETO;
            }

            if(RemoveFromList(SubscriptionPrefix+userid,UserPrefix+subscribeto,_storageServerName)!=KVStoreStatus.OK)
            {
                return TribbleStatus.STORE_FAILED;
            }

            return TribbleStatus.OK;
        }
        catch (Exception ex)
        {
            return TribbleStatus.STORE_FAILED;
        }
    }


    //Client interface to posting a tribble provides only the contents, server is responsible for timestampping the entry and creating a Tribble struct
    //Non-existing user IDs should not be allowed to post or read tribbles
    @Override
    public TribbleStatus PostTribble(String userid, String tribbleContents) throws TException {
        try
        {
            GetResponse userResponse = Get(UserPrefix+userid);
            if(userResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return TribbleStatus.INVALID_USER;
            }
            Gson gson = new Gson();

            TribbleUser tribbleUser = gson.fromJson(userResponse.getValue(),TribbleUser.class);

            //convert date to long
            long currentTime = new Date().getTime();
            tribbleUser.tribbleDateList.addLast(currentTime);

            ClockResponse clockResponse = Clock(_atLeast);

            if(!clockResponse.isSuccess())
            {
               //updating logical clock has failed!!!
               //Server must be down!!! Throw exception
                throw new Exception();
            }

            _atLeast = clockResponse.getReturnTime();


            Tribble newTribble = new Tribble(userid,_atLeast,tribbleContents);

            String key = TribblePrefix+userid+currentTime;

            KVStoreStatus storeStatus;

            storeStatus = Put(key,gson.toJson(newTribble),_storageServerName);

            if(storeStatus!=KVStoreStatus.OK)
            {
                return TribbleStatus.STORE_FAILED;
            }

            storeStatus = Put(UserPrefix+userid,gson.toJson(tribbleUser),_storageServerName);

            if(storeStatus!=KVStoreStatus.OK)
            {
                //Cannot update USER details after a Tribble has saved against
                //the user's name
                //MAJOR FAILURE

                //Tribble previously saved is now lost!!!

                throw new Exception();
            }

            return TribbleStatus.OK;
        }
        catch (Exception ex)
        {
            return TribbleStatus.STORE_FAILED;
        }
    }

    //Basic function, retrieves a list of most recent tribbles by a particular user,
    //retrieved in reverse chronological order with most recent first, up to 100 max
    //sorts them using the logical clock timings saved in the tribbles
    @Override
    public TribbleResponse GetTribbles(String userid) throws TException {

        TribbleResponse tribbleResponse;

        try
        {
            GetResponse userResponse = Get(UserPrefix+userid);
            if(userResponse.status==KVStoreStatus.EITEMNOTFOUND)
            {
                return new TribbleResponse(null,TribbleStatus.INVALID_USER);
            }

            Gson gson = new Gson();
            TribbleUser tribbleUser = gson.fromJson(userResponse.getValue(),TribbleUser.class);

            int numberOfTribbles = tribbleUser.tribbleDateList.size();

            tribbleResponse = new TribbleResponse();

            Object tribbleArray[] =  tribbleUser.tribbleDateList.toArray();

            List<Tribble> tribbleList = new ArrayList<Tribble>();

            for(int i=0;i<numberOfTribbles&&i<MAXRetrievalLimit;i++)
            {
                GetResponse getResponse = Get(TribblePrefix+userid+tribbleArray[i]);

                if(getResponse.status!= KVStoreStatus.OK )
                {
                    tribbleResponse.status = TribbleStatus.STORE_FAILED;
                    return  tribbleResponse;
                }

                Tribble tribble = gson.fromJson(getResponse.getValue(),Tribble.class);
                tribbleList.add(tribble);
            }

            Comparator<Tribble> tribbleComparator = new Comparator<Tribble>() {
                @Override
                public int compare(Tribble o1, Tribble o2) {
                    Long longO1 = o1.getPosted();
                    Long longO2 = o2.getPosted();
                    return longO1.compareTo(longO2);
                }
            };

            Collections.sort(tribbleList,tribbleComparator);
            tribbleResponse.setTribbles(tribbleList);

        }
        catch (Exception ex)
        {
            tribbleResponse = new TribbleResponse(null, TribbleStatus.STORE_FAILED);
            return  tribbleResponse;
        }

        tribbleResponse.setStatus(TribbleStatus.OK);
        return tribbleResponse;

    }


    //Retrieve a max of 100 most recent tribbles in reverse chronological order
    //from all the users a particular user has subscribed to,
    //sorts them using the logical clock timings saved in the tribbles
    @Override
    public TribbleResponse GetTribblesBySubscription(String userid) throws TException {
        GetResponse userResponse = Get(UserPrefix+userid);
        if(userResponse.status==KVStoreStatus.EITEMNOTFOUND)
        {
            return new TribbleResponse(null,TribbleStatus.INVALID_USER);
        }

        Gson gson = new Gson();

        GetListResponse subscriptionRetrievalResponse = GetList(SubscriptionPrefix+userid);

        if(subscriptionRetrievalResponse.status!=KVStoreStatus.OK)
        {
            //NOT_IMPLEMENTED means user has not subscribed to any other user
            return new TribbleResponse(null,TribbleStatus.NOT_IMPLEMENTED);
        }

        List<TribbleUser> tribbleUserList = new LinkedList<TribbleUser>();
        TribbleResponse tribbleResponse = new TribbleResponse();
        List<Tribble> tribbleList = new ArrayList<Tribble>();


        try{

            for(String key:subscriptionRetrievalResponse.getValues())
            {
                GetResponse getResponse = Get(key);
                if(getResponse.status==KVStoreStatus.EITEMNOTFOUND)
                {
                    return new TribbleResponse(null,TribbleStatus.INVALID_SUBSCRIBETO);
                }

                tribbleUserList.add(gson.fromJson(getResponse.getValue(),TribbleUser.class));
            }

            int i =0;

            for(TribbleUser user:tribbleUserList)
            {
                if(i>99)
                {
                    //tribbleResponse.status = TribbleStatus.OK;
                    //return tribbleResponse;
                    break;
                }

                for(Long date:user.tribbleDateList)
                {
                    GetResponse getResponse = Get(TribblePrefix+user.userId+date);
                    if(getResponse.status!=KVStoreStatus.OK)
                    {
                        tribbleResponse.status = TribbleStatus.STORE_FAILED;
                        return tribbleResponse;
                    }

                    tribbleList.add(gson.fromJson(getResponse.getValue(),Tribble.class));
                    i++;
                }
            }

            Comparator<Tribble> tribbleComparator = new Comparator<Tribble>() {
                @Override
                public int compare(Tribble o1, Tribble o2) {
                    Long longO1 = o1.getPosted();
                    Long longO2 = o2.getPosted();
                    return longO1.compareTo(longO2);
                }
            };

            Collections.sort(tribbleList,tribbleComparator);
            tribbleResponse.setTribbles(tribbleList);

        }catch (Exception ex)
        {
            return new TribbleResponse(null,TribbleStatus.STORE_FAILED);
        }

        tribbleResponse.status = TribbleStatus.OK;
        return tribbleResponse;
    }

    //function lists the users to whom the target user subscribes
    //make sure you not to report subscriptions for nonexistent userID.
    @Override
    public SubscriptionResponse GetSubscriptions(String userid) throws TException {
        Gson gson = new Gson();

        GetResponse getResponse = Get(UserPrefix+userid);

        if(getResponse.status==KVStoreStatus.EITEMNOTFOUND)
        {
            return new SubscriptionResponse(null,TribbleStatus.INVALID_USER);
        }

        TribbleUser requestUser = gson.fromJson(getResponse.getValue(),TribbleUser.class);

        GetListResponse listResponse = GetList(SubscriptionPrefix+userid);

        if(listResponse.status!=KVStoreStatus.OK)
        {
            //invalid subscribe to means no subscription available for current user
            return new SubscriptionResponse(null,TribbleStatus.INVALID_SUBSCRIBETO);
        }

        SubscriptionResponse response = new SubscriptionResponse(listResponse.getValues(),TribbleStatus.OK);
        return response;

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


    public static GetResponse Get(String key) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;

        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        transport.open();

        GetResponse response = client.Get(key);

        transport.close();

        return response;
    }


    public static GetListResponse GetList(String key) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;

        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        transport.open();

        GetListResponse response = client.GetList(key);

        transport.close();

        return response;
    }


    public static KVStoreStatus Put(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);

        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        KVStoreStatus storeStatus;

        transport.open();

        storeStatus = client.Put(key,value,clientid);

        transport.close();

        return storeStatus;
    }


    public static KVStoreStatus AddToList(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);

        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        KVStoreStatus storeStatus ;

        transport.open();

        storeStatus = client.AddToList(key,value,clientid);

        transport.close();

        return storeStatus;
    }


    public static KVStoreStatus RemoveFromList(String key, String value, String clientid) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);
        KVStoreStatus storeStatus;

        transport.open();

        storeStatus = client.RemoveFromList(key,value,clientid);

        transport.close();

        return  storeStatus;
    }


    public static ClockResponse Clock(long atLeast) throws TException {
        TSocket socket = new TSocket(_storageServerAddress,_storageServerPort);
        TTransport transport = socket;
        TProtocol protocol = new TBinaryProtocol(transport);
        KeyValueStore.Client client = new KeyValueStore.Client(protocol);

        ClockResponse clockResponse;

        clockResponse =  client.Clock(atLeast);

        transport.open();

        return clockResponse;
    }


}