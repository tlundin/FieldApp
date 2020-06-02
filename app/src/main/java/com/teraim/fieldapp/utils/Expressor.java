package com.teraim.fieldapp.utils;

import android.database.Cursor;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.ExportDialogInterface;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;


/**
 * toolset class with
 *
 * Parser and Tokenizer for arithmetic and logic expressions.
 *
 *
 * Part of the Vortex Core classes. 
 *
 * @author Terje Lundin 
 *
 * Teraim Holding reserves the property rights of this Class (2015)
 *
 *
 */

public class Expressor {


    private static List<List<String>>  targetList = null;

    //Types of tokens recognized by the Engine.
    //Some of these are operands, some functions, etc as denoted by the first argument.
    //Last argument indicates Cardinality or prescedence in case of Operands (Operands are X op Y)
    public enum TokenType {
        function(null,-1),
        booleanFunction(function,-1),
        valueFunction(function,-1),
        has(booleanFunction,1),
        hasAll(booleanFunction,1),
        hasMore(booleanFunction,1),
        hasSome(booleanFunction,1),
        hasMost(booleanFunction,1),
        hasSame(booleanFunction,-1),
        hasValue(booleanFunction,-1),
        hasNullValue(booleanFunction,-1),
        photoExists(booleanFunction,1),
        allHaveValue(booleanFunction,-1),
        not(booleanFunction,1),
        iff(valueFunction,3),
        getColumnValue(valueFunction,1),
        historical(valueFunction,1),
        hasSameValueAsHistorical(valueFunction,2),
        getHistoricalListValue(valueFunction,1),
        getListValue(valueFunction,1),
        getCurrentYear(valueFunction,0),
        getCurrentMonth(valueFunction,0),
        getCurrentDay(valueFunction,0),
        getCurrentHour(valueFunction,0),
        getCurrentMinute(valueFunction,0),
        getCurrentSecond(valueFunction,0),
        getCurrentWeekNumber(valueFunction,0),
        getSweDate(valueFunction,0),
        getStatusVariableValues(valueFunction,1),
        getGISobjectLength(valueFunction,0),
        getGISobjectArea(valueFunction,0),
        getSweRefX(valueFunction,1),
        getSweRefY(valueFunction,1),
        getAppName(valueFunction,0),
        getUserRole(valueFunction,0),
        getTeamName(valueFunction,0),
        getUserName(valueFunction,0),
        export(valueFunction,4),
        sum(valueFunction,-1),
        concatenate(valueFunction,-1),
        getDelytaArea(valueFunction,1),
        abs(valueFunction,1),
        acos(valueFunction,1),
        asin(valueFunction,1),
        atan(valueFunction,1),
        ceil(valueFunction,1),
        cos(valueFunction,1),
        exp(valueFunction,1),
        floor(valueFunction,1),
        log(valueFunction,1),
        round(valueFunction,1),
        sin(valueFunction,1),
        sqrt(valueFunction,1),
        tan(valueFunction,1),
        atan2(valueFunction,1),
        max(valueFunction,2),
        min(valueFunction,2),
        pow(valueFunction,2),
        unaryMinus(valueFunction,1),
        variable(null,-1),
        text(variable,0),
        numeric(variable,0),
        bool(variable,0),
        list(variable,0),
        existence(variable,0),
        auto_increment(variable,0),
        none(null,-1),
        literal(null,-1),
        number(null,-1),
        operand(null,0),
        and(operand,5),
        or(operand,4),
        add(operand,8),
        subtract(operand,8),
        multiply(operand,10),
        divide(operand,10),
        gte(operand,6),
        lte(operand,6),
        eq(operand,6),
        neq(operand,6),
        gt(operand,6),
        lt(operand,6),

        parenthesis(literal,-1),
        comma(literal,-1),
        leftparenthesis(parenthesis,-1),
        rightparenthesis(parenthesis,-1),

        unknown(null,-1),
        startMarker(null,-1),
        endMarker(null,-1),
        ;

        private TokenType parent = null;
        private final List<TokenType> children = new ArrayList<>();
        private final int cardinalityOrPrescedence;

        //only operands has prescedence.
        int prescedence() {
            if (this.parent==operand)
                return cardinalityOrPrescedence;
            else
                return -1;
        }
        TokenType(TokenType parent, int cardinalityOrPrescedence) {
            this.parent = parent;
            if (this.parent != null) {
                this.parent.addChild(this);
            }
            this.cardinalityOrPrescedence = cardinalityOrPrescedence;
        }
        //Methods to extract parent/child relationships.

        private void addChild(TokenType child) {
            children.add(child);
        }

        TokenType getParent() {
            return parent;
        }

        //Normally case is of no consequence
        static TokenType valueOfIgnoreCase(String token) {
            for (TokenType t:TokenType.values()) {
                if (t.name().equalsIgnoreCase(token))
                    return t;
            }
            return null;
        }

    }


    //Operands are transformed into functions. Below a name mapping.
    private final static String[] Operands = new String[]	     {"=",">","<","+","-","*","/",">=","<=","<>","=>","=<"};
    private final static String[] OperandFunctions= new String[]	 {"eq","gt","lt","add","subtract","multiply","divide","gte","lte","neq","gte","lte"};
    private static LoggerI o;
    private static Map<String,String> currentKeyChain=null;
    private static Set<Variable> variables=null;
    //TODO solution for the  string reverse for string a+b..
    //static String tret=null;

// --Commented out by Inspection START (9/29/2018 4:31 PM):
//	/**
//	 * Takes an input string and replaces all expr with values.
//	 */
//	public static String analyze(String text) {
//		StringBuilder endResult=null;
//		List<Token> result = tokenize(text);
//		if (result!=null) {
//			if (testTokens(result)) {
//				if (result!=null) {
//					StreamAnalyzer streamAnalyzer = new StreamAnalyzer(result);
//					endResult = new StringBuilder();
//					while (streamAnalyzer.hasNext()) {
//
//						Object rez=null;
//						rez = streamAnalyzer.next();
//						if (rez!=null) {
//							endResult.append(rez);
//						}
//					}
//				}
//				//System.out.println();
//				return endResult.toString();
//			}
//			else
//				System.err.println("FAIL testtokens");
//
//
//		} else
//			System.err.println("FAIL tokenize");
//		return null;
//	}
// --Commented out by Inspection STOP (9/29/2018 4:31 PM)


    public static List<EvalExpr> preCompileExpression(String expression) {
        if (expression==null) {
            Log.e("vortex","Precompile expression returns immediately on null string input");
            return null;
        }
        o = WorkFlowBundleConfiguration.debugConsole;
        Log.d("franco","Precompiling: "+expression);
        List<Token> result = tokenize(expression);
        //printTokens(result);
        List<EvalExpr> endResult = new ArrayList<>();
        if (result!=null && testTokens(result)) {
            StreamAnalyzer streamAnalyzer = new StreamAnalyzer(result);
            while (streamAnalyzer.hasNext()) {

                EvalExpr rez=null;
                rez = streamAnalyzer.next();
                if (rez!=null) {
                    endResult.add(rez);
                } else {
                    o.addRow("");
                    o.addRedText("Subexpr evaluated to null while evaluating "+expression);
                    System.err.println("Tokenstream evaluated to null for: "+streamAnalyzer.getFaultyTokens());
                }
            }
            if (endResult.size()>0){
                //StringBuilder sb = new StringBuilder();
                //for (EvalExpr e:endResult)
                //	sb.append(e);
                //o.addRow("");
                //o.addRow("Precompiled: "+sb);
                Log.d("franco","Precompiled: "+endResult.toString());
                return endResult;
            }

        }
        o.addRow("");
        o.addRedText("failed to precompile: "+expression);
        o.addRow("");
        o.addRedText("End Result: "+endResult);
        Log.e("vortex","failed to precompile: "+expression);
        Log.e("vortex","End Result: "+endResult);
        printTokens(result);

        return null;
    }


    public static String analyze(List<EvalExpr> expressions) {
        return analyze(expressions,GlobalState.getInstance().getVariableCache().getContext().getContext());
    }
    public static int[] intAnalyzeList(List<EvalExpr> expressions) {
        currentKeyChain = GlobalState.getInstance().getVariableCache().getContext().getContext();
        int i=0;
        int[] res = new int[expressions.size()];
        for (EvalExpr expr:expressions) {
            //tret = null;
            Object rez = expr.eval();
            Integer intVal = null;
            if (rez instanceof Integer)
                intVal = (Integer) rez;
            else if (rez instanceof Double)
                intVal = ((Double) rez).intValue();
            if (intVal !=null) {
                res[i++] = intVal;
            }
            else
                System.err.println("Got NULL back when evaluating " + expr.toString() + " . will not be included in endresult.");
        }

        return res;
    }
    //analyze within a given variable set as context. This allows for incomplete variable references.
    //Eg. cars:Vovlo_lot_count can be referred to as "lot_count".
    public static String analyze(EvalExpr expression, Set<Variable> variablez) {

        if (variablez==null || variablez.isEmpty()) {
            Log.e("vortex","Empty variable set in analyze:Expressor. Will use current context");
            currentKeyChain = GlobalState.getInstance().getVariableCache().getContext().getContext();
            variables = null;
        } else {
            variables = variablez;
            currentKeyChain = variablez.iterator().next().getKeyChain();
        }
        Object ob = expression.eval();
        if (ob!=null)
            return ob.toString();
        else
            return null;
    }

