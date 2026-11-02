package cn.liuyang.desk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final Store s; private final Actor actor; private final PasswordEncoder passwords; private final HttpSessionCsrfTokenRepository csrf;
    private final ConcurrentHashMap<String,long[]> failures = new ConcurrentHashMap<>();
    private final String dummy;
    public AuthController(Store s,Actor actor,PasswordEncoder passwords,HttpSessionCsrfTokenRepository csrf){this.s=s;this.actor=actor;this.passwords=passwords;this.csrf=csrf;dummy=passwords.encode("timing-mitigation-only");}
    record Login(@NotBlank @Size(max=50) String username,@NotBlank @Size(max=100) String password){}
    @GetMapping("/auth/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
    @GetMapping("/auth/me") Object me(){return actor.current();}
    @PostMapping("/auth/login") Object login(@Valid @RequestBody Login input,HttpServletRequest request,HttpServletResponse response){
        String key=request.getRemoteAddr()+":"+input.username(); long time=System.currentTimeMillis();
        synchronized(failures){
            failures.entrySet().removeIf(e->time-e.getValue()[1]>60000);
            var previous=failures.get(key); if(previous!=null && previous[0]>=8)throw new ApiException(429,"登录尝试过多，请一分钟后重试");
            if(failures.size()>10000)throw new ApiException(429,"登录繁忙，请稍后重试");
        }
        var users=s.db.queryForList("SELECT * FROM app_users WHERE username=?",input.username());
        boolean valid=passwords.matches(input.password(),users.isEmpty()?dummy:(String)users.getFirst().get("password_hash"));
        if(!valid || users.isEmpty()){synchronized(failures){failures.compute(key,(k,v)->new long[]{v==null?1:v[0]+1,time});}throw new ApiException(401,"用户名或密码错误");}
        failures.remove(key); var user=users.getFirst();
        request.getSession();request.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.get("username"),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.get("role")))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,request,response);csrf.saveToken(null,request,response);
        s.audit((String)user.get("id"),"LOGIN",null,"用户登录");return actor.current();
    }
    @GetMapping("/meta") Object meta(@Value("${desk.demo}")boolean demo){return Map.of("name","知序客服工作台","version","1.0.0","demo",demo);}
}
