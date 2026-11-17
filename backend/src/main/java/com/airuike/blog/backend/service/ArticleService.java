package com.airuike.blog.backend.service;
import com.airuike.blog.backend.common.BlogException;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.entity.ArticleEntity;
import com.airuike.blog.backend.mapper.ArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ArticleService {
    private final ArticleMapper articles;private final JdbcTemplate jdbc;private final TaxonomyService taxonomy;private final PublicCache cache;
    public ArticleService(ArticleMapper articles,JdbcTemplate jdbc,TaxonomyService taxonomy,PublicCache cache) {this.articles=articles;this.jdbc=jdbc;this.taxonomy=taxonomy;this.cache=cache;}
    public Page<Article> publicList(int page,int size,String keyword,Long category,Long tag) {
        return cache.read("articles:"+page+":"+size+":"+keyword+":"+category+":"+tag,new TypeReference<Page<Article>>() {},
                ()->list(false,page,size,keyword,category,tag,null,null,null));
    }
    public Page<Article> list(boolean admin,int page,int size,String keyword,Long category,Long tag,String status,String visibility,Boolean favorite) {
        if(page<1||page>10000||size<1||size>50)throw new BlogException(400,"分页参数超出范围");
        var q=new QueryWrapper<ArticleEntity>();
        if(!admin)q.eq("status","PUBLISHED").eq("visibility","PUBLIC");
        else {
            if(status!=null&&!status.isBlank()){if(!Set.of("DRAFT","PUBLISHED").contains(status))throw new BlogException(400,"文章状态无效");q.eq("status",status);}
            if(visibility!=null&&!visibility.isBlank()){if(!Set.of("PUBLIC","PRIVATE").contains(visibility))throw new BlogException(400,"可见范围无效");q.eq("visibility",visibility);}
            if(favorite!=null)q.eq("is_favorite",favorite);
        }
        if(keyword!=null&&!keyword.isBlank()) {
            if(keyword.length()>100)throw new BlogException(400,"关键词最多 100 个字符");
            String pattern="%"+keyword.strip().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            q.and(p->p.apply("title LIKE {0} ESCAPE '!'",pattern).or().apply("summary LIKE {0} ESCAPE '!'",pattern));
        }
        if(category!=null){if(category<=0)throw new BlogException(400,"分类参数无效");q.eq("category_id",category);}
        if(tag!=null){if(tag<=0)throw new BlogException(400,"标签参数无效");q.apply("EXISTS (SELECT 1 FROM article_tag atag WHERE atag.article_id=article.id AND atag.tag_id={0})",tag);}
        long total=articles.selectCount(q);
        if(admin)q.orderByDesc("updated_at","id");else q.orderByDesc("is_top","published_at","id");
        q.select("id","title","summary","cover","category_id","status","visibility","is_top","is_favorite","view_count","word_count","version","created_at","updated_at","published_at");
        var values=articles.selectList(q.last("LIMIT "+size+" OFFSET "+((long)(page-1)*size)));
        return new Page<>(views(values,admin),total,page,size);
    }
    public Article adminDetail(long id) {return views(List.of(required(id)),true).get(0);}
    public Article publicDetail(long id) {
        Article value=cache.read("article:"+id,new TypeReference<Article>() {},()->{
            var item=articles.selectOne(new QueryWrapper<ArticleEntity>().eq("id",id).eq("status","PUBLISHED").eq("visibility","PUBLIC"));
            if(item==null)throw new BlogException(404,"文章不存在或尚未公开");return views(List.of(item),false).get(0);
        });
        if(jdbc.update("UPDATE article SET view_count=view_count+1 WHERE id=? AND status='PUBLISHED' AND visibility='PUBLIC'",id)==0)
            throw new BlogException(404,"文章不存在或尚未公开");
        long count=jdbc.queryForObject("SELECT view_count FROM article WHERE id=?",Long.class,id);
        return new Article(value.id(),value.title(),value.summary(),value.content(),value.cover(),value.category(),value.tags(),value.status(),value.visibility(),value.isTop(),false,count,value.version(),value.createdAt(),value.updatedAt(),value.publishedAt(),value.wordCount(),value.readingMinutes());
    }
    public List<Archive> archives() {
        return cache.read("archives",new TypeReference<List<Archive>>() {},()->{
            var items=articles.selectList(new QueryWrapper<ArticleEntity>().eq("status","PUBLISHED").eq("visibility","PUBLIC")
                    .select("id","title","summary","cover","category_id","status","visibility","is_top","is_favorite","view_count","word_count","version","created_at","updated_at","published_at")
                    .orderByDesc("published_at","id"));
            var groups=new LinkedHashMap<String,List<Article>>();for(var article:views(items,false)){
                var time=article.publishedAt();String month=time.getYear()+"-"+time.getMonthValue();groups.computeIfAbsent(month,k->new ArrayList<>()).add(article);
            }
            return groups.values().stream().map(list->{var first=list.get(0).publishedAt();return new Archive(first.getYear(),first.getMonthValue(),list);}).toList();
        });
    }
    @Transactional public Article save(Long id,ArticleInput b) {
        validate(b);var existing=id==null?null:required(id);
        if(existing!=null && (b.version()==null||!Objects.equals(existing.getVersion(),b.version())))throw conflict();
        LocalDateTime now=LocalDateTime.now();int words=b.content().codePointCount(0,b.content().length());
        String summary=b.summary()==null?"":b.summary().strip();
        if(summary.isEmpty())summary=b.content().replaceAll("(?s)```.*?```|<[^>]*>"," ").replaceAll("[#>*_`\\[\\]]"," ").replaceAll("\\s+"," ").strip();
        if(summary.length()>240&& (b.summary()==null||b.summary().isBlank()))summary=summary.substring(0,240);
        LocalDateTime published=b.status().equals("PUBLISHED")?(existing!=null&&existing.getPublishedAt()!=null?existing.getPublishedAt():now):existing==null?null:existing.getPublishedAt();
        if(existing==null) {
            var item=new ArticleEntity();item.setTitle(b.title().strip());item.setSummary(summary);item.setContent(b.content());item.setCover(blank(b.cover()));item.setCategoryId(b.categoryId());
            item.setStatus(b.status());item.setVisibility(b.visibility());item.setIsTop(b.isTop());item.setIsFavorite(b.isFavorite());item.setWordCount(words);
            item.setVersion(1L);item.setViewCount(0L);item.setCreatedAt(now);item.setUpdatedAt(now);item.setPublishedAt(published);articles.insert(item);id=item.getId();
        } else {
            int changed=articles.update(null,new UpdateWrapper<ArticleEntity>().eq("id",id).eq("version",b.version())
                .set("title",b.title().strip()).set("summary",summary).set("content",b.content()).set("cover",blank(b.cover())).set("category_id",b.categoryId())
                .set("status",b.status()).set("visibility",b.visibility()).set("is_top",b.isTop()).set("is_favorite",b.isFavorite()).set("word_count",words)
                .set("updated_at",now).set("published_at",published).setSql("version=version+1"));
            if(changed!=1)throw conflict();
        }
        jdbc.update("DELETE FROM article_tag WHERE article_id=?",id);
        for(long tag:b.tagIds())jdbc.update("INSERT INTO article_tag(article_id,tag_id) VALUES(?,?)",id,tag);
        cache.changed();return adminDetail(id);
    }
    @Transactional public Article publication(long id,Publication b) {
        var article=required(id);if(b.status().equals("PUBLISHED")&&article.getContent().isBlank())throw new BlogException(400,"请填写正文后发布");
        var now=LocalDateTime.now();
        int count=articles.update(null,new UpdateWrapper<ArticleEntity>().eq("id",id).eq("version",b.version()).set("status",b.status())
                .set("published_at",b.status().equals("PUBLISHED")&&article.getPublishedAt()==null?now:article.getPublishedAt()).set("updated_at",now).setSql("version=version+1"));
        if(count!=1)throw conflict();cache.changed();return adminDetail(id);
    }
    @Transactional public Article favorite(long id,Favorite b) {
        required(id);if(articles.update(null,new UpdateWrapper<ArticleEntity>().eq("id",id).eq("version",b.version()).set("is_favorite",b.isFavorite())
            .set("updated_at",LocalDateTime.now()).setSql("version=version+1"))!=1)throw conflict();cache.changed();return adminDetail(id);
    }
    @Transactional public void delete(long id,long version) {
        required(id);if(articles.delete(new QueryWrapper<ArticleEntity>().eq("id",id).eq("version",version))!=1)throw conflict();cache.changed();
    }
    private ArticleEntity required(long id) {var item=articles.selectById(id);if(item==null)throw new BlogException(404,"文章不存在");return item;}
    private BlogException conflict() {return new BlogException(409,"文章已在其他页面修改，请保留当前内容并重新读取最新版本");}
    private String blank(String s) {return s==null||s.isBlank()?null:s.strip();}
    private void validate(ArticleInput b) {
        if(b.title().strip().isEmpty())throw new BlogException(400,"标题不能为空");
        if(b.status().equals("PUBLISHED")&&b.content().isBlank())throw new BlogException(400,"请填写正文后发布");
        if(new HashSet<>(b.tagIds()).size()!=b.tagIds().size())throw new BlogException(400,"标签不能重复");
        if(b.categoryId()!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=?",Long.class,b.categoryId())!=1)throw new BlogException(400,"分类不存在");
        for(long id:b.tagIds())if(jdbc.queryForObject("SELECT COUNT(*) FROM tag WHERE id=?",Long.class,id)!=1)throw new BlogException(400,"标签不存在");
        if(blank(b.cover())!=null && !b.cover().matches("/api/files/[a-f0-9-]{36}"))throw new BlogException(400,"封面请从资源库选择或上传");
        if(blank(b.cover())!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM file_resource WHERE id=? AND content_type LIKE 'image/%'",Long.class,b.cover().substring(11))!=1)
            throw new BlogException(400,"封面图片不存在");
    }
    private List<Article> views(List<ArticleEntity> rows,boolean admin) {
        if(rows.isEmpty())return List.of();
        var categories=taxonomy.list(false,admin).stream().collect(Collectors.toMap(x->Long.parseLong(x.id()),x->x));
        Map<Long,List<Taxonomy>> tags=new HashMap<>();String placeholders=String.join(",",Collections.nCopies(rows.size(),"?"));
        jdbc.query("SELECT atag.article_id,t.id,t.name,t.description,t.sort_order FROM article_tag atag JOIN tag t ON t.id=atag.tag_id WHERE atag.article_id IN ("+placeholders+") ORDER BY t.sort_order,t.id",
                (org.springframework.jdbc.core.RowCallbackHandler)r->tags.computeIfAbsent(r.getLong("article_id"),k->new ArrayList<>()).add(new Taxonomy(Long.toString(r.getLong("id")),r.getString("name"),r.getString("description"),r.getInt("sort_order"),0)),rows.stream().map(ArticleEntity::getId).toArray());
        return rows.stream().map(n->new Article(n.getId().toString(),n.getTitle(),n.getSummary(),n.getContent(),n.getCover(),categories.get(n.getCategoryId()),tags.getOrDefault(n.getId(),List.of()),
            n.getStatus(),n.getVisibility(),Boolean.TRUE.equals(n.getIsTop()),admin&&Boolean.TRUE.equals(n.getIsFavorite()),n.getViewCount(),n.getVersion(),n.getCreatedAt(),n.getUpdatedAt(),n.getPublishedAt(),n.getWordCount(),n.getWordCount()==0?0:Math.max(1,(n.getWordCount()+399)/400))).toList();
    }
}
