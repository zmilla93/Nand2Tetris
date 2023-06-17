package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.data.Token;
import com.zrmiller.jackcompiler.enums.*;

import java.io.File;
import java.util.Arrays;

public class CompilationEngine {

    //File stuff
    private final String fileName;
    private final JackTokenizer tokenizer;
    private boolean stackTrace = false;

    //Keyword, token, and char groups
    private final Keyword[] types = {Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN};
    private final Keyword[] statement = {Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN};
    private final Keyword[] subroutineKeywords = {Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD};
    private final Keyword[] classVarDecKeywords = {Keyword.STATIC, Keyword.FIELD};
    private final Keyword[] termKeywords = {Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS};
    private final char[] unaryOp = {'-', '~'};
    private final char[] op = {'+', '-', '*', '/', '&', '|', '<', '>', '='};

    //Stuff for symbol table
    private static final String WHILE_EXP = "WHILE_EXP";
    private static final String WHILE_END = "WHILE_END";
    private static final String IF_TRUE = "IF_TRUE";
    private static final String IF_FALSE = "IF_FALSE";
    private static final String IF_END = "IF_END";
    private final SymbolTable symbolTable = new SymbolTable();
    private String latestName;
    private String latestType;
    private SymbolKind latestKind;
    private boolean insideMethod = false;

    //VM Output
    private final VMWriter vmWriter;
    private String className;

    private int ifCount = 0;
    private int whileCount = 0;

    //TODO : Should add more accurate expectation when advance() is used

    public CompilationEngine(JackTokenizer tokenizer, File output, boolean stackTrace) {
        fileName = output.getName().replaceAll("\\.vm\\Z", "");
        this.tokenizer = tokenizer;
        this.stackTrace = stackTrace;
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
        vmWriter = new VMWriter(output);
    }

    //  'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() {
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
        vmWriter.close();
    }

    //  ('static', 'field') type varName (',' varName)* '}'
    private int compileClassVarDec() {
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
        return count;
    }

    //  (CONSTRUCTOR, FUNCTION, METHOD) (VOID, type) subroutineName '(' parameterList ')' subroutineBody
    //  !IMPORTANT NOTE! Methods should have +1 arg, args[0] refers to the 'this' object
    private void compileSubroutineDec(int fieldCount) {
        symbolTable.startSubroutine();
        ifCount = 0;
        whileCount = 0;
        String subroutineName = null;
        Keyword keyword = tokenizer.keyword();
//        CONSTRUCTOR, FUNCTION, METHOD
        if (!(keyword == Keyword.CONSTRUCTOR)) fieldCount = 0;
        if (checkKeyword(false, false, subroutineKeywords)) {
            advance();
        }
        if (checkKeyword(true, false, keywordList(types, Keyword.VOID))) {
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
    }

    // TODO : Reduced passed info as much as possible!!!
    //  '{' varDec* statements '}'
    private int compileSubroutineBody(Keyword keyword, String subroutineName, int fieldCount) {
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
        return varCount;
    }

    //  'var' type varName (',' varName)* ;
    private int compileVarDec() {
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
        return count;
    }

    // statement*
    private void compileStatements() {
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
    }

    private void compileDo() {
        checkKeyword(false, Keyword.DO);
        compileSubroutineCall();
        checkSymbol(';');
        vmWriter.writePop(Segment.TEMP, 0);
    }

    //  'let' varName ('[' expression ']')? '=' expression ';'
    private void compileLet() {
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
    }

    private void compileWhile() {
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
    }

    private void compileReturn() {
        advance();
        if (checkSymbol(false, ';')) {
            checkSymbol(';');
            vmWriter.writePush(Segment.CONST, 0);
        } else {
            compileExpression();
            checkSymbol(';');
        }
        vmWriter.writeReturn();
    }

    private void compileIf() {
        int index = ifCount;
        ifCount++;
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
    }


    private int compileExpressionList() {
        int count = 0;
        if (!checkSymbol(false, ')')) {
            compileExpression();
            count++;
        }
        while (checkSymbol(false, ',')) {
            advance();
            compileExpression();
            count++;
        }
        return count;
    }

    //  term (op term)*
    private void compileExpression() {
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
    }

    //  integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | UnaryOp term
    private void compileTerm() {
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
            error("Token", Arrays.toString(tokenType));
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
        if (!result && advance) {
            error(TokenType.KEYWORD.toString(), Arrays.toString(expectedKeywords));
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

    private void error(String message) {
        System.err.println("Error in " + fileName + ".jack (Line " + tokenizer.getLineCount() + ") : " + message);
        abortCompilation();
    }

    private void error(String exceptedType, char[] expectedValue) {
        if (expectedValue.length == 1)
            error(exceptedType, "'" + expectedValue[0] + "'");
        else
            error(exceptedType, Arrays.toString(expectedValue));
        abortCompilation();
    }

    private void error(String exceptedType, String expectedValue) {
        vmWriter.close();
        System.err.println("Error in " + fileName + ".jack (Line " + tokenizer.getLineCount() + ") at token '" + tokenizer.identifier() + "' : " + "Excepted " + exceptedType + " " + expectedValue);
        abortCompilation();
    }

    private void abortCompilation() {
        if (stackTrace) printStackTrace();
        vmWriter.close();
        System.out.println("Compiler terminated.");
        System.exit(1);
    }

    private void printStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stackTrace) {
            System.out.println("[STACK TRACE]\t" + e);
        }
    }

}
