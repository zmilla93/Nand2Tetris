package com.zrmiller.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Main {

	static String inputFile = "D:\\Programming\\Test Programs\\compile\\compile.asm";
	static String outputFile = "";
	static PrintWriter output;
	
	static boolean debugText = true;
	static int debugMaxLine = 999999;
	
	public static void main(String[] args){
		
		//FILE HANDLING
		if(args.length>0 || !inputFile.equals("")){
			if(args.length>0){
				inputFile = args[0];
			}
			File inputFile = new File(Main.inputFile);
			if(args.length>1){
				outputFile = args[1];
			}else if(inputFile.isFile()){
				outputFile = Main.inputFile.replaceAll("\\.asm", ".hack");
			}
			File outputFile = new File(Main.outputFile);
			if(outputFile.isFile()){
				System.out.println("[MAIN] \"" + Main.outputFile + " already exists. Overwriting previous file.");
			}
			try {
				output = new PrintWriter(Main.outputFile);
			} catch (FileNotFoundException e) {
				System.out.println("[MAIN] Invalid output file name : " + Main.outputFile);
			}
			
			// Create parser
			Parser p = new Parser(Main.inputFile);
			SymbolTable sym = new SymbolTable();
			System.out.println("Parsing...");
			
			// First Pass - Adds all symbols to symbol table
			System.out.println("First Pass...");
			while(p.hasMoreCommands()){
				p.advance();
				if(p.commandType() == CommandType.L){
					sym.addEntry(p.symbol(), p.lineCount());
				}
			}
			//Second Pass
			p = new Parser(Main.inputFile);
			System.out.println("Second Pass...");
			boolean lineBreak = false;
			while(p.hasMoreCommands()){
				p.advance();
				if(!p.rawText().equals("")){
					String s = "";
					if(debugText && p.lineCount()<=debugMaxLine){
						String debugD = p.dest()!=null ? p.dest() : "NULL";
						String debugC = p.comp()!=null ? p.comp() : "NULL";
						String debugJ = p.jump()!=null ? p.jump() : "NULL";
						String[] debugArr = {"("+ p.lineCount() +")", "-----", p.rawText(), p.commandType().toString(), debugD, debugC, debugJ};
						System.out.println(getFormattedLine(debugArr));
					}
					switch(p.commandType()){
					case A:
						if(p.symbol().matches("\\d+")){
							s = Integer.toBinaryString(Integer.parseInt(p.symbol()));
						}else{
							if(!sym.contains(p.symbol())){
								sym.addEntry(p.symbol());
							}
							int i = sym.getAddress(p.symbol());
							s = Integer.toBinaryString(i);
						}
						while(s.length()<16){
							s="0"+s;
						}
					case L:
						break;
					case C:
						s = "111" + Code.comp(p.comp()) + Code.dest(p.dest()) + Code.jump(p.jump());
						break;
					default:
						break;
					}
					if(!s.equals("")){
						output.write(s + "\n");
					}
				}
			}
			p.close();
			output.close();
			System.out.println("Parsing completed successfully!\nLine Count : " + p.lineCount());
		}
		System.out.println("Assembler Closed");
	}
	
	public static String getFormattedLine(String[] in){
		String line = "";
		int columnWidth = 10;
		int i;
		for(String s : in){
			line += s;
			i=s.length();
			while(i<columnWidth){
				line += " ";
				i++;
			}
		}
		return line;
	}
	
}
