package app;

import java.io.*;

import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	public static String delims = " \t*+-/()[]";
			
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
    public static void 
    makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	/** DO NOT create new vars and arrays - they are already created before being sent in
    	 ** to this method - you just need to fill them in.
    	 **/
    	
    	expr = deleteSpaces(expr);
    	
    	Matcher m = Pattern.compile("[A-Za-z]+").matcher(expr);
		
		while (m.find()) {
			
			//if it's an array
			if (m.end() < expr.length()-1 && expr.charAt(m.end()) == '[') {
			
				Array arrObj= new Array(m.group());
				
				if (!arrays.contains(arrObj)) {
					arrays.add(arrObj);
				}
			}
			//if its a variable
			else {
				Variable varObj= new Variable(m.group());
				
				if (!vars.contains(varObj)) {
					vars.add(varObj);
				}
				
			}
			
		}
    	
    	
    }
    
    /**
     * Loads values for variables and arrays in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void 
    loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression.
     * 
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float 
    evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	
    	expr = deleteSpaces(expr);
    	
    	Stack<Float> operands = new Stack<Float>();
    	Stack<Character> operators = new Stack<Character>();
    	int startIndex = 0;
    	int endIndex = 0;
    	
    	Pattern p = Pattern.compile("[A-Za-z]+|[0-9]+|[-+*/]|[\\(\\)\\[\\]]");
    		
    	Matcher m = p.matcher(expr);
    	
    	while (m.find()) {
    		//System.out.println(m.group());
    	
    		//if it's an open parenthesis
    		if (Pattern.matches("[\\(]", m.group())) {
    			startIndex = m.start()+1;// the character after the parenthesis
    			endIndex = findCloseParen(m); 
    			
    			operands.push(evaluate(expr.substring(startIndex, endIndex), vars, arrays));
    			//after evaluate, the current group is the last parenthesis
    		}
    	
    		//if it's a series of numbers
    		if (Pattern.matches("[0-9]+", m.group())){
    			operands.push(convertToNum(m.group()));
    		}
    	
    		//if it's a series of letters
    		if (Pattern.matches("[A-Za-z]+", m.group())) {
    			
    			//for arrays
    			if (m.end() < expr.length()-1 && expr.charAt(m.end()) == '[') {
        			String arrayName = m.group();
    				
    				startIndex = m.end()+1;// the character after the bracket 
        			endIndex = findCloseBracket(m);
    				
        			int arrayIndex = (int)(evaluate(expr.substring(startIndex, endIndex), vars, arrays));
        			
        			for (Array array : arrays) {
    					if (array.name.equals(arrayName)){
    						operands.push((float)array.values[arrayIndex]);
    						break;
    					}
    				}
        		}
    			
    			//for variables
    			else {
    				for (Variable var : vars) {
    					if (var.name.equals(m.group())){
    						operands.push((float)var.value);
    						break;
    					}
    				}
    			}
    		}
    	
    		//if it's a * or /
    		if (m.group().charAt(0) == '*' || m.group().charAt(0) == '/') {
    			operators.push(m.group().charAt(0));
    		}
    			
    		//if it's a + or -
    		if (m.group().charAt(0) == '+' || m.group().charAt(0) == '-') {
    			//this method should still work even if there aren't any * or / in stack
    			calculateMultDiv(operands, operators);
    			operators.push(m.group().charAt(0));
    		}
    	}
    	
    	calculateMultDiv(operands,operators);
    	calculateAddSub(operands, operators);
    		
    	
    	
    	return operands.pop();
    	
    }
    
   
    
    //reduce stacks until operator stack is empty or peek() yields + or -
    private static void calculateMultDiv(Stack<Float> operands, Stack<Character> operators) {
    	Stack<Float> operandsTemp = new Stack<Float>();
    	Stack<Character> operatorsTemp = new Stack<Character>();
    	
    	if (!operands.isEmpty()) {
    		operandsTemp.push(operands.pop());
    	}
    	while (!operators.isEmpty() && !operands.isEmpty() && !(operators.peek() == '+') && !(operators.peek() == '-')) {
    		operatorsTemp.push(operators.pop());
    		operandsTemp.push(operands.pop());
    	}
    	
    	float second = 0;
    	float first = 0;
    	char operator = ' ';
    	float answer = 0;
    	
    	while (!operatorsTemp.isEmpty()) {
    		first = operandsTemp.pop();
    		second = operandsTemp.pop();
        	operator = operatorsTemp.pop();
        	
        	if (operator == '*') {
        		answer = first*second;
        	}
        	if (operator == '/') {
        		answer = first/second;
        	}
        	
        	operandsTemp.push(answer);
    	}
    	
    	while (!operandsTemp.isEmpty()) {
    		operands.push(operandsTemp.pop());
    	}
    }
    
    //reduce stacks until operator stack is empty
    private static void calculateAddSub(Stack<Float> operands, Stack<Character> operators) {
    	Stack<Float> operandsTemp = new Stack<Float>();
    	Stack<Character> operatorsTemp = new Stack<Character>();
    	
    	float second = 0;
    	float first = 0;
    	char operator = ' ';
    	float answer = 0;
    	
    	//transfer to Temps in flipped order
    	while (!operands.isEmpty()) {
    		operandsTemp.push(operands.pop());
    	}
    	while (!operators.isEmpty()) {
    		operatorsTemp.push(operators.pop());
    	}
    	
    	//evaluate effectively from left to right
    	while (!operatorsTemp.isEmpty()) {
    		first = operandsTemp.pop();
        	second = operandsTemp.pop();
        	operator = operatorsTemp.pop();
        	
        	if (operator == '+') {
        		answer = first+second;
        	}
        	else {
        		answer = first-second;
        	}
        	
        	operandsTemp.push(answer);
    	}
    	
    	//reset operands stack
    	//operators and operatorsTemp should be empty at this point so I don't need to reset
    	while (!operandsTemp.isEmpty()) {
    		operands.push(operandsTemp.pop());
    	}
    }
    
    private static float convertToNum(String str) {
    	float place = 1;
    	float num= 0;
    	for (int i=str.length()-1; i>=0; i--) {
    		//System.out.println("Place: " + place);
    		num+= Character.digit(str.charAt(i),10) * place;
    		//System.out.println("current num: " + num);
    		place*=10;
    	}
    	
    	//System.out.println();
    	
    	return num;
    }
    
    //returns index of the matching closing parenthesis
    private static int findCloseParen(Matcher m) {
    	int openParenCount = 1;
    	int closeParenCount = 0;
    	while (openParenCount!=closeParenCount) {
    		m.find();
    		if (Pattern.matches("[\\(]", m.group())) {
    			openParenCount++;
    		}
    		if (Pattern.matches("[\\)]", m.group())){
    			closeParenCount++;
    		}
    	}
    	
    	return m.start();
    }
    
    //returns index of matching closing bracket
    private static int findCloseBracket(Matcher m) {
    	m.find();
    	
    	int openBracketCount = 1;
    	int closeBracketCount = 0;
    	while (openBracketCount!=closeBracketCount) {
    		m.find();
    		if (Pattern.matches("[\\[]", m.group())) {
    			openBracketCount++;
    		}
    		if (Pattern.matches("[\\]]", m.group())){
    			closeBracketCount++;
    		}
    	}
    	
    	return m.start();
    }
    
    private static String deleteSpaces(String expr) {
    	for (int i=0; i<expr.length(); i++) {
    		if (Character.isWhitespace(expr.charAt(i))) {
    			expr = expr.substring(0,i) + expr.substring(i+1);
    			i--;
    		}
    	}
    	
    	return expr;
    }
    
    /*
    public static void main(String[] args) {
    	
    	System.out.println(deleteSpaces(" Soumya and      Kiran"));
    	
    }
    
    /*
    //very intuitive, bottom were pushed first, top were pushed after
    private static <E> void printStack(Stack<E> S) {
    	Stack<E> temp = new Stack<E>();
    	E elem;
    	while (!S.isEmpty()) {
    		elem = S.pop();
    		System.out.println(elem + " ");
    		temp.push(elem);
    	}
    	
    	while (!temp.isEmpty()) {
    		S.push(temp.pop());
    	}
    }
    */
}