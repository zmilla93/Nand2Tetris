package com.zrmiller.jackcompiler.core;

public class Main {

    public static void main(String[] args) {
        System.out.println("Compiler started.");
        if (args.length < 1) terminateEarly("Must specify file or directory to be compiled.");
        String directory = args[0];
        boolean xmlDebug = false;
        boolean tokenDebug = false;
        for (String s : args) {
            if (s.equals("-xml")) xmlDebug = true;
            if (s.equals("-tokens")) tokenDebug = true;
        }
        JackAnalyzer analyzer = new JackAnalyzer(directory, xmlDebug, tokenDebug);
        analyzer.compile();
    }

    public static void terminateEarly(String text) {
        System.out.println(text);
        System.out.println("Compiler terminated.");
        System.exit(0);
    }

}
