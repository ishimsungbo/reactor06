package com.im.reactor06;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@RestController
public class Test_AsyncRestController {

        RestTemplate rt = new RestTemplate();
        AsyncRestTemplate art = new AsyncRestTemplate();

        //네티용
        AsyncRestTemplate nrt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        /**
         블럭킹으로 호출하기 때문에 8081 2초의 개별 호출시간을 기달려야 하는 상황이다
         */
        @GetMapping("/rest_blocking")
        public String rest_Blokcing(int idx){
            String res = rt.getForObject("http://localhost:8081/service?req={req}",String.class,"hello"+idx);
            log.info("==> 8081를 호출합니다. " +idx);
            return res;
        }

        /**
         * 비동기로 변경 해보자 Callable, defferead 로는 만들기 힘듬.
         */
        @GetMapping("/rest_nonBlocking")
        public ListenableFuture<ResponseEntity<String>> restNonblocking(int idx) {
            return art.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
        }

    @GetMapping("/rest1")
    public ListenableFuture<ResponseEntity<String>> restNetty01(int idx) {
        return nrt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
    }

    /**
     * 결과 값을 가공하고 반환 하는 방식
     */
    @GetMapping("/rest")
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


}