    public static String analyze(List<EvalExpr> expressions, Map<String,String> evalContext) {


        o = GlobalState.getInstance().getLogger();
        if (expressions == null) {
            o.addRow("");
            o.addRedText("Expression was null in Analyze. This is likely due to a syntax error in the original formula");
            return null;
        }
        //evaluate in default context.
        currentKeyChain = evalContext;
        //Log.d("franco","Analyzing "+expressions.toString());
        StringBuilder endResult = new StringBuilder();
        for (EvalExpr expr:expressions) {
            //tret=null;
            Object rez;
            //System.out.println("Analyze: "+expr.toString());
            rez = expr.eval();
            if (rez!=null) {
                //System.out.println("Part Result "+rez.toString());
                endResult.append(rez);
            } else
                System.err.println("Got null back when evaluating "+expr.toString()+" . will not be included in endresult.");

        }

        //Log.d("franco",expressions.toString()+" -->  "+endResult.toString());
        if (endResult.toString().isEmpty())
            return null;
        else
            return endResult.toString();
    }

    //Analyze expression using a targetlist.

    public static Boolean analyzeBooleanExpression(EvalExpr expr, List<List<String>> target) {
        return analyzeBooleanExpression(expr,GlobalState.getInstance().getVariableCache().getContext().getContext(),target);
    }

    //Analyze expression with current full table as target.

    public static Boolean analyzeBooleanExpression(EvalExpr expr) {
        return analyzeBooleanExpression(expr,GlobalState.getInstance().getVariableCache().getContext().getContext(),null);
    }


    //analyze within a given variable set as context. This allows for incomplete variable references.
    //Eg. cars:Vovlo_lot_count can be referred to as "lot_count".

    public static Boolean analyzeBooleanExpression(EvalExpr expression, Set<Variable> variablez) {
        if (variablez==null || variablez.isEmpty()) {
            Log.e("vortex","Empty variable set in analyze:Expressor. Will use current context");
            return analyzeBooleanExpression(expression, GlobalState.getInstance().getVariableCache().getContext().getContext(),null);
        } else {
            variables = variablez;
            return analyzeBooleanExpression(expression, variablez.iterator().next().getKeyChain(),null);

        }
    }


    public static Boolean analyzeBooleanExpression(EvalExpr expr, Map<String,String> evalContext, List<List<String>> targetList) {
        //tret=null;
        if (expr==null) {
            variables = null;
            return null;
        }
        Expressor.targetList=targetList;

        o = GlobalState.getInstance().getLogger();
        currentKeyChain = evalContext;
        // Log.d("Vortex","Class "+expr.getClass().getCanonicalName());
        Object eval = expr.eval();
        //Log.d("Vortex","BoolExpr: "+expr.toString()+" evaluated to "+eval);
        //o.addRow("Expression "+expr.toString()+" evaluated to "+eval);
        variables = null;
        if (eval !=null && !(eval instanceof Boolean)) {
            Log.e("vortex","eval was not bool back in analyzeBoolean...likely missing [..]?");
            o.addRow("");
            o.addRedText("The expression "+expr.toString()+" evaluated to: '"+eval.getClass()+"' but must be Boolean. Missing [ ] around the expression can cause this");
            return false;
        } else
            return (Boolean)eval;
    }
    /**
     * Class Token
     * @author Terje
     * Expressions are made up of Tokens. Tokens are for instance Numbers, Literals, Functions.
     *
     */
    static class Token implements Serializable {

        private static final long serialVersionUID = -1975204853256767316L;
        String str;
        TokenType type;
        Token(String raw, TokenType t) {
            str=raw;
            type=t;
        }
    }

    //Exception for Evaluation failures.

    static class ExprEvaluationException extends Exception {

        private static final long serialVersionUID = 1107622084592264591L;

    }

    /**
     * Class StreamAnalyzer
     * @author Terje
     * Takes an Iterator and allows caller to read the evaluation objects as a stream.
     */
    private static class StreamAnalyzer {
        final Iterator<Token> mIterator;
        List<Token> curr;
        int depth = 0;
        StreamAnalyzer(List<Token> tokens) {
            mIterator = tokens.iterator();
            curr=null;
        }
        boolean hasNext() {
            return mIterator.hasNext();
        }

        EvalExpr next() {


            while (mIterator.hasNext()) {
                Token t = mIterator.next();
                if (t.type==TokenType.text)
                    return new Text(t);
                if (curr!=null&&t.type==TokenType.leftparenthesis)
                    depth++;
                if (curr!=null&&t.type==TokenType.rightparenthesis)
                    depth--;
                if (t.type==TokenType.startMarker) {
                    curr = new ArrayList<>();
                    //new token either if endmarker, or a comma on toplevel.
                } else if (t.type==TokenType.endMarker || (t.type==TokenType.comma && depth==0)) {
                    if (curr!=null && !curr.isEmpty()) {
                        //Log.d("franco","CURR tokens: ");
                        //printTokens(curr);
                        EvalExpr ret = analyzeExpression(curr);
                        if (ret==null)
                            System.err.println("Eval of expression "+curr.toString()+" failed");
                        curr = new ArrayList<>();
                        return ret;
                    } else {
                        System.err.println("Empty Expr or missing startTag.");
                        return null;
                    }
                }
                if (curr!=null)
                    curr.add(t);
                else
                    System.err.println("Discarded "+t.str);


            }
            System.err.println("Missing end marker for Expr ']'");
            return null;
        }

        String getFaultyTokens() {
            StringBuilder sres=new StringBuilder();
            for (Token c:curr) {
                sres.append(c.toString());
            }
            return sres.toString();
        }
    }

    //Temp Entry point for testing purposes.



    private static List<Token> tokenize(String formula) {
        System.out.println("Tokenize this: "+formula);
        List<Token> result= new ArrayList<>();
        char c;
        StringBuilder currToken=new StringBuilder();
        TokenType t = TokenType.none;

        //This is added to support regexp sections that should not be interpreted.
        //Everything within {} will be treated as literals.
        boolean chompAnyCharacter=false;
        boolean inside = false,unary=false;

        for (int i = 0; i < formula.length(); i++){
            c = formula.charAt(i);

            if (!inside) {
                if (c=='[') {
                    inside = true;
                    add(currToken,TokenType.text,result);
                    currToken.append(c);
                    add(currToken,TokenType.startMarker,result);
                    t = TokenType.none;
                }
                else {
                    t=TokenType.text;
                    currToken.append(c);
                }
                continue;
            }
            if (chompAnyCharacter) {
                if (c=='}') {
                    add(currToken,t,result);
                    t=TokenType.none;
                    chompAnyCharacter=false;

                } else
                    currToken.append(c);

                continue;
            }

            //if a digit, variable or letter comes after an operand, save it.
            if (t == TokenType.operand && (Character.isDigit(c) || Character.isLetter(c)||c=='$')) {
                //save operand.
                add(currToken,t,result);
                //add number.
                t = TokenType.none;
            }
            if (Character.isDigit(c)) {
                if (t == TokenType.none)
                    t = TokenType.number;
                currToken.append(c);
            }
            else if (Character.isLetter(c)) {
                switch (t) {
                    case none:
                    case number:
                        t=TokenType.literal;
                        break;
                }
                currToken.append(c);
            }


            else if (Character.isWhitespace(c)) {
                add(currToken,t,result);
                //Discard whitespace.
                t= TokenType.none;
            }
            else if (c=='$') {
                t = TokenType.variable;
            }
            else if (c=='(' || c==')' || c==',') {
                if (t != TokenType.none){
                    //add any token on left of operand
                    add(currToken,t,result);
                }
                switch (c) {
                    case '(':
                        t=TokenType.leftparenthesis;
                        break;
                    case ')':
                        t=TokenType.rightparenthesis;
                        break;
                    case ',':
                        t=TokenType.comma;
                        break;
                }
                add(c,t,result);
                t= TokenType.none;
            }

            else if (c=='<' || c=='>' || c=='=' || c == '+' || c == '*' || c == '/' || c=='-') {
                if (t != TokenType.none && t != TokenType.operand){
                    //add any token on left of operand
                    add(currToken,t,result);
                }
                //unary minus
                //System.out.println("Found zunary operator "+c+" i "+i);
                if ((t == TokenType.operand || (t == TokenType.none&&(i==1||result.get(result.size()-1).type==TokenType.leftparenthesis || result.get(result.size()-1).type==TokenType.operand))) && (c =='-') && (i+1)!=formula.length() && (!Character.isWhitespace(formula.charAt(i+1)))) {
                    //if ((t == TokenType.operand || (t == TokenType.none&&(i==1||result.get(result.size()-1).type==TokenType.leftparenthesis))) && (c =='-') && (i+1)!=formula.length() && (!Character.isWhitespace(formula.charAt(i+1)))) {
                    //System.out.println("Found unary operator "+c);
                    //System.out.println("Currtorken: "+currToken.toString());
                    add(currToken,t,result);
                    currToken.append(c);
                    t=TokenType.unaryMinus;
                    add(currToken,t,result);
                    t=TokenType.none;
                } else {
                    currToken.append(c);
                    t=TokenType.operand;
                }
            }
            else if (c=='{') {
                //all characters now treated as being literal.
                add(currToken,t,result);
                t=TokenType.literal;
                chompAnyCharacter=true;
            }
            else if (c==']') {
                add(currToken,t,result);
                currToken.append(c);
                add(currToken,TokenType.endMarker,result);

                inside = false;
            }

            else {
                currToken.append(c);
                //System.out.println("unrecognized: "+c+" AT POS "+i+" in "+formula+" chomp: "+chompAnyCharacter);
            }
        }
        if (inside) {
            System.err.println("Missing end bracket");
            return null;
        }
        //System.out.println("Reached end of tokenizer. CurrentToken is "+currToken+" and t is "+t.name());

        if (t != TokenType.none)
            add(currToken,t,result);

        return result;
    }


