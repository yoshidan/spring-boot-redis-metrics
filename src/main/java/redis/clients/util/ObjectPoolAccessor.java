package redis.clients.util;


import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Created by yoshidan on 2015/09/20.
 */
public class ObjectPoolAccessor {

    public static GenericObjectPool getInternalPool (redis.clients.util.Pool pool){
        return pool.internalPool;
    }

}
