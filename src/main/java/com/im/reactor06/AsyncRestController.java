package com.im.reactor06;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@RestController
@EnableAsync
@Deprecated  //AsyncRestTemplate Webflux에서는 사용중지 권고가 됨.
public class AsyncRestController {

    private final String URL1 = "http://localhost:8081/service?req={req}";
    private final String URL2 = "http://localhost:8081/service2?req={req}";

    AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @Autowired MyService myService;

    @Service
    public static class MyService{
        @Async
        public ListenableFuture<String> work(String req){
            return new AsyncResult<>(req + "/NewAsyncwork");
        }
    }


    @GetMapping("/rest")
    public DeferredResult<String> restNetty(int idx) {

        DeferredResult<String> dr = new DeferredResult<>();

        //직렬화, 체이닝

        Completion
                .from(rt.getForEntity(URL1, String.class, "hello" + idx))
                .andApply(s->  rt.getForEntity(URL2, String.class, s.getBody()))  // 받고 리턴이 필요하다.
                .andAccept(s -> dr.setResult(s.getBody()));   //위의 결과를 아래로 넘김  받은 값을 소비한다는 개념. 수행하고 끝낸다. Consumer<T>

        return dr;
    }

    /**
     * 리팩터링 후
     */
    public static class Completion{

        Completion next;

        Consumer<ResponseEntity<String>> con;

        Completion(){};

        public Completion(Consumer<ResponseEntity<String>> con) {
            this.con = con;
        }

        public Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn;

        public Completion(Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn){
            this.fn = fn;
        }

        public Completion andApply(Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn){
            Completion c = new Completion(fn);
            this.next = c;
            return c;
        }

        public void andAccept(Consumer<ResponseEntity<String>> con){
            Completion c = new Completion(con); //2부째줄 from과 연결이 필요
            this.next = c;
        }

        public static Completion from(ListenableFuture<ResponseEntity<String>> lf) {
            Completion c = new Completion();
            lf.addCallback(s->{
                c.complete(s);
            },e->{
                c.error(e);
            });
            return c;
        }

        private void complete(ResponseEntity<String> s) {
            if(next != null) next.run(s);
        }

        private void run(ResponseEntity<String> value) {
            if(con != null) con.accept(value);
            else if (fn != null){
                ListenableFuture<ResponseEntity<String>> lf = fn.apply(value);
                lf.addCallback(s->{ complete(s); },e->{error(e); });
            }
        }

        private void error(Throwable e) {

        }
    }
}