    private static void add(char c, TokenType t, List<Token> result) {
        result.add(new Token(String.valueOf(c),t));
    }


    private static void add(StringBuilder currToken, TokenType t,List<Token> result) {
        //need to change tokentype if literal and keyword.
        if (currToken.length()!=0)
            result.add(new Token(currToken.toString(),t));
        currToken.setLength(0);
    }



    //check rules between token pairs.
    private static boolean testTokens(List<Token> result) {
        //Rule 1: op op
        o = WorkFlowBundleConfiguration.debugConsole;
        boolean valueF=false,booleanF=false;
        Token current=null,prev=null;
        int pos=-1,lparC=0,rparC=0;
        for (Token t:result) {
            pos++;
            if (t.type==TokenType.text) {
                //Skipp text.
                continue;
            }
            //check number of parenthesis...
            if (t.type.getParent()==TokenType.parenthesis) {
                if (t.type==TokenType.rightparenthesis)
                    rparC++;
                else
                    lparC++;
            }
            if (current==null && prev == null) {
                prev=t;
                continue;
            }
            else if (current == null) {
                current = t;
            }
            else {
                prev = current;
                current = t;
            }

            //try to find supported functions and change to correct type.
            if (prev.type==TokenType.literal) {

                //Check for PI
                if (prev.str.equals("PI")) {
                    prev.type=TokenType.number;
                    prev.str=Double.toString(Math.PI);
                    //System.out.println("Found PI!"+prev.str);
                    continue;
                }

                TokenType x = TokenType.valueOfIgnoreCase(prev.str);

                //check if AND OR
                if (isLogicalOperand(x))

                    prev.type=TokenType.operand;

                else
                    //check if function
                    if (current.type == TokenType.leftparenthesis) {
                        if (x==null) {
                            o.addRedText("Syntax Error: Function "+prev.str+" does not exist!");
                            return false;
                        }
                        if (isFunction(x)) {
                            TokenType parent = x.getParent();
                            //System.out.println("found function match : "+prev.str);
                            prev.type = x;
                            //Check that there aren't both logical and value functions in the same expression.
                            //System.out.println("parent: "+parent);
                            if (parent == TokenType.valueFunction)
                                valueF=true;
                            if (parent == TokenType.booleanFunction)
                                booleanF = true;
                        } else {
                            o.addRow("");
                            o.addRedText("The token "+prev.str+" is used as function, but is in fact a "+prev.type);
                            return false;
                        }
                    }
            }

            else if (prev.type==TokenType.operand) {
                boolean found=false;
                for (int i=0;i<Operands.length;i++) {
                    if(prev.str.equalsIgnoreCase(Operands[i])) {
                        prev.str=OperandFunctions[i];
                        //System.out.println("Replaced "+Operands[i]+" with corresponding operand function: "+prev.str);
                        found =true;
                    }

                }
                if (!found) {
                    o.addRow("");
                    o.addRedText("Syntax Error: Operator "+prev.str+" does not exist.");
                    System.err.println("Syntax Error: Operator "+prev.str+" does not exist.");
                    return false;
                }
            }
            //if (prev.type == current.type && current.type.getParent()!=TokenType.parenthesis) {
            //	System.err.println("Rule 1. Syntax does not allow repetition of same type at token "+pos+": "+prev.str+":"+current.str);
            //	return false;
            //}
        }
        //Check for unbalanced paranthesis
        if (lparC!=rparC) {
            o.addRow("");
            o.addRedText("Unequal number of left and right parenthesis. Left: "+lparC+" right: "+rparC);
            System.err.println("Rule 2. Equal number of left and right parenthesis. Left: "+lparC+" right: "+rparC);
            return false;
        }
        //Check for mix between data types
        //if (valueF&&booleanF) {
        //	System.err.println("Rule 3. Both logical(true-false) and value functions present. This is not allowed");
        //	return false;
        //}
        return true;
    }



    //And expression is one or a set of tokens, making up a semantic entity, such as function.
    //This tool cuts out the next expression from the token stream.

    private static class ExpressionAnalyzer {
        //stream to use
        private final Iterator<Token>it;

        ExpressionAnalyzer(Iterator<Token> iterator) {
            it = iterator;
        }


        boolean hasNext() {
            return it.hasNext();
        }
        Expr next() {
            Token t;
            if (it.hasNext()) {
                t = it.next();
                //System.out.println("In next: "+t.type);
                assert(t!=null);
                TokenType type = t.type;

                switch (type) {
                    case leftparenthesis:
                        return new Push();
                    case rightparenthesis:
                        return new Pop();
                    case variable:
                    case number:
                    case literal:
                    case comma:
                        return new Atom(t);
                    case operand:
                        return new Operand(t);
                    case text:
                        return new Text(t);

                }
                TokenType p = type.parent;
                if (isFunction(type)) {
                    return new Function(type, it);
                }
            }

            return null;

        }
    }

    //marker class
    abstract static class Expr implements Serializable {
        private static final long serialVersionUID = -1968204853256767316L;
        private final TokenType type;

        Expr(TokenType t) {
            type = t;
        }
        TokenType getType() {
            return type;
        }
    }

    public abstract static class EvalExpr extends Expr {
        EvalExpr(TokenType t) {
            super(t);
        }

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        abstract Object eval();


    }

    public static class Atom extends EvalExpr {
        final Token myToken;
        Atom(Token t) {
            super (t.type);
            myToken = t;
        }
        @Override
        public String toString() {
            if (myToken!=null)
                return myToken.str;
            else
                return null;
        }

        public boolean isVariable() {
            return (getType()==TokenType.variable);
        }

        public Object eval() {
            //Log.d("vortex","In eval for Atom type "+type);
            String value;
            switch(getType()) {
                case variable:
                    Variable v=Expressor.getVariable(myToken.str);

                    if (v==null || v.getValue() == null ) {
                        System.out.println("Variable '"+this.toString()+"' does not have a value or Variable is missing.");
                        return null;
                    }

                    value = v.getValue();
                    //Log.d("vortex","Atom variable ["+v.getId()+"] Type "+v.getType()+" Value: "+value);
                    if (v.getType()!= DataType.text && Tools.isNumeric(value)) {
                        Log.d("vortex","numeric");
                        double d = Double.parseDouble(value);
                        if (v.getType()== Variable.DataType.decimal || value.contains(".") || d>Integer.MAX_VALUE || d<Integer.MIN_VALUE)
                            return d;
                        else
                            return Integer.parseInt(value);
                    }
                    if (v.getType()==Variable.DataType.bool) {
                        	Log.d("vortex","bool");
                        if (value.equalsIgnoreCase("false")) {
                            Log.d("vortex","Returning false");
                            return false;
                        }
                        else if (value.equalsIgnoreCase("true")) {
                            Log.d("vortex","Returning true");
                            return true;
                        }
                        else {
                            Log.e("vortex","Not a bool value: "+value);
                            return null;
                        }
                    }
                    Log.d("vortex","literal");
                    if (value.isEmpty()) {
                        Log.e("vortex","empty literal...returning null");
                        return null;
                    }
                    return value;
                case number:
                    //Log.d("vortex","this is a numeric atom");
                    if (myToken !=null && myToken.str!=null) {
                        //	System.out.println("Numeric value: "+myToken.str);
                        if (myToken.str.contains("."))
                            return Double.parseDouble(myToken.str);
                        else
                            return Integer.parseInt(myToken.str);
                    }
                    else {
                        System.err.println("Numeric value was null");
                        return null;
                    }
                case literal:
                    //Log.d("vortex","this is a literal atom");
                    if (myToken.str.equalsIgnoreCase("false"))
                        return false;
                    else if (myToken.str.equalsIgnoreCase("true"))
                        return true;
                    else
                        return toString();

                default:
                    System.err.println("Atom type has no value: "+this.getType());
                    return null;
            }
        }
    }

    private static class Operand extends Expr {
        final Token myToken;
        Operand(Token t) {
            super(t.type);
            myToken = t;

        }
        @Override
        public String toString() {
            return myToken.str;
        }



    }

    private static class Convoluted extends EvalExpr {
        final EvalExpr arg1,arg2;
        final Operand operator;


        Convoluted(Expr newArg, Expr existingArg, Operand operator) {

            super(null);
            this.arg1 = (EvalExpr) existingArg;
            this.arg2 = (EvalExpr) newArg;
            this.operator=operator;
        }
        @Override
        public String toString() {
            String arg1s = arg1.toString();
            String arg2s = arg2.toString();
            if (arg1s==null)
                arg1s="?";
            if (arg2s==null)
                arg2s="?";
            return String.format("%s(%s,%s)", operator.toString(), arg1s, arg2s);
        }

