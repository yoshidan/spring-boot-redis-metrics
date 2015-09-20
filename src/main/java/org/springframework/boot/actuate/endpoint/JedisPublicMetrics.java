package org.springframework.boot.actuate.endpoint;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import redis.clients.jedis.JedisShardInfo;
import redis.clients.util.ObjectPoolAccessor;
import redis.clients.util.Pool;
import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by yoshidan on 2015/09/20.
 */
public class JedisPublicMetrics implements PublicMetrics {

    private Map<String,JedisConnectionFactory> connectionFactories;

    private List<Pair<String,Supplier<Pool>>> pools;

    /**
     * Constructor
     *
     * @param connectionFactories to check
     */
    public JedisPublicMetrics(Map<String,JedisConnectionFactory> connectionFactories){
        this.connectionFactories = connectionFactories;
    }

    @PostConstruct
    public void initialize() {
      this.pools = connectionFactories.entrySet().stream().map(keyValue -> {
          Field pool = ReflectionUtils.findField(JedisConnectionFactory.class, "pool");
          ReflectionUtils.makeAccessible(pool);
          Supplier<Pool> supplier = () -> (Pool) ReflectionUtils.getField(pool, keyValue.getValue());
          return new Pair<>(keyValue.getKey(), supplier);
      }).collect(Collectors.toList());
    }

    @Override
    public Collection<Metric<?>> metrics() {

        return pools.stream().map(p -> {
            Collection<Metric<?>> perCon = new LinkedHashSet<>();
            Pool pool = p.value.get();

            GenericObjectPool internalPool = ObjectPoolAccessor.getInternalPool(pool);
            int numActive = pool.getNumActive();
            int maxTotal = internalPool.getMaxTotal();
            int numIdle = pool.getNumIdle();
            String prefix = String.format("redis.%s", p.key);

            perCon.add(new Metric<>(prefix + ".active", numActive));
            perCon.add(new Metric<>(prefix + ".idle", numIdle));
            perCon.add( new Metric<>(prefix + ".usage", (float) (numActive / maxTotal)));
            return perCon;
        }).flatMap(perCon -> perCon.stream()).collect(Collectors.toList());
    }

    private static class Pair<K,V> {
        public final K key;
        public final V value;
        public Pair(K key , V value){
            this.key = key;
            this.value = value;
        }
    }
}