package com.airuike.blog.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("article")
public class ArticleEntity {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private String status;
    private String visibility;
    private Boolean isTop;
    private Boolean isFavorite;
    private Long viewCount;
    private Integer wordCount;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id=value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title=value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary=value; }
    public String getContent() { return content; }
    public void setContent(String value) { content=value; }
    public String getCover() { return cover; }
    public void setCover(String value) { cover=value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status=value; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String value) { visibility=value; }
    public Boolean getIsTop() { return isTop; }
    public void setIsTop(Boolean value) { isTop=value; }
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean value) { isFavorite=value; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long value) { viewCount=value; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer value) { wordCount=value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { version=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime value) { publishedAt=value; }
}
