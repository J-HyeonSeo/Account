package com.example.jhsfully.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Configuration
public class LocalRedisConfig {

    @Value("${spring.redis.port}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis(){
        redisServer = new RedisServer(redisPort);

        try{
            redisServer.start();
        }catch (Exception e){
            log.error("Redis Server가 이미 가동 중에 있습니다.");
        }

    }

    @PreDestroy
    public void stopRedis(){
        if(redisServer != null){
            redisServer.stop();
        }
    }

}
