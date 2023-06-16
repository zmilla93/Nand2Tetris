package com.zrmiller.jackcompiler.data;

import com.zrmiller.jackcompiler.enums.Keyword;
import com.zrmiller.jackcompiler.enums.TokenType;

public class TokenSlim {

    private TokenType type;
    private String value;

    public TokenSlim(String value) {

    }

    public TokenSlim(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType type() {
        return type;
    }

    public String stringValue() {
        return value;
    }

    public Keyword keyword() {
        return Keyword.valueOf(value.toUpperCase());
    }

    public char symbol() {
        return value.charAt(0);
    }

    public int intValue() {
        return Integer.valueOf(value);
    }

}
