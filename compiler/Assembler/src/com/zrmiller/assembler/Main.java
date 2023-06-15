package com.zrmiller.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Main {

    static String inputFile = "";
    static String outputFile = "";
    static PrintWriter output;

    static boolean debugText = false;

    public static void main(String[] args) {

        // Argument handling
        if (args.length > 0 || !inputFile.equals("")) {
            // Input file
            if (args.length > 0) {
                inputFile = args[0];
            }
            File inputFile = new File(Main.inputFile);
            // Output file
            if (args.length > 1 && !args[1].equals("-debug")) {
                outputFile = args[1];
                if (!outputFile.endsWith(".hack")) {
                    System.out.println("Output file must be a .hack file! Assembly aborted.");
                    System.exit(0);
                }
            } else if (inputFile.isFile()) {
                outputFile = Main.inputFile.replaceAll("\\.asm", ".hack");
            }
            File outputFile = new File(Main.outputFile);
            if (outputFile.isFile()) {
                System.out.println("Output file \"" + Main.outputFile + "\" already exists. Overwriting previous file.");
            }
            try {
                output = new PrintWriter(Main.outputFile);
            } catch (FileNotFoundException e) {
                System.out.println("Invalid output file name : " + Main.outputFile);
            }
            // Debug text
            if (args.length > 1) {
                String target = args[1];
                if (args.length > 2) target = args[2];
                if (target.equals("-debug")) debugText = true;
            }
        } else {
            System.out.println("No input file specified! Assembly aborted.");
            System.exit(0);
        }

        // Create parser
        Parser p = new Parser(Main.inputFile);
        SymbolTable sym = new SymbolTable();
        System.out.println("Assembler started...");

        // First Pass - Adds all symbols to symbol table
        System.out.println("First pass, adding symbols to symbol table...");
        while (p.hasMoreCommands()) {
            p.advance();
            if (p.commandType() == CommandType.L) {
                sym.addEntry(p.symbol(), p.lineCount());
                if (debugText) System.out.println(" --- " + p.symbol());
            }
        }

        // Second Pass
        p = new Parser(Main.inputFile);
        System.out.println("Second pass, generating machine code...");
        if (debugText) {
            System.out.println(getFormattedLine(new String[]{"Line", "Text", "Type", "Dest", "Comp", "Jump"}));
            System.out.println(getFormattedLine(new String[]{"----", "----", "----", "----", "----", "----"}));
        }
        while (p.hasMoreCommands()) {
            p.advance();
            if (!p.rawText().equals("")) {
                StringBuilder line = new StringBuilder();
                if (debugText) {
                    String debugD = p.dest() != null ? p.dest() : "NULL";
                    String debugC = p.comp() != null ? p.comp() : "NULL";
                    String debugJ = p.jump() != null ? p.jump() : "NULL";
                    String[] debugArr = {"(" + p.lineCount() + ")", p.rawText(), p.commandType().toString(), debugD, debugC, debugJ};
                    System.out.println(getFormattedLine(debugArr));
                }
                switch (p.commandType()) {
                    case A:
                        if (p.symbol().matches("\\d+")) {
                            line.append(Integer.toBinaryString(Integer.parseInt(p.symbol())));
                        } else {
                            if (!sym.contains(p.symbol())) {
                                sym.addEntry(p.symbol());
                            }
                            int i = sym.getAddress(p.symbol());
                            line.append(Integer.toBinaryString(i));
                        }
                        while (line.length() < 16) {
                            line.insert(0, "0");
                        }
                        break;
                    case C:
                        line = new StringBuilder("111" + Code.comp(p.comp()) + Code.dest(p.dest()) + Code.jump(p.jump()));
                        break;
                    case L: // L commands are symbols and do not produce any machine code
                    default:
                        break;
                }
                if (!line.toString().equals("")) {
                    output.write(line + "\n");
                }
            }
        }
        p.close();
        output.close();
        System.out.println("Line count : " + p.lineCount());
        System.out.println("Assembly completed successfully!");

    }

    public static String getFormattedLine(String[] in) {
        StringBuilder line = new StringBuilder();
        int columnWidth = 10;
        int symbolColumnWidth = 20;
        int width;
        for (int i = 0; i < in.length; i++) {
            String s = in[i].equals("") ? "-" : in[i];
            line.append(s);
            width = s.length();
            int targetWidth = i == 1 ? symbolColumnWidth : columnWidth;
            while (width < targetWidth) {
                line.append(" ");
                width++;
            }
        }
        return line.toString();
    }

}
