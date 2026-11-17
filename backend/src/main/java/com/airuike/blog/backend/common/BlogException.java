package com.airuike.blog.backend.common;
public class BlogException extends RuntimeException {
    private final int status;
    public BlogException(int status,String message) { super(message);this.status=status; }
    public int status() { return status; }
}
