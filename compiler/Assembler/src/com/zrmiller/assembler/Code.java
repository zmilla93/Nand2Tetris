package com.zrmiller.assembler;

public class Code {
	
	/**
	 * Method for generating destination binary code
	 * @param d Dest portion of HACK assembly code
	 * @return 3 bits of machine code
	 */
	public static String dest(String d){
		String s = "";
		s += d.contains("A") ? "1" : "0";
		s += d.contains("D") ? "1" : "0";
		s += d.contains("M") ? "1" : "0";
		return s;
	}
	
	/**
	 * Method for generating computation binary code
	 * @param in Comp portion of HACK assembly code
	 * @return 7 bits of machine code
	 */
	public static String comp(String in){
		String s = in;
		String a = "0";
		String r = "";
		if(in.contains("A")){s=s.replaceAll("A","X");}
		if(in.contains("M")){s=s.replaceAll("M","X");a="1";}
		switch(s){
		case "0":
			r = "101010";
			break;
		case "1":
			r = "111111";
			break;
		case "-1":
			r = "111010";
			break;
		case "D":
			r = "001100";
			break;
		case "X":
			r = "110000";
			break;
		case "!D":
			r = "001101";
			break;
		case "!X":
			r = "110001";
			break;
		case "-D":
			r = "001111";
			break;
		case "-X":
			r = "110011";
			break;
		case "D+1":
			r = "011111";
			break;
		case "X+1":
			r = "110111";
			break;
		case "D-1":
			r = "001110";
			break;
		case "X-1":
			r = "110010";
			break;
		case "D+X":
			r = "000010";
			break;
		case "D-X":
			r = "010011";
			break;
		case "X-D":
			r = "000111";
			break;
		case "D&X":
			r = "000000";
			break;
		case "D|X":
			r = "010101";
			break;
		}
		return a+r;
	}
	
	/**
	 * Method for generating jump binary code
	 * @param j Jmp portion of HACK assembly code
	 * @return 3 bits of machine code
	 */
	public static String jump(String j){
		switch(j){
		case "JGT":
			return "001";
		case "JEQ":
			return "010";
		case "JGE":
			return "011";
		case "JLT":
			return "100";
		case "JNE":
			return "101";
		case "JLE":
			return "110";
		case "JMP":
			return "111";
		default:
			return "000";
		}
	}
	
	
}
