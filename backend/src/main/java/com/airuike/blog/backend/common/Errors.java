package com.airuike.blog.backend.common;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class Errors {
    @ExceptionHandler(BlogException.class)
    ResponseEntity<?> business(BlogException e) { return response(e.status(),e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return response(400,e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getDefaultMessage()).orElse("输入内容不符合要求"));
    }
    @ExceptionHandler({HandlerMethodValidationException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> bad(Exception e) { return response(400,"请检查输入格式与参数"); }
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<?> duplicate(DuplicateKeyException e) { return response(409,"名称或账号已存在，请刷新后重试"); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> large(Exception e) { return response(413,"单个文件不能超过 20 MiB"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) { LoggerFactory.getLogger(Errors.class).error("博客请求失败",e);return response(500,"服务暂时不可用，请稍后重试"); }
    private ResponseEntity<?> response(int code,String message) { return ResponseEntity.status(code).body(ApiResponse.error(code,message)); }
}
