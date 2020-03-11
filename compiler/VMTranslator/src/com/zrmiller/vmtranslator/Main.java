package com.zrmiller.vmtranslator;

import java.io.File;
import java.io.IOException;

import javax.print.attribute.standard.PDLOverrideSupported;

public class Main {

	private static String fileInString = "D:\\Programming\\Test Programs\\compile";
	private static String fileOutString = "";
	private static int fileCount = 0;
	private static String[] fileInStringArray;
	
	public static void main(String[] args){
		
		System.out.println("VM STARTED...");
		File fileIn = new File(fileInString);
		if(fileIn.isFile() && fileInString.matches(".+\\.vm\\z")){
			fileInStringArray = new String[1];
			fileInStringArray[0] = fileInString;
			fileOutString = fileInString.replaceAll("\\.vm", ".asm");
		}else if(fileIn.isDirectory()){
			//System.out.println(fileIn.getName());
			fileOutString = fileInString + "\\" + fileIn.getName() + ".asm";
			for(File f : fileIn.listFiles()){
				if(f.getName().matches(".+\\.vm\\z")){
					fileCount++;
				}
			}
			fileInStringArray = new String[fileCount];
			int i = 0;
			for(File f : fileIn.listFiles()){
				if(f.getName().matches(".+\\.vm\\z")){
					fileInStringArray[i] = f.getAbsolutePath();
					i++;
				}
			}
		}else{
			System.out.println("[MAIN] No file or directory found at \"" + fileInString + "\"\nTerminating Virtual Machine");
			System.exit(0);
		}
		CodeWriter code = new CodeWriter(fileOutString, false, false);
		for(String s : fileInStringArray){
			System.out.println("\nPARSING NEW FILE... \"" + s + "\"...");
			Parser p = new Parser(s);
			code.setFileName(s);
			//TODO: move to Code
			while(p.hasMoreCommands()){
				p.advance();
				if(!p.getRawText().equals("")){
					switch(p.commandType()){
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
				String[] debug = {p.getRawText(), p.commandType().toString()};
				System.out.println(debugText(debug));
				//code.breakPoint();
				}
			}
		}
		code.close();

	}
	
	private static String debugText(String[] text){
		String ret = " ----- ";
		int columnWidth = 30;
		for(String s : text){
			ret += s;
			int i = s.length();
			while(i<columnWidth){
				ret += " ";
				i++;
			}
		}
		return ret;
	}

}
