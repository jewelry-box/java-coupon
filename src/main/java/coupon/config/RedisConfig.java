package coupon.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

    private final String redissonHost;
    private final String redissonPort;
    private final String lettuceHost;
    private final int lettucePort;

    public RedisConfig(@Value("${redisson.host}") String redissonHost,
                       @Value("${redisson.port}") String redissonPort,
                       @Value("${spring.data.redis.host}") String lettuceHost,
                       @Value("${spring.data.redis.port}") int lettucePort) {
        this.redissonHost = redissonHost;
        this.redissonPort = redissonPort;
        this.lettuceHost = lettuceHost;
        this.lettucePort = lettucePort;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(lettuceHost, lettucePort);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redissonHost + ":" + redissonPort);
        return Redisson.create(config);
    }
}
