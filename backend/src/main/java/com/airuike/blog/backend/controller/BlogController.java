package com.airuike.blog.backend.controller;
import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
public class BlogController {
    private final ArticleService articles;private final TaxonomyService taxonomy;private final PublicCache cache;private final JdbcTemplate jdbc;
    public BlogController(ArticleService articles,TaxonomyService taxonomy,PublicCache cache,JdbcTemplate jdbc) {this.articles=articles;this.taxonomy=taxonomy;this.cache=cache;this.jdbc=jdbc;}
    @GetMapping("/api/health") public ApiResponse<?> health() {return ApiResponse.ok(java.util.Map.of("status","UP","application","blog"));}
    @GetMapping("/api/site") public ApiResponse<Site> site() {return ApiResponse.ok(cache.read("site",new TypeReference<Site>(){},()->jdbc.queryForObject("SELECT title,description,about_markdown FROM blog_state WHERE id=1",(r,i)->new Site(r.getString(1),r.getString(2),r.getString(3)))));}
    @GetMapping("/api/articles") public ApiResponse<Page<Article>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int pageSize,
        @RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long tagId) {return ApiResponse.ok(articles.publicList(page,pageSize,keyword,categoryId,tagId));}
    @GetMapping("/api/articles/{id}") public ApiResponse<Article> detail(@PathVariable long id) {return ApiResponse.ok(articles.publicDetail(id));}
    @GetMapping("/api/categories") public ApiResponse<List<Taxonomy>> categories() {return ApiResponse.ok(cache.read("categories",new TypeReference<List<Taxonomy>>(){},()->taxonomy.list(false,false)));}
    @GetMapping("/api/tags") public ApiResponse<List<Taxonomy>> tags() {return ApiResponse.ok(cache.read("tags",new TypeReference<List<Taxonomy>>(){},()->taxonomy.list(true,false)));}
    @GetMapping("/api/archives") public ApiResponse<List<Archive>> archives() {return ApiResponse.ok(articles.archives());}
}
