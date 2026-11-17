package com.airuike.blog.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("file_resource")
public class FileResource {
    @TableId(type=IdType.INPUT)
    private String id;
    private String name;
    private String objectKey;
    private String contentType;
    private Long size;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    public String getId() { return id; }
    public void setId(String value) { id=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String value) { objectKey=value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { contentType=value; }
    public Long getSize() { return size; }
    public void setSize(Long value) { size=value; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean value) { isPublic=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
}
