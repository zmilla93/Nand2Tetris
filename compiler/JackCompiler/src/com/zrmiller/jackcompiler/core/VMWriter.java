package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.enums.Command;
import com.zrmiller.jackcompiler.enums.Segment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {

    FileWriter fw;

    public VMWriter(File outputFile){
        try {
            fw = new FileWriter(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePush(Segment segment, int index){
        write("push " + segment + " " + index);
    }

    public void writePop(Segment segment, int index){
        write("pop " + segment + " " + index);
    }

    public void writeArithmetic(Command command){
        write(command.toString());
    }

    public void writeLabel(String label){
        write("label " + label );
    }

    public void writeGoto(String label){
        write("goto " + label);
    }

    public void writeIf(String label){
        write("if-goto " + label);
    }

    public void writeCall(String name, int argCount){
        write("call " + name + " " + argCount);
    }

    public void writeFunction(String name, int localCount){
        write("function " + name + " " + localCount);
    }

    public void writeReturn(){
        write("return");
    }

    protected void close(){
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String s){
        try {
            fw.write(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
