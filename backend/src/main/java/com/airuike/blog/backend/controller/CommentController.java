package com.airuike.blog.backend.controller;

import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.dto.BlogDtos.Page;
import com.airuike.blog.backend.dto.CommentDtos.*;
import com.airuike.blog.backend.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {
    private final CommentService comments;
    public CommentController(CommentService comments) {this.comments=comments;}
    @GetMapping("/api/articles/{articleId}/comments")
    public ApiResponse<Page<PublicComment>> list(@PathVariable long articleId,@RequestParam(defaultValue="1")int page,
            @RequestParam(defaultValue="10")int pageSize) {return ApiResponse.ok(comments.publicList(articleId,page,pageSize));}
    @PostMapping("/api/articles/{articleId}/comments") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Submission> submit(@PathVariable long articleId,@Valid @RequestBody Submit body,HttpServletRequest request) {
        return ApiResponse.ok(comments.submit(articleId,body,request.getRemoteAddr()));
    }
    @GetMapping("/api/admin/comments")
    public ApiResponse<AdminPage> admin(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="12")int pageSize,
            @RequestParam(required=false)String status) {return ApiResponse.ok(comments.adminList(page,pageSize,status));}
    @PutMapping("/api/admin/comments/{id}/review")
    public ApiResponse<AdminComment> review(@PathVariable long id,@Valid @RequestBody Review body) {return ApiResponse.ok(comments.review(id,body));}
    @PutMapping("/api/admin/comments/{id}/reply")
    public ApiResponse<AdminComment> reply(@PathVariable long id,@Valid @RequestBody ReplyInput body) {return ApiResponse.ok(comments.saveReply(id,body));}
    @DeleteMapping("/api/admin/comments/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,@RequestParam long version) {comments.delete(id,version);return ApiResponse.ok(null);}
}