        public Object eval()  {
            //Log.d("vortex","In eval for convo");
            Object arg1v = arg1.eval();


            if (arg1v==null) {
                String opS =operator.myToken.str;
                if (opS!=null) {
                    TokenType op = TokenType.valueOfIgnoreCase(opS);
                    if (op != null && op.equals(TokenType.or)) return arg2.eval();
                }
                return null;
            }

            Object arg2v = arg2.eval();

            Log.e("vortex",(arg1v.toString())+ " " + operator.myToken.str+" "+((arg2v==null)?"null":arg2v.toString()));
            if (arg2v==null) {
                Log.e("vortex","Arg2 is null! Operator is "+operator.myToken.str);
                String opS =operator.myToken.str;
                if (opS!=null) {
                    TokenType op = TokenType.valueOfIgnoreCase(opS);
//					Log.d("vortex","op is "+op+" which equals and? "+op.equals(TokenType.and));
                    if (op != null) {
                        if (op.equals(TokenType.or)) {
                            if (arg1v instanceof Boolean)
                                if ((Boolean) arg1v) {
                                    Log.d("vortex","arg1 is true so returning true");
                                    return true;
                                } else
                                    Log.d("vortex","arg1 is false");
                        }
                        else if (op.equals(TokenType.and)) {
//								Log.d("vortex","operator is AND! Arg1: "+arg1v);
                            if (arg1v instanceof Boolean)
                                if (!((Boolean) arg1v))
                                    return false;
                        }
                    }
                }
                Log.d("vortex","...returning null");
                return null;
            }

            //functions require both arguments be of same kind.

            boolean isNumericOperator = Tools.isNumeric(arg1v) && Tools.isNumeric(arg2v);
            boolean isBooleanOperator = arg1v instanceof Boolean
                    && arg2v instanceof Boolean;


            //System.err.println("arg1: "+arg1v+" arg2: "+arg2v+ "arg1vClass: "+arg1v.getClass()+" arg2vClass: "+arg2v.getClass());
            //Requires Double arguments.
            try {
                if (isNumericOperator) {
                    double arg1F, arg2F;
                    Object res = null;
                    arg1F = castToDouble(arg1v);
                    arg2F = castToDouble(arg2v);
/*
					if (isDoubleOperator) {
						arg1F = ((Double) arg1v).doubleValue();
						arg2F = ((Double) arg2v).doubleValue();
					} else {
						if (arg1v instanceof Integer)
							arg1F = ((Integer) arg1v).doubleValue();
						else
							arg1F = (Double) arg1v;
						if (arg2v instanceof Integer)
							arg2F = ((Integer) arg2v).doubleValue();
						else
							arg2F = (Double) arg2v;
					}
*/
                    String opS = operator.myToken.str;
                    if (opS != null) {
                        TokenType op = TokenType.valueOf(opS);
                        switch (op) {

                            case add:
                                res =  (arg1F + arg2F);
                                break;
                            case subtract:
                                res = (arg1F - arg2F);
                                break;
                            case multiply:
                                res = (arg1F * arg2F);
                                break;
                            case divide:
                                res =  (arg1F / arg2F);
                                break;
                            case eq:
                                res = arg2F == arg1F;
                                Log.e("vortex", "arg1F eq arg2F? " + arg1F + " eq " + arg2F + ": " + res);
                                break;
                            case neq:
                                res = arg1F != arg2F;
                                Log.e("vortex", "arg1F neq arg2F? " + arg1F + " neq " + arg2F + ": " + res);
                                break;
                            case gt:
                                res = arg1F > arg2F;
                                break;
                            case lt:
                                res = arg1F < arg2F;
                                break;
                            case lte:
                                res = arg1F <= arg2F;
                                break;
                            case gte:
                                res = arg1F >= arg2F;
                                break;
                            default:
                                System.err.println("Unsupported operand: " + op);
                                o.addRow("");
                                o.addRedText("Unsupported arithmetic operator: " + op);
                                break;
                        }
                    } else {
                        System.err.println("Unsupported arithmetic operand: " + operator.getType());
                        o.addRow("");
                        o.addRedText("Unsupported arithmetic operand: " + operator.getType());
                    }
                    Log.e("vortex","RESULT: "+res);
                    return res;
                }

                //Requires boolean arguments.
                else if (isBooleanOperator) {
                    Boolean arg1B,arg2B,res=null;

                    arg1B=(Boolean)arg1v;
                    arg2B=(Boolean)arg2v;
                    String opS =operator.myToken.str;
                    if (opS!=null) {
                        TokenType op = TokenType.valueOfIgnoreCase(opS);
                        if (op != null) {
                            switch (op) {
                                case or:
                                    res = (arg1B||arg2B);
                                    //Log.e("vortex","OR Evaluates to "+res+" for "+arg1B+" and "+arg2B);
                                    break;
                                case and:
                                    //System.err.println("Gets to and");
                                    res = (arg1B&&arg2B);
                                    break;
                                case eq:
                                    res = (arg1B==arg2B);
                                    break;
                                default:

                                    System.err.println("Unsupported boolean operand: "+op);
                                    o.addRow("");
                                    o.addRedText("Unsupported boolean operator: "+op);
                                    break;
                            }
                        }

                    }
                    return res;
                    // if not boolean and not numeric it is literal.
                } else  {
                    String arg1S=arg1v.toString();
                    String arg2S=arg2v.toString();
                    TokenType op = TokenType.valueOfIgnoreCase(operator.myToken.str);
                    //System.out.println("in isliteral with exp: "+arg1S+" "+operator.myToken.str+" "+arg2S);
                    o.addText("calculating literal expression "+arg1S+" "+operator.myToken.str+" "+arg2S);

                    if (op != null) {
                        switch (op) {
                            case add:
                        /*
                        if (tret==null) {
                            tret ="foock";
                            Log.d("vortex","first so returning "+arg1S+arg2S);
                            return arg1S+arg2S;
                        } else
                        */
                                return arg1S+arg2S;

                            case eq:
                                return arg1S.equals(arg2S);
                            case neq:
                                return !arg1S.equals(arg2S);
                            default:
                                System.err.println("Unsupported literal operand: "+op);
                                o.addRow("");
                                o.addRedText("Unsupported literal operator: "+op+" a1: "+arg1S+" a2: "+arg2S);
                                break;
                        }
                    }
                }
            } catch (ClassCastException e) {
                Log.d("vortex","Classcast exception for expression "+this.toString()+"arg1: "+arg1v);
                o.addRow("");
                o.addRedText("Illegal arguments (wrong type) in expression: " +this.toString()+". Missing $ operator?");

            }
            return null;
        }

        private double castToDouble(Object arg) {
            if (arg instanceof Double)
                return (Double) arg;
            if (arg instanceof Integer)
                return ((Integer) arg).doubleValue();
            if (arg instanceof Float)
                return ((Float) arg).doubleValue();
            if (arg instanceof String)
                return Double.parseDouble((String)arg);
            o.addRedText("I never get here...Object is a "+arg.getClass());
            return -1;
        }

    }





    private static class Push extends Expr {

        Push() {
            super(null);
        }
        private static final long serialVersionUID = 4443625476068076080L;
        @Override
        public String toString() {
            return "parenthesis";
        }
    }
    private static class Pop extends Expr {
        Pop() {
            super(null);
        }
        private static final long serialVersionUID = 2499591806981280542L;
        @Override
        public String toString() {
            return "parenthesis";
        }
    }
    static class Text extends EvalExpr {
        private final String str;
        Text(Token t) {
            super(TokenType.text);
            this.str=t.str;
        }
        @Override
        String eval() {
            return str;
        }
        @Override
        public String toString() {
            return str;
        }
    }

    //cut out function from full expression.
    private static class Function extends EvalExpr {
        //try to build a function from the tokens in the beg. of the given token stream.

        private static final int No_Null = 1;
        private static final int No_Null_Numeric=2;
        private static final int No_Null_Literal=3;
        private static final int NO_CHECK = 4;
        private static final int Null_Numeric = 5;
        private static final int Null_Literal = 6;
        private static final int No_Null_Boolean = 7;
        private static final int Null_Boolean = 8;

        private final List<EvalExpr> args = new ArrayList<>();

        Function(TokenType type, Iterator<Token> it) {
            super(type);
            //iterator reaches end?
            int depth=0;
            Token e;
            final List<List<Token>> argsAsTokens = new ArrayList<>();
            List<Token> funcArg = new ArrayList<>();
            boolean argumentReady=false;


            while(it.hasNext()) {
                e = it.next();
                //system.out.println("Expr "+e.type);

                if (e.type==TokenType.leftparenthesis) {
                    depth++;
                    //system.out.println("+ depth now "+depth);
                    //discard paranthesis...not used.
                    if (depth==1)
                        continue;
                } else if (e.type==TokenType.rightparenthesis) {
                    depth--;
                    //system.out.println("- depth now "+depth);
                    if (depth==0)
                        argumentReady=true;
                }  else if (e.type==TokenType.comma && depth==1 ) {
                    argumentReady = true;
                }

                if (!argumentReady) {
                    //System.out.println("Added "+e.str+" to funcArg. I am  "+type);
                    funcArg.add(e);
                    if (depth==0 && type == TokenType.unaryMinus) {
                        //System.out.println("Found argument for unary f "+funcArg.get(0).str+" l "+funcArg.size());
                        argumentReady = true;
                    }
                }

                if (argumentReady) {
                    if (!funcArg.isEmpty()) {
                        argsAsTokens.add(funcArg);
                        funcArg= new ArrayList<>();
                    }
                    //					else
                    //						;
                    //system.out.println("No argument in function "+type.name());


                    if (e.type==TokenType.rightparenthesis || type==TokenType.unaryMinus)
                        break;
                    else
                        argumentReady = false;

                }
            }
            if (!argumentReady) {
                System.err.println("Missing closing paranthesis in function "+type.name());
                return;
                //printTokens(funcArg);
            }
            //recurse for each argument.
            int i=1;
            for (List<Token> arg:argsAsTokens) {
                //system.out.println("Recursing over argument "+i++ +"in function "+type.name()+" :");
                //printTokens(arg);
                EvalExpr analyzedArg;
//				try {
                analyzedArg = analyzeExpression(arg);
                if (analyzedArg != null) {
                    args.add(analyzedArg);
                } else {
                    System.err.println("Fail to parse: ");
                    printTokens(arg);
                }
					/*
				} catch (ExprEvaluationException e1) {
					System.err.println("Fail to parse :");
					printTokens(arg);
				}
				*/

            }
        }

