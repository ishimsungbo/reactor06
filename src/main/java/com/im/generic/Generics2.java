package com.im.generic;

import java.util.ArrayList;
import java.util.List;

/**
 * 제네릭과 상속
 */
public class Generics2 {

    List<?> list;  //와일드 카드  관심없음, 모른다.  사이즈클리어...


    List<? extends Object> list2 ; //오브젝트에 있는 기능만 쓸꺼얍.

    public static void main(String[] args) {

    }


}
