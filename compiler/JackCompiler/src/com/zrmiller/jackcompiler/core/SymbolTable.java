package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.data.SymbolData;
import com.zrmiller.jackcompiler.enums.SymbolKind;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private HashMap<String, SymbolData> classHash = new HashMap<>();
    private HashMap<String, SymbolData> subroutineHash = new HashMap<>();

    private int staticCount;
    private int fieldCount;
    private int argCount;
    private int varCount;

//    String[] types = {"String", "int", "boolean"};

    public void startSubroutine() {
        argCount = 0;
        varCount = 0;
        subroutineHash = new HashMap<>();
    }

    public void define(String name, String type, SymbolKind kind) {
//        System.out.println("\t+SYMBOL\t(" + type + ") \t\t" + name + "\t\t" + kind);
        int index = varCount(kind);
        switch (kind) {
            case STATIC:
                staticCount++;
                classHash.put(name, new SymbolData(type, kind, index));
                break;
            case FIELD:
                fieldCount++;
                classHash.put(name, new SymbolData(type, kind, index));
                break;
            case ARG:
                argCount++;
                subroutineHash.put(name, new SymbolData(type, kind, index));
                break;
            case VAR:
                varCount++;
                subroutineHash.put(name, new SymbolData(type, kind, index));
                break;
        }
    }

    public int varCount(SymbolKind kind) {
        switch (kind) {
            case STATIC:
                return staticCount;
            case FIELD:
                return fieldCount;
            case ARG:
                return argCount;
            case VAR:
                return varCount;
        }
        return 0;
    }

    public SymbolKind kindOf(String name) {
        for (Map.Entry<String, SymbolData> entry : subroutineHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().kind;
            }
        }
        for (Map.Entry<String, SymbolData> entry : classHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().kind;
            }
        }
        return null;
    }

    public String typeOf(String name) {
        for (Map.Entry<String, SymbolData> entry : subroutineHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().type;
            }
        }
        for (Map.Entry<String, SymbolData> entry : classHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().type;
            }
        }
        return null;
    }

    public int indexOf(String name) {
        for (Map.Entry<String, SymbolData> entry : subroutineHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().index;
            }
        }
        for (Map.Entry<String, SymbolData> entry : classHash.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue().index;
            }
        }
        return -1;
    }

}
