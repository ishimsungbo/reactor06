package com.im.reactor06;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
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
@Deprecated
public class AsyncRestController2 {

    private final String URL1 = "http://localhost:8081/service?req={req}";
    private final String URL2 = "http://localhost:8081/service2?req={req}";

    AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @GetMapping("/rest")
    public DeferredResult<String> restNetty(int idx) {

        DeferredResult<String> dr = new DeferredResult<>();

        //직렬화, 체이닝
        Completion
                .from(rt.getForEntity(URL1, String.class, "hello" + idx))
                .andApply(s->  rt.getForEntity(URL2, String.class, s.getBody()))  // 받고 리턴이 필요하다.
                .andError(e -> dr.setErrorResult(e.toString()))
                .andAccept(s -> dr.setResult(s.getBody()));   //위의 결과를 아래로 넘김  받은 값을 소비한다는 개념. 수행하고 끝낸다. Consumer<T>

        return dr;
    }

    public static class AcceptCompletion extends Completion {
        Consumer<ResponseEntity<String>> con;
        public AcceptCompletion(Consumer<ResponseEntity<String>> con) {
            this.con = con;
        }

        @Override
        void run(ResponseEntity<String> value) {
            con.accept(value);
        }
    }

    public static class ApplyCompletion extends Completion {

        public Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn;

        public ApplyCompletion(Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn){
            this.fn = fn;
        }

        @Override
        void run(ResponseEntity<String> value) {
             ListenableFuture<ResponseEntity<String>> lf = fn.apply(value);
             lf.addCallback(s-> {  complete(s); },e->{ error(e);});
        }
    }

    public static class ErrorCompletion extends Completion {

        Consumer<Throwable> econ;

        public ErrorCompletion(Consumer<Throwable> econ) {
            this.econ = econ;
        }

        @Override
        void run(ResponseEntity<String> value) {
            if(next != null) next.run(value);
        }

        @Override
        void error(Throwable e) {
            super.error(e);
        }
    }

    public static class Completion{

        Completion next;

        public void andAccept(Consumer<ResponseEntity<String>> con){
            Completion c = new AcceptCompletion(con);
            this.next = c;
        }

        public Completion andError(Consumer<Throwable> econ){
            Completion c = new ErrorCompletion(econ);
            this.next = c;
            return c;
        }

        public Completion andApply(Function<ResponseEntity<String>,ListenableFuture<ResponseEntity<String>>> fn){
            Completion c = new ApplyCompletion(fn);
            this.next = c;
            return c;
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

        void complete(ResponseEntity<String> s) {
            if(next != null) next.run(s);
        }

        void run(ResponseEntity<String> value) {
        }

        void error(Throwable e) {
            if(next != null ) next.error(e);
        }
    }
}
