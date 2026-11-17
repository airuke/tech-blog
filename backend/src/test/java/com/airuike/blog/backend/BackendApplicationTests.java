package com.airuike.blog.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses an isolated in-memory database and temporary storage, never application-local credentials. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BackendApplicationTests {
    private static final String PASSWORD = "Test-only_Password_2026";
    private static final String HASH = new BCryptPasswordEncoder(12).encode(PASSWORD);
    private static final Path STORAGE;
    static {
        try { STORAGE = Files.createTempDirectory("blog-tests-files-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("blog.storage.root", STORAGE::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    private String token;

    @BeforeEach void owner() throws Exception {
        jdbc.update("INSERT INTO sys_user (id,username,password_hash,nickname,status,token_version) VALUES(1,'blog_test',?,'测试博主',1,0)", HASH);
        token = call(post("/api/auth/login"), Map.of("username","blog_test","password",PASSWORD),200,false)
                .path("accessToken").asText();
        assertFalse(token.isBlank());
    }
    @AfterEach void files() throws Exception {
        try(var entries=Files.list(STORAGE)){ for(Path entry:entries.toList()) Files.deleteIfExists(entry); }
    }
    @AfterAll static void storage() throws Exception { Files.deleteIfExists(STORAGE); }

    private JsonNode call(MockHttpServletRequestBuilder req,Object body,int status,boolean admin) throws Exception {
        if(admin) req.header("Authorization","Bearer " + token);
        if(body!=null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        var result=mvc.perform(req).andExpect(status().is(status)).andReturn();
        var value=json.readTree(result.getResponse().getContentAsByteArray());
        assertEquals(status<400?0:status,value.path("code").asInt());
        return value.path("data");
    }
    private Map<String,Object> input(String title,String status,String visibility) {
        var b=new LinkedHashMap<String,Object>();
        b.put("title",title); b.put("summary", "文章摘要"); b.put("content","# 内容\n\n正文与知识积累。");
        b.put("status",status); b.put("visibility",visibility); b.put("tagIds",List.of());
        b.put("isTop",false); b.put("isFavorite",false);
        return b;
    }
    private JsonNode create(Map<String,Object> b) throws Exception {return call(post("/api/admin/articles"),b,201,true);}
    private JsonNode taxonomy(String plural,String name) throws Exception {
        return call(post("/api/admin/"+plural),Map.of("name",name,"description","说明","sortOrder",0),201,true);
    }
    private JsonNode upload(String name,byte[] bytes) throws Exception {
        var file=new MockMultipartFile("file",name,"application/octet-stream",bytes);
        return call(multipart("/api/admin/files/upload").file(file),null,201,true);
    }

    @Test void firstSetupIsLocalAndRunsOnce() throws Exception {
        jdbc.update("DELETE FROM sys_user");
        var setup=Map.of("username","owner_1","nickname","","password",PASSWORD);
        var status=call(get("/api/auth/status"),null,200,false);
        assertTrue(status.path("setupNeeded").asBoolean());
        call(post("/api/auth/setup").with(q->{q.setRemoteAddr("192.0.2.1");return q;}),setup,403,false);
        var owner=call(post("/api/auth/setup"),setup,201,false);
        assertEquals("owner_1",owner.path("nickname").asText());
        assertEquals("owner_1",jdbc.queryForObject("SELECT nickname FROM sys_user WHERE id=1",String.class));
        call(post("/api/auth/setup"),setup,409,false);
        assertFalse(call(get("/api/auth/status"),null,200,false).path("setupNeeded").asBoolean());
        assertNotEquals(PASSWORD,jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id=1",String.class));
    }
    @Test void commentLifecycleRequiresModerationAndOwnerPrivileges() throws Exception {
        var article=create(input("评论流程测试","PUBLISHED","PUBLIC"));String articleId=article.path("id").asText();
        String publicUrl="/api/articles/"+articleId+"/comments";
        var body=new LinkedHashMap<String,Object>();body.put("nickname","");body.put("content","<img src=x onerror=alert(1)> 作为纯文本评论");
        body.put("submissionId",UUID.randomUUID().toString());body.put("status","APPROVED");
        String id=call(post(publicUrl),body,201,false).path("id").asText();String adminUrl="/api/admin/comments/"+id;
        assertEquals(0,call(get(publicUrl),null,200,false).path("total").asInt());
        call(get("/api/admin/comments"),null,401,false);
        call(put(adminUrl+"/review"),Map.of("status","APPROVED","version",1),401,false);
        call(put(adminUrl+"/reply"),Map.of("content","无权回复","version",1),401,false);
        call(delete(adminUrl).param("version","1"),null,401,false);
        var pending=call(get("/api/admin/comments").param("status","PENDING"),null,200,true);
        assertEquals(1,pending.path("counts").path("pending").asInt());
        assertEquals("访客",pending.path("items").get(0).path("nickname").asText());
        call(put(adminUrl+"/reply"),Map.of("content","先审核","version",1),400,true);
        call(put(adminUrl+"/review"),Map.of("status","APPROVED","version",1),200,true);
        var visible=call(get(publicUrl),null,200,false);
        assertEquals(1,visible.path("total").asInt());
        var item=visible.path("items").get(0);assertEquals(body.get("content"),item.path("content").asText());
        assertFalse(item.has("status"));assertFalse(item.has("version"));assertFalse(item.has("submissionId"));
        assertEquals(id,call(post(publicUrl),body,201,false).path("id").asText());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM article_comment",Integer.class));
        var changed=new LinkedHashMap<>(body);changed.put("content","同标识改正文");call(post(publicUrl),changed,409,false);
        call(put(adminUrl+"/reply"),Map.of("content","过期版本","version",1),409,true);
        call(put(adminUrl+"/reply"),Map.of("content","博主回复内容","version",2),200,true);
        assertEquals("博主回复内容",call(get(publicUrl),null,200,false).path("items").get(0).path("reply").path("content").asText());
        call(put(adminUrl+"/review"),Map.of("status","REJECTED","version",3),200,true);
        assertEquals(0,call(get(publicUrl),null,200,false).path("total").asInt());
        call(put(adminUrl+"/review"),Map.of("status","APPROVED","version",4),200,true);
        call(put(adminUrl+"/reply"),Map.of("content","","version",5),200,true);
        assertTrue(call(get(publicUrl),null,200,false).path("items").get(0).path("reply").isNull());
        call(delete(adminUrl).param("version","5"),null,409,true);
        call(delete(adminUrl).param("version","6"),null,200,true);
        assertEquals(0,call(get(publicUrl),null,200,false).path("total").asInt());
    }
    @Test void commentVisibilityValidationRateLimitAndCascade() throws Exception {
        var visible=create(input("评论范围测试","PUBLISHED","PUBLIC"));String articleId=visible.path("id").asText();
        var draft=create(input("草稿评论测试","DRAFT","PUBLIC"));var secret=create(input("私人评论测试","PUBLISHED","PRIVATE"));
        String url="/api/articles/"+articleId+"/comments";
        var body=Map.of("nickname","访客测试","content","测试评论","submissionId",UUID.randomUUID().toString());
        for(var hidden:List.of(draft,secret)) {
            String target="/api/articles/"+hidden.path("id").asText()+"/comments";
            call(get(target),null,404,false);call(post(target),body,404,false);
        }
        call(post(url),Map.of("content"," ","submissionId",UUID.randomUUID().toString()),400,false);
        call(post(url),Map.of("nickname","x".repeat(41),"content","正文","submissionId",UUID.randomUUID().toString()),400,false);
        call(post(url),Map.of("content","x".repeat(2001),"submissionId",UUID.randomUUID().toString()),400,false);
        call(get(url).param("page","0"),null,400,false);call(get(url).param("pageSize","51"),null,400,false);
        call(get("/api/admin/comments").param("status","INVALID"),null,400,true);
        String first="";
        for(int i=0;i<5;i++) {
            var current=i==0?body:Map.of("content","测试评论 "+i,"submissionId",UUID.randomUUID().toString());
            var created=call(post(url).with(q->{q.setRemoteAddr("192.0.2.22");return q;}),current,201,false);
            if(i==0)first=created.path("id").asText();
        }
        call(post(url).with(q->{q.setRemoteAddr("192.0.2.22");return q;}),Map.of("content","超限评论","submissionId",UUID.randomUUID().toString()),429,false);
        assertEquals(first,call(post(url).with(q->{q.setRemoteAddr("192.0.2.22");return q;}),body,201,false).path("id").asText());
        call(put("/api/admin/comments/"+first+"/review"),Map.of("status","APPROVED","version",1),200,true);
        call(put("/api/admin/articles/"+articleId+"/publication"),Map.of("status","DRAFT","version",1),200,true);
        call(get(url),null,404,false);call(post(url),body,404,false);
        call(put("/api/admin/articles/"+articleId+"/publication"),Map.of("status","PUBLISHED","version",2),200,true);
        assertEquals(1,call(get(url),null,200,false).path("total").asInt());
        call(delete("/api/admin/articles/"+articleId).param("version","3"),null,200,true);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM article_comment WHERE article_id=?",Integer.class,Long.valueOf(articleId)));
    }
    @Test void passwordChangeRevokesPreviousSessions() throws Exception {
        call(put("/api/auth/password"),Map.of("currentPassword","wrong","newPassword",PASSWORD+"New"),400,true);
        call(get("/api/auth/me"),null,200,true);
        call(put("/api/auth/password"),Map.of("currentPassword",PASSWORD,"newPassword",PASSWORD+"New"),200,true);
        call(get("/api/auth/me"),null,401,true);
        call(post("/api/auth/login"),Map.of("username","blog_test","password",PASSWORD),401,false);
        token=call(post("/api/auth/login"),Map.of("username","blog_test","password",PASSWORD+"New"),200,false).path("accessToken").asText();
        call(get("/api/auth/me"),null,200,true);
    }
    @Test void publicListsDetailsSearchAndArchivesHideDraftsAndPrivateArticles() throws Exception {
        var visible=create(input("可见手记","PUBLISHED","PUBLIC"));
        var draft=create(input("隐藏草稿","DRAFT","PUBLIC"));
        var secret=create(input("隐藏知识","PUBLISHED","PRIVATE"));
        var list=call(get("/api/articles"),null,200,false);
        assertEquals(1,list.path("total").asInt()); assertEquals(visible.path("id"),list.path("items").get(0).path("id"));
        assertTrue(list.path("items").get(0).path("content").isNull());
        call(get("/api/articles/"+draft.path("id").asText()),null,404,false);
        call(get("/api/articles/"+secret.path("id").asText()),null,404,false);
        assertEquals(0,call(get("/api/articles").param("keyword","隐藏"),null,200,false).path("total").asInt());
        var archives=call(get("/api/archives"),null,200,false);
        assertEquals(1,archives.get(0).path("articles").size());
        assertEquals(3,call(get("/api/admin/articles"),null,200,true).path("total").asInt());
    }
    @Test void staleSaveDoesNotOverwriteContentOrTaxonomy() throws Exception {
        var tag=taxonomy("tags","原标签");var b=input("原文","DRAFT","PUBLIC");b.put("tagIds",List.of(tag.path("id").asLong()));
        var article=create(b);String id=article.path("id").asText();b.put("version",1);b.put("title","新文");
        call(put("/api/admin/articles/"+id),b,200,true);
        b.put("title","旧页覆盖");b.put("tagIds",List.of());call(put("/api/admin/articles/"+id),b,409,true);
        var latest=call(get("/api/admin/articles/"+id),null,200,true);assertEquals("新文",latest.path("title").asText());assertEquals(1,latest.path("tags").size());assertEquals(2,latest.path("version").asInt());
    }
    @Test void taxonomyRenameUpdatesArticlesAndInUseDeletionIsRejected() throws Exception {
        var c=taxonomy("categories","分类一");var t=taxonomy("tags","标签一");
        var b=input("分类文章","DRAFT","PRIVATE");b.put("categoryId",c.path("id").asLong());b.put("tagIds",List.of(t.path("id").asLong()));var a=create(b);
        call(put("/api/admin/categories/"+c.path("id").asText()),Map.of("name","分类二","description","","sortOrder",1),200,true);
        assertEquals("分类二",call(get("/api/admin/articles/"+a.path("id").asText()),null,200,true).path("category").path("name").asText());
        call(delete("/api/admin/categories/"+c.path("id").asText()),null,409,true);
        call(delete("/api/admin/tags/"+t.path("id").asText()),null,409,true);
        call(delete("/api/admin/articles/"+a.path("id").asText()).param("version","1"),null,200,true);
        call(delete("/api/admin/categories/"+c.path("id").asText()),null,200,true);
        call(delete("/api/admin/tags/"+t.path("id").asText()),null,200,true);
    }
    @Test void filesArePrivateByDefaultAndSignedPreviewCannotReadAnotherFile() throws Exception {
        var a=upload("私人.txt","private".getBytes());var b=upload("另一个.txt","other".getBytes());
        mvc.perform(get(a.path("url").asText())).andExpect(status().isNotFound());
        mvc.perform(get(a.path("previewUrl").asText())).andExpect(status().isOk()).andExpect(content().string("private"));
        String ticket=a.path("previewUrl").asText().split("\\?ticket=")[1];
        mvc.perform(get(b.path("url").asText()).param("ticket",ticket)).andExpect(status().isNotFound());
        mvc.perform(get(a.path("url").asText()).param("ticket","1.invalid")).andExpect(status().isNotFound());
        mvc.perform(get(a.path("url").asText()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void publishedReferenceGrantsResourceAccessAndWithdrawalRevokesIt() throws Exception {
        var resource=upload("手记.md","file content".getBytes());var b=input("附件文章","DRAFT","PUBLIC");b.put("content","[附件]("+resource.path("url").asText()+")");var a=create(b);String id=a.path("id").asText();
        mvc.perform(get(resource.path("url").asText())).andExpect(status().isNotFound());
        call(put("/api/admin/articles/"+id+"/publication"),Map.of("status","PUBLISHED","version",1),200,true);
        mvc.perform(get(resource.path("url").asText())).andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment")));
        call(put("/api/admin/articles/"+id+"/publication"),Map.of("status","DRAFT","version",2),200,true);
        mvc.perform(get(resource.path("url").asText())).andExpect(status().isNotFound());
    }
}
