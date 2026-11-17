package com.airuike.blog.backend.service;
import com.airuike.blog.backend.common.BlogException;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.entity.FileResource;
import com.airuike.blog.backend.mapper.FileResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
@Service
public class ResourceService {
    public record Read(FileResource metadata,Path path) {}
    private final FileResourceMapper resources;private final JdbcTemplate jdbc;private final Path root;private final SecretKey key;
    public ResourceService(FileResourceMapper resources,JdbcTemplate jdbc,@Value("${blog.storage.root}") String root,SecretKey key) {this.resources=resources;this.jdbc=jdbc;this.root=Path.of(root).toAbsolutePath().normalize();this.key=key;}
    public Page<Resource> list(int page,int size,String keyword) {
        if(page<1||page>10000||size<1||size>200)throw new BlogException(400,"分页参数超出范围");
        var q=new QueryWrapper<FileResource>();if(keyword!=null&&!keyword.isBlank()){if(keyword.length()>100)throw new BlogException(400,"关键词过长");q.like("name",keyword.strip());}
        long count=resources.selectCount(q);return new Page<>(resources.selectList(q.orderByDesc("created_at","id").last("LIMIT "+size+" OFFSET "+((long)(page-1)*size))).stream().map(this::view).toList(),count,page,size);
    }
    public Resource upload(MultipartFile file) {
        if(file.isEmpty())throw new BlogException(400,"请选择非空文件");if(file.getSize()>20L*1024*1024)throw new BlogException(413,"单个文件不能超过 20 MiB");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("文件").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).strip();
        if(name.isEmpty()||name.length()>200||name.chars().anyMatch(Character::isISOControl))throw new BlogException(400,"文件名不符合要求");
        String id=UUID.randomUUID().toString();Path target=root.resolve(id),temp=null;
        try {
            Files.createDirectories(root);temp=Files.createTempFile(root,"upload-",".part");
            try(var input=file.getInputStream()){Files.copy(input,temp,StandardCopyOption.REPLACE_EXISTING);}
            String mime=type(temp,name);Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            var item=new FileResource();item.setId(id);item.setName(name);item.setObjectKey(id);item.setContentType(mime);item.setSize(file.getSize());item.setIsPublic(false);item.setCreatedAt(LocalDateTime.now());
            try {resources.insert(item);}catch(RuntimeException e){Files.deleteIfExists(target);throw e;}
            return view(item);
        } catch(BlogException e){throw e;}catch(java.io.IOException e){throw new BlogException(503,"文件暂时无法保存，请检查存储目录");}
        finally {if(temp!=null)try{Files.deleteIfExists(temp);}catch(java.io.IOException ignored){}}
    }
    public Resource metadata(String id) {return view(required(id));}
    public Read read(String id,String ticket,Jwt jwt) {
        var item=required(id);boolean admin=jwt!=null&&"BLOG_ADMIN".equals(jwt.getClaimAsString("scope"));
        if(!admin&&!validTicket(id,ticket)&&!Boolean.TRUE.equals(item.getIsPublic())&&!publishedReference(id))throw new BlogException(404,"文件不存在或尚未公开");
        Path file=root.resolve(item.getObjectKey()).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file))throw new BlogException(404,"文件不存在");return new Read(item,file);
    }
    @Transactional public Resource visibility(String id,boolean visible) {
        required(id);resources.update(null,new UpdateWrapper<FileResource>().eq("id",id).set("is_public",visible));return view(required(id));
    }
    @Transactional public void delete(String id) {
        var item=required(id);Long refs=jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE cover=? OR content LIKE ?",Long.class,url(id),"%"+url(id)+"%");
        if(refs!=null&&refs>0)throw new BlogException(409,"文件仍被文章引用，请先移除正文或封面的引用");
        resources.deleteById(id);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{Files.deleteIfExists(root.resolve(item.getObjectKey()));}catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger(ResourceService.class).warn("资源物理文件待清理 {}",id);}}});
    }
    private boolean publishedReference(String id) {return jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE status='PUBLISHED' AND visibility='PUBLIC' AND (cover=? OR content LIKE ?)",Long.class,url(id),"%"+url(id)+"%")>0;}
    private FileResource required(String id) {if(id==null||!id.matches("[a-f0-9-]{36}"))throw new BlogException(404,"文件不存在");var item=resources.selectById(id);if(item==null)throw new BlogException(404,"文件不存在");return item;}
    private Resource view(FileResource item) {long expires=System.currentTimeMillis()/1000+600;return new Resource(item.getId(),item.getName(),url(item.getId()),url(item.getId())+"?ticket="+expires+"."+sign(item.getId()+":"+expires),item.getContentType(),item.getSize(),Boolean.TRUE.equals(item.getIsPublic()),item.getCreatedAt());}
    private String url(String id) {return "/api/files/"+id;}
    private String sign(String text) {try{var mac=Mac.getInstance("HmacSHA256");mac.init(key);return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));}catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}}
    private boolean validTicket(String id,String ticket) {
        if(ticket==null||ticket.length()>100)return false;try{String[] parts=ticket.split("\\.",2);long expiry=Long.parseLong(parts[0]),now=System.currentTimeMillis()/1000;
            return parts.length==2&&expiry>=now&&expiry<=now+600&&MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.US_ASCII),sign(id+":"+expiry).getBytes(StandardCharsets.US_ASCII));
        }catch(RuntimeException e){return false;}
    }
    private String type(Path file,String name) throws java.io.IOException {
        byte[] b;try(var input=Files.newInputStream(file)){b=input.readNBytes(16);}String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if(Set.of("png","jpg","jpeg","gif","webp").contains(ext)) {
            if(ext.equals("png")&&b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
            if(Set.of("jpg","jpeg").contains(ext)&&b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
            String start=new String(b,StandardCharsets.ISO_8859_1);
            if(ext.equals("gif")&&(start.startsWith("GIF87a")||start.startsWith("GIF89a")))return "image/gif";
            if(ext.equals("webp")&&start.startsWith("RIFF")&&start.length()>=12&&start.substring(8,12).equals("WEBP"))return "image/webp";
            throw new BlogException(400,"图片格式与文件内容不一致");
        }
        if(ext.equals("pdf")&&new String(b,StandardCharsets.ISO_8859_1).startsWith("%PDF-"))return "application/pdf";
        if(ext.equals("zip")&&b.length>=4&&b[0]==80&&b[1]==75&&b[2]==3&&b[3]==4)return "application/zip";
        if(Set.of("txt","md").contains(ext)){try{StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(file)));}catch(java.nio.charset.CharacterCodingException e){throw new BlogException(400,"文本文件请使用 UTF-8 编码");}return "text/plain;charset=UTF-8";}
        throw new BlogException(400,"支持 PNG、JPEG、GIF、WebP、PDF、UTF-8 文本及 ZIP 文件");
    }
}
