package com.airuike.blog.backend.controller;
import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.service.ResourceService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
public class ResourceController {
    private final ResourceService service;
    public ResourceController(ResourceService service){this.service=service;}
    @GetMapping("/api/admin/files") public ApiResponse<Page<Resource>> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="24")int pageSize,@RequestParam(required=false)String keyword){return ApiResponse.ok(service.list(page,pageSize,keyword));}
    @GetMapping("/api/admin/files/{id}") public ApiResponse<Resource> metadata(@PathVariable String id){return ApiResponse.ok(service.metadata(id));}
    @PostMapping("/api/admin/files/upload") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Resource> upload(@RequestParam MultipartFile file){return ApiResponse.ok(service.upload(file));}
    @PutMapping("/api/admin/files/{id}/visibility") public ApiResponse<Resource> visibility(@PathVariable String id,@RequestBody ResourceVisibility b){return ApiResponse.ok(service.visibility(id,b.isPublic()));}
    @DeleteMapping("/api/admin/files/{id}") public ApiResponse<Void> delete(@PathVariable String id){service.delete(id);return ApiResponse.ok(null);}
    @GetMapping("/api/files/{id}") public ResponseEntity<FileSystemResource> read(@PathVariable String id,@RequestParam(required=false)String ticket,@AuthenticationPrincipal Jwt jwt){
        var read=service.read(id,ticket,jwt);String mime=read.metadata().getContentType();var headers=new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mime));headers.setCacheControl("private, no-store");headers.set("X-Content-Type-Options","nosniff");
        headers.set("Content-Security-Policy","default-src 'none'; sandbox");
        var disposition=mime.startsWith("image/")?ContentDisposition.inline():ContentDisposition.attachment();
        headers.setContentDisposition(disposition.filename(read.metadata().getName(),StandardCharsets.UTF_8).build());headers.setContentLength(read.metadata().getSize());
        return new ResponseEntity<>(new FileSystemResource(read.path()),headers,HttpStatus.OK);
    }
}
