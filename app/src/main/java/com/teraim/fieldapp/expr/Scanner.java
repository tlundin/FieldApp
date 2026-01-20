package com.teraim.fieldapp.expr;

//Scan lexical tokens in input strings.

import java.util.Vector;


class Scanner {
	private static final String TAG = "Scanner";


 private final String s;
 private final String operatorChars;

 final Vector<Token> tokens = new Vector<>();
 int index = -1;

 Scanner(String string, String operatorChars) {
     this.s = string;
	this.operatorChars = operatorChars;

     int i = 0;
	do {
	    i = scanToken(i);
	} while (i < s.length());
 }

 public String getInput() {
	return s;
 }

 // The tokens may have been diddled, so this can be different from 
 // getInput().
 public String toString() {
	StringBuilder sb = new StringBuilder();
	int whitespace = 0;
	for (int i = 0; i < tokens.size(); ++i) {
	    Token t = tokens.elementAt(i);

	    int spaces = (whitespace != 0 ? whitespace : t.leadingWhitespace);
	    if (i == 0) 
		spaces = 0;
	    else if (spaces == 0 && !joinable(tokens.elementAt(i-1), t))
		spaces = 1;
	    for (int j = spaces; 0 < j; --j)
		sb.append(" ");

	    sb.append(t.sval);
	    whitespace = t.trailingWhitespace;
	}
	return sb.toString();
 }

 private boolean joinable(Token s, Token t) {
	return !(isAlphanumeric(s) && isAlphanumeric(t));
 }

 private boolean isAlphanumeric(Token t) {
	return t.ttype == Token.TT_WORD || t.ttype == Token.TT_NUMBER;
 }

 public boolean isEmpty() {
	return tokens.size() == 0;
 }

 boolean atStart() {
	return index <= 0;
 }

 boolean atEnd() {
	return tokens.size() <= index;
 }

 Token nextToken() {
	++index;
	return getCurrentToken();
 }

 Token getCurrentToken() {
	if (atEnd())
	    return new Token(Token.TT_EOF, 0, s, s.length(), s.length());
	return tokens.elementAt(index);
 }

 private int scanToken(int i) {
     while (i < s.length() && Character.isWhitespace(s.charAt(i)))
         ++i;

     if (i == s.length()) {
	    return i;
     } else if (0 <= operatorChars.indexOf(s.charAt(i))) {
	    if (i+1 < s.length()) {
		String pair = s.substring(i, i+2);
		int ttype = 0;
			switch (pair) {
				case "<=":
				case "LE":
					ttype = Token.TT_LE;
					break;
				case ">=":
				case "GE":
					ttype = Token.TT_GE;
					break;
				case "<>":
				case "NE":
					ttype = Token.TT_NE;
					break;
				case "GT":
					ttype = Token.TT_GT;
					break;
				case "LT":
					ttype = Token.TT_LT;
					break;
				case "EQ":
					ttype = Token.TT_EQ;
					break;
			}
		if (0 != ttype) {
		    tokens.addElement(new Token(ttype, 0, s, i, i+2));
		    return i+2;
		}
	    }
	    tokens.addElement(new Token(s.charAt(i), 0, s, i, i+1));
         return i+1;
     } else if (isLetter(s.charAt(i))) {
         return scanSymbol(i);
     } else if (Character.isDigit(s.charAt(i)) || '.' == s.charAt(i)) {
         return scanNumber(i);
     } else {
         tokens.addElement(makeErrorToken(i, i+1));
         return i+1;
     }
 }

 private int scanSymbol(int i) {
	int from = i;
     while (i < s.length() 
	       && (isLetter(s.charAt(i))
		   || Character.isDigit(s.charAt(i))))
    	 
         ++i;
    if (s.substring(from, i).equalsIgnoreCase("null")) {
    	//Log.d(TAG,"FOUND NULL IN FORMULA");
    	tokens.addElement(new Token(Token.TT_NULL, 0, s, from, i));
    }
    else
    	tokens.addElement(new Token(Token.TT_WORD, 0, s, from, i));
	
	return i;
 }

 private int scanNumber(int i) {
	int from = i;

     // We include letters in our purview because otherwise we'd
     // accept a word following with no intervening space.
     for (; i < s.length(); ++i)
	    if ('.' != s.charAt(i)
		&& !Character.isDigit(s.charAt(i))
		&& !isLetter(s.charAt(i)))
             break;

     String text = s.substring(from, i);
	double nval;
     try {
         nval = Double.valueOf(text);
     } catch (NumberFormatException nfe) {
         tokens.addElement(makeErrorToken(from, i));
	    return i;
     }

	tokens.addElement(new Token(Token.TT_NUMBER, nval, s, from, i));
	return i;
 }

 private Token makeErrorToken(int from, int i) {
	return new Token(Token.TT_ERROR, 0, s, from, i);
 }
 
 private boolean isLetter(char c) {
	 return Character.isLetter(c)|| (c=='_');
 }
}
