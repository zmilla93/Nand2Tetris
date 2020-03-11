package com.zrmiller.jackcompiler.enums;

public enum TokenType {

	KEYWORD("keyword"), SYMBOL("symbol"), IDENTIFIER("identifier"), INT_CONST("integerConstant"), STRING_CONST("stringConstant"), UNKNOWN("unknown");
	
	private String name;
	
	TokenType(String name){
		this.name = name;
	}
	
	public String toString(){
		return this.name;
	}

}
