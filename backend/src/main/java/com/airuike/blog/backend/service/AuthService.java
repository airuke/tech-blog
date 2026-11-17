package com.airuike.blog.backend.service;
import com.airuike.blog.backend.common.BlogException;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.entity.SysUser;
import com.airuike.blog.backend.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
@Service
public class AuthService {
    private final SysUserMapper users;private final PasswordEncoder password;private final AuthenticationManager manager;private final JwtEncoder jwt;
    private final String issuer;private final boolean setupEnabled;
    public AuthService(SysUserMapper users,PasswordEncoder password,AuthenticationManager manager,JwtEncoder jwt,
            @Value("${blog.jwt.issuer}") String issuer,@Value("${blog.setup.enabled:false}") boolean setupEnabled) {
        this.users=users;this.password=password;this.manager=manager;this.jwt=jwt;this.issuer=issuer;this.setupEnabled=setupEnabled;
    }
    public AuthStatus status(String address) { return new AuthStatus(users.selectCount(null)==0,setupEnabled&&loopback(address)); }
    private boolean loopback(String address) { return "127.0.0.1".equals(address)||"0:0:0:0:0:0:0:1".equals(address)||"::1".equals(address); }
    @Transactional public User setup(Setup body,String address) {
        if(!setupEnabled||!loopback(address)) throw new BlogException(403,"首次设置仅在本机开放");
        if(users.selectCount(null)>0) throw new BlogException(409,"博主账号已经创建，请直接登录");
        checkLength(body.password());
        var user=new SysUser();user.setId(1L);user.setUsername(body.username());user.setPasswordHash(password.encode(body.password()));
        String nickname=body.nickname()==null||body.nickname().isBlank()?body.username():body.nickname().strip();
        user.setNickname(nickname);user.setStatus(1);user.setTokenVersion(0L);users.insert(user);return view(user);
    }
    public Session login(Login body) {
        manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.username(),body.password()));
        var user=users.selectOne(new QueryWrapper<SysUser>().eq("username",body.username()));Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(user.getId().toString()).audience(java.util.List.of("blog-api"))
                .issuedAt(now).expiresAt(now.plusSeconds(7200)).claim("scope","BLOG_ADMIN").claim("tokenVersion",user.getTokenVersion()).build();
        String token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Session(token,"Bearer",7200,view(user));
    }
    public User current(Jwt jwt) { return view(require(jwt)); }
    public SysUser require(Jwt jwt) {
        if(jwt==null) throw new BlogException(401,"请先登录");
        var user=users.selectById(Long.parseLong(jwt.getSubject()));
        if(user==null||user.getStatus()!=1) throw new BlogException(401,"登录已失效");return user;
    }
    @Transactional public void changePassword(Jwt jwt,Password body) {
        var user=require(jwt);if(!password.matches(body.currentPassword(),user.getPasswordHash())) throw new BlogException(400,"当前密码不正确");
        checkLength(body.newPassword());users.update(null,new UpdateWrapper<SysUser>().eq("id",user.getId()).set("password_hash",password.encode(body.newPassword()))
                .setSql("token_version=token_version+1").set("updated_at",java.time.LocalDateTime.now()));
    }
    private void checkLength(String text) {if(text.getBytes(StandardCharsets.UTF_8).length>72) throw new BlogException(400,"密码 UTF-8 长度不能超过 72 字节");}
    private User view(SysUser user) { return new User(user.getId().toString(),user.getUsername(),user.getNickname()); }
}
