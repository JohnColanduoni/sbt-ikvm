package test;

import ikvm.lang.*;

public class Main {
    public static void main(String[] args) {
        System.out.println(CIL.unbox_int(CIL.box_int(1)));
    }
}