package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.data.Token;
import com.zrmiller.jackcompiler.enums.Keyword;
import com.zrmiller.jackcompiler.enums.SymbolKind;
import com.zrmiller.jackcompiler.enums.TokenType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * This file outputs the Abstract Syntax Tree (AST) of the program in XML format.
 * This was written as a learning exercise before the actual compiler, so it skips over some of the steps necessary in actual compilation.
 * Refer to 'CompilationEngine.java' to see the full compilation process.
 */

public class CompilationEngine_XML {

    //File stuff
    private JackTokenizer tokenizer;
    private FileWriter fw;
    private int indent = 0;

    //Keyword, token, and char groups
    private final Keyword[] types = {Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN};
    private final Keyword[] statement = {Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN};
    private final Keyword[] subroutineKeywords = {Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD};
    private final Keyword[] classVarDecKeywords = {Keyword.STATIC, Keyword.FIELD};
    private final Keyword[] termKeywords = {Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS};
    private final TokenType[] termTokens = {TokenType.INT_CONST, TokenType.STRING_CONST};
    private final char[] unaryOp = {'-', '~'};
    private final char[] op = {'+', '-', '*', '/', '&', '|', '<', '>', '='};

    //Stuff for symbol table
    private SymbolTable symbolTable = new SymbolTable();
    private String latestName;
    private String latestType;
    private SymbolKind latestKind;

