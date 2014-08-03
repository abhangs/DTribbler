package src;


import include.Tribbler.SubscriptionResponse;
import include.Tribbler.TribbleResponse;
import include.Tribbler.TribbleStatus;
import include.Tribbler.Tribbler;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class TribblerClient {

    public static void printSubscriptions(SubscriptionResponse subscriptionResponse, String user)
    {
        if(subscriptionResponse.status!= TribbleStatus.OK)
        {
            System.out.println("\nGetSubscriptions for user: " + user + " failed" + "\n" +subscriptionResponse.status.toString() );
            return;
        }


        System.out.println("\nuser: "+user +" has " +  subscriptionResponse.getSubscriptionsSize() + " subscriptions");

        for(String userid:subscriptionResponse.getSubscriptions())
        {
            System.out.println("\nuser: "+ user+ " subscribed to " + userid);
        }


    }


    public static void main(String[] args)
    {
        //get the server and the port from args

        //passing them manually for time being

        //following as specified in the TribblerServer
        String tribblerServer = "localhost";
        int port = 8585;

        try {

            TSocket socket = new TSocket(tribblerServer,port);
            TTransport transport = socket;
            TProtocol protocol = new TBinaryProtocol(transport);

            Tribbler.Client client = new Tribbler.Client(protocol);
            TribbleStatus status;

            transport.open();

            status = client.CreateUser("alice");

            if(status!=TribbleStatus.OK)
            {
                System.out.println("\nFailed to create user alice");
            }


            status = client.CreateUser("bob");

            if(status!=TribbleStatus.OK)
            {
                System.out.println("\nFailed to create user bob");
            }

            status = client.CreateUser("cindy");

            if(status!=TribbleStatus.OK)
            {
                System.out.println("\nFailed to create user cindy");
            }

            TribbleStatus addStatus;

            addStatus = client.AddSubscription("alice","bob");
            if(addStatus!=TribbleStatus.OK)
            {
                System.out.println("\nalice failed to subscribe to bob");
                System.out.println("\n" + addStatus.toString());
            }

            addStatus = client.AddSubscription("alice","cindy");
            if(addStatus!=TribbleStatus.OK)
            {
                System.out.println("\nalice failed to subscribe to cindy");
                System.out.println("\n" + addStatus.toString());
            }

            addStatus = client.AddSubscription("bob","cindy");
            if(addStatus!=TribbleStatus.OK)
            {
                System.out.println("\nbob failed to subscribe to cindy");
                System.out.println("\n" + addStatus.toString());
            }

            addStatus = client.AddSubscription("cindy","alice");
            if(addStatus!=TribbleStatus.OK)
            {
                System.out.println("\ncindy failed to subscribe to alice");
                System.out.println("\n" + addStatus.toString());
            }


            SubscriptionResponse subscriptionResponseAlice = client.GetSubscriptions("alice");
            SubscriptionResponse subscriptionResponseBob = client.GetSubscriptions("bob");
            SubscriptionResponse subscriptionResponseCindy = client.GetSubscriptions("cindy");

            printSubscriptions(subscriptionResponseAlice,"alice");
            printSubscriptions(subscriptionResponseBob,"bob");
            printSubscriptions(subscriptionResponseCindy,"cindy");

            TribbleStatus statusRemove;

            statusRemove = client.RemoveSubscription("alice","cindy");

            if(statusRemove!=TribbleStatus.OK)
            {
                System.out.println("\nalice failed to remove subscription cindy");
                System.out.println("\n"+statusRemove.toString());
            }

            subscriptionResponseAlice = client.GetSubscriptions("alice");
            printSubscriptions(subscriptionResponseAlice,"alice");

            TribbleResponse tribbleResponseAlice;

            TribbleStatus tribbleStatusPost;

            tribbleStatusPost = client.PostTribble("alice","What a beautiful day!!");

            if(tribbleStatusPost!=TribbleStatus.OK)
            {
                System.out.println("\n alice could not post");
                System.out.println("\n"+tribbleStatusPost.toString());
            }

            tribbleResponseAlice = client.GetTribbles("alice");

            if(tribbleResponseAlice.status!=TribbleStatus.OK)
            {
                System.out.println("\nalice failed to get tribbles");
                System.out.println("\n"+tribbleResponseAlice.status);

            }

            TribbleResponse tribbleResponseBob;

            tribbleResponseBob = client.GetTribblesBySubscription("bob");

            if(tribbleResponseBob.status!=TribbleStatus.OK)
            {
                System.out.println("\nbob failed to get tribbles by subscription");
                System.out.println("\n"+tribbleResponseBob.status);
            }

            transport.close();
        }
        catch (Exception ex)
        {
            System.out.println("\n"+ex.getMessage());
        }

    }



}
