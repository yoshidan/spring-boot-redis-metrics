# spring-boot-redis-actuator

spring-boot-actuatorを利用してRedisのコネクションプールの情報を取得します。

## how to use

JedisPublicMetricsをSpringのComponentとしてください。

```
@Bean
public JedisConnectionFactory someConnectionFactory() {
     JedisConnectionFactory factory = new JedisConnectionFactory();
     // todo connection setting
     return factory;
}

@Bean
@Autowired
public JedisPublicMetrics jedisPublicMetrics(Map<String,JedisConnectionFactory> factories) {
    return new JedisPublicMetrics(factories);
}
```

上記の状態でspring-boot-actuatorのmetricsを参照すると結果が取得できます。

curl localhost:8080/metrics/redis.*

```
{"redis.someConnectionFactory.active":0,
 "redis.someConnectionFactory.idle":0,
 "redis.someConnectionFactory.usage":0.0}
```

metricsの各種値野意味は以下の通りです。

| プロパティ | 数値 |
|-------|--------|
| redis.${beanName}.active | 現在Activeになっているコネクション数 |
| redis.${beanName}.idle | 現在Idle状態のコネクション数 |
| redis.${beanName}.usage | コネクション数の利用率 |

## dependency

```
<dependency>
    <groupId>spring.support</groupId>
    <artifactId>spring-boot-redis-metrics</artifactId>
    <version>1.3.0.M5.1</version>
</dependency>
```

## repository

```
<repository>
    <id>nysq</id>
    <url>http://nysd.github.io/archivar</url>
</repository>
```




