package com.airuike.blog.backend.controller;
import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.service.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final ArticleService articles;private final TaxonomyService taxonomy;private final PublicCache cache;private final JdbcTemplate jdbc;
    public AdminController(ArticleService articles,TaxonomyService taxonomy,PublicCache cache,JdbcTemplate jdbc) {this.articles=articles;this.taxonomy=taxonomy;this.cache=cache;this.jdbc=jdbc;}
    @GetMapping("/stats") public ApiResponse<?> stats() {
        var counts=jdbc.queryForMap("SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN status='DRAFT' THEN 1 ELSE 0 END),0) AS drafts,COALESCE(SUM(CASE WHEN status='PUBLISHED' AND visibility='PUBLIC' THEN 1 ELSE 0 END),0) AS published,COALESCE(SUM(CASE WHEN visibility='PRIVATE' THEN 1 ELSE 0 END),0) AS privateArticles,COALESCE(SUM(CASE WHEN is_favorite=TRUE THEN 1 ELSE 0 END),0) AS favorites,COALESCE(SUM(view_count),0) AS views FROM article");
        return ApiResponse.ok(Map.of("counts",counts,"cache",cache.status(),"resources",jdbc.queryForObject("SELECT COUNT(*) FROM file_resource",Long.class)));
    }
    @GetMapping("/articles") public ApiResponse<Page<Article>> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="12")int pageSize,
        @RequestParam(required=false)String keyword,@RequestParam(required=false)Long categoryId,@RequestParam(required=false)Long tagId,
        @RequestParam(required=false)String status,@RequestParam(required=false)String visibility,@RequestParam(required=false)Boolean favorite) {return ApiResponse.ok(articles.list(true,page,pageSize,keyword,categoryId,tagId,status,visibility,favorite));}
    @GetMapping("/articles/{id}") public ApiResponse<Article> detail(@PathVariable long id) {return ApiResponse.ok(articles.adminDetail(id));}
    @PostMapping("/articles") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Article> create(@Valid @RequestBody ArticleInput b) {return ApiResponse.ok(articles.save(null,b));}
    @PutMapping("/articles/{id}") public ApiResponse<Article> save(@PathVariable long id,@Valid @RequestBody ArticleInput b) {return ApiResponse.ok(articles.save(id,b));}
    @PutMapping("/articles/{id}/publication") public ApiResponse<Article> publish(@PathVariable long id,@Valid @RequestBody Publication b) {return ApiResponse.ok(articles.publication(id,b));}
    @PutMapping("/articles/{id}/favorite") public ApiResponse<Article> favorite(@PathVariable long id,@Valid @RequestBody Favorite b) {return ApiResponse.ok(articles.favorite(id,b));}
    @DeleteMapping("/articles/{id}") public ApiResponse<Void> delete(@PathVariable long id,@RequestParam long version) {articles.delete(id,version);return ApiResponse.ok(null);}
    @GetMapping("/categories") public ApiResponse<List<Taxonomy>> categories(){return ApiResponse.ok(taxonomy.list(false,true));}
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Taxonomy> createCategory(@Valid @RequestBody TaxonomyInput b){return ApiResponse.ok(taxonomy.save(false,null,b));}
    @PutMapping("/categories/{id}") public ApiResponse<Taxonomy> saveCategory(@PathVariable long id,@Valid @RequestBody TaxonomyInput b){return ApiResponse.ok(taxonomy.save(false,id,b));}
    @DeleteMapping("/categories/{id}") public ApiResponse<Void> deleteCategory(@PathVariable long id){taxonomy.delete(false,id);return ApiResponse.ok(null);}
    @GetMapping("/tags") public ApiResponse<List<Taxonomy>> tags(){return ApiResponse.ok(taxonomy.list(true,true));}
    @PostMapping("/tags") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Taxonomy> createTag(@Valid @RequestBody TaxonomyInput b){return ApiResponse.ok(taxonomy.save(true,null,b));}
    @PutMapping("/tags/{id}") public ApiResponse<Taxonomy> saveTag(@PathVariable long id,@Valid @RequestBody TaxonomyInput b){return ApiResponse.ok(taxonomy.save(true,id,b));}
    @DeleteMapping("/tags/{id}") public ApiResponse<Void> deleteTag(@PathVariable long id){taxonomy.delete(true,id);return ApiResponse.ok(null);}
    @PutMapping("/site") @Transactional public ApiResponse<Site> site(@Valid @RequestBody SiteInput b) {jdbc.update("UPDATE blog_state SET title=?,description=?,about_markdown=? WHERE id=1",b.title().strip(),b.description().strip(),b.aboutMarkdown());cache.changed();return ApiResponse.ok(new Site(b.title().strip(),b.description().strip(),b.aboutMarkdown()));}
}
