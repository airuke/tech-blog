package com.airuike.blog.backend.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Service
public class PublicCache {
    private final StringRedisTemplate redis;private final JdbcTemplate jdbc;private final ObjectMapper json;private final boolean enabled;
    private final LongAdder hits=new LongAdder(),misses=new LongAdder();private volatile long retryAfter;
    public PublicCache(StringRedisTemplate redis,JdbcTemplate jdbc,ObjectMapper json,@Value("${blog.cache.enabled:true}") boolean enabled) {this.redis=redis;this.jdbc=jdbc;this.json=json;this.enabled=enabled;}
    public <T> T read(String query,TypeReference<T> type,Supplier<T> source) {
        long revision=jdbc.queryForObject("SELECT revision FROM blog_state WHERE id=1",Long.class);
        String key="blog:public:v"+revision+":"+hash(query);
        boolean usable=enabled&&System.currentTimeMillis()>=retryAfter;
        if(usable) try {String data=redis.opsForValue().get(key);if(data!=null) {T value=json.readValue(data,type);hits.increment();return value;}}
        catch(Exception e) {retryAfter=System.currentTimeMillis()+15000;usable=false;}
        misses.increment();T value=source.get();
        if(usable) try {redis.opsForValue().set(key,json.writeValueAsString(value),Duration.ofMinutes(5));}catch(Exception e) {retryAfter=System.currentTimeMillis()+15000;}
        return value;
    }
    public void changed() {
        // The database revision and business write commit together; old keys cannot become current again.
        jdbc.update("UPDATE blog_state SET revision=revision+1 WHERE id=1");
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {evict();}
        });else evict();
    }
    private void evict() {
        if(!enabled || System.currentTimeMillis()<retryAfter) return;
        try(var cursor=redis.scan(ScanOptions.scanOptions().match("blog:public:*").count(100).build())) {
            java.util.List<String> batch=new java.util.ArrayList<>();
            while(cursor.hasNext()) {batch.add(cursor.next());if(batch.size()==100){redis.delete(batch);batch.clear();}}
            if(!batch.isEmpty()) redis.delete(batch);
        } catch(Exception e) {retryAfter=System.currentTimeMillis()+15000;}
    }
    public Map<String,Object> status() {
        boolean available=false;
        if(enabled&&System.currentTimeMillis()>=retryAfter) try {available="PONG".equals(redis.execute((org.springframework.data.redis.core.RedisCallback<String>)c->c.ping()));}
        catch(Exception e) {retryAfter=System.currentTimeMillis()+15000;}
        return Map.of("enabled",enabled,"available",available,"hits",hits.sum(),"misses",misses.sum());
    }
    private String hash(String query) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(query.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
}
