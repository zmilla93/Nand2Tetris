package com.zrmiller.vmtranslator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Parser {

    private FileReader fr;
    private BufferedReader br;
    private String curLine = "";
    private String[] curLineArray;
    private CommandType cmdType;

    public Parser(String fileName) {
        try {
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("[PARSER] File not found \"" + fileName + "\"\nTerminating Virtual Machine");
            System.exit(0);
        }
        br = new BufferedReader(fr);
    }

    public boolean hasMoreCommands() {
        try {
            return br.ready();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void advance() {
        try {
            //Removes end of line comments and standardized all white spaces
            curLine = br.readLine().replaceAll("\\/\\/.*", "").replaceAll("(\\A\\s+|\\s+\\z)", "").replaceAll("\\s+", " ");
            curLineArray = curLine.split("[ ]+");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRawText() {
        return curLine;
    }

    public CommandType commandType() {
        switch (curLineArray[0]) {
            case "add":
            case "sub":
            case "neg":
            case "eq":
            case "gt":
            case "lt":
            case "and":
            case "or":
            case "not":
                cmdType = CommandType.C_ARITHMETIC;
                break;
            case "push":
                cmdType = CommandType.C_PUSH;
                break;
            case "pop":
                cmdType = CommandType.C_POP;
                break;
            case "label":
                cmdType = CommandType.C_LABEL;
                break;
            case "goto":
                cmdType = CommandType.C_GOTO;
                break;
            case "if-goto":
                cmdType = CommandType.C_IF;
                break;
            case "function":
                cmdType = CommandType.C_FUNCTION;
                break;
            case "return":
                cmdType = CommandType.C_RETURN;
                break;
            case "call":
                cmdType = CommandType.C_CALL;
                break;
            default:
                cmdType = CommandType.ERROR;
                break;
        }
        return cmdType;
    }

    public String arg1() {
        if (cmdType == CommandType.C_ARITHMETIC) {
            return curLineArray[0];
        }
        if (curLineArray.length < 2) return null;
        return curLineArray[1];
    }

    public int arg2() {
        return Integer.parseInt(curLineArray[2]);
    }

    public String arg2String() {
        if (curLineArray.length < 3) return null;
        return curLineArray[2];
    }

}