        @Override
        public Object eval() {

            //Log.d("vortex","Function eval: "+getType());

            Object result=null;
            List<Object> evalArgs = new ArrayList<>();
            VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
            VariableCache varCache = GlobalState.getInstance().getVariableCache();
            int j=0;
            double arg1F=0,arg2F=0;
            for (EvalExpr arg:args) {
                result = arg.eval();
                evalArgs.add(result);
                if (j==0) {
                    if (result instanceof Integer)
                        arg1F = ((Integer) result).doubleValue();
                    if (result instanceof Double)
                        arg1F = (Double) result;
                }
                else if (j==1) {
                    if (result instanceof Integer)
                        arg2F = ((Integer) result).doubleValue();
                    if (result instanceof Double)
                        arg2F = (Double) result;
                }
                j++;

            }


            boolean gH=false;


            //Now all arguments are evaluated. Execute function!

            switch (getType()) {

                case max:
                    if (checkPreconditions(evalArgs,2,No_Null_Numeric))
                        return Math.max(arg1F, arg2F);
                    break;
                case abs:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.abs(arg1F);
                    break;
                case acos:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.acos(arg1F);
                    break;
                case asin:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.asin(arg1F);
                    break;
                case atan:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.atan(arg1F);
                    break;
                case ceil:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.ceil(arg1F);
                    break;
                case cos:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.cos(arg1F);
                    break;
                case exp:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.exp(arg1F);
                    break;
                case floor:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.floor(arg1F);
                    break;
                case log:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.log(arg1F);
                    break;
                case round:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.round(arg1F);
                    break;
                case sin:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.sin(arg1F);
                    break;
                case sqrt:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.sqrt(arg1F);
                    break;
                case tan:
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric))
                        return Math.tan(arg1F);
                    break;
                case atan2:
                    if (checkPreconditions(evalArgs,2,No_Null_Numeric))
                        return Math.atan2(arg1F,arg2F);
                    break;
                case min:
                    if (checkPreconditions(evalArgs,2,No_Null_Numeric))
                        return Math.min(arg1F,arg2F);
                    break;
                case pow:
                    if (checkPreconditions(evalArgs,2,No_Null_Numeric))
                        return Math.pow(arg1F,arg2F);
                    break;

                case iff:
                    if (checkPreconditions(evalArgs,3,NO_CHECK)) {
                        if (evalArgs.get(0) instanceof Boolean) {
                            if ((Boolean)evalArgs.get(0))
                                return evalArgs.get(1);
                            else
                                return evalArgs.get(2);
                        }
                    }
                    break;
                case unaryMinus:
                    //Log.d("vortex","In function unaryminus");
                    if (checkPreconditions(evalArgs,1,No_Null_Numeric)){
                        Log.d("vortex","returning: "+ (-(Integer)evalArgs.get(0)));
                        return -((Integer)evalArgs.get(0));
                    }
                    break;
                case not:
 //                   Log.d("vortex","in function not with evalArgs: "+evalArgs);
                    if (checkPreconditions(evalArgs,1,Null_Boolean)) {
//                        Log.d("vortex","evalArgs.get0 is "+evalArgs.get(0)+" type "+evalArgs.get(0).getClass().getSimpleName());
                        return evalArgs.get(0)==null?null:!((Boolean)evalArgs.get(0));

                    }
                    break;

                case historical:
                    Log.d("vortex","In historical with "+evalArgs.get(0));
                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        Variable var = GlobalState.getInstance().getVariableCache().getVariable(evalArgs.get(0).toString());
                        if (var != null) {
                            String value = var.getHistoricalValue();
                            Log.d("vortex","Found historical value "+value+" for variable "+var.getLabel());
                            return value;
                        } else {
                            Log.e("vortex","Variable not found for literal: ["+evalArgs.get(0)+"]");
                            o.addRow("");
                            o.addRedText("Variable not found in historical: ["+evalArgs.get(0)+"]");
                        }
                    } else
                        Log.e("vortex","Argument failed nonull literal"+evalArgs.get(0));
                    break;
                case hasSameValueAsHistorical:
                    String groupName = (String)evalArgs.get(0);
                    String varName = (String)evalArgs.get(1);
                    Log.d("vortex","in samevalueas historical with group ["+groupName+"] and variable ["+varName+"]");

