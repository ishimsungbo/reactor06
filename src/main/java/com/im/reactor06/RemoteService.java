package com.im.reactor06;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Slf4j
public class RemoteService {

    @RestController
    public static class MyController{

        @GetMapping("/service")
        public String service(String req) throws InterruptedException{
            log.info(" Call Service1 " + req);
            Thread.sleep(2000);
            return req + "/service1";
        }


        @GetMapping("/service2")
        public String service2(String req) throws InterruptedException{
            log.info(" Call Service2 " + req);
            Thread.sleep(2000);
            return req + "/service2";
        }
    }

    public static void main(String[] args) {
        System.setProperty("server.port","8081");
        System.setProperty("server.tomcat.threads.max","1000");
        SpringApplication.run(RemoteService.class, args);
    }

}
