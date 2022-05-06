package com.im.reactor06;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    @RestController
    public static class MainMyController{

        RestTemplate rt = new RestTemplate();
        
        @GetMapping("/rest")
        public String rest(int idx){
            String res = rt.getForObject("http://localhost:8081/service?req={req}",String.class,"hello"+idx);
            log.info("==> 8081를 호출합니다. " +idx);
            return res;
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
