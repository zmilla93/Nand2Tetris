package com.zrmiller.jackcompiler.enums;

public enum Segment {

    CONST("constant"), ARG("argument"), LOCAL("local"), STATIC("static"),
    THIS("this"), THAT("that"), POINTER("pointer"), TEMP("temp"), UNKNOWN("UNKNOWN");

    private String name;

    Segment(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return this.name;
    }

}
