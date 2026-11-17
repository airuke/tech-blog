package com.airuike.blog.backend.config;
import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.mapper.SysUserMapper;
import com.airuike.blog.backend.security.AccountJwtFilter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService users(SysUserMapper mapper) {
        return username->{var u=mapper.selectOne(new QueryWrapper<com.airuike.blog.backend.entity.SysUser>().eq("username",username));
            if(u==null) throw new UsernameNotFoundException("账号不存在");
            return User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles("BLOG_ADMIN").disabled(u.getStatus()!=1).build();};
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users,PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);
    }
    @Bean SecretKey jwtKey(@Value("${blog.jwt.secret-base64}") String text) {
        byte[] bytes=Base64.getDecoder().decode(text);if(bytes.length<32) throw new IllegalArgumentException("JWT密钥至少32字节");return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder encoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder decoder(SecretKey key,@Value("${blog.jwt.issuer}") String issuer) {
        var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims=jwt->jwt.getExpiresAt()!=null && jwt.getSubject()!=null
                && jwt.getAudience().contains("blog-api") && jwt.getClaim("tokenVersion")!=null
                ?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),claims));return decoder;
    }
    @Bean SecurityFilterChain chain(HttpSecurity http,SysUserMapper users,ObjectMapper json) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/error","/api/health","/api/auth/login","/api/auth/status","/api/auth/setup").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/site","/api/articles/**","/api/categories","/api/tags","/api/archives","/api/files/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/articles/*/comments").permitAll()
                .anyRequest().hasAuthority("SCOPE_BLOG_ADMIN"))
            .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->write(json,r,401,"请先登录博客管理台"))
                .accessDeniedHandler((q,r,x)->write(json,r,403,"没有访问权限")))
            .oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()).authenticationEntryPoint((q,r,x)->write(json,r,401,"登录已失效，请重新登录")))
            .addFilterAfter(new AccountJwtFilter(users,json),BearerTokenAuthenticationFilter.class);
        return http.build();
    }
    private static void write(ObjectMapper mapper,jakarta.servlet.http.HttpServletResponse r,int code,String message) throws java.io.IOException {
        r.setStatus(code);r.setContentType("application/json;charset=UTF-8");mapper.writeValue(r.getOutputStream(),ApiResponse.error(code,message));
    }
}
