package cn.liuyang.desk;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.context.annotation.*;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class HttpClients {
    @Bean RestClientCustomizer timeouts(){return builder->{var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(95));builder.requestFactory(factory);};}
}
