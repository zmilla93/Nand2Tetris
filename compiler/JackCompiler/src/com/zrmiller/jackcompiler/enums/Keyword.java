package com.zrmiller.jackcompiler.enums;

public enum Keyword {

	CLASS(), CONSTRUCTOR(), FUNCTION(),
	METHOD(), FIELD(), STATIC(), VAR(),
	INT(), CHAR(), BOOLEAN(), VOID(), TRUE(),
	FALSE(), NULL(), THIS(), LET(), DO(),
	IF(), ELSE(), WHILE(), RETURN ();

	@Override
	public String toString(){
		return this.name().toLowerCase();
	}
}