                    if (checkPreconditions(evalArgs,2,No_Null_Literal)) {
                        Cursor c = GlobalState.getInstance().getDb().getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(GlobalState.getInstance().getVariableCache().getContext().getContext(),groupName,varName);
                        Map<String,String> vars = new HashMap<>();
                        Map<String,String> histVars = new HashMap<>();
                        while (c.moveToNext())
                            vars.put (c.getString(0),c.getString(1));
                        c.close();
                        Map<String, String> histKeyMap = Tools.copyKeyHash(GlobalState.getInstance().getVariableCache().getContext().getContext());
                        histKeyMap.put("år",Constants.HISTORICAL_TOKEN_IN_DATABASE);
                        c = GlobalState.getInstance().getDb().getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(histKeyMap,groupName,varName);
                        while (c.moveToNext())
                            histVars.put (c.getString(0),c.getString(1));
                        c.close();
                        if (!vars.isEmpty()) {
                            Log.d("vortex","Found candidates!");
                            for (String name:vars.keySet()) {
                                String value = vars.get(name);
                                if (value!=null) {
                                    String historicalValue = histVars.get(name);
                                    if (historicalValue==null) {
                                        Log.d("vortex","hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" but no historical value.");
                                        o.addRow("hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" but no historical value.");
                                        return false;
                                    } else {
                                        if (!historicalValue.equals(value)) {
                                            Log.d("vortex","hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" that is not the same as the historical value: "+historicalValue);
                                            o.addRow("hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" that is not the same as the historical value: "+historicalValue);

                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true;

                case getHistoricalListValue:
                    gH=true;
                case getListValue:
                    String strRes="?";
                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        varName = (String)evalArgs.get(0);
                        Log.d("vortex","in listvalue with variable "+varName);
                        Variable v = GlobalState.getInstance().getVariableCache().getVariable(varName);
                        if (v != null) {
                            strRes = gH?v.getHistoricalValue():v.getValue();
                            if (v.getType() == Variable.DataType.list) {
                                Log.d("vortex", "The variable is a list with elemnts: " + GlobalState.getInstance().getVariableConfiguration().getListElements(v.getBackingDataSet()));
                                List<String> lElems = GlobalState.getInstance().getVariableConfiguration().getListElements(v.getBackingDataSet());

                                if (lElems != null && lElems.size() > 0) {
                                    if (lElems.get(0).equals("@file")) {
                                        Log.d("vortex", "FILE!");
                                        List<SpinnerDefinition.SpinnerElement> spinnerDefs = GlobalState.getInstance().getSpinnerDefinitions().get(v.getId().toLowerCase());
                                        Log.e("vortex", "got definitions: " + spinnerDefs.toString());
                                        for (SpinnerDefinition.SpinnerElement spd:spinnerDefs) {
                                            //Log.d("vortex","value: "+spd.value+" string: "+spd.opt);
                                            if (spd.value.equals(strRes)) {
                                                strRes = spd.opt;
                                                Log.d("vortex","match! "+spd.opt);
                                                break;
                                            }
                                        }
                                    } else if (!lElems.get(0).startsWith("@")) {

                                        Log.e("vortex","value: "+strRes);
                                        for (String elem : lElems) {
                                            Log.d("vortex","elem: "+elem);
                                            elem = elem.replace("{","").replace("}","");
                                            String[] pair = (elem.split("="));
                                            if (pair.length > 1) {
                                                Log.d("vortex","pair[0]: "+pair[0]+" pair[1]: "+pair[1]);
                                                if (pair[1].equals(strRes)) {
                                                    strRes = pair[0];
                                                    break;
                                                }
                                            } else {
                                                Log.e("vortex", "could not split on = ... exit");
                                            }

                                        }
                                    }
                                }
                            } else {
                                Log.e("vortex", "cannot apply function to non list variable");
                                o.addRow("");
                                o.addRedText("Cannot apply function "+getType()+"to non list variable "+evalArgs.get(0));
                            }
                        }
                    }
                    return strRes;

                case getCurrentYear:
                    return Constants.getYear();
                case getCurrentMonth:
                    return Constants.getMonth();
                case getCurrentDay:
                    return Constants.getDayOfMonth();
                case getCurrentHour:
                    return Constants.getHour();
                case getCurrentMinute:
                    return Constants.getMinute();
                case getCurrentSecond:
                    return Constants.getSecond();
                case getCurrentWeekNumber:
                    return Constants.getWeekNumber();
                case getSweDate:
                    return Constants.getSweDate();
                case getColumnValue:
                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        if (currentKeyChain==null) {
                            Log.e("vortex","Currentkeychain is missing in Expressor!");
                            return null;
                        }
                        else {

                            Log.d("votex","value for column "+evalArgs.get(0)+" is "+currentKeyChain.get(evalArgs.get(0)));
                            Log.d("votex","current keychain: "+currentKeyChain);
                            return currentKeyChain.get(evalArgs.get(0));
                        }
                    }
                    break;
                case getAppName:
                    return GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
                case getStatusVariableValues:

                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        //Gets the status from all buttons on the page.
                        String statusVariableName = (String) evalArgs.get(0);
                        boolean  empty = true;
                        DbHelper.DBColumnPicker cp = null;
                        String combinedStatus = null;
                        boolean oneInitial = false;

                        if (statusVariableName!=null) {
                            if (!statusVariableName.startsWith(Constants.STATUS_VARIABLES_GROUP_NAME+":")) {
                                Log.d("vortex","missing group or is not a statusvariable. Try add group");
                                statusVariableName=Constants.STATUS_VARIABLES_GROUP_NAME+":"+statusVariableName;
                            }
                            cp = GlobalState.getInstance().getDb().getLastVariableInstance(GlobalState.getInstance().getDb().createSelection(GlobalState.getInstance().getVariableCache().getContext().getContext(), statusVariableName));


                            if (cp != null) {

                                while (cp.next()) {
                                    Log.d("statusvar", "picker return for " + evalArgs.get(0) + " is\n" + cp.getKeyColumnValues());
                                    empty = false;
                                    String varValue = cp.getVariable().value;
                                    Log.d("vortex", "VALUE: " + varValue);
                                    if (combinedStatus == null && varValue.equals(Constants.STATUS_AVSLUTAD_OK)) {
                                        combinedStatus = Constants.STATUS_AVSLUTAD_OK;
                                    } else if (varValue.equals(Constants.STATUS_STARTAD_MED_FEL)) {
                                        //Here we can exit. We know the value.
                                        return 2;
                                    } else if (varValue.equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
                                        //Continue and check that there is no error state down the line.
                                        combinedStatus = Constants.STATUS_STARTAD_MEN_INTE_KLAR;
                                        continue;
                                    } else if (varValue.equals(Constants.STATUS_INITIAL)) {
                                        oneInitial = true;
                                        continue;
                                    }
                                }


                            }
                        }
                        if (cp==null || empty) {
                            o.addRow("");
                            o.addRedText("getStatusVariableValues finds no match with argument "+ evalArgs.get(0));
                            Log.e("vortex","found no results in getStatusVariableValues for variable "+ evalArgs.get(0));
                        }

                        if (combinedStatus != null) {
                            if (oneInitial && combinedStatus.equals(Constants.STATUS_AVSLUTAD_OK)) {
                                Log.d("vortex","found one that is not done!");
                                combinedStatus = Constants.STATUS_STARTAD_MEN_INTE_KLAR;
                            }
                            return Integer.parseInt(combinedStatus);
                        }
                        else
                            return 0;
                    }

                    break;
                case getGISobjectLength:

                case getGISobjectArea:
                    Log.d("vortex","getArea called");
                    GisObject touchedGop = GlobalState.getInstance().getSelectedGop();
                    if (touchedGop!=null) {

                        if ("getGISobjectLength".equals(getType().name()))
                            return Geomatte.getCircumference(touchedGop.getCoordinates());
                        else
                            return Geomatte.getArea(touchedGop.getCoordinates());
                    } else
                        Log.d("zappa","no gop");
                    return null;

                case getSweRefX:
                    Map<String, String> gpsKeyMap = GlobalState.getInstance().getVariableConfiguration().createGpsKeyMap();
                    Variable x = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyMap, NamedVariables.MY_GPS_LAT);
                    if (x!=null && x.getValue()!=null) {
                        return x.getValue();
                    }
                    else {
                        Log.d("vortex","missing value for user xpos.");
                        return null;
                    }

                case getSweRefY:
                    gpsKeyMap = GlobalState.getInstance().getVariableConfiguration().createGpsKeyMap();
                    Variable y = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyMap, NamedVariables.MY_GPS_LONG);
                    if (y!=null && y.getValue()!=null) {
                        return y.getValue();
                    }
                    else {
                        Log.d("vortex","missing value for user ypos.");
                        return null;
                    }

                case getUserRole:
                    return GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.DEVICE_COLOR_KEY_NEW);

                case getUserName:
                    return GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY);

                case getTeamName:
                    return GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
                case export:
                    if (checkPreconditions(evalArgs,4,No_Null_Literal)) {
                        final String exportFileName = evalArgs.get(0).toString();
                        final String rawExportContext = evalArgs.get(1).toString();
                        final String exportFormat = evalArgs.get(2).toString();
                        final String exportMethod = evalArgs.get(3).toString();
                        List<EvalExpr> pre_eval_context = Expressor.preCompileExpression(rawExportContext);
                        DB_Context exportContext = DB_Context.evaluate(pre_eval_context);
                        final Exporter exporter = Exporter.getInstance(null,exportFormat.toLowerCase(),new ExportDialogDummy());
                        Thread t = new Thread() {
                            String msg = null;
                            @Override
                            public void run() {
                                Exporter.Report jRep = GlobalState.getInstance().getDb().export(exportContext.getContext(), exporter, exportFileName);
                                Exporter.ExportReport exportResult = jRep.getReport();
                                if (exportResult == Exporter.ExportReport.OK) {
                                    msg = jRep.noOfVars + " variables exported to file: " + exportFileName + "." + exporter.getType() + "\n";
                                    msg += "In folder:\n " + Constants.EXPORT_FILES_DIR + " \non this device";


                                    if (exportMethod == null || exportMethod.equalsIgnoreCase("file")) {
                                        //nothing more to do...file is already on disk.
                                    } else if (exportMethod.startsWith("mail")) {
                                        //not supported
                                    }

                                } else {
                                    if (exportResult == Exporter.ExportReport.NO_DATA)
                                        msg = "Nothing to export! Have you entered any values? Have you marked your export variables as 'global'? (Local variables are not exported)";
                                    else
                                        msg = "Export failed. Reason: " + exportResult;


                                }
                                boolean isDeveloper = GlobalState.getInstance().getGlobalPreferences().getB(PersistenceHelper.DEVELOPER_SWITCH);
                                if (isDeveloper) {
                                    o.addRow("Export done");
                                    o.addRow("");
                                    o.addText(msg);
                                }

                            }
                        };
                        t.start();
                        return true;

                    }
                    Log.d("vortex","precondition failed for function export. returning false");
                    return false;

                case photoExists:
                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        //System.out.println("Arg 0: "+evalArgs.get(0).toString());
                        File dir = new File(Constants.PIC_ROOT_DIR);
                        final String regexp = evalArgs.get(0).toString(); // needs to be final so the anonymous class can use it
                        File[] matchingFiles = dir.listFiles(fileName -> {
                            //System.out.println("Testing "+fileName);
                            return fileName.getName().matches(regexp);
                        });
                        return matchingFiles != null && matchingFiles.length != 0;

                    }
                    //Return 0 if one of the values are undefined.
                case sum:
                    if (!checkPreconditions(evalArgs,-1,Null_Numeric))
                        return 0;
                    else {
                        Object sum = 0;
                        int intSum = 0; double doubleSum = 0;
                        for (Object arg : evalArgs) {
                            if (arg!=null) {
                                if (arg instanceof Integer)
                                    intSum += (Integer) arg;
                                else if (arg instanceof Double)
                                    doubleSum += (Double) arg;
                            }

                        }
                        if (doubleSum > 0)
                            return (double)intSum+doubleSum;
                        else
                            return intSum;
                    }
                case concatenate:
                    if (!checkPreconditions(evalArgs,-1,Null_Literal)) {
                        return null;
                    }
                    else {
                        StringBuilder stringSum= new StringBuilder();
                        for (Object arg : evalArgs) {
                            if (arg!=null)
                                stringSum.append(arg);
                        }
                        return stringSum.toString();
                    }
                case hasNullValue:
                    return !checkPreconditions(evalArgs,-1,No_Null);
                case getDelytaArea:
                    if (checkPreconditions(evalArgs,1,No_Null)) {
                        Log.d("vortex", "running getDelytaArea function");
                        DelyteManager dym = DelyteManager.getInstance();
                        if (dym == null) {
                            o.addRow("");
                            o.addRedText("Cannot calculate delyta area...no provyta selected");
                            return null;
                        }
                        float area = dym.getArea((Integer)evalArgs.get(0));
                        if (area == 0) {
                            Log.e("vortex","area 0 in getdelytaarea");
                            o.addRow("Area 0");
                            o.addRedText("Either Delyta "+evalArgs.get(0)+" does not exist or area is 0 (in function getDelytaArea)");
                            return null;
                        }
                        return Float.toString(area/100);
                    }
                    return  null;
                case hasSame:
                case hasValue:
                case allHaveValue:

                    String function = getType().name();
                    Log.d("bortex","targetList is "+targetList);
                    if (checkPreconditions(evalArgs,-1,No_Null)) {

                        List<List<String>> rows;
                        //hasValue(pattern,op,constant)
                        String pattern = (String) evalArgs.get(0);
                        //Get all variables in Functional Group x.
                        Table table = al.getTable();


                        String column = (getType() == TokenType.hasSame)?VariableConfiguration.Col_Functional_Group:
                                VariableConfiguration.Col_Variable_Name;
                        if (targetList==null)
                            rows = al.getTable().getRowsContaining(column, pattern);
                        else {
                            rows = targetList;
                            Log.d("bortex","used targetlist for hasX!");
                            Log.d("vortex", "found " + (rows.size() - 1) + " variables for " + pattern);
                            if (pattern!=null) {
                                pattern.trim();
                                List<List<String>> ret = new ArrayList<>();
                                for (int i = 0; i < rows.size(); i++) {
                                    //Log.d("nils","i: "+i+" col: "+column.get(i));
                                    if (al.getVarName(rows.get(i)).equals(pattern) || al.getVarName(rows.get(i)).matches(pattern)) {
                                        ret.add(rows.get(i));
                                    }
                                }
                                Log.d("nils", "Returning " + ret.size() + " rows in getRows(Table)");
                                rows = ret;
                            }
                        }

						/*
						if (getType() == TokenType.hasSame)
							rows = table.getRowsContaining(VariableConfiguration.Col_Functional_Group, pattern);
						else
							rows = table.getRowsContaining(VariableConfiguration.Col_Variable_Name, pattern);
						*/
                        if (rows == null || rows.size() == 0) {
                            Log.e("vortex", "no variables found for filter " + pattern);
                            return null;
                        } else {
                            Log.d("vortex", "found " + (rows.size() - 1) + " variables for " + pattern);
                            //for(int i=0;i<200;i++)
                            //	System.out.println(al.getVarName(rows.get(i)));
                        }

                        //Parse the expression. Find all references to Functional Group.
                        //Each argument need to either exist or not exist.
                        Map<String, String[]> values = new HashMap<>();
                        boolean allNull = true;
                        Log.d("vortex","1st: "+evalArgs.get(0)+"op: "+evalArgs.get(1)+"constant: "+evalArgs.get(2));
                        final String op = (String) evalArgs.get(1);
                        final Object constant = evalArgs.get(2);
                        EvalExpr fifo;
                        for (List<String> row : rows) {
                            Log.d("vortex", "Var name: " + al.getVarName(row));

                            if (getType() == TokenType.hasValue ||
                                    getType() == TokenType.allHaveValue) {
                                String formula = "[$" + al.getVarName(row) + op + constant+"]";
                                Variable myVar = varCache.getVariable(al.getVarName(row));
                                Boolean res=null;
                                if (myVar != null && myVar.getValue() != null) {
                                    allNull = false;
                                    List<Token> resulto = Expressor.tokenize(formula);
                                    if (resulto!=null) {
                                        Expressor.testTokens(resulto);
                                        fifo = Expressor.analyzeExpression(resulto);
                                        if (fifo!=null)
                                            res = Expressor.analyzeBooleanExpression(fifo);
                                        else
                                            Log.e("vortex","Could not analyse "+formula+" since analyzeexpr returned null!");
                                    }
                                    if (res == null) {
                                        Log.e("vortex", formula + " evaluates to null..something wrong");
                                    } else {

                                        if (!res && getType() == TokenType.allHaveValue) {
                                            o.addRow("");
                                            o.addYellowText("allHaveValue failed on expression " + formula);
                                            Log.e("vortex", "allHaveValue failed on " + formula);
                                            return false;
                                        } else {
                                            if (!res)
                                                continue;
                                            else {
                                                if (getType() == TokenType.hasValue) {
                                                    o.addRow("");
                                                    o.addGreenText("hasvalue succeeded on expression " + formula);
                                                    Log.d("vortex", "hasvalue succeeded on expression " + formula);
                                                    return (true);
                                                }
                                            }

                                        }
                                    }
                                } else {
                                    Log.d("vortex", "null value...skipping");
                                }

                            } else if (getType() == TokenType.hasSame) {
                                for (int i = 1; i < evalArgs.size(); i++) {
                                    String[] varNameA = al.getVarName(row).split(Constants.VariableSeparator);
                                    int size = varNameA.length;
                                    if (size < 3) {
                                        o.addRow("");
                                        o.addRedText("This variable has no Functional Group...cannot apply hasSame function. Variable id: " + Arrays.toString(varNameA));
                                        Log.e("vortex", "This is not a group variable...stopping.");
                                        return null;
                                    } else {
                                        String name = varNameA[size - 1];
                                        String art = varNameA[size - 2];
                                        String group = varNameA[0];
                                        //Log.d("vortex", "name: " + name + " art: " + art + " group: " + group + " args[" + i + "]: " + evalArgs.get(i));
                                        if (name.equalsIgnoreCase((String) evalArgs.get(i))) {
                                            Log.d("vortex", "found varname. Adding " + art);
                                            Variable v = varCache.getVariable(al.getVarName(row));
                                            String varde = null;
                                            if (v == null) {
                                                Log.d("vortex", "var was null!");

                                            } else
                                                varde = v.getValue();
                                            String[] rezult;
                                            if (values.get(art) == null) {
                                                Log.d("vortex", "empty..creating new val arr");
                                                rezult = new String[evalArgs.size() - 1];
                                                values.put(art, rezult);
                                            } else
                                                rezult = values.get(art);
                                            rezult[i - 1] = varde;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (getType() == TokenType.hasSame) {
                            //now we should have an array containing all values for all variables.
                            Log.d("vortex", "printing resulting map");
                            for (String key : values.keySet()) {
                                String vCompare = values.get(key)[0];
                                for (int i = 1; i < evalArgs.size() - 1; i++) {
                                    String vz = values.get(key)[i];
                                    //if (vCompare!=null || vz !=null) {
                                    //	Log.e("vortex","Found a value! "+vCompare+" , "+vz);
                                    //}
                                    if (vCompare == null && vz == null || vCompare != null && vz != null)
                                        continue;
                                    else {
                                        o.addRow("hasSame difference detected for " + key + ". Stopping");
                                        Log.e("vortex", "Diffkey values for " + key + ": " + (vCompare == null ? "null" : vCompare) + " " + (vz == null ? "null" : vz));
                                        return false;
                                    }

                                }
                            }
                            Log.d("vortex", "all values same. Success for hasSame!");
                            return true;

                        } else if (getType() == TokenType.hasValue) {
                            //Hasvalue fails since none of the variables fullfilled the criteria
                            o.addRow("");
                            o.addYellowText("hasvalue failed to find any match");
                            Log.e("vortex", "hasValue failed. No match found");
                            return false;
                        } else {
                            if (!allNull) {
                                o.addRow("");
                                o.addYellowText("allHaveValue succeeded!");
                                Log.e("vortex", "allHaveValue succeeded!");
                                return true;
                            } else {
                                o.addRow("");
                                o.addYellowText("allHaveValue failed - no values");
                                Log.e("vortex", "allHaveValue failed on empty list");
                                return false;
                            }
                        }
                    }
                    break;
                case has:
                    Variable var = null;
                    if (evalArgs.get(0)!=null)
                        var = Expressor.getVariable(evalArgs.get(0).toString());
                    if (var != null) {
                        String value = var.getValue();
                        //Log.d("vortex","Found value "+value+" for variable "+var.getLabel()+" in has!");
                        return value != null;
                    } else {
                        Log.e("vortex","Variable not found for literal: ["+evalArgs.get(0)+"]");
                        o.addRow("");
                        o.addRedText("Variable not found in Has(): ["+evalArgs.get(0)+"]");
                        return null;
                    }
                    //return checkPreconditions(evalArgs,1,No_Null);

                case hasSome:
                case hasMost:
                case hasAll:
                    if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
                        Log.d("vortex", "HASx function");
                        //Apply filter parameter <filter> on all variables in current table. Return those that match.
                        float failC = 0;
                        //If any of the variables matching filter doesn't have a value, return 0. Otherwise 1.
                        List<List<String>> rows=null;
                        if (targetList==null)
                            rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Variable_Name, evalArgs.get(0).toString());
                        else {
                            rows = targetList;
                            Log.d("bortex","used targetlist for hasX!");
                        }
                        if (rows == null || rows.size() == 0) {
                            o.addRow("");
                            o.addRedText("Filter returned empty list in HASx construction. Filter: " + getType());
                            o.addRow("");
                            o.addRedText("Check your pattern: " + evalArgs.get(0));
                            return null;
                        }
                        float rowC = rows.size();

                        for (List<String> row : rows) {
                            String value = varCache.getVariableValue(currentKeyChain, al.getVarName(row));
                            if (value == null) {
                                if (getType() == TokenType.hasAll) {
                                    o.addRow("");
                                    o.addYellowText("hasAll filter stopped on variable " + al.getVarName(row) + " that is missing a value");
                                    return false;
                                } else
                                    failC++;
                            } else if (getType() == TokenType.hasSome) {
                                o.addRow("");
                                o.addYellowText("hasSome filter succeeded on variable " + al.getVarName(row) + " that has value " + value);
                                return true;
                            }
                        }
                        if (failC == rowC && getType() == TokenType.hasSome) {
                            o.addRow("");
                            o.addYellowText("hasSome filter failed. No variables with values found for "+evalArgs.get(0));
                            return false;
                        }
                        if (getType() == TokenType.hasAll) {
                            o.addRow("");
                            o.addYellowText("hasAll filter succeeded.");
                            return true;
                        }
                        if (failC <= rowC / 2) {
                            o.addRow("");
                            o.addYellowText("hasMost filter succeeded. Filled in: " + (int) ((failC / rowC) * 100f) + "%");
                            return true;
                        }
                        o.addRow("");
                        o.addYellowText("hasMost filter failed. Not filled in: " + (int) ((failC / rowC) * 100f) + "%");
                        return false;
                    }
                    break;


                default:
                    System.err.println("Unimplemented function: "+getType().toString());
                    break;


            }
            return null;
        }
        /*
        private Boolean booleanValue(Object obj) {
            if (obj==null)
                return (Boolean)null;
            if (obj instanceof String) {
                if (obj.equals("true")||obj.equals("1")||obj.equals("1.0"))
                    return true;
                if (obj.equals("false")||obj.equals("0")||obj.equals("0.0"))
                    return false;



            } else if (obj instanceof Double) {
                if ((Double)obj==1.0d)
                    return true;
                if ((Double)obj==0.0d)
                    return false;

            }
            Log.e("vortex","no boolean value found for "+obj);
            o.addRedText("no boolean value found for "+obj);
            return null;
        }
         */
        private boolean checkPreconditions(List<Object> evaluatedArgumentsList,int cardinality, int flags) {
            if ((flags==No_Null || flags== No_Null_Numeric || flags == No_Null_Literal || flags == No_Null_Boolean)
                    && evaluatedArgumentsList.contains(null)) {
                //o.addRow("");
                //o.addRedText("Argument in function '"+getType().toString()+"' is null, but function does not allow NULL arguments.");
                Log.e("Vortex","Argument in function '"+getType().toString()+"' is null");

                return false;
            }
            if (cardinality!=-1 && cardinality!=evaluatedArgumentsList.size()) {
                o.addRow("");
                o.addRedText("Too many or too few arguments for function '"+getType().toString()+"'. Should be "+cardinality+" argument(s), not "+evaluatedArgumentsList.size()+"!");
                Log.e("Vortex","Too many or too few arguments for function '"+getType().toString()+"'. Should be "+cardinality+" argument(s), not "+evaluatedArgumentsList.size()+"!");
                return false;
            }
            if (flags== No_Null_Numeric) {
                for (Object obj:evaluatedArgumentsList) {
                    if ((obj instanceof Double)||(obj instanceof Integer)||(obj instanceof Float)) {
                        continue;
                    } else {
                        o.addRow("");
                        o.addRedText("Type error. Non numeric argument for function '"+getType().toString()+"'. Argument is a "+obj.getClass().getSimpleName());
                        Log.e("Vortex","Type error. Non numeric argument for function '"+getType().toString()+"'. Argument is a "+obj.getClass().getSimpleName());
                        return false;
                    }

                }

            }
            if (flags == No_Null_Literal) {
                for (Object obj:evaluatedArgumentsList) {
                    if (!(obj instanceof String)) {
                        o.addRow("");
                        o.addRedText("Type error. Non literal argument for function '" + getType().toString() + "'.");
                        Log.e("Vortex","Type error. Non literal argument for function '"+getType().toString()+"'.");
                        return false;
                    }
                }
            }
            if (flags == Null_Numeric) {
                for (Object obj:evaluatedArgumentsList) {
                    //Log.d("vortex","In null_numeric with "+obj);
                    if (obj !=null && !(obj instanceof Double)&&!(obj instanceof Integer)&&!(obj instanceof Float)) {
                        o.addRow("");
                        o.addRedText("Type error. Not null & not numeric argument for function '" + getType().toString() + "'. Argument evaluated to : "+obj+" Type: "+obj.getClass().getName());
                        Log.e("Vortex","Type error. Not null & not numeric argument for function '"+getType().toString()+"'.");
                        return false;
                    }
                }
            }
            if (flags == Null_Literal) {
                for (Object obj:evaluatedArgumentsList) {
                    if (obj !=null && !(obj instanceof String)) {
                        o.addRow("");
                        o.addRedText("Type error. Not null & Non literal argument for function '" + getType().toString() + "'. Argument evaluated to : "+obj+" Type: "+obj.getClass().getName());
                        Log.e("Vortex","Type error. Not null & Non literal argument for function '"+getType().toString()+"'.");
                        return false;
                    }
                }
            }
            if (flags == No_Null_Boolean) {
                for (Object obj:evaluatedArgumentsList) {
                    if (!(obj instanceof Boolean)) {
                        Log.e("Vortex","Type error. Non boolean argument for function '"+getType().toString()+ "'. Argument evaluated to : "+obj+" Type: "+obj.getClass().getName());
                        o.addRow("");
                        o.addRedText("Type error. Non boolean argument for function '" + getType().toString() + "'. Argument evaluated to : "+obj+" Type: "+obj.getClass().getName());
                        return false;
                    }
                }
            }
            if (flags == Null_Boolean) {
                for (Object obj:evaluatedArgumentsList) {
                    if (obj !=null && !(obj instanceof String || !obj.equals("true") || !obj.equals("false") ) ) {
                        o.addRow("");
                        o.addRedText("Type error. Non boolean argument for function '" + getType().toString() + "'.");
                        Log.e("Vortex","Type error. Not null & Non boolean argument for function '"+getType().toString()+"'.");
                        return false;
                    }
                }
            }

            return true;

        }



        @Override
        public String toString() {
            return getType().name()+"("+args.toString()+")";
        }

        private class ExportDialogDummy implements ExportDialogInterface {
            @Override
            public void setGenerateStatus(String msg) {
            }
            @Override
            public void setSendStatus(String msg) {
            }
            @Override
            public void setBackupStatus(String msg) {
            }
            @Override
            public void setCheckGenerate(boolean success) {
            }
            @Override
            public void setCheckBackup(boolean success) {
            }
            @Override
            public void setCheckSend(boolean success) {
            }
            @Override
            public void setOutCome(String msg) {
            }
        }
    }

    private static EvalExpr analyzeExpression(List<Token> tokens) {
        boolean err = false;

        // Operation stack.
        Stack<Expr> opStack = new Stack<>();
        // Value stack.
        Stack<Expr> valStack = new Stack<>();

        // empty expr
        if (tokens == null || tokens.isEmpty())
            return null;

        ExpressionAnalyzer ef = new ExpressionAnalyzer(tokens.iterator());

        Expr e = null, top;
        //System.out.println("Before:  " + tokens);

        while (!err && ef.hasNext()) {
            if (e == null)
                e = ef.next();
            if (e == null) {
                System.out.println("continue on null");
                continue;
            }

            //System.out.println("vs: " + valStack);
            //System.out.println("os: " + opStack);
            //System.out.println("e: " + e);

            if (e instanceof Push) {
                opStack.push(e);
            } else if (e instanceof Pop) {
                while (!opStack.isEmpty() && opStack.peek() instanceof Operand) {
                    valStack.push(new Convoluted(valStack.pop(),
                            valStack.pop(), (Operand) opStack.pop()));
                }
                if (!opStack.isEmpty() && opStack.peek() instanceof Push) {
                    opStack.pop();
                } else {
                    System.err.println("Error: unbalanced parenthesis.");
                    err = true;
                }
            } else if (e instanceof Operand) {
                // Stack empty? Then push.

                if (opStack.isEmpty()) {
                    //System.out.println("empty->push");
                    opStack.push(e);
                } else {
                    //System.out.println("Top of stack: " + opStack.peek());
                    int operatorPrecedence = Objects.requireNonNull(TokenType.valueOfIgnoreCase(
                            e.toString())).prescedence();
                    int topStackPrecedence = Objects.requireNonNull(TokenType.valueOfIgnoreCase(
                            opStack.peek().toString())).prescedence();
                    // This has higher precedence? Then push.
                    if (operatorPrecedence > topStackPrecedence) {
                        //System.out.println("precedence->push");
                        opStack.push(e);
                    } else {
                        //System.out.println("calctop");
                        if (valStack.size() < 2) {
                            System.err.println("smallstack: " + valStack);
                            err = true;
                        }
                        // Evaluate until stack empty or precedence of stack
                        // lower than this op.
                        else
                            valStack.push(new Convoluted(valStack.pop(),
                                    valStack.pop(), (Operand) opStack.pop()));

                        // use same operator.
                        continue;
                    }

                }

            } else {
                //System.out.println("Pushing: " + e + " of class "
                //		+ e.getClass());
                valStack.push(e);
            }
            e = null;
        }
        //If any items remain on stack, add them.
        while (!opStack.isEmpty() && opStack.peek() instanceof Operand && valStack.size()>1) {
            valStack.push(new Convoluted(valStack.pop(),
                    valStack.pop(), (Operand) opStack.pop()));
        }
        //System.out.println("Returning: " + ret);
        return valStack.isEmpty()?null:(EvalExpr) valStack.pop();
    }


    private static boolean isLogicalOperand(TokenType x) {
        if (x==null)
            return false;
        String name = x.name();
        return ("AND".equalsIgnoreCase(name) || "OR".equalsIgnoreCase(name));
    }


    private static boolean  isFunction(TokenType t) {
        TokenType parent = t.getParent();
        return (parent !=null && parent.getParent() == TokenType.function);
    }




    //static int cc=0;
    private static void printTokens(List<Token> expr) {
        //System.out.print(cc+":");
        if (expr !=null) {
            for (Token t : expr) {
                System.out.print(t.str + "[" + t.type.name() + "]");

            }
            System.out.println();
        }
    }


    private static Variable getVariable(String varId) {
        //check first if variable context exists

        if (variables!=null) {

            for (Variable v:variables) {
                if (v.getId().endsWith(varId)) {
                    return v;
                }


            }
            Log.e("vortex","Variable "+varId+" not found in var context!");

        }
        return GlobalState.getInstance().getVariableCache().getVariable(currentKeyChain,varId);
    }


    private static void printfail(Expr rez, Expr arg2, Expr op) throws ExprEvaluationException {
        if (o!=null) {
            o.addRow("");
            o.addRedText("Missing or wrong parameters. This is likely caused by a misplaced paranthesis.");
            o.addRedText("arg1: "+rez);
            o.addRedText("arg2: "+arg2);
            o.addRedText("operator: "+op);
        }

        System.err.println("Missing or wrong parameters. This is likely caused by a misplaced paranthesis.");
        System.err.println("arg1: "+rez);
        System.err.println("arg2: "+arg2);
        System.err.println("operator: "+op);

        throw new ExprEvaluationException();

    }





}




