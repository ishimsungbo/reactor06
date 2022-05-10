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
@Deprecated
public class AsyncRestController3 {

    private final String URL1 = "http://localhost:8081/service?req={req}";
    private final String URL2 = "http://localhost:8081/service2?req={req}";

    AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @Autowired MyService myService;

    @Bean
    public ThreadPoolTaskExecutor myTreadPool(){
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();
        return te;
    }

    @GetMapping("/rest")
    public DeferredResult<String> restNetty(int idx) {

        DeferredResult<String> dr = new DeferredResult<>();

        //직렬화, 체이닝
        Completion
                .from(rt.getForEntity(URL1, String.class, "hello" + idx))
                .andApply(s->  rt.getForEntity(URL2, String.class, s.getBody()))  // 받고 리턴이 필요하다.
                .andApply(s-> myService.work(s.getBody()))
                .andError(e -> dr.setErrorResult(e.toString()))
                .andAccept(s -> dr.setResult(s));   //위의 결과를 아래로 넘김  받은 값을 소비한다는 개념. 수행하고 끝낸다. Consumer<T>

        return dr;
    }

    public static class AcceptCompletion<S> extends Completion<S,Void> {
        Consumer<S> con;
        public AcceptCompletion(Consumer<S> con) {
            this.con = con;
        }

        @Override
        void run(S value) {
            con.accept(value);
        }
    }

    public static class ApplyCompletion<S, T> extends Completion<S, T> {

        public Function<S,ListenableFuture<T>> fn;

        public ApplyCompletion(Function<S ,ListenableFuture<T>> fn){
            this.fn = fn;
        }

        @Override
        void run(S value) {
             ListenableFuture<T> lf = fn.apply(value);
             lf.addCallback(s-> {  complete(s); },e->{ error(e);});
        }
    }

    public static class ErrorCompletion<T> extends Completion<T, T> {

        Consumer<Throwable> econ;

        public ErrorCompletion(Consumer<Throwable> econ) {
            this.econ = econ;
        }

        @Override
        void run(T value) {
            if(next != null) next.run(value);
        }

        @Override
        void error(Throwable e) {
            super.error(e);
        }
    }

    public static class Completion<S,T> {

        Completion next;

        //제네릭을 적용
        public void andAccept(Consumer<T> con){
            Completion<T, Void> c = new AcceptCompletion(con);
            this.next = c;
        }

        public Completion<T,T> andError(Consumer<Throwable> econ){
            Completion<T,T> c = new ErrorCompletion<>(econ);
            this.next = c;
            return c;
        }

        public <V> Completion<T, V> andApply(Function< T, ListenableFuture<V> > fn){
            Completion<T, V> c = new ApplyCompletion<>(fn);
            this.next = c;
            return c;
        }

        public static <S,T>Completion<S,T> from(ListenableFuture<T> lf) {
            Completion<S,T> c = new Completion<>();
            lf.addCallback(s->{
                c.complete(s);
            },e->{
                c.error(e);
            });
            return c;
        }

        void complete(T s) {
            if(next != null) next.run(s);
        }

        void run(S value) {
        }

        void error(Throwable e) {
            if(next != null ) next.error(e);
        }
    }

    @Service
    public static class MyService{
        @Async
        public ListenableFuture<String> work(String req){
            return new AsyncResult<>(req + "/NewAsyncwork");
        }
    }
}
