package com.zrmiller.vmtranslator;

import java.io.File;
import java.util.Objects;

public class Main {

    private static String fileInString = "";
    private static String fileOutString = "";
    private static int fileCount = 0;
    private static String[] fileInStringArray;
    private static boolean debug = false;

    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        System.out.println("Virtual machine started...");
        if (args.length > 0) fileInString = args[0];
        for (String s : args) {
            if (s.equals("-debug")) {
                debug = true;
                break;
            }
        }
        File fileIn = new File(fileInString);
        if (fileIn.isFile()) {
            if (!fileInString.matches(".+\\.vm\\z")) {
                terminateEarly("Input file must be a .vm file or a directory containing vm files.");
            }
            // Single file
            fileInStringArray = new String[1];
            fileInStringArray[0] = fileInString;
            fileOutString = fileInString.replaceAll("\\.vm", ".asm");
        } else if (fileIn.isDirectory()) {
            // Directory
            fileOutString = fileInString + "\\" + fileIn.getName() + ".asm";
            for (File f : Objects.requireNonNull(fileIn.listFiles())) {
                if (f.getName().matches(".+\\.vm\\z")) {
                    fileCount++;
                }
            }
            if (fileCount == 0) terminateEarly("No .vm files found in folder \"" + fileInString + "\".");
            fileInStringArray = new String[fileCount];
            int i = 0;
            for (File f : Objects.requireNonNull(fileIn.listFiles())) {
                if (f.getName().matches(".+\\.vm\\z")) {
                    fileInStringArray[i] = f.getAbsolutePath();
                    i++;
                }
            }
        } else {
            // Invalid input file/directory
            if (fileInString.equals("")) terminateEarly("No file or directory specified.");
            else terminateEarly("No file or directory found at \"" + fileInString + "\".");
        }
        CodeWriter code = new CodeWriter(fileOutString, false, false);
        for (String s : fileInStringArray) {
            System.out.println("\tParsing file \"" + s + "\"...");
            Parser p = new Parser(s);
            code.setFileName(s);
            while (p.hasMoreCommands()) {
                p.advance();
                if (!p.getRawText().equals("")) {
                    switch (p.commandType()) {
                        case C_PUSH:
                        case C_POP:
                            code.writePushPop(p.commandType(), p.arg1(), p.arg2());
                            break;
                        case C_ARITHMETIC:
                            code.writeArithmetic(p.arg1());
                            break;
                        case C_LABEL:
                            code.writeLabel(p.arg1());
                            break;
                        case C_GOTO:
                            code.writeGoto(p.arg1());
                            break;
                        case C_IF:
                            code.writeIf(p.arg1());
                            break;
                        case C_FUNCTION:
                            code.writeFunction(p.arg1(), p.arg2());
                            break;
                        case C_CALL:
                            code.writeCall(p.arg1(), p.arg2());
                            break;
                        case C_RETURN:
                            code.writeReturn();
                            break;
                        default:
                            System.out.println("Unregistered Command : " + p.commandType());
                    }
                    String[] debugText = {p.getRawText(), p.commandType().toString(), p.arg1(), p.arg2String()};
                    if (debug) System.out.println(debugText(debugText));
                }
            }
            if (debug) System.out.println("");
        }
        code.close();
        System.out.println("Output written to \"" + fileOutString + "\".");
        time = System.currentTimeMillis() - time;
        System.out.println("Virtual machine finished successfully in " + time + " milliseconds.");
    }

    private static String debugText(String[] text) {
        StringBuilder ret = new StringBuilder("\t\t - ");
        int columnWidth = 30;
        for (String s : text) {
            if (s == null) s = "NULL";
            ret.append(s);
            int length = s.length();
            while (length < columnWidth) {
                ret.append(" ");
                length++;
            }
        }
        return ret.toString();
    }

    private static void terminateEarly(String text) {
        System.out.println(text);
        System.out.println("Terminating virtaul machine.");
        System.exit(0);
    }

}
