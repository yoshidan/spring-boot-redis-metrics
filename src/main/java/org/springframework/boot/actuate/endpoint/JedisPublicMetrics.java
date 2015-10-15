package org.springframework.boot.actuate.endpoint;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.ReflectionUtils;
import redis.clients.util.ObjectPoolAccessor;
import redis.clients.util.Pool;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
            if( pool != null ) {
                GenericObjectPool internalPool = ObjectPoolAccessor.getInternalPool(pool);
                double numActive = pool.getNumActive();
                double maxTotal = internalPool.getMaxTotal();
                double numIdle = pool.getNumIdle();
                String prefix = String.format("redis.%s", p.key);

                BigDecimal usage = BigDecimal.valueOf(numActive / maxTotal).setScale(3, BigDecimal.ROUND_HALF_UP);

                perCon.add(new Metric<>(prefix + ".active", numActive));
                perCon.add(new Metric<>(prefix + ".idle", numIdle));
                perCon.add(new Metric<>(prefix + ".usage",usage.doubleValue()));
            }
            return perCon;
        }).filter(p->!p.isEmpty())
                .flatMap(perCon -> perCon.stream()).collect(Collectors.toList());
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