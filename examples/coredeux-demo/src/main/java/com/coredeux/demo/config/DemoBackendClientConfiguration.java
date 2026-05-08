package com.coredeux.demo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class DemoBackendClientConfiguration {

    @Bean(destroyMethod = "close")
    @Lazy
    public MongoClient demoMongoClient(
            @Value("${coredeux.demo.mongodb.host:localhost}") String host,
            @Value("${coredeux.demo.mongodb.port:27017}") int port) {
        return MongoClients.create("mongodb://" + host + ":" + port);
    }

    @Bean
    @Lazy
    public MongoTemplate demoMongoTemplate(MongoClient demoMongoClient,
            @Value("${coredeux.demo.mongodb.database:coredeux_oss}") String database) {
        return new MongoTemplate(demoMongoClient, database);
    }

    @Bean(destroyMethod = "close")
    @Lazy
    public RestHighLevelClient demoElasticsearchClient(
            @Value("${coredeux.demo.elasticsearch.host:localhost}") String host,
            @Value("${coredeux.demo.elasticsearch.port:9200}") int port,
            @Value("${coredeux.demo.elasticsearch.scheme:http}") String scheme) {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
    }

    @Bean
    @Lazy
    public ElasticsearchOperations demoElasticsearchOperations(RestHighLevelClient demoElasticsearchClient) {
        return new ElasticsearchRestTemplate(demoElasticsearchClient);
    }

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedisClient demoRedisClient(
            @Value("${coredeux.demo.redis.host:localhost}") String host,
            @Value("${coredeux.demo.redis.port:6379}") int port) {
        return RedisClient.create(RedisURI.Builder.redis(host, port).build());
    }

    @Bean(destroyMethod = "close")
    @Lazy
    public StatefulRedisConnection<String, String> demoRedisConnection(RedisClient demoRedisClient) {
        return demoRedisClient.connect(StringCodec.UTF8);
    }
}
