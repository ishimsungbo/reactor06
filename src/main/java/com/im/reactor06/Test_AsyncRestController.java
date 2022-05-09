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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@RestController
@EnableAsync
public class Test_AsyncRestController {

        @Autowired MyService2 myService;

        RestTemplate rt = new RestTemplate();
        AsyncRestTemplate art = new AsyncRestTemplate();

        //네티용
        AsyncRestTemplate nrt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        /**
         * 1. 블럭킹으로 호출하기 때문에 8081 2초의 개별 호출시간을 기달려야 하는 상황이다
         */
        @GetMapping("/rest_blocking")
        public String rest_Blokcing(int idx){
            String res = rt.getForObject("http://localhost:8081/service?req={req}",String.class,"hello"+idx);
            log.info("==> 8081를 호출합니다. " +idx);
            return res;
        }

        /**
         * 2. 비동기로 변경 해보자 Callable, defferead 로는 만들기 힘듬.
         */
        @GetMapping("/rest_nonBlocking")
        public ListenableFuture<ResponseEntity<String>> restNonblocking(int idx) {
            return art.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
        }

    /**
     * 3. 네티를 이용한 비동기 작업으로 변경
     */
    @GetMapping("/rest1")
    public ListenableFuture<ResponseEntity<String>> restNetty01(int idx) {
        return nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
    }

    /**
     * 4. 네티 + 콜백 방식, 결과 값을 가공하고 반환 하는 방식
     */
    @GetMapping("/rest2")
    public DeferredResult<String> restNetty02(int idx) {

        ListenableFuture<ResponseEntity<String>> f1 = nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);

        DeferredResult<String> dr = new DeferredResult<>();

        //CallBack구조로 만든다. 콜백에서는 비동기작업이기에 예외는  DeferredResult 내부객체를 사용한다
        f1.addCallback(s->{
            dr.setResult(s.getBody() + "/work");
        }, e->{
            dr.setErrorResult(e.getMessage());
        });

        return dr;
    }

    /**
     * 5. 결과 값을 가공 후, 또 다시 다른 서비스를 호출 ==> 순차적 API
     * 회원가입, 이메일발송, 등등
     */
    @GetMapping("/rest3")
    public DeferredResult<String> restNetty03(int idx) {

        ListenableFuture<ResponseEntity<String>> f1 = nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);

        DeferredResult<String> dr = new DeferredResult<>();

        //CallBack구조로 만든다. 콜백에서는 비동기작업이기에 예외는  DeferredResult 내부객체를 사용한다
        f1.addCallback(s->{
            //중첩으로 하면 됨.
            ListenableFuture<ResponseEntity<String>> f2 = nrt.getForEntity("http://localhost:8081/service2?req={req}", String.class, s.getBody());
            f2.addCallback(s2->{
                dr.setResult(s2.getBody());
            }, e->{
                dr.setErrorResult(e.getMessage());
            });

        }, e->{
            dr.setErrorResult(e.getMessage());
        });

        return dr;
    }


    /**
     * 6. 결과 값을 가공 후, 또 다시 다른 서비스를 호출 ==> 순차적 API
     * 회원가입, 이메일발송, 등등
     * Add 스프링의 서비스를 달아보자
     */
    @GetMapping("/rest4")
    public DeferredResult<String> restNetty04(int idx) {

        ListenableFuture<ResponseEntity<String>> f1 = nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);

        DeferredResult<String> dr = new DeferredResult<>();

        f1.addCallback(s->{
            log.info("S1 CallBack");
            //중첩으로 하면 됨.
            ListenableFuture<ResponseEntity<String>> f2 = nrt.getForEntity("http://localhost:8081/service2?req={req}", String.class, s.getBody());
            f2.addCallback(s2->{
                log.info("S2 CallBack");
                   ListenableFuture<String> f3 = myService.work(s2.getBody());
                   f3.addCallback( s3 -> {
                       log.info("S3 CallBack");
                       dr.setResult(s3);
                   }, e ->{
                       dr.setErrorResult(e.getMessage());
                   });

            }, e->{
                dr.setErrorResult(e.getMessage());
            });

        }, e->{
            dr.setErrorResult(e.getMessage());
        });

        return dr;
    }


    /**
     * 7. 리팩터링 및 함수형 스타일로 바꾸어 본다.
     * 자바 8 이상으로는...어떻게?
     * Solution
     * 클래스를 하나 만들어서 아래를 분리한다. 중첩이 아닌 스트림식 구조로 변경 ==> CallBack Hell 구조인 상태, 에러의 중복 구조
     */
    @GetMapping("/rest5")
    public DeferredResult<String> restNetty05(int idx) {

        ListenableFuture<ResponseEntity<String>> f1 = nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
        DeferredResult<String> dr = new DeferredResult<>();

        f1.addCallback(s->{
            log.info("S1 CallBack");
            //중첩으로 하면 됨.
            ListenableFuture<ResponseEntity<String>> f2 = nrt.getForEntity("http://localhost:8081/service2?req={req}", String.class, s.getBody());
            f2.addCallback(s2->{
                log.info("S2 CallBack");
                ListenableFuture<String> f3 = myService.work(s2.getBody());
                f3.addCallback( s3 -> {
                    log.info("S3 CallBack");
                    dr.setResult(s3);
                }, e ->{
                    dr.setErrorResult(e.getMessage());
                });
            }, e->{
                dr.setErrorResult(e.getMessage());
            });

        }, e->{
            dr.setErrorResult(e.getMessage());
        });

        return dr;
    }


    /** -------------------------------------------------------------------------------------
     * --------------------------------------------------------------------------------------- **/

    @Service
    public static class MyService2 {
        @Async
        public ListenableFuture<String> work(String req){
            return new AsyncResult<>(req + "/asyncwork");
        }
    }


    @Bean
    public ThreadPoolTaskExecutor myTreadPool(){
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();
        return te;
    }

}


