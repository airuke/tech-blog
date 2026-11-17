package com.airuike.blog.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("category")
public class CategoryEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { id=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
}
