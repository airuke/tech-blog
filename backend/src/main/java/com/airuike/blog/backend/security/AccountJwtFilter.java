package com.airuike.blog.backend.security;
import com.airuike.blog.backend.common.ApiResponse;
import com.airuike.blog.backend.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
public class AccountJwtFilter extends OncePerRequestFilter {
    private final SysUserMapper users;private final ObjectMapper json;
    public AccountJwtFilter(SysUserMapper users,ObjectMapper json) { this.users=users;this.json=json; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof JwtAuthenticationToken jwt) {
            boolean valid=false;
            try {var user=users.selectById(Long.parseLong(jwt.getToken().getSubject()));
                Number version=jwt.getToken().getClaim("tokenVersion");
                valid=user!=null&&user.getStatus()==1&&version!=null&&user.getTokenVersion().longValue()==version.longValue();
            } catch(NumberFormatException|ClassCastException e) { valid=false; }
            if(!valid) {SecurityContextHolder.clearContext();response.setStatus(401);response.setContentType("application/json;charset=UTF-8");json.writeValue(response.getOutputStream(),ApiResponse.error(401,"登录已失效，请重新登录"));return;}
        }
        chain.doFilter(request,response);
    }
}
