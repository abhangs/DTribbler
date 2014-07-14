package src;

import include.KeyValueStore.GetListResponse;
import include.KeyValueStore.GetResponse;
import include.KeyValueStore.KVStoreStatus;
import include.KeyValueStore.KeyValueStore;
import org.apache.thrift.TException;

public class KVStoreServerHandler implements KeyValueStore.Iface
{

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
}
