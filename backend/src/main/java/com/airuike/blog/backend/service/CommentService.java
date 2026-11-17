package com.airuike.blog.backend.service;

import com.airuike.blog.backend.common.BlogException;
import com.airuike.blog.backend.dto.BlogDtos.Page;
import com.airuike.blog.backend.dto.CommentDtos.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private final JdbcTemplate jdbc;
    private final CommentRateLimiter limiter;
    public CommentService(JdbcTemplate jdbc,CommentRateLimiter limiter) {this.jdbc=jdbc;this.limiter=limiter;}

    @Transactional(readOnly=true)
    public Page<PublicComment> publicList(long articleId,int page,int size) {
        pagination(page,size);requirePublic(articleId,false);
        String where=" FROM article_comment c JOIN article a ON a.id=c.article_id WHERE c.article_id=? AND c.status='APPROVED' AND a.status='PUBLISHED' AND a.visibility='PUBLIC'";
        long total=jdbc.queryForObject("SELECT COUNT(*)"+where,Long.class,articleId);
        var items=jdbc.query("SELECT c.*"+where+" ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?",
            (r,i)->new PublicComment(r.getString("id"),r.getString("nickname"),r.getString("content"),time(r,"created_at"),reply(r)),
            articleId,size,(long)(page-1)*size);
        return new Page<>(items,total,page,size);
    }

    @Transactional
    public Submission submit(long articleId,Submit body,String address) {
        // Parent lock serializes a submission with withdrawal/deletion of its article.
        requirePublic(articleId,true);
        String nickname=body.nickname()==null||body.nickname().isBlank()?"访客":body.nickname().strip();
        String content=body.content().strip(),key=body.submissionId().toLowerCase(Locale.ROOT);
        if(content.isBlank()) throw new BlogException(400,"请填写评论");
        var existing=jdbc.queryForList("SELECT id,article_id,nickname,content FROM article_comment WHERE submission_id=?",key);
        if(!existing.isEmpty()) {
            var row=existing.get(0);
            if(((Number)row.get("article_id")).longValue()!=articleId || !nickname.equals(row.get("nickname")) || !content.equals(row.get("content")))
                throw new BlogException(409,"提交内容已改变，请重新提交");
            return new Submission(row.get("id").toString(),"评论已收到");
        }
        limiter.acquire(address);
        var generated=new GeneratedKeyHolder();
        try { jdbc.update(connection->{
            var statement=connection.prepareStatement("INSERT INTO article_comment (article_id,submission_id,nickname,content,status) VALUES(?,?,?,?,'PENDING')",new String[]{"id"});
            statement.setLong(1,articleId);statement.setString(2,key);statement.setString(3,nickname);statement.setString(4,content);return statement;
        },generated); }
        catch(DuplicateKeyException failure) {throw new BlogException(409,"提交标识已被使用，请重新提交");}
        return new Submission(Objects.requireNonNull(generated.getKey()).toString(),"评论已提交，审核通过后显示");
    }

    @Transactional(readOnly=true)
    public AdminPage adminList(int page,int size,String status) {
        pagination(page,size);
        if(status!=null && !Set.of("PENDING","APPROVED","REJECTED").contains(status)) throw new BlogException(400,"审核状态无效");
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM article_comment",Long.class);
        long pending=count("PENDING"),approved=count("APPROVED"),rejected=count("REJECTED");
        String filter=status==null?"":" WHERE c.status=?";
        var args=new ArrayList<Object>();if(status!=null)args.add(status);args.add(size);args.add((long)(page-1)*size);
        var items=jdbc.query("SELECT c.*,a.title AS article_title FROM article_comment c JOIN article a ON a.id=c.article_id"+filter+
            " ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?",(r,i)->admin(r),args.toArray());
        long selected=status==null?total:status.equals("PENDING")?pending:status.equals("APPROVED")?approved:rejected;
        return new AdminPage(items,selected,page,size,new Counts(total,pending,approved,rejected));
    }
    private long count(String status) {return jdbc.queryForObject("SELECT COUNT(*) FROM article_comment WHERE status=?",Long.class,status);}

    @Transactional
    public AdminComment review(long id,Review body) {
        required(id);
        changed(jdbc.update("UPDATE article_comment SET status=?,reviewed_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=? AND version=?",body.status(),id,body.version()));
        return required(id);
    }
    @Transactional
    public AdminComment saveReply(long id,ReplyInput body) {
        var comment=required(id);
        if(!"APPROVED".equals(comment.status())) throw new BlogException(400,"审核通过后才能回复");
        String content=body.content().strip();boolean empty=content.isBlank();
        changed(jdbc.update("UPDATE article_comment SET reply_content=?,reply_updated_at=?,version=version+1 WHERE id=? AND version=? AND status='APPROVED'",
            empty?null:content,empty?null:LocalDateTime.now(),id,body.version()));
        return required(id);
    }
    @Transactional
    public void delete(long id,long version) {
        if(version<1) throw new BlogException(400,"评论版本无效");
        required(id);changed(jdbc.update("DELETE FROM article_comment WHERE id=? AND version=?",id,version));
    }
    private AdminComment required(long id) {
        var rows=jdbc.query("SELECT c.*,a.title AS article_title FROM article_comment c JOIN article a ON a.id=c.article_id WHERE c.id=?",(r,i)->admin(r),id);
        if(rows.isEmpty()) throw new BlogException(404,"评论不存在");return rows.get(0);
    }
    private AdminComment admin(ResultSet r) throws SQLException {
        return new AdminComment(r.getString("id"),r.getString("article_id"),r.getString("article_title"),r.getString("nickname"),r.getString("content"),
            r.getString("status"),r.getLong("version"),time(r,"created_at"),time(r,"reviewed_at"),reply(r));
    }
    private Reply reply(ResultSet r) throws SQLException {
        String content=r.getString("reply_content");return content==null?null:new Reply(content,time(r,"reply_updated_at"));
    }
    private LocalDateTime time(ResultSet r,String column) throws SQLException {var value=r.getTimestamp(column);return value==null?null:value.toLocalDateTime();}
    private void requirePublic(long id,boolean lock) {
        if(id<1 || jdbc.queryForList("SELECT id FROM article WHERE id=? AND status='PUBLISHED' AND visibility='PUBLIC'"+(lock?" FOR UPDATE":""),id).isEmpty())
            throw new BlogException(404,"文章不存在或尚未公开");
    }
    private void pagination(int page,int size) {if(page<1||page>10000||size<1||size>50) throw new BlogException(400,"分页参数超出范围");}
    private void changed(int rows) {if(rows==0)throw new BlogException(409,"评论已更新，请刷新后重试");}
}
