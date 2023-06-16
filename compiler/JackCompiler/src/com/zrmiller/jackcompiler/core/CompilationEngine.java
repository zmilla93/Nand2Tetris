package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.data.Token;
import com.zrmiller.jackcompiler.enums.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class CompilationEngine {

    //File stuff
    private String fileName;
    private JackTokenizer tokenizer;
    private FileWriter fw;
    private int indent = 0;
    private int loggerIndent = 2;
    private int loggerWidth = 15;

    //Keyword, token, and char groups
    private final Keyword[] types = {Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN};
    private final Keyword[] statement = {Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN};
    private final Keyword[] subroutineKeywords = {Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD};
    private final Keyword[] classVarDecKeywords = {Keyword.STATIC, Keyword.FIELD};
    private final Keyword[] termKeywords = {Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS};
    //    private final TokenType[] termTokens = {TokenType.INT_CONST, TokenType.STRING_CONST};
    private final char[] unaryOp = {'-', '~'};
    private final char[] op = {'+', '-', '*', '/', '&', '|', '<', '>', '='};

    //Stuff for symbol table
//    private String if
    private final String WHILE_EXP = "WHILE_EXP";
    private final String WHILE_END = "WHILE_END";
    private final String IF_TRUE = "IF_TRUE";
    private final String IF_FALSE = "IF_FALSE";
    private final String IF_END = "IF_END";
    // TODO : Move 'latest' variables to local scope
    private SymbolTable symbolTable = new SymbolTable();
    private String latestName;
    private String latestType;
    private SymbolKind latestKind;
    private boolean insideMethod = false;

    //VM Output
    private VMWriter vmWriter;
    //    private int argCount;
    private String className;
    private String latestSubroutineKind;
    private String latestSubroutineType;
    private String latestSubroutine;
    private String latestCall;
    private String latestDo;
    private ArrayList<String> latestExpression = new ArrayList<>();
    private ArrayList<String> callList = new ArrayList<>();

    private int ifCount = 0;
    private int whileCount = 0;

    //TODO : Should add more accurate expectation when advance() is used

    public CompilationEngine(JackTokenizer tokenizer, File output) {
        fileName = output.getName().replaceAll("\\.vm\\Z", "");
        this.tokenizer = tokenizer;
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
        vmWriter = new VMWriter(output);
    }

    //  'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() {
        openTag("class");
        checkKeyword(Keyword.CLASS);
        if (checkToken(false, TokenType.IDENTIFIER)) {
            className = tokenizer.identifier();
            if (tokenizer.identifier().equals(fileName.replaceAll("\\.vm\\Z", ""))) {
                advance();
            } else {
                error("Class name '" + tokenizer.identifier() + "' does not match file name");
            }
        } else {
            error("Invalid class name '" + tokenizer.identifier() + "'");
        }
        checkSymbol('{');
        int fieldCount = 0;
        while (newCheckKeyword(false, classVarDecKeywords)) {
            fieldCount += compileClassVarDec();
        }
        while (newCheckKeyword(false, subroutineKeywords)) {
            compileSubroutineDec(fieldCount);
        }
        checkSymbol('}');
        closeTag("class");
        vmWriter.close();
    }

    //  ('static', 'field') type varName (',' varName)* '}'
    private int compileClassVarDec() {
        this.openTag("classVarDec");
        int count = 0;
        // Kind
        if (checkKeyword(false, false, classVarDecKeywords)) {
            latestKind = kindFromString(tokenizer.stringValue());
            advance();
        }
        // Type
        if (checkKeyword(true, false, types)) {
            latestType = tokenizer.stringValue();
            advance();
        }
        // varName
        if (checkToken(false, TokenType.IDENTIFIER)) {
            latestName = tokenizer.identifier();
            advance();
        }
        symbolTable.define(latestName, latestType, latestKind);
        // Count field symbols
        if (latestKind == SymbolKind.FIELD) {
            count++;
        }
        // Optional Additional Terms
        while (checkSymbol(false, ',')) {
            advance();
            if (checkToken(false, TokenType.IDENTIFIER)) {
                latestName = tokenizer.identifier();
                symbolTable.define(latestName, latestType, latestKind);
                advance();
                count++;
            }
        }
        checkSymbol(';');
        this.closeTag("classVarDec");
        return count;
    }

    //  (CONSTRUCTOR, FUNCTION, METHOD) (VOID, type) subroutineName '(' parameterList ')' subroutineBody
    //  !IMPORTANT NOTE! Methods should have +1 arg, args[0] refers to the 'this' object
    private void compileSubroutineDec(int fieldCount) {
        symbolTable.startSubroutine();
        ifCount = 0;
        whileCount = 0;
        openTag("subroutineDec");
        String subroutineName = null;
        Keyword keyword = tokenizer.keyword();
//        CONSTRUCTOR, FUNCTION, METHOD
        if (!(keyword == Keyword.CONSTRUCTOR)) fieldCount = 0;
        if (checkKeyword(false, false, subroutineKeywords)) {
            latestSubroutineKind = tokenizer.identifier();
            advance();
        }
        if (checkKeyword(true, false, keywordList(types, Keyword.VOID))) {
            latestSubroutineType = tokenizer.identifier();
            advance();
        }
        if (checkToken(false, TokenType.IDENTIFIER)) {
            subroutineName = className + "." + tokenizer.identifier();
            advance();
        }
        checkSymbol('(');
        compileParameterList();
        checkSymbol(')');
        compileSubroutineBody(keyword, subroutineName, fieldCount);
        closeTag("subroutineDec");

    }

    //  subroutineName '(' expressionList ')' |
    //  (className | varName) '.' subroutineName '(' expressionList ')'
    private void compileSubroutineCall() {
        String className = null;
        String subroutineName = null;
        int argCount = 0;
        // Function call
        if (checkToken(false, TokenType.IDENTIFIER)) {
            subroutineName = tokenizer.identifier();
            advance();
        }
        if (checkSymbol(false, '(')) {
            advance();
            vmWriter.writePush(Segment.POINTER, 0);
            className = fileName;
            argCount = compileExpressionList();
            argCount++;
            checkSymbol(')');
        } else if (checkSymbol(false, '.')) {
            advance();
            className = subroutineName;
            SymbolKind kind = symbolTable.kindOf(className);
            String type = symbolTable.typeOf(className);
            int index = symbolTable.indexOf(className);
            if (kind == SymbolKind.STATIC | kind == SymbolKind.FIELD | kind == SymbolKind.VAR | kind == SymbolKind.ARG) {
                className = type;
                vmWriter.writePush(kind.toSegment(), index);
                argCount++;
            }
            if (checkToken(false, TokenType.IDENTIFIER)) {
                subroutineName = tokenizer.identifier();
                advance();
            }
            checkSymbol('(');
            argCount += compileExpressionList();
            checkSymbol(')');
        }
        String callName = className == null ? subroutineName : className + "." + subroutineName;
        vmWriter.writeCall(callName, argCount);
    }

    //  ((type varName) (',' type varName)*)?
    private void compileParameterList() {
        // TODO : check if this count is ever used
        this.openTag("parameterList");
        latestKind = SymbolKind.ARG;
        if (checkKeyword(true, false, types)) {
            latestType = tokenizer.identifier();
            advance();
            if (checkToken(false, TokenType.IDENTIFIER)) {
                latestName = tokenizer.identifier();
                advance();
            }
            symbolTable.define(latestName, latestType, latestKind);
            while (checkSymbol(false, ',')) {
                checkSymbol(',');
                if (checkKeyword(true, false, types)) {
                    latestType = tokenizer.identifier();
                    advance();
                }
                if (checkToken(false, TokenType.IDENTIFIER)) {
                    latestName = tokenizer.identifier();
                    advance();
                }
                symbolTable.define(latestName, latestType, latestKind);
            }
        }
        this.closeTag("parameterList");
    }

    // TODO : Reduced passed info as much as possible!!!
    //  '{' varDec* statements '}'
    private int compileSubroutineBody(Keyword keyword, String subroutineName, int fieldCount) {
        this.openTag("subroutineBody");
        checkSymbol('{');
        int varCount = 0;
        while (checkKeyword(false, false, Keyword.VAR)) {
            varCount += compileVarDec();
        }
        vmWriter.writeFunction(subroutineName, varCount);
        if (keyword == Keyword.METHOD) {
            insideMethod = true;
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (fieldCount > 0) {
            vmWriter.writePush(Segment.CONST, fieldCount);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }
        compileStatements();
        insideMethod = false;
        checkSymbol('}');
        this.closeTag("subroutineBody");
        return varCount;
    }

    //  'var' type varName (',' varName)* ;
    private int compileVarDec() {
        openTag("varDec");
        int count = 0;
        checkKeyword(Keyword.VAR);
        latestKind = SymbolKind.VAR;
        if (checkKeyword(true, false, types)) {
            latestType = tokenizer.identifier();
            advance();
        }
        if (checkToken(false, TokenType.IDENTIFIER)) {
            latestName = tokenizer.identifier();
            advance();
            count++;
        }
        symbolTable.define(latestName, latestType, latestKind);
        while (checkSymbol(false, ',')) {
            checkSymbol(',');
            if (checkToken(false, TokenType.IDENTIFIER)) {
                latestName = tokenizer.identifier();
                advance();
                count++;
            }
            symbolTable.define(latestName, latestType, latestKind);
        }
        checkSymbol(';');
        closeTag("varDec");
        return count;
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
        vmWriter.writePop(Segment.TEMP, 0);
    }

    //  'let' varName ('[' expression ']')? '=' expression ';'
    private void compileLet() {
        openTag("letStatement");
        checkKeyword(Keyword.LET);
        // varName
        String name = tokenizer.identifier();
        SymbolKind kind = symbolTable.kindOf(tokenizer.identifier());
        int index = symbolTable.indexOf(tokenizer.identifier());
        checkToken(TokenType.IDENTIFIER);
        boolean containsArray = false;
        if (checkSymbol(false, '[')) {
            checkSymbol('[');
            compileExpression();
            vmWriter.writePush(kind.toSegment(), index);
            vmWriter.writeArithmetic(Command.ADD);
            checkSymbol(']');
            containsArray = true;
        }
        checkSymbol('=');
        compileExpression();
        checkSymbol(';');
        Segment seg = kind.toSegment();
        if (containsArray) {
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            vmWriter.writePop(seg, index);
        }
        closeTag("letStatement");
    }

    private void compileWhile() {
        openTag("whileStatement");
        int index = whileCount;
        whileCount++;
        advance();
        checkSymbol('(');
        vmWriter.writeLabel(WHILE_EXP + index);
        compileExpression();
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(WHILE_END + index);
        checkSymbol(')');
        checkSymbol('{');
        compileStatements();
        checkSymbol('}');
        vmWriter.writeGoto(WHILE_EXP + index);
        vmWriter.writeLabel(WHILE_END + index);
        closeTag("whileStatement");
    }

    private void compileReturn() {
        openTag("returnStatement");
        advance();
        if (checkSymbol(false, ';')) {
            checkSymbol(';');
            vmWriter.writePush(Segment.CONST, 0);
        } else {
            compileExpression();
            checkSymbol(';');
        }
        vmWriter.writeReturn();
        closeTag("returnStatement");
    }

    private void compileIf() {
        int index = ifCount;
        ifCount++;
        openTag("ifStatement");
        advance();
        checkSymbol('(');
        compileExpression();
        checkSymbol(')');
        checkSymbol('{');
        vmWriter.writeIf(IF_TRUE + index);
        vmWriter.writeGoto(IF_FALSE + index);
        vmWriter.writeLabel(IF_TRUE + index);
        compileStatements();
        checkSymbol('}');
        boolean hasElse = checkKeyword(false, false, Keyword.ELSE);
        if (hasElse) {
            vmWriter.writeGoto(IF_END + index);
        }
        vmWriter.writeLabel(IF_FALSE + index);
        if (hasElse) {
            advance();
            checkSymbol('{');
            compileStatements();
            checkSymbol('}');
            vmWriter.writeLabel(IF_END + index);
        }
        closeTag("ifStatement");
    }


    private int compileExpressionList() {
        int count = 0;
        openTag("expressionList");
        if (!checkSymbol(false, ')')) {
            compileExpression();
            count++;
        }
        while (checkSymbol(false, ',')) {
            advance();
            compileExpression();
            count++;
        }
        closeTag("expressionList");
        return count;
    }

    //  term (op term)*
    private void compileExpression() {
        openTag("expression");
        compileTerm();
        // term op term
        while (tokenizer.tokenType() == TokenType.SYMBOL && isOp(tokenizer.identifier().charAt(0))) {
            char lookaheadOp = tokenizer.identifier().charAt(0);
            advance();
            compileTerm();
            switch (lookaheadOp) {
                case '+':
                    vmWriter.writeArithmetic(Command.ADD);
                    break;
                case '-':
                    vmWriter.writeArithmetic(Command.SUB);
                    break;
                case '*':
                    vmWriter.writeCall("Math.multiply", 2);
                    break;
                case '/':
                    vmWriter.writeCall("Math.divide", 2);
                    break;
                case '&':
                    vmWriter.writeArithmetic(Command.AND);
                    break;
                case '|':
                    vmWriter.writeArithmetic(Command.OR);
                    break;
                case '<':
                    vmWriter.writeArithmetic(Command.LT);
                    break;
                case '>':
                    vmWriter.writeArithmetic(Command.GT);
                    break;
                case '=':
                    vmWriter.writeArithmetic(Command.EQ);
                    break;
            }
        }
        closeTag("expression");
    }

    //  integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | UnaryOp term
    private void compileTerm() {
        openTag("term");
        // TODO : finish expression handling
        // Unary OP
        char op = tokenizer.identifier().charAt(0);
        if (isUnaryOp(op)) {
            tokenizer.advance();
            compileTerm();
            if (op == '-') {
                vmWriter.writeArithmetic(Command.NEG);
            } else if (op == '~') {
                vmWriter.writeArithmetic(Command.NOT);
            }
        }
        //NUMBER
        else if (tokenizer.tokenType() == TokenType.INT_CONST) {
            vmWriter.writePush(Segment.CONST, tokenizer.intValue());
            advance();
            return;
        }
        // TRUE, FALSE, NULL, THIS
        else if (checkKeyword(false, false, termKeywords)) {
            if (tokenizer.tokenType() == TokenType.KEYWORD) {
                switch (tokenizer.keyword()) {
                    case TRUE:
                        vmWriter.writePush(Segment.CONST, 0);
                        vmWriter.writeArithmetic(Command.NOT);
                        break;
                    case FALSE:
                    case NULL:
                        vmWriter.writePush(Segment.CONST, 0);
                        break;
                    case THIS:
                        vmWriter.writePush(Segment.POINTER, 0);
                        break;
                }
            }
            advance();
        }
        // String Constants
        else if (checkToken(false, TokenType.STRING_CONST)) {
            vmWriter.writePush(Segment.CONST, tokenizer.stringValue().length());
            vmWriter.writeCall("String.new", 1);
            for (char c : tokenizer.stringValue().toCharArray()) {
                vmWriter.writePush(Segment.CONST, (int) c);
                vmWriter.writeCall("String.appendChar", 2);
            }
            advance();
        }
        //  '(' expression ')'
        else if (checkSymbol(false, '(')) {
            advance();
            compileExpression();
            checkSymbol(')');
        }
        //  varName | varName '[' expression ']'
        else if (checkToken(false, TokenType.IDENTIFIER)) {
            Token lookaheadToken = tokenizer.getLookaheadToken();
            if (lookaheadToken.tokenType() == TokenType.SYMBOL) {
                // Subroutine Call
                if (lookaheadToken.symbol() == '(' || lookaheadToken.symbol() == '.') {
                    compileSubroutineCall();
                }
                // Array Handling
                else if (lookaheadToken.symbol() == '[') {
                    SymbolKind kind = symbolTable.kindOf(tokenizer.identifier());
                    int index = symbolTable.indexOf(tokenizer.identifier());
                    checkToken(TokenType.IDENTIFIER);
                    advance();
                    compileExpression();
                    vmWriter.writePush(kind.toSegment(), index);
                    vmWriter.writeArithmetic(Command.ADD);
                    vmWriter.writePop(Segment.POINTER, 1);
                    vmWriter.writePush(Segment.THAT, 0);
                    checkSymbol(']');
                }
                // Identifier
                else {
                    SymbolKind kind = symbolTable.kindOf(tokenizer.identifier());
                    int count = symbolTable.indexOf(tokenizer.identifier());
                    if (kind == SymbolKind.ARG) {
                        // Args inside methods get +1 to account for arg 0 referring to 'this'
                        if (insideMethod) {
                            count++;
                        }
                    }
                    checkToken(TokenType.IDENTIFIER);
                    vmWriter.writePush(kind.toSegment(), count);
                }
            }
        }
        closeTag("term");

    }


    /*
     *  The rest of this file contains utility functions for checking values against arrays.
     *  They are used in an attempt to increase readability and produce generic error messages.
     */


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
                result = true;
                break;
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
//            halt(TokenType.SYMBOL.toString(), Arrays.toString(symbols));
            error(TokenType.SYMBOL.toString(), symbols);
        }
        return result;
    }

    /*
     * 	Keyword Checking
     */

    private boolean newCheckKeyword(Keyword... expectedKeywords) {
        return newCheckKeyword(false, expectedKeywords);
    }

    private boolean newCheckKeyword(boolean advance, Keyword... expectedKeywords) {
        boolean result = false;
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            for (Keyword k : expectedKeywords) {
                if (tokenizer.keyword() == k) {
                    result = true;
                    if (advance) {
                        advance();
                    }
                }
            }
        }
        if (advance && !result) {
            error(TokenType.KEYWORD.toString(), Arrays.toString(expectedKeywords));
//            halt(TokenType.KEYWORD.toString(), Arrays.toString(expectedKeywords));
        }
        return result;
    }

    private boolean checkKeyword(Keyword... expectedKeywords) {
        return checkKeyword(true, true, expectedKeywords);
    }

    private boolean checkKeyword(boolean allowIdentifier, Keyword... expectedKeywords) {
        return checkKeyword(allowIdentifier, true, expectedKeywords);
    }

    private boolean checkKeyword(boolean allowIdentifier, boolean advance, Keyword... expectedKeywords) {
        boolean result = false;
        if (allowIdentifier && tokenizer.tokenType() == TokenType.IDENTIFIER) {
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

    private SymbolKind kindFromString(String s) {
        String lower = s.toLowerCase();
        for (SymbolKind k : SymbolKind.values()) {
            if (lower.equals(k.toString().toLowerCase())) {
                return k;
            }
        }
        return null;
    }

    private boolean advance() {
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            return true;
        } else {
            return false;
        }
    }

    private boolean isOp(char sym) {
        for (char c : op) {
            if (c == sym) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnaryOp(char sym) {
        for (char c : unaryOp) {
            if (c == sym) {
                return true;
            }
        }
        return false;
    }

    private void write(String s) {
        try {
            fw.write(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openTag(String text) {
        indent++;
    }

    private void closeTag(String text) {
        indent--;
    }

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
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                log("[PARSER]", "Error closing file...");
            }
        }
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
        //TODO : fix line counter
//        log("[PARSER]", "Exception on line " + tokenizer.getLineCount() + " at token '" + tokenizer.currentTerm + "'");
        log("", "Expected " + expectedType + " " + expectedValue);
        vmWriter.close();
        System.exit(1);
    }

    private void error(String message) {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
            }
        }
        //TODO : Fix line counter
        System.err.println("[ERROR] In " + fileName + ".jack (Line " + tokenizer.getLineCount() + ") : " + message);
        vmWriter.close();
        System.exit(1);
    }

    private void error(String exceptedType, char[] expectedValue) {
        if (expectedValue.length == 1) {
            error(exceptedType, "'" + Character.toString(expectedValue[0]) + "'");
        } else {
            error(exceptedType, Arrays.toString(expectedValue));
        }
    }

    private void error(String exceptedType, String expectedValue) {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
            }
        }
        //TODO : Fix line counter
        System.err.println("[ERROR] In " + fileName + ".jack (Line " + tokenizer.getLineCount() + ") at token '" + tokenizer.identifier() + "' : " + "Excepted " + exceptedType + " " + expectedValue);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean first = true;
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            if (first) {
                log("[STACK TRACE]", stackTrace[i].toString());
                first = false;
            } else {
                log("", stackTrace[i].toString());
            }
        }
        vmWriter.close();
        System.exit(1);
    }

}
