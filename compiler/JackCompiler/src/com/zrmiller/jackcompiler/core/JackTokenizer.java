package com.zrmiller.jackcompiler.core;

import com.zrmiller.jackcompiler.data.Token;
import com.zrmiller.jackcompiler.enums.Keyword;
import com.zrmiller.jackcompiler.enums.TokenType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class JackTokenizer {

    // Internal
    private BufferedReader reader;
    private final StringBuilder inputBuffer = new StringBuilder();
    private final StringBuilder tokenBuilder = new StringBuilder();

    // Readable by user
    private Token currentToken = null;
    private Token lookaheadToken = null;
    private int lineCount = 0;

    private final Pattern charPattern = Pattern.compile("[{}()\\[\\].,;+\\-*\\/&|<>=~]");

    /**
     * Creates a Jack Tokenizer for a given input file.
     *
     * @param inputFile The file to be parsed.
     */

    public JackTokenizer(File inputFile) {
        try {
            FileReader fileReader = new FileReader(inputFile);
            reader = new BufferedReader(fileReader);
            advance();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the current token to the value of the lookahead token, then gets a new lookahead token from the input buffer.
     */

    public void advance() {

        currentToken = lookaheadToken;
        lookaheadToken = null;
        boolean parsingComment = false;
        boolean parsingString = false;
        while ((lookaheadToken == null || parsingComment) && checkInputBuffer()) {
            for (int i = 0; i < inputBuffer.length(); i++) {

                // Current character information; segment is the current character appended with the next character.
                String character = inputBuffer.substring(i, i + 1);
                String segment = null;
                if (i < inputBuffer.length() - 1) {
                    segment = character + inputBuffer.substring(i + 1, i + 2);
                }

                // String Handling
                if (!parsingComment && character.equals("\"")) {
                    if (!parsingString && tokenBuilder.length() > 0) {
                        setLookaheadToken(i);
                        continue;
                    }
                    if (parsingString) {
                        tokenBuilder.append(inputBuffer.charAt(i));
                        setLookaheadToken(i + 1);
                        return;
                    } else {
                        parsingString = true;
//                        continue;
                    }
                }
                if (parsingString) {
                    tokenBuilder.append(inputBuffer.charAt(i));
                    continue;
                }

                // Comment Handling
                // TODO : Special handling for api comments
                if (segment != null) {
                    if (parsingComment) {
                        if (segment.equals("*/")) {
                            parsingComment = false;
                            i++;
                            inputBuffer.delete(0, i + 1);
                        }
                        continue;
                    } else {
                        if (segment.equals("/*")) {
                            parsingComment = true;
                            setLookaheadToken(i);
                            i++;
                            continue;
                        } else if (segment.equals("//")) {
                            setLookaheadToken(i);
                            inputBuffer.setLength(0);
                            break;
                        }
                    }
                }
                if (parsingComment) {
                    continue;
                }

                // White Space Check
                if (character.matches("\\s+")) {
                    setLookaheadToken(i + 1);
                }

                // Single Character Symbol Check
                else if (charPattern.matcher(character).matches()) {
                    if (tokenBuilder.length() > 0) {
                        setLookaheadToken(i);
                    } else {
                        tokenBuilder.append(inputBuffer.charAt(i));
                        setLookaheadToken(i + 1);
                    }
                }

                // If all else fails, add character to existing token
                else {
                    tokenBuilder.append(inputBuffer.charAt(i));
                }

                // Return when a token is found
                if (lookaheadToken != null) {
                    return;
                }

            }

            // End of Line Handling
            if (tokenBuilder.length() == 0) {
                inputBuffer.setLength(0);
            } else {
                setLookaheadToken(inputBuffer.length());
            }
        }


        if (parsingComment) {
            //TODO : ERROR: Unclosed comment
            System.out.println("[TOKENIZER] ERROR : Unclosed comment");
        }
    }

    /**
     * Fills the inputBuffer with text from the input file as needed. Returns false when the end of file has been reached.
     *
     * @return
     */
    private boolean checkInputBuffer() {
        if (inputBuffer.length() > 0) {
            return true;
        }
        try {
            while (inputBuffer.length() == 0 && reader.ready()) {
                inputBuffer.append(reader.readLine());
                lineCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputBuffer.length() > 0) {
            return true;
        }
        return false;
    }

    /**
     * @param spliceIndex The input buffer will be spliced to (spliceIndex, endingIndex) inclusive.
     * @return
     */
    private boolean setLookaheadToken(int spliceIndex) {
        if (tokenBuilder.length() > 0) {
            lookaheadToken = new Token(tokenBuilder.toString());
            tokenBuilder.setLength(0);
            inputBuffer.delete(0, spliceIndex);
            return true;
        }
        return false;
    }

    public boolean hasMoreTokens() {
        return !(lookaheadToken == null);
    }

    public TokenType tokenType() {
        return currentToken.tokenType();
    }

    public Keyword keyword() {
        return currentToken.keyword();
    }

    public char symbol() {
        return currentToken.symbol();
    }

    public String identifier() {
        return currentToken.identifier();
    }

    public int intValue() {
        return currentToken.intVal();
    }

    public String stringValue() {
        return currentToken.identifier();
    }

    public int getLineCount() {
        return this.lineCount;
    }

    public String toXML() {
        return currentToken.toXML();
    }

    public Token getLookaheadToken() {
        return lookaheadToken;
    }

}

