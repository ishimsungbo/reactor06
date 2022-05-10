package com.im.generic;

import java.util.Arrays;
import java.util.List;

public class Generics1 {
    // Bounded type parameter  ==> 제한된?  타입에 제한을 두겠다.
    // 예) T extends List

//    static long countGreaterrThan(Integer[] arr, Integer elem){
//        return Arrays.stream(arr).filter(s-> s > elem).count();
//    }

    // Comparable이 없을 수도 있으니까 제한을 걸어야 한다.
    static <T extends Comparable<T>> long countGreaterrThan(T[] arr, T elem){
        return Arrays.stream(arr).filter(s-> s.compareTo(elem) > 0 ).count();
    }



    public static void main(String[] args) {
        //1. 숫자 비교인데...
        //Integer[] arr = new Integer[] {1,2,3,4,5,6,7,8,9,10};
        //System.out.println(countGreaterrThan(arr,4));

        //문자로 만들고 싶어짐.
        String[] arr = new String[] {"a","b","c","d","e"};

        System.out.println(countGreaterrThan(arr,"a"));
    }

    static <T extends Integer & List> void print(T t){};  //멀티플 타입 제한이 가능함.

}
