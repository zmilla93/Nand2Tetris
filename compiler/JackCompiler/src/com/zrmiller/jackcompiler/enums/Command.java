package com.zrmiller.jackcompiler.enums;

public enum Command {

    ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT;


    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

}
