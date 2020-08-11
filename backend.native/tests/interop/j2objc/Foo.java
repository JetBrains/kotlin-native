package com.test;

public class Foo {
    public int add2(int firstparam, int secondparam) {
        return firstparam + secondparam;
    }
    public int returnNum(int x) {
        return x;
    }
    public int return100() {
        return 100;
    }
    public String returnString(String s) {
        return s;
    }
    static public int return100Static() {
        return 100;
    }

    // TODO: Add fields to this test after PR is merged
    class InnerClass{
        double myInnerFunc(double a, double b) {
            return a * b;
        }
    }

    static class NestedClass {
        double myNestedFunc(double a, double b) {
            return a * b;
        }
    }
}