    public CompilationEngine_XML(JackTokenizer tokenizer, File output) {
        this.tokenizer = tokenizer;
        try {
            fw = new FileWriter(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    public void close() {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() {
        openTag("class");
        checkKeyword(Keyword.CLASS);
        checkToken(TokenType.IDENTIFIER);
        checkSymbol('{');
        while (checkKeyword(false, false, classVarDecKeywords)) {
            compileClassVarDec();
        }
        while (checkKeyword(false, false, subroutineKeywords)) {
            compileSubroutineDec();
        }
        checkSymbol('}');
        closeTag("class");
    }

    //  ('static', 'field') type varName (',' varName)* '}'
    private void compileClassVarDec() {
        this.openTag("classVarDec");
        // Kind
        if(checkKeyword(false, false, classVarDecKeywords)){
            latestKind = kindFromString(tokenizer.identifier());
            advance();
        }
        // Type
        if (checkKeyword(true, false, types)){
            latestType = tokenizer.identifier();
            advance();
        }
        // varName
        if(checkToken(false, TokenType.IDENTIFIER)){
            latestName = tokenizer.identifier();
            advance();
        }
        symbolTable.define(latestName, latestType, latestKind);
        // Optional Additional Terms
        while (checkSymbol(false, ',')) {
            advance();
            if(checkToken(false, TokenType.IDENTIFIER)){
                latestName = tokenizer.identifier();
                symbolTable.define(latestName, latestType, latestKind);
                advance();
            }
        }
        checkSymbol(';');
        this.closeTag("classVarDec");
    }

    //  (CONSTRUCTOR, FUNCTION, METHOD) (VOID, type) subroutineName '(' parameterList ')' subroutineBody
    // Method should have +1 args, args[0] refers to the this object
    private void compileSubroutineDec() {
        symbolTable.startSubroutine();
        openTag("subroutineDec");
        checkKeyword(true, subroutineKeywords);
        checkKeyword(true, keywordList(types, Keyword.VOID));
        checkToken(TokenType.IDENTIFIER);
        checkSymbol('(');
        compileParameterList();
        checkSymbol(')');
        compileSubroutineBody();
        closeTag("subroutineDec");
    }

    //  subroutineName '(' expressionList ')' |
    //  (className | varName) '.' subroutineName '(' expressionList ')'
    private void compileSubroutineCall() {
        checkToken(TokenType.IDENTIFIER);
        if (checkSymbol(false, '(')) {
            advance();
            compileExpressionList();
            checkSymbol(')');
        } else if (checkSymbol(false, '.')) {
            advance();
            checkToken(TokenType.IDENTIFIER);
            checkSymbol('(');
            compileExpressionList();
            checkSymbol(')');
        }
    }

    //  ((type varName) (',' type varName)*)?
    private void compileParameterList() {
        this.openTag("parameterList");
        latestKind = SymbolKind.ARG;
        if (checkKeyword(true, false, types)) {
//            checkKeyword(true, types);
            latestType = tokenizer.identifier();
            advance();
            if(checkToken(false, TokenType.IDENTIFIER)){
                latestName = tokenizer.identifier();
                advance();
            }
            symbolTable.define(latestName, latestType, latestKind);
            while (checkSymbol(false, ',')) {
                checkSymbol(',');
                if(checkKeyword(true, false, types)){
                    latestType = tokenizer.identifier();
                    advance();
                }
                if(checkToken(false, TokenType.IDENTIFIER)){
                    latestName = tokenizer.identifier();
                    advance();
                }
                symbolTable.define(latestName, latestType, latestKind);
            }
        }
        this.closeTag("parameterList");
    }

    //  '{' varDec* statements '}'
    private void compileSubroutineBody() {
        this.openTag("subroutineBody");
        checkSymbol('{');
        while (checkKeyword(false, false, Keyword.VAR)) {
            this.compileVarDec();
        }
        this.compileStatements();
        checkSymbol('}');
        this.closeTag("subroutineBody");
    }

    //  'var' type varName (',' varName)* ;
    private void compileVarDec() {
        openTag("varDec");
        checkKeyword(Keyword.VAR);
        latestKind = SymbolKind.VAR;
        if(checkKeyword(true,false, types)){
            latestType = tokenizer.identifier();
            advance();
        }
        if(checkToken(false, TokenType.IDENTIFIER)){
            latestName = tokenizer.identifier();
            advance();
        }
        symbolTable.define(latestName, latestType, latestKind);
        while (checkSymbol(false, ',')) {
            checkSymbol(',');
            if(checkToken(false, TokenType.IDENTIFIER)){
                latestName = tokenizer.identifier();
                advance();
            }
            symbolTable.define(latestName, latestType, latestKind);
        }
        checkSymbol(';');
        closeTag("varDec");
    }

    // statement*
    private void compileStatements() {
        openTag("statements");
        while (checkKeyword(false, false, statement)) {
            if (tokenizer.keyword() == Keyword.LET) {
                compileLet();
            } else if (tokenizer.keyword() == Keyword.IF) {
                compileIf();
            } else if (tokenizer.keyword() == Keyword.WHILE) {
                compileWhile();
            } else if (tokenizer.keyword() == Keyword.DO) {
                compileDo();
            } else if (tokenizer.keyword() == Keyword.RETURN) {
                compileReturn();
            }
        }
        closeTag("statements");
    }

    private void compileDo() {
        openTag("doStatement");
        checkKeyword(false, Keyword.DO);
        compileSubroutineCall();
        checkSymbol(';');
        closeTag("doStatement");
    }

    //  'let' varName ('[' expression ']')? '=' expression ';'
    private void compileLet() {
        openTag("letStatement");
        checkKeyword(Keyword.LET);
        checkToken(TokenType.IDENTIFIER);
        if (checkSymbol(false, '[')) {
            checkSymbol('[');
            compileExpression();
            checkSymbol(']');
        }
        checkSymbol('=');
        compileExpression();
        checkSymbol(';');
        closeTag("letStatement");
    }

    private void compileWhile() {
        openTag("whileStatement");
        advance();
        checkSymbol('(');
        compileExpression();
        checkSymbol(')');
        checkSymbol('{');
        compileStatements();
        checkSymbol('}');
        closeTag("whileStatement");
    }

    private void compileReturn() {
        openTag("returnStatement");
        advance();
        if (checkSymbol(false, ';')) {
            checkSymbol(';');
        } else {
            compileExpression();
            checkSymbol(';');
        }
        closeTag("returnStatement");
    }

    private void compileIf() {
        openTag("ifStatement");
        advance();
        checkSymbol('(');
        compileExpression();
        checkSymbol(')');
        checkSymbol('{');
        compileStatements();
        checkSymbol('}');
        while (checkKeyword(false, false, Keyword.ELSE)) {
            advance();
            checkSymbol('{');
            compileStatements();
            checkSymbol('}');
        }
        closeTag("ifStatement");
    }


    //  term (op term)*
    private void compileExpression() {
        openTag("expression");
        compileTerm();
        while (checkSymbol(false, op)) {
            advance();
            compileTerm();
        }
        closeTag("expression");
    }

    private void compileExpressionList() {
        openTag("expressionList");
//        isExpression();
        if (!checkSymbol(false, ')')) {
            compileExpression();
        }
        while (checkSymbol(false, ',')) {
            advance();
            compileExpression();
        }
        closeTag("expressionList");
    }

    //  integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | UnaryOp term
    private void compileTerm() {
        openTag("term");
        //  integerConstant | stringConstant | keywordConstant
        if (checkToken(false, TokenType.INT_CONST, TokenType.STRING_CONST) || checkKeyword(false, false, termKeywords)) {
            advance();
        }
        //  '(' expression ')'
        else if(checkSymbol(false, '(')){
            advance();
            compileExpression();
            checkSymbol(')');
        }
        else if (checkToken(false, termTokens) || checkKeyword(false, false, termKeywords)) {
//            if(checkSymbol(false, '['))
            advance();
        }
        else if(checkSymbol(false, unaryOp)){
            advance();
            compileTerm();
        }
        //  varName | varName '[' expression ']'
        else if (checkToken(false, TokenType.IDENTIFIER)) {
//            ArrayList<String> buffer = tokenizer.getInputBuffer();
            Token lookaheadToken = tokenizer.getLookaheadToken();
            if (lookaheadToken != null) {
                if(lookaheadToken.tokenType() == TokenType.SYMBOL){
                    if(lookaheadToken.symbol() == '(' || lookaheadToken.symbol() == '.'){
                        compileSubroutineCall();
                    }
                    else if(lookaheadToken.symbol() == '['){
                        checkToken(TokenType.IDENTIFIER);
                        advance();
                        compileExpression();
                        checkSymbol(']');
                    }
                    else{
//                        advance();
                        checkToken(TokenType.IDENTIFIER);
                    }
                }

            }
        }
        closeTag("term");

    }

    /*
     *   Add keywords to existing array
     */

    private Keyword[] keywordList(Keyword[] keywords1, Keyword... keywords2) {
        int max = keywords1.length + keywords2.length;
        Keyword[] keywordList = new Keyword[max];
        int i = 0;
        for (i = 0; i < keywords1.length; i++) {
            keywordList[i] = keywords1[i];
        }
        int end = i;
        for (i = 0; i < keywords2.length; i++) {
            keywordList[end] = keywords2[i];
            end++;
        }
        for (Keyword k : keywordList) {
        }
        return keywordList;
    }

    /*
     *   Token Checking
     */
    private boolean checkToken(TokenType... tokenType) {
        return (checkToken(true, tokenType));
    }

    private boolean checkToken(boolean advance, TokenType... tokenType) {
        boolean result = false;
        for (TokenType t : tokenType) {
            if (t == tokenizer.tokenType()) {
                if(t == TokenType.IDENTIFIER && advance){
//                    System.out.println("IDENTIFIER : " + tokenizer.currentTerm);
//                    System.out.println(tokenizer.toXML());
                }
                result = true;
            }
        }
        if (!result && advance) {
            halt("TOKEN", Arrays.toString(tokenType));
        }
        if (advance && result) {
            advance();
        }
        return result;
    }

    /*
     *	Symbol Checking
     */

    private boolean checkSymbol(char... symbols) {
        return checkSymbol(true, symbols);
    }

    private boolean checkSymbol(boolean advance, char... symbols) {
        boolean result = false;
        if (tokenizer.tokenType() == TokenType.SYMBOL) {
            for (char c : symbols) {
                if (tokenizer.symbol() == c) {
                    if (advance) {
                        advance();
                    }
                    result = true;
                }
            }
        }
        if (!result && advance) {
            halt(TokenType.SYMBOL.toString(), Arrays.toString(symbols));
        }
        return result;
    }

    /*
     * 	Keyword Checking
     */

    private boolean checkKeyword(Keyword... expectedKeywords) {
        return checkKeyword(true, true, expectedKeywords);
    }

    private boolean checkKeyword(boolean allowIdentifier, Keyword... expectedKeywords) {
        return checkKeyword(allowIdentifier, true, expectedKeywords);
    }

    private boolean checkKeyword(boolean allowIdentifier, boolean advance, Keyword... expectedKeywords) {
        boolean result = false;
        if (allowIdentifier && tokenizer.tokenType() == TokenType.IDENTIFIER) {
//            System.out.println("IDENTIFIER : " + tokenizer.currentTerm);
//            System.out.println(tokenizer.toXML());
            result = true;
        } else if (tokenizer.tokenType() == TokenType.KEYWORD) {
            for (Keyword k : expectedKeywords) {
                if (tokenizer.keyword() == k) {
                    result = true;
                }
            }
        }
        if (result && advance) {
            advance();
        }
        if (result == false && advance) {
            halt(TokenType.KEYWORD.toString(), Arrays.toString(expectedKeywords));
        }
        return result;
    }

    private SymbolKind kindFromString(String s){
        String lower = s.toLowerCase();
        for(SymbolKind k : SymbolKind.values()){
            if(lower.equals(k.toString().toLowerCase())){
                return k;
            }
        }
        return null;
    }

    private boolean advance() {
//		if (halt) {
//			System.exit(0);
//		}
        write(tokenizer.toXML());
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            return true;
        } else {
            return false;
        }
    }

    private void write(String s) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < indent; i++) {
                // sb.append("\t");
                sb.append("  ");
            }
            sb.append(s);
            fw.write(sb.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeXML() {
        this.write(tokenizer.toXML());
    }

    private void openTag(String text) {
        write("<" + text + ">");
        indent++;
    }

    private void closeTag(String text) {
        indent--;
        write("</" + text + ">");
    }

    int loggerIndent = 2;
    int loggerWidth = 15;

    private void log(String... strings) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < loggerIndent; i++) {
            out.append(" ");
        }
        for (String s : strings) {
            int len = s.length();
            out.append(s);
            for (int i = 0; i < loggerWidth - len; i++) {
                out.append(" ");
            }
        }
        System.out.println("[LOG] " + out.toString());
    }

    private void halt() {
        halt(null, null);
    }

    private void halt(String expectedType, String expectedValue) {
        try {
            fw.close();
        } catch (IOException e) {
            log("[PARSER]", "Error closing file...");
        }
//        System.out.println();
        boolean first = true;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            if (first) {
                log("[STACK TRACE]", stackTrace[i].toString());
                first = false;
            } else {
                log("", stackTrace[i].toString());
            }
        }
//        System.out.println("");
        log("[PARSER]", "Exception on line " + tokenizer.getLineCount() + " at token '" + tokenizer.identifier() + "'");
        log("", "Expected " + expectedType + " " + expectedValue);
        System.exit(1);
    }

}
