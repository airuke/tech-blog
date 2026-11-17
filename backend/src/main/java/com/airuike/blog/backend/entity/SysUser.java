package com.airuike.blog.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_user")
public class SysUser {
    @TableId(type=IdType.INPUT)
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private Long tokenVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id=value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username=value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash=value; }
    public String getNickname() { return nickname; }
    public void setNickname(String value) { nickname=value; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer value) { status=value; }
    public Long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Long value) { tokenVersion=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
}
