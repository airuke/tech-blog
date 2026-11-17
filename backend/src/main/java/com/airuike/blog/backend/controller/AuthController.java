package com.airuike.blog.backend.controller;
import com.airuike.blog.backend.common.*;
import com.airuike.blog.backend.dto.BlogDtos.*;
import com.airuike.blog.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>();
    private record Attempts(int count,long expires) {}
    public AuthController(AuthService auth) { this.auth=auth; }
    @GetMapping("/status") public ApiResponse<AuthStatus> status(HttpServletRequest q) {return ApiResponse.ok(auth.status(q.getRemoteAddr()));}
    @PostMapping("/setup") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<User> setup(@Valid @RequestBody Setup b,HttpServletRequest q) {return ApiResponse.ok(auth.setup(b,q.getRemoteAddr()));}
    @PostMapping("/login") public ApiResponse<Session> login(@Valid @RequestBody Login b,HttpServletRequest q) {
        long now=System.currentTimeMillis();attempts.entrySet().removeIf(x->x.getValue().expires()<now);
        String key=q.getRemoteAddr()+":"+b.username();var past=attempts.get(key);
        if(past!=null&&past.count()>=8 || attempts.size()>=2000) throw new BlogException(429,"登录尝试较多，请 15 分钟后再试");
        try {var session=auth.login(b);attempts.remove(key);return ApiResponse.ok(session);}
        catch(AuthenticationException e) {attempts.compute(key,(k,v)->new Attempts(v==null?1:v.count()+1,now+900000));throw new BlogException(401,"用户名或密码不正确");}
    }
    @GetMapping("/me") public ApiResponse<User> me(@AuthenticationPrincipal Jwt jwt) {return ApiResponse.ok(auth.current(jwt));}
    @PutMapping("/password") public ApiResponse<Void> password(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Password b) {auth.changePassword(jwt,b);return ApiResponse.ok(null);}
}
