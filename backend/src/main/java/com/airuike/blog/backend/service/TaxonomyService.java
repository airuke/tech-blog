package com.airuike.blog.backend.service;
import com.airuike.blog.backend.common.BlogException;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.entity.*;
import com.airuike.blog.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TaxonomyService {
    private final JdbcTemplate jdbc;private final CategoryMapper categories;private final TagMapper tags;private final PublicCache cache;
    public TaxonomyService(JdbcTemplate jdbc,CategoryMapper categories,TagMapper tags,PublicCache cache) {this.jdbc=jdbc;this.categories=categories;this.tags=tags;this.cache=cache;}
    public List<Taxonomy> list(boolean isTag,boolean admin) {
        String join=isTag?"LEFT JOIN article_tag atag ON atag.tag_id=t.id LEFT JOIN article a ON a.id=atag.article_id":"LEFT JOIN article a ON a.category_id=t.id";
        String count=admin?"COUNT(a.id)":"COUNT(CASE WHEN a.status='PUBLISHED' AND a.visibility='PUBLIC' THEN 1 END)";
        return jdbc.query("SELECT t.id,t.name,t.description,t.sort_order,"+count+" AS article_count FROM "+table(isTag)+" t "+join+
                " GROUP BY t.id,t.name,t.description,t.sort_order"+(admin?"":" HAVING "+count+">0")+" ORDER BY t.sort_order,t.id",
                (r,i)->new Taxonomy(Long.toString(r.getLong("id")),r.getString("name"),r.getString("description"),r.getInt("sort_order"),r.getLong("article_count")));
    }
    @Transactional public Taxonomy save(boolean isTag,Long id,TaxonomyInput b) {
        String name=b.name().strip();if(name.isEmpty()) throw new BlogException(400,"名称不能为空");
        String description=b.description()==null?"":b.description().strip();
        long saved;
        if(isTag) {var item=id==null?new TagEntity():tags.selectById(id);if(item==null)throw new BlogException(404,"标签不存在");item.setName(name);item.setDescription(description);item.setSortOrder(b.sortOrder());if(id==null)tags.insert(item);else tags.updateById(item);saved=item.getId();}
        else {var item=id==null?new CategoryEntity():categories.selectById(id);if(item==null)throw new BlogException(404,"分类不存在");item.setName(name);item.setDescription(description);item.setSortOrder(b.sortOrder());if(id==null)categories.insert(item);else categories.updateById(item);saved=item.getId();}
        cache.changed();long target=saved;return list(isTag,true).stream().filter(x->Long.parseLong(x.id())==target).findFirst().orElseThrow();
    }
    @Transactional public void delete(boolean isTag,long id) {
        // Parent-row lock serializes deletion against article foreign-key inserts.
        if(jdbc.queryForList("SELECT id FROM "+table(isTag)+" WHERE id=? FOR UPDATE",id).isEmpty()) throw new BlogException(404,"分类或标签不存在");
        Long count=jdbc.queryForObject(isTag?"SELECT COUNT(*) FROM article_tag WHERE tag_id=?":"SELECT COUNT(*) FROM article WHERE category_id=?",Long.class,id);
        if(count!=null&&count>0) throw new BlogException(409,"仍有关联文章，请先修改文章的分类或标签");
        if(isTag)tags.deleteById(id);else categories.deleteById(id);cache.changed();
    }
    private String table(boolean isTag) { return isTag?"tag":"category"; }
}
