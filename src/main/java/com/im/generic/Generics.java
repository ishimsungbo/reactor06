package com.im.generic;

import java.util.List;

/**
 제네릭과 람다  <=== 자유롭게 사용할 수 있도록 익혀두어야 함.
 어노테이션 라이브러리, 프레임워크 만드는 개발자들이 주료 사용.
**/
public class Generics {

    /**
     * 타입 파라미터를 하나 추가함. 타입 베리어블
     * 왜 사용할까?
     * 1. 컴파일 시점에 타입을 정확하게 체크함. 에러의 확률을 줄인다.
     *
     */
    static class Hello<T> {  //type parameter
        //T t;
        //T method(T val) { return null;};
    }

    // 메서드에서 사용하기 위해서는 메서드명 앞에 type parameter 를 명시한다
    <T> void print(T v){
        System.out.println(v);
    }

    public static void main(String[] args) {
        new Generics().print("Generics");
        new Generics().print(1);

        new Hello<String>();  //type argument

    }




}
