package com.zrmiller.assembler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	private FileReader fr;
	private BufferedReader br;
	
	private String curLine = "";
	private int lineCount = 0;
	
	private String symbol;
	private String dest;
	private String comp;
	private String jmp;
	private CommandType cmdType;
    private Matcher match;

	// A Pattern - Digit or Symbol not starting with a digit
	private Pattern aPattern = Pattern.compile("\\A@(\\d+|[A-z_.$:][\\w.$:]*)\\z");

	// C Pattern - Dest=Comp;Jump
	private Pattern cPattern = Pattern.compile("\\A((A?M?D?)=)?(!?[AMD|\\-?\\d+][+\\-&|]?[AMD|\\d]?)(;(JGT|JEQ|JGE|JLT|JNE|JLE|JMP))?\\z");

	// L Pattern - Symbol not starting with a digit in parentheses
	private Pattern lPattern = Pattern.compile("\\A\\(([A-z_.$:][\\w.$:]*)\\)\\z");

	public Parser(String fileName){
		try {
			fr = new FileReader(fileName);
		} catch (FileNotFoundException e) {
			System.out.println("[PARSER ERROR] File not found : \"" + fileName + "\"");
			System.out.println("Program Terminated");			
			System.exit(0);
		}
		br = new BufferedReader(fr);
	}

    /**
     * Parses the next line of the input file and exposes relevant information.
     * Should only be called when hasMoreCommands returns true.
     */
	public void advance(){
		try {
			//Get current line while removing all white spaces and comments
			curLine = br.readLine().replaceAll("(\\s+)?(\\/\\/.*)?", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(curLine.length()==0){
			return;
		}
		symbol = "";
		dest = "";
		comp = "";
		jmp =  "";
		cmdType = CommandType.ERR;
		match = aPattern.matcher(curLine);
		if(match.matches()){
			cmdType = CommandType.A;
			symbol = match.group(1);
			lineCount++;
			return;
		}
		match = cPattern.matcher(curLine);
		if(match.matches()){
			cmdType = CommandType.C;
			dest = match.group(2) != null ? match.group(2) : "000";
			comp = match.group(3);
			jmp = match.group(5) != null ? match.group(5) : "000";
			lineCount++;
			return;
		}
		match = lPattern.matcher(curLine);
		if(match.matches()){
			cmdType = CommandType.L;
			symbol = match.group(1);
			return;
		}
		System.out.println("[PARSER ERROR] Could not parse line : " + curLine);
	}

	public boolean hasMoreCommands(){
		try {
			return br.ready();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void close(){
		try {
			br.close();
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}
	
	public String rawText(){
		return curLine;
	}
	
	public int lineCount(){
		return lineCount;
	}

	public CommandType commandType(){
		return cmdType;
	}
	
	public String symbol(){
		return symbol;
	}
	
	public String dest(){
		return dest;
	}
	
	public String comp(){
		return comp;
	}
	
	public String jump(){
		return jmp;
	}
	
}
