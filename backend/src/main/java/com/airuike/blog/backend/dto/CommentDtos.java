package com.airuike.blog.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class CommentDtos {
    private CommentDtos() {}
    public record Submit(
        @Size(max=40,message="昵称最多 40 个字符") String nickname,
        @NotBlank(message="请填写评论") @Size(max=2000,message="评论最多 2000 个字符") String content,
        @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",message="提交标识无效，请刷新页面") String submissionId) {}
    public record Submission(String id,String message) {}
    public record Reply(String content,LocalDateTime updatedAt) {}
    public record PublicComment(String id,String nickname,String content,LocalDateTime createdAt,Reply reply) {}
    public record AdminComment(String id,String articleId,String articleTitle,String nickname,String content,
        String status,long version,LocalDateTime createdAt,LocalDateTime reviewedAt,Reply reply) {}
    public record Counts(long total,long pending,long approved,long rejected) {}
    public record AdminPage(List<AdminComment> items,long total,int page,int pageSize,Counts counts) {}
    public record Review(@NotNull @Pattern(regexp="APPROVED|REJECTED",message="审核状态无效") String status,
        @NotNull @Positive Long version) {}
    public record ReplyInput(@NotNull @Size(max=2000,message="回复最多 2000 个字符") String content,
        @NotNull @Positive Long version) {}
}
