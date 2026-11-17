package com.airuike.blog.backend.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
public final class BlogDtos {
    private BlogDtos() {}
    public record Login(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{3,32}",message="用户名使用 3–32 位字母、数字或下划线") String username,
            @NotBlank @Size(max=72,message="密码过长") String password) {}
    public record Setup(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{3,32}",message="用户名使用 3–32 位字母、数字或下划线") String username,
            @NotBlank @Size(min=10,max=72,message="密码使用 10–72 个字符") String password,
            @Size(max=40,message="昵称最多 40 个字符") String nickname) {}
    public record Password(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=72,message="新密码使用 10–72 个字符") String newPassword) {}
    public record User(String id,String username,String nickname) {}
    public record Session(String accessToken,String tokenType,long expiresIn,User user) {}
    public record AuthStatus(boolean setupNeeded,boolean setupAllowed) {}
    public record ArticleInput(@NotBlank @Size(max=160,message="标题最多 160 个字符") String title,
            @Size(max=400,message="摘要最多 400 个字符") String summary,
            @NotNull @Size(max=1000000,message="正文最多 100 万个字符") String content,
            @Size(max=255) String cover,@Positive Long categoryId,
            @NotNull @Size(max=20,message="每篇文章最多 20 个标签") List<@NotNull @Positive Long> tagIds,
            @NotNull @Pattern(regexp="DRAFT|PUBLISHED") String status,
            @NotNull @Pattern(regexp="PUBLIC|PRIVATE") String visibility,
            boolean isTop,boolean isFavorite,@Positive Long version) {}
    public record Publication(@NotNull @Pattern(regexp="DRAFT|PUBLISHED") String status,@NotNull @Positive Long version) {}
    public record Favorite(boolean isFavorite,@NotNull @Positive Long version) {}
    public record TaxonomyInput(@NotBlank @Size(max=40,message="名称最多 40 个字符") String name,
            @Size(max=240,message="说明最多 240 个字符") String description,@Min(0) @Max(9999) int sortOrder) {}
    public record Taxonomy(String id,String name,String description,int sortOrder,long articleCount) {}
    public record Article(String id,String title,String summary,String content,String cover,Taxonomy category,
            List<Taxonomy> tags,String status,String visibility,boolean isTop,boolean isFavorite,long viewCount,long version,
            LocalDateTime createdAt,LocalDateTime updatedAt,LocalDateTime publishedAt,int wordCount,int readingMinutes) {}
    public record Page<T>(List<T> items,long total,int page,int pageSize) {}
    public record Archive(int year,int month,List<Article> articles) {}
    public record Site(String title,String description,String aboutMarkdown) {}
    public record SiteInput(@NotBlank @Size(max=50) String title,@NotBlank @Size(max=180) String description,
            @NotNull @Size(max=20000) String aboutMarkdown) {}
    public record Resource(String id,String name,String url,String previewUrl,String contentType,long size,boolean isPublic,LocalDateTime createdAt) {}
    public record ResourceVisibility(boolean isPublic) {}
}
