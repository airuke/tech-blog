package com.airuike.blog.backend.service;

import com.airuike.blog.backend.common.BlogException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bounded per-address window for this single-backend personal blog. */
@Component
public class CommentRateLimiter {
    private record Window(int count,long expiresAt) {}
    private final Map<String,Window> windows=new HashMap<>();
    public synchronized void acquire(String address) {
        long now=System.currentTimeMillis();
        windows.values().removeIf(window->window.expiresAt()<=now);
        Window previous=windows.get(address);
        if((previous!=null && previous.count()>=5) || (previous==null && windows.size()>=10000))
            throw new BlogException(429,"提交过于频繁，请一分钟后再试");
        windows.put(address,previous==null?new Window(1,now+60000):new Window(previous.count()+1,previous.expiresAt()));
    }
}
