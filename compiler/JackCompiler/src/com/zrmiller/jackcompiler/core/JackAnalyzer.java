package com.zrmiller.jackcompiler.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class JackAnalyzer {

    private final String source;
    private final boolean xmlDebug;
    private final boolean tokenDebug;
    private final boolean stackTrace;

    public JackAnalyzer(String source, boolean xmlDebug, boolean tokenDebug, boolean stackTrace) {
        this.source = source;
        this.xmlDebug = xmlDebug;
        this.tokenDebug = tokenDebug;
        this.stackTrace = stackTrace;
    }

    public void compile() {
        long time = System.currentTimeMillis();
        System.out.println("Compiling \"" + source + "\"...");
        final ArrayList<File> inputFiles = new ArrayList<>();
        File sourceFile = new File(source);
        if (!sourceFile.exists()) {
            Main.terminateEarly("Invalid source file \"" + sourceFile + "\".");
        } else if (sourceFile.isFile()) {
            if (!source.endsWith(".jack"))
                Main.terminateEarly("Compilation target must be a .jack file or a directory containing .jack files.");
            inputFiles.add(sourceFile);
        } else if (sourceFile.isDirectory()) {
            for (File f : Objects.requireNonNull(sourceFile.listFiles())) {
                if (f.getPath().endsWith(".jack")) {
                    inputFiles.add(f);
                }
            }
            if (inputFiles.size() == 0) Main.terminateEarly("Input directory contains no .jack files.");
        }

        //Compile each file
        for (File currentFile : inputFiles) {
            String fileName = currentFile.getName().replaceAll(".jack", "");
            JackTokenizer tokenizer;
            // Tokenizer Debug compilation
            if (tokenDebug) {
                System.out.println("\tGenerating token XML for " + currentFile.getPath() + "...");
                String tokenizerOutputPath = currentFile.getPath().replaceAll(currentFile.getName(), fileName + "Tokens.xml");
                File tokenizerOutput = new File(tokenizerOutputPath);
                tokenizer = new JackTokenizer(currentFile);
                FileWriter fw;
                try {
                    fw = new FileWriter(tokenizerOutput);
                    fw.write("<tokens>\n");
                    while (tokenizer.hasMoreTokens()) {
                        tokenizer.advance();
                        fw.write(tokenizer.toXML() + "\n");
                    }
                    fw.write("</tokens>\n");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /*
             *	XML Debug Compilation
             */

            if (xmlDebug) {
                System.out.println("\tGenerating debug XML for \"" + currentFile.getPath() + "\".");
                String outPathXML = currentFile.getPath().replaceAll(currentFile.getName(), fileName + "Compiled.xml");
                File outputXML = new File(outPathXML);
                //Run
                tokenizer = new JackTokenizer(currentFile);
                CompilationEngineXML engine = new CompilationEngineXML(tokenizer, outputXML);
                engine.compileClass();
                engine.close();
            }

            /*
             *  Actual Compilation
             */

            System.out.println("\tCompiling " + currentFile.getName() + "... ");
            String outPath = currentFile.getPath().replaceAll(currentFile.getName(), fileName + ".vm");
            File output = new File(outPath);
            tokenizer = new JackTokenizer(currentFile);
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer, output, stackTrace);
            compilationEngine.compileClass();
            if ((xmlDebug || tokenDebug) && inputFiles.size() > 1) System.out.println("\t----------");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("Compiled successfully in " + time + " milliseconds. ");
    }

}
