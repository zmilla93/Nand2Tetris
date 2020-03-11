package com.zrmiller.jackcompiler.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class JackAnalyzer {

    public JackAnalyzer(String source) {

        long time = System.currentTimeMillis();

        final ArrayList<File> inputFiles = new ArrayList<>();
        // Debug Flags
        boolean debugTokenizer = false;

        System.out.println("Compiling \"" + source + "\"...");
        File sourceFile = new File(source);
        if (!sourceFile.exists()) {
            System.out.println("[Analyzer] ERROR : Invalid source");
            return;
        } else if (sourceFile.isFile()) {
            inputFiles.add(sourceFile);
        } else if (sourceFile.isDirectory()) {
            for (File f : sourceFile.listFiles()) {
                if (f.getPath().endsWith(".jack")) {
                    inputFiles.add(f);
                }
            }
        }

        //Compile each file
        for (File currentFile : inputFiles) {

            String fileName = currentFile.getName().replaceAll(".jack", "");

            // Tokenizer Debugging
            if (debugTokenizer) {
                System.out.println("[Analyzer] [TOKENIZER DEBUG] - Generating tokens for " + currentFile.getPath() + "...");
                String tokenizerOutputPath = currentFile.getPath().replaceAll(currentFile.getName(), fileName + "Tokens.xml");
                File tokenizerOutput = new File(tokenizerOutputPath);
                JackTokenizer tokenizer = new JackTokenizer(currentFile);
                FileWriter fw;
                try {
                    fw = new FileWriter(tokenizerOutput);
                    fw.write("<tokens>\n");
                    while (tokenizer.hasMoreTokens()) {
                        tokenizer.advance();
//                        System.out.println("TOKEN ::: " + tokenizer.getCurrentToken().identifier());
                        fw.write(tokenizer.toXML() + "\n");
                    }
                    System.out.println("[Analyzer] [TOKENIZER DEBUG] - Total line count: " + tokenizer.getLineCount());
                    fw.write("</tokens>\n");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            /*
             *		COMPILATION ENGINE XML DEBUGGING
             */

            //Start

            String outPathXML = currentFile.getPath().replaceAll(currentFile.getName(), fileName + "CompiledXML.xml");
            File outputXML = new File(outPathXML);

            //Run
            JackTokenizer tokenizer = new JackTokenizer(currentFile);
//            CompilationEngine_XML engine = new CompilationEngine_XML(tokenizer, outputXML);
//            engine.compileClass();
//            engine.close();


            /*
             *  Actual Compilation
             */

            System.out.println("\tCompiling " + currentFile.getName() + "... ");
            String outPath = currentFile.getPath().replaceAll(currentFile.getName(), fileName + ".vm");
            File output = new File(outPath);
            tokenizer = new JackTokenizer(currentFile);
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer, output);
            compilationEngine.compileClass();


        }
        time = System.currentTimeMillis() - time;
        System.out.println("Compiled successfully in  " + time + " milliseconds. ");

    }

}
