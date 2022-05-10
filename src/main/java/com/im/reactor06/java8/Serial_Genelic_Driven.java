package com.im.reactor06.java8;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class Serial_Genelic_Driven {

    public static void main(String[] args) {

        // 소비...리턴 값이 없는 로직
        Consumer<String> cons = (s -> {
            log.info("Consumer 기능은 : " + s );
        });
        cons.accept("이런 겁니다");

        //객체를 사용해보기
        List<Integer> integers = Arrays.asList(1,2,3,4,5);

        Consumer<MyCompletion> myCompletionConsumer =(s-> {
            s.myPrint();
        });

        myCompletionConsumer.accept(new MyCompletion(integers));


    }


    public static class MyCompletion {

        private List<Integer> integers;

        public MyCompletion(List<Integer> integers) {
            this.integers = integers;
        }

        public void myPrint(){
            for(int i=0;i < integers.size();i++){
                log.info("Print Function {}",i);
            }
        }
    }

}
