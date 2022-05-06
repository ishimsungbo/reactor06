package com.im.reactor06.pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test01")
    public String mainAddr(){
        return "Hello";
    }

}
