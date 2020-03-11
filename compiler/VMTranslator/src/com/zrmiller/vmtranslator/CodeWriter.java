package com.zrmiller.vmtranslator;

import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {

	//Set by parameters
	FileWriter output;
	boolean debug;
	boolean autoComment;
	boolean autoCollapse;

	//Internal variables
	private String fileName = "";
	private int lineCount = 0;
	private int eqCount = 0;
	private int gtCount = 0;
	private int ltCount = 0;
	private int callCount = 0;
	
	public CodeWriter(String fileName, boolean comment, boolean collapse){
		try {
			output = new FileWriter(fileName);
		} catch (IOException e) {
			System.out.println("[CODE WRTIER] Invalid file name \"" + fileName + "\"Terminating Virtual Machine");
			System.exit(0);
		}
		autoComment = comment;
		autoCollapse = collapse;
		writeInit();
	}
	
	private void writeInit(){
		autoComment("BOOTSTRAP CODE");
		writeLine("@256", "D=A", "@SP", "M=D");
		writeCall("Sys.init", 0);
		autoComment();
	}
	
	public void breakPoint(){
		autoComment("BREAKPOINT");
		writeLine("@255");
		writeLine("M=1");
		writeLine("M=0");
		autoComment();
	}
	
	public void setFileName(String fileName){
		this.fileName = fileName.replaceAll(".+\\\\", "").replaceAll(".vm", "");
	}
	
	public void writeArithmetic(String command) {
		autoComment("ARITHMETIC " + command.toUpperCase());
		switch(command){
		case "neg":
			writeLine("@0", "D=A", "@SP", "A=M-1", "M=D-M");
			break;
		case "not":
			writeLine("@SP", "A=M-1", "M=!M");
			break;
		case "add":
		case "sub":
			writeLine("@SP", "AM=M-1", "D=M", "A=A-1");
			if(command.equals("add")){
				writeLine("D=M+D");
			}
			else{
				writeLine("D=M-D");
			}
			writeLine("@SP", "A=M-1", "M=D");
			break;
		case "eq":
		case "gt":
		case "lt":
			writeLine("@SP", "AM=M-1", "D=M", "A=A-1", "D=D-M", "M=0");
			switch(command){
			case "eq":
				writeLine("@EQ" + eqCount, "D;JNE", "@SP", "A=M-1", "M=-1", "(EQ" + eqCount + ")");
				eqCount++;
				break;
			case "gt":
				writeLine("@GT" + gtCount, "D;JGE", "@SP", "A=M-1", "M=-1", "(GT" + gtCount + ")");
				gtCount++;
				break;
			case "lt":
				writeLine("@LT" + ltCount, "D;JLE", "@SP", "A=M-1", "M=-1", "(LT" + ltCount + ")");
				ltCount++;
				break;
			}
			break;
		case "and":
		case "or":
			writeLine("@SP", "AM=M-1", "D=M", "A=A-1");
			if(command.equals("and")){
				writeLine("D=D&M");
			}else {
				writeLine("D=D|M");
			}
			writeLine("@SP", "A=M-1", "M=D");
			break;
		}
		autoComment();
	}
	
	public void writePushPop(CommandType command, String segment, int index){
		autoComment(command.toString() + " " + segment + " " + index);
		switch(command){
		case C_PUSH:
			writePush(segment, index);
			break;
		case C_POP:
			writePop(segment, index);
			break;
		default:
			System.out.println("[CODE WRITER] Invalid push/pop command type - missing break statement? - " + command);
		}
		autoComment();
	}
	
	private void writePush(String segment, int index){
		//Put value to be pushed into D register
		switch(segment){
		case "constant":
			writeLine("@" + index, "D=A");
			break;
		case "local":
		case "argument":
		case "this":
		case "that":
			writeLine("@" + convertSegment(segment));
			if(index>0){
				writeLine("D=M", "@" + index, "A=D+A");
			}else if(index == 0){
				writeLine("A=M");
				
			}
			writeLine("D=M");
			break;
		case "temp":
			writeLine("@5");
			if(index>0){				
				writeLine("D=A", "@" + index, "A=D+A");
			}
			writeLine("D=M");
			break;
		case "pointer":
			if(index==0){
				writeLine("@THIS");
			}else if(index==1){
				writeLine("@THAT");
			}
			writeLine("D=M");
			break;
		case "static":
			writeLine("@" + fileName + "." + index, "D=M");
			break;
		}
		//Push value to stack
		writeLine("@SP", "A=M", "M=D", "@SP", "M=M+1");
	}	
	
	private void writePop(String segment, int index){
		//If Index > 0, Store address in R13
		if(index>0){
			switch(segment){
			case "local":
			case "argument":
			case "this":
			case "that":
				writeLine("@" + convertSegment(segment), "D=M", "@" + index, "D=D+A", "@R13", "M=D");
				break;
			case "temp":
				writeLine("@5", "D=A", "@" + index, "D=D+A", "@R13", "M=D");
				break;
			}
		}
		//Pop value to D register
		writeLine("@SP", "AM=M-1", "D=M");
		//Set A to segment
		switch(segment){
		case "local":
		case "argument":
		case "this":
		case "that":
			if(index == 0){
				writeLine("@" + convertSegment(segment));
			}else if(index>0){
				writeLine("@R13");
			}
			writeLine("A=M");
			break;
		case "temp":
			if(index==0){
				writeLine("@5");
			}else if(index>0){
				writeLine("@R13", "A=M");
			}
			break;
		case "pointer":
			if(index==0){
				writeLine("@THIS");
			}else if(index==1){
				writeLine("@THAT");
			}
			break;
		case "static":
			writeLine("@" + fileName + "." + index);
			break;
		}
		//Save Value
		writeLine("M=D");
	}
	
	private String convertSegment(String in){
		switch(in){
		case "local":
			return "LCL";
		case "argument":
			return "ARG";
		case "this":
			return "THIS";
		case "that":
			return "THAT";
		default:
			return "ERROR : " + in;
		}
	}
	
	public void writeLabel(String label){
		autoComment("LABEL", false);
		writeLine("("+label+")");
	}
	
	public void writeGoto(String label){
		autoComment("GOTO (" + label + ")");
		writeLine("@" + label, "0;JMP");
		autoComment();
	}
	
	public void writeIf(String label){
		autoComment("GOTO-IF (" + label + ")");
		writeLine("@SP", "AM=M-1", "D=M", "@" + label, "D;JNE");
		autoComment();
	}
	
	public void writeFunction(String functionName, int numLocals){
		//Label
		autoComment("DEFINE FUNCTION " + functionName);
		writeLine("(" + functionName + ")");
		//Reserve memory for local variables
		writeLine("@0", "D=A");
		for(int i=0;i<numLocals;i++){
			writeLine("@SP", "A=M", "M=D", "@SP", "M=M+1");
		}
		autoComment();
	}
	
	public void writeCall(String functionName, int numArgs){
		autoComment("WRITE CALL " + functionName + " " + numArgs);
		//PUSH RET_ADDRESS
		writeLine("@RET_ADDRESS_CALL" + callCount, "D=A", "@SP", "A=M", "M=D", "@SP", "M=M+1");
		String[] adrArr = {"@LCL", "@ARG", "@THIS", "@THAT"};
		//PUSH LCL, ARG, THIS, THAT
		for(String adr : adrArr){
			writeLine(adr, "D=M", "@SP", "A=M", "M=D", "@SP", "M=M+1");
		}
		//ARG = SP-N-5
		writeLine("@SP", "D=M", "@" + numArgs, "D=D-A", "@5", "D=D-A", "@ARG", "M=D");
		//LCL = SP
		writeLine("@SP", "D=M", "@LCL", "M=D");
		//GOTO F
		writeLine("@" + functionName, "0;JMP");
		//RETURN ADDRESS
		writeLine("(RET_ADDRESS_CALL" + callCount + ")");
		callCount++;
		autoComment();
	}

	//TODO : SHOULD R13/R14 BE R5/R6?
	public void writeReturn(){
		autoComment("WRITE RETURN");
		//FRAME(R13) = LCL
		writeLine("@LCL", "D=M", "@R13", "M=D");
		//RET(R14) = *(FRAME-5)
		writeLine("@5", "A=D-A", "D=M", "@R14", "M=D");
		//*ARG = pop()
		writeLine("@SP", "AM=M-1", "D=M", "@ARG", "A=M", "M=D");
		//SP = ARG + 1
		writeLine("@ARG", "D=M+1", "@SP", "M=D");
		//THAT = *(FRAME-1)
		//THIS = *(FRAME-2)
		//ARG = *(FRAME-3)
		//LCL = *(FRAME-4)
		String[] regArr = {"THAT", "THIS", "ARG", "LCL"};
		int dis = 1;
		for(String reg : regArr){
			writeLine("@R13", "D=M", "@" + dis, "A=D-A", "D=M", "@" + reg, "M=D");
			dis++;
		}
		//GOTO RET
		writeLine("@R14", "A=M", "0;JMP");
		autoComment();
	}
	
	public void close(){
		try {
			autoComment("END");
			writeLine("@END");
			writeLine("(END)");
			writeLine("0;JMP");
			autoComment();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 *	UTILITY
	 *
	 */
	
	private void writeLine(String ...lines){
		for(String ln : lines){
			writeModdedLine(ln);
		}
	}
	
	private void writeModdedLine(String line){
		int lineWidth = 25;
		boolean incLine = true;
		if(line.matches("\\A\\(.+\\)\\z")){
			incLine = false;
		}
		try {
			if(autoComment && incLine){
				String s = line;
				int i = line.length();
				while(i<lineWidth){
					s+= " ";
					i++;
				}
				output.write(s + "//" + lineCount + "\n");
				lineCount++;
			}else{
				output.write(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void autoComment(){
		if(autoComment && autoCollapse){
			try {
				output.write("// <<\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void autoComment(String comment){
		if(autoComment){
			try {
				if(autoCollapse){
					{
						output.write("//" + comment + " >>\n");
					}
				}else{
					output.write("//" + comment + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void autoComment(String comment, boolean collapse){
		if(autoComment){
			try {
				if(collapse){
					{
						output.write("//" + comment + " >>\n");
					}
				}else{
					output.write("//" + comment + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
