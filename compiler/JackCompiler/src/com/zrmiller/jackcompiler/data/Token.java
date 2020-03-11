package com.zrmiller.jackcompiler.data;
import com.zrmiller.jackcompiler.enums.Keyword;
import com.zrmiller.jackcompiler.enums.TokenType;

import java.util.regex.Pattern;

public class Token {

    private String term;
    private TokenType tokenType;
    private Keyword keyword;
    private char symbol;

    // TODO : Might need to change string string to just checking if first and last character == quotation mark
    private Pattern patChar = Pattern.compile("[{}()\\[\\].,;+\\-*\\/&|<>=~]");
    private Pattern patConst = Pattern.compile("\\d+");
    private Pattern patString = Pattern.compile("\".+\"");
    private Pattern patIdentifier = Pattern.compile("[A-z_][\\w]*");

    public Token(String input) {
        this.term = input;
        if(patChar.matcher(input).matches()){
            this.tokenType = TokenType.SYMBOL;
            this.symbol = input.charAt(0);
            return;
        }
        for (Keyword k : Keyword.values()) {
            if (term.equals(k.toString())) {
                this.tokenType = TokenType.KEYWORD;
                this.keyword = k;
                return;
            }
        }
        if(patConst.matcher(input).matches()){
            this.tokenType = TokenType.INT_CONST;
            return;
        }
        if(patString.matcher(input).matches()){
            term = term.substring(1, term.length()-1);
            this.tokenType = TokenType.STRING_CONST;
            return;
        }
        if(patIdentifier.matcher(input).matches()){
            this.tokenType = TokenType.IDENTIFIER;
            return;
        }
        this.tokenType = TokenType.UNKNOWN;
    }

    public TokenType tokenType(){
        return this.tokenType;
    }

    public Keyword keyword(){
        return this.keyword;
    }

    public char symbol(){
        return this.symbol;
    }

    public String identifier(){
        return this.term;
    }

    public int intVal(){
        return Integer.parseInt(this.term);
    }

    public String toXML(){
        String text = term;
        if (text.length() == 1) {
            switch (text) {
                case "<":
                    text = "&lt;";
                    break;
                case ">":
                    text = "&gt;";
                    break;
                case "&":
                    text = "&amp;";
                    break;
            }
        }
        text = "<" + this.tokenType().toString() + "> " + text + " </" + this.tokenType().toString() + ">";
        return text;
    }

}
