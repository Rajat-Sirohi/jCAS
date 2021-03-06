/*
 * INPUT FORMAT
 * SOLVE:
 ** solve('x^2=1')
 * SIMPLIFY:
 ** simplify((x+1)*2+3*x-4) 
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

/*
 * This is the main class which represents the
 * entire underlying CAS (Computer Algebra System).
 * It is responsible for:
 * - creating the AST from infix
 * - creating the infix from AST
 * - solving, given infix
 * - simplifying
 */
public class jCAS {
	/*
	 * Hopefully a debug variable is an acceptable
	 * use of a global variable, despite their
	 * general prohibition.
	 */
	public static boolean debug=false;
	private String output="";
	
	public static void main(String[] args) {
		jGUI gui = new jGUI();
	}
	
	/*
	 * The constructor for jCAS objects. Acts as essentially a main
	 * function in that all "main" methods are called from here.
	 */
	public jCAS(String variables, String infix, boolean display, boolean solve) {
		// add spaces between terms
		infix = addSpaces(infix);
		
		// build AST from infix
		Node root = infixToAST(infix, variables);
		String infixVer="";
		
		// continues to simplify AST until fully simplified
		while (!infixVer.equals(ASTtoInfix(root, "")))
		{
			infixVer = ASTtoInfix(root, "");
			simplifyAST(root, root);
		}
		
		// Differentiates between solve and simplify command-calls
		if (solve)
		{
			List<Double> solution = new ArrayList<Double>();
			for (double i = -100; i <= 100; i+=1)
			{
				double sol = solve(infix, variables, i);
				if (solution.indexOf(sol) == -1)
				{
					solution.add(sol);
				}
			}
			
			output += variables.charAt(0) + " = [";
			for (int i = 0; i < solution.size(); i++)
			{
				output += solution.get(i);
				if (i != solution.size()-1)
					output += ", ";
			}
			output += "] \n";
			
			// safe check for garbage solutions
			if (output.length() > 100)
			{
				output = variables.charAt(0) + " = []";
			}
		}
		else
		{
			output=infixVer;
		}
	}
	
	public String getOutput()
	{
		return output;
	}
	
	/*
	 * Implements Newton's Method to solve the equation a=b
	 * by finding roots of (a-b), which is solving a-b=0.
	 */
	public static double solve(String eq, String variables, double guess)
	{
		/* initial guess */ double x = guess;
		/* # of iterations */ int n = 1000;
		/* approximately 0 */ double h = 0.0001;
        
		for (int i = 0; i < n; i++)
		{
			x -= eval(plugIn(eq, x, variables))/deriv(x,h, eq, variables);
		}
		
		BigDecimal bd = new BigDecimal(x);
	    bd = bd.setScale(3, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	/*
	 * The following three methods all are crucial in simplifying
	 * the expression. They work together to break down the expression.
	 * 
	 * simplifyAST is the driving force which deals with major simplification
	 * processes (e.g. distribution, combine like terms, etc.)
	 * 
	 * canonicalForm makes sure the AST is structured in a predicatable
	 * manner for ease of simplification. For example, all "x" should be
	 * "1*x^1" and all "a-b" should be "a + (-1*b)"
	 * 
	 * constSimp combines constants starting from three locations:
	 * - const & const
	 * - const & op
	 * - op & op
	 * From these three starting points, the method combines constants
	 * that are linked across all acceptable ranges (i.e. within same precedence)
	 */
	public static void simplifyAST(Node node, Node root)
	{
		if (debug)
		{
			System.out.println("----------------------------------------");
			printAST(root, " ");
			System.out.println(ASTtoInfix(root,""));
			System.out.println("++++++++++++++++++++++++++++++++++++++++");
			printAST(node, " ");
		}
		
		canonicalForm(node, root);
		
		// distribution of * over + or -
		if (node.dataIs("*"))
		{
			for (int i = 0; i < node.getChildren().size(); i++)
			{
				if (node.getChild(i).dataIsOr("+","-"))
				{
					Node child;
					Node dist1 = new Node(new OperatorExpr("*"));
					child=new Node();
					dist1.addChild(child.setNode(node.getChild((i+1)%2)));
					child=new Node();
					dist1.addChild(child.setNode(node.getChild(i).getChild(0)));
					
					Node dist2 = new Node(new OperatorExpr("*"));
					child=new Node();
					dist2.addChild(child.setNode(node.getChild((i+1)%2)));
					child=new Node();
					dist2.addChild(child.setNode(node.getChild(i).getChild(1)));
					
					Node dist = new Node(new OperatorExpr(node.getChild(i).getData().getData()));
					dist.addChild(dist1);
					dist.addChild(dist2);
					
					node.setNode(dist);
					break;
				}
			}
		}
		
		// combine like terms
		// +
		//   *
		//     ^
		//       p
		//       x
		//     a
		//   *
		//     ^
		//       p
		//       x
		//     b
		// (a+b)*x^p
		
		
		
		// const simplification (extension of combine like terms for x^0)
		if (node.hasChildren())
		{
			constSimp(node);
		}
		
		canonicalForm(node, root);
		fillParentNodes(root);
		
		// branch further down and repeat simplification
		for (Node child : node.getChildren())
		{
			simplifyAST(child, root);
		}
	}
	
	public static void canonicalForm(Node node, Node root)
	{		
		// ensures nonconst*const --> const*nonconst
		if (node.dataIs("*"))
		{
			// constant and variable
			if (node.getChild(0).typeIs("const") && !node.getChild(1).typeIs("const"))
			{
				swapNodes(node.getChild(0), node.getChild(1));
			}
		}
		
		// convert a-b to a + (-1*b)
		else if (node.dataIs("-"))
		{
			Node child, child2;
			Node newSum = new Node(new OperatorExpr("+"));
			
			child=new Node();
			newSum.addChild(child.setNode(node.getChild(1)));
			child = new Node(new OperatorExpr("*"));
			child2=new Node();
			child.addChild(child2.setNode(node.getChild(0)));
			child2=new Node();
			child.addChild(child2.setNode(new Node(new ConstExpr("-1"))));
			newSum.addChild(child);
			
			node.setNode(newSum);
		}
		
		// x ---> x^1
		else if (node.typeIs("variable"))
		{
			if (!(node.getParent().dataIs("^")))
			{
				Node child=new Node();
				Node newPower = new Node(new OperatorExpr("^"));
				newPower.addChild(new Node(new ConstExpr("1")));
				newPower.addChild(child.setNode(node));
				
				node.setNode(newPower);
			}
		}
		
		// ^... ---> 1*^...
		else if (node.dataIs("^"))
		{
			if (node.isRoot() || (!(node.getParent().dataIs("*") && node.getOtherChild().typeIs("const"))))
			{
				Node child=new Node();
				Node newMult = new Node(new OperatorExpr("*"));
				newMult.addChild(child.setNode(node));
				newMult.addChild(new Node(new ConstExpr("1")));
				node.setNode(newMult);
			}
		}
		
	}
	
	public static void constSimp(Node node)
	{
		if (!(node.getChild(0).typeIs("variable") && node.getChild(1).typeIs("variable")))
		{
			// const & const
			if (node.getChild(0).typeIs("const") && node.getChild(1).typeIs("const"))
			{
				double result = operate(node.getData().getData(), node.getChild(1).getData().getData(), node.getChild(0).getData().getData());
				Node res = new Node(new ConstExpr(toString(result)));				
				node.setNode(res);
			}
			
			// const & op
			else if (node.hasChildOfType("const") && node.hasChildOfType("operator"))
			{
				if (node.getChildOfType("operator").sameData(node))
				{
					String const1 = node.getChildOfType("const").getData().getData();
					String op = node.getData().getData();
					Node tempNode = new Node();
					tempNode.setNode(node.getChildOfType("operator"));
					ArrayList<Node> checkpointNodes = new ArrayList<Node>();
					Node newNode = new Node();
					
					while (tempNode.hasChildOfType("const") || tempNode.hasChildOfType("operator"))
					{
						if (debug)
						{
							System.out.println("================================");
							printAST(tempNode, " ");
						}
						
						if (tempNode.hasChildOfType("const"))
						{
							Node res = new Node(new OperatorExpr(op));
							Node child;
							
							double result = operate(op, const1, tempNode.getChildOfType("const").getData().getData());
							child = new Node(new ConstExpr(toString(result)));
							res.addChild(child);
							
							Node child2 = new Node();
							fillParentNodes(node);
							child2.setNode(tempNode.getChildOfType("const").getOtherChild());
							
							fillParentNodes(node);
							while (!tempNode.getOtherChild().dataIs(const1))
							{
								Node tempChild2 = new Node(new OperatorExpr(op));
								newNode = new Node();
								tempChild2.addChild(newNode.setNode(child2));
								newNode = new Node();
								tempChild2.addChild(newNode.setNode(tempNode.getOtherChild()));
								newNode = new Node();
								child2.setNode(newNode.setNode(tempChild2));
								newNode = new Node();
								tempNode.setNode(newNode.setNode(tempNode.getParent()));
							}
							newNode = new Node();
							res.addChild(newNode.setNode(child2));
							
							newNode = new Node();
							node.setNode(newNode.setNode(res));
							break;
						}
						else if (tempNode.hasChildOfType("operator"))
						{
							if (tempNode.getChild(0).getData().getData().equals(op) && tempNode.getChild(1).getData().getData().equals(op))
							{
								newNode = new Node();
								checkpointNodes.add(newNode.setNode(tempNode.getChild(1)));
								newNode = new Node();
								tempNode.setNode(newNode.setNode(tempNode.getChild(0)));
							}
							else
							{
								if (tempNode.getChild(0).getData().getData().equals(op))
								{
									newNode = new Node();
									tempNode.setNode(newNode.setNode(tempNode.getChild(0)));
								}
								else if (tempNode.getChild(1).getData().getData().equals(op))
								{
									newNode = new Node();
									tempNode.setNode(newNode.setNode(tempNode.getChild(1)));
								}
								else
								{
									if (checkpointNodes.size() > 0)
									{
										newNode = new Node();
										tempNode.setNode(newNode.setNode(checkpointNodes.get(0)));
										checkpointNodes.remove(0);
									}
									else
									{
										break;
									}
								}
							}
						}
					}
				}
			}
			
			
			// op & op
			else if (node.getChild(0).typeIs("operator") && node.getChild(1).typeIs("operator"))
			{
				if (node.getChild(0).sameData(node) && node.getChild(1).sameData(node))
				{
					boolean operable = false;
					for (int i = 0; i < node.getChildren().size(); i++)
					{
						String op = node.getData().getData();
						Node tempNode = new Node();
						tempNode.setNode(node.getChild(i));
						ArrayList<Node> checkpointNodes = new ArrayList<Node>();
						Node newNode = new Node();
						
						while (tempNode.hasChildOfType("const") || tempNode.hasChildOfType("operator"))
						{
							if (debug)
							{
								System.out.println("================================");
								printAST(tempNode, " ");
							}
							
							if (tempNode.hasChildOfType("const"))
							{
								Node res = new Node(new OperatorExpr(op));
								
								Node child = new Node();
								child.setNode(newNode.setNode(tempNode.getChildOfType("const")));
								res.addChild(child);
								
								Node child2 = new Node();
								fillParentNodes(node);
								child2.setNode(tempNode.getChildOfType("const").getOtherChild());
								
								fillParentNodes(node);
								while (!tempNode.getOtherChild().isSame(node.getChild(i).getOtherChild()))
								{
									Node tempChild2 = new Node(new OperatorExpr(op));
									newNode = new Node();
									tempChild2.addChild(newNode.setNode(child2));
									newNode = new Node();
									tempChild2.addChild(newNode.setNode(tempNode.getOtherChild()));
									newNode = new Node();
									child2.setNode(newNode.setNode(tempChild2));
									newNode = new Node();
									tempNode.setNode(newNode.setNode(tempNode.getParent()));
								}
								newNode = new Node();
								res.addChild(newNode.setNode(child2));
								
								newNode = new Node();
								node.getChild(i).setNode(newNode.setNode(res));
								
								if (i == 0)
								{
									operable=true;
								}
								else
								{
									operable=operable&&true;
								}
								break;
							}
							else if (tempNode.hasChildOfType("operator"))
							{
								if (tempNode.getChild(0).getData().getData().equals(op) && tempNode.getChild(1).getData().getData().equals(op))
								{
									newNode = new Node();
									checkpointNodes.add(newNode.setNode(tempNode.getChild(1)));
									newNode = new Node();
									tempNode.setNode(newNode.setNode(tempNode.getChild(0)));
								}
								else
								{
									if (tempNode.getChild(0).getData().getData().equals(op))
									{
										newNode = new Node();
										tempNode.setNode(newNode.setNode(tempNode.getChild(0)));
									}
									else if (tempNode.getChild(1).getData().getData().equals(op))
									{
										newNode = new Node();
										tempNode.setNode(newNode.setNode(tempNode.getChild(1)));
									}
									else
									{
										if (checkpointNodes.size() > 0)
										{
											newNode = new Node();
											tempNode.setNode(newNode.setNode(checkpointNodes.get(0)));
											checkpointNodes.remove(0);
										}
										else
										{
											operable = operable && false;
											break;
										}
									}
								}
							}
						}
					}
					
					if (operable)
					{
						double result = operate(node.getData().getData(), node.getChild(0).getChild(0).getData().getData(), 
												node.getChild(1).getChild(0).getData().getData());
						Node res = new Node(new OperatorExpr(node.getData().getData()));
						Node newNode;
						
						newNode = new Node();
						Node child1 = new Node(new ConstExpr(toString(result)));
						res.addChild(newNode.setNode(child1));
						
						Node child = new Node(new OperatorExpr(node.getData().getData()));
						newNode = new Node();
						child.addChild(newNode.setNode(node.getChild(0).getChild(1)));
						newNode = new Node();
						child.addChild(newNode.setNode(node.getChild(1).getChild(1)));
						newNode = new Node();
						res.addChild(newNode.setNode(child));
						
						newNode = new Node();
						node.setNode(newNode.setNode(res));
					}
				}
			}
		}
	}
	
	
	/*
	 * These methods are responsible for converting the inputed
	 * infix to the representative AST. This process is complex
	 * and involves many steps. A concise summary of the process
	 * is that it first converts the infix into reverse polish
	 * notation. Then, then reverse polish expression is converted
	 * into the AST. An understanding of reverse polish notation
	 * will explain how this works. Unfortunately, that is beyond
	 * the scope of this comment.
	 */
	public static Node infixToAST(String infix, String variables)
	{
		Queue<String> revPol = infToPost(infix);
		Node root = createAST(revPol, variables);
		canonicalForm(root, root);
		simplifyAST(root, root);
		
		return root;
	}
	
	public static Node createAST(Queue<String> revPol, String variables)
	{
		Stack<Node> nodeStk = new Stack<>();
		
		for (String tkn : revPol)
		{
			// number (constant or variable)
			if (getPrec(tkn) == -1)
			{
				Node num;
				if (variables.indexOf(tkn) >= 0)
					num = new Node(new VariableExpr(tkn));
				else
					num = new Node(new ConstExpr(tkn));
				nodeStk.push(num);
			}
			
			// operator
			else
			{
				Node op = new Node(new OperatorExpr(tkn));
				
				Node child;
				
				for (int i=0; i<2; i++)
				{
					child = nodeStk.pop();
					op.addChild(child);
					child.setParent(op);
				}
				
				nodeStk.push(op);
			}
		}
		return nodeStk.peek();
	}
		
	public static Queue<String> infToPost(String infix)
	{
		Stack<String> opStk = new Stack<>();
		Queue<String> output = new LinkedList<>();
		
		for(String tkn : infix.split("\\s"))
		{
			if (tkn.isEmpty())
				continue;
			
			int prec = getPrec(tkn);
			
			// operator or (
			if (prec > 0)
			{
				if (opStk.isEmpty())
					opStk.push(tkn);
				else
				{
					// if other operators have higher precedence, then add them to stack
					// NOTE: Disregard ( as it is not technically an operator
					while ((!opStk.isEmpty()) && (!opStk.peek().equals("(")) && ((getPrec(opStk.peek()) > prec) || (getPrec(opStk.peek()) == prec && !tkn.equals("^"))))
					{
						output.add(opStk.pop());
					}
					opStk.push(tkn);
				}
			}
			
			// (
			else if (prec == 0)
			{
				// while top operator is not (
				while (!opStk.peek().equals("("))
				{
					output.add(opStk.pop());
				}
				// pop and discard the ( then
				// do nothing with the )
				opStk.pop();
			}
			
			// number
			else if (prec == -1)
			{
				output.add(tkn);
			}
		}
		
		// clear out remaining operators
		while (!opStk.isEmpty())
		{
			output.add(opStk.pop());
		}
		
		return output;
	}
	
	
	/*
	 * The deriv method simply computes the derivative of a given
	 * expression, eq, at the point x. It is approximated by taking
	 * h to be very small to simulate lim h ---> 0.
	 */
	public static double deriv(double x, double h, String eq, String variables)
	{
		return (eval(plugIn(eq, x+h, variables)) - eval(plugIn(eq, x, variables)))/h;
	}
	
	/*
	 * The eval method is a Recursive Descent Parser. It uses
	 * the order of operations to solve a mathematical expression.
	 * eval is used (unsurprisingly) as an evaluation method, akin
	 * to f(x). It is crucial in computing the derivative
	 */
	public static double eval(String eq) {
	    return new Object() {
	        int pos = -1, ch;

	        void nextChar() {
	            ch = (++pos < eq.length()) ? eq.charAt(pos) : -1;
	        }

	        boolean eat(int charToEat) {
	            while (ch == ' ') nextChar();
	            if (ch == charToEat) {
	                nextChar();
	                return true;
	            }
	            return false;
	        }

	        double parse() {
	            nextChar();
	            double x = parseExpression();
	            if (pos < eq.length()) throw new RuntimeException("Unexpected: " + (char)ch);
	            return x;
	        }

	        // Grammar:
	        // expression = term | expression `+` term | expression `-` term
	        // term = factor | term `*` factor | term `/` factor
	        // factor = `+` factor | `-` factor | `(` expression `)`
	        //        | number | functionName factor | factor `^` factor

	        double parseExpression() {
	            double x = parseTerm();
	            for (;;) {
	                if      (eat('+')) x += parseTerm(); // addition
	                else if (eat('-')) x -= parseTerm(); // subtraction
	                else return x;
	            }
	        }

	        double parseTerm() {
	            double x = parseFactor();
	            for (;;) {
	                if      (eat('*')) x *= parseFactor(); // multiplication
	                else if (eat('/')) x /= parseFactor(); // division
	                else return x;
	            }
	        }

	        double parseFactor() {
	            if (eat('+')) return parseFactor(); // unary plus
	            if (eat('-')) return -parseFactor(); // unary minus

	            double x;
	            int startPos = this.pos;
	            if (eat('(')) { // parentheses
	                x = parseExpression();
	                eat(')');
	            } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
	                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
	                x = Double.parseDouble(eq.substring(startPos, this.pos));
	            } else if (ch >= 'a' && ch <= 'z') { // functions
	                while (ch >= 'a' && ch <= 'z') nextChar();
	                String func = eq.substring(startPos, this.pos);
	                x = parseFactor();
	            } else {
	                throw new RuntimeException("Unexpected: " + (char)ch);
	            }

	            if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

	            return x;
	        }
	    }.parse();
	}
	
	/*
	 * This method returns a string of the mathematical
	 * expression, eq, with all the variables replaced with
	 * the given double value, x.
	 * 
	 * For example:
	 * (x+5) * (x-3)
	 * goes to:
	 * (1+5) * (1-3)
	 * when Double x == 1.
	 */
	public static String plugIn(String eq, Double x, String variables)
	{
		String newEq="";
		for (int i = 0; i < eq.length(); i++)
		{
			if (eq.charAt(i)==variables.charAt(0))
			{
				DecimalFormat df = new DecimalFormat("#");
		        df.setMaximumFractionDigits(8);
				newEq+="("+df.format(x)+")";
			}
			else
			{
				newEq+=eq.charAt(i);
			}
		}
		return newEq;
	}
	
	/*
	 * In the attempt to avoid problems with pointers
	 * and unwanted references in java, almost all nodes
	 * are constructed as new nodes with same information.
	 * As a result, they are no longer a part of the AST
	 * and exist instead separately. To put them into the tree
	 * in their respective positions, the fillParentNodes() method
	 * must occasionally be called.
	 */
	// fills all nodes with correct parents starting from "node"
	public static void fillParentNodes(Node node)
	{
		for (Node child : node.getChildren())
		{
			child.setParent(node);
			if (!child.getChildren().isEmpty())
			{
				fillParentNodes(child);
			}
		}
	}
	
	/*
	 * These "to..." methods and swapNodes are self-explanatory
	 */
	public static String toString(Double d)
	{
		return Double.toString(d);
	}
	
	public static double toDouble(String s)
	{
		BigDecimal bd = new BigDecimal(Double.parseDouble(s));
	    bd = bd.setScale(3, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static void swapNodes(Node a, Node b)
	{
		Node temp = new Node();
		temp.setNode(a);
		a.setNode(b);
		b.setNode(temp);
	}
	
	/*
	 * toInfix converts a given AST to its respective
	 * infix. This is a complex process due to traditional
	 * mathematical nomenclature. It contains many arbitrarily
	 * ugly rules which don't implement nicely into computers,
	 * hence the excessive if-else statements.
	 */
	public static String toInfix(String infix, Node node)
	{
		if (!node.typeIs("operator"))
		{
			if (node.getParent() != null)
			{
				if (!(node.getParent().dataIsOr("*", "/") && node.dataIsIn(new String[]{"1", "1.0", "-1", "-1.0"})))
				{
					if (!(node.getParent().dataIsOr("+","-") && node.dataIsOr("0", "0.0")))
					{
						if (node.getData().getData().charAt(0)=='-' && infix.length()>=2)
						{
							if (infix.charAt(infix.length()-2)=='+')
								infix = infix.substring(0, infix.length()-2);
						}
						infix+=node.getData().getData() + " ";
					}
					else if (node.dataIsOr("0", "0.0"))
					{
						if (infix.length()>=2)
						{
							if (infix.charAt(infix.length()-2)=='+' || infix.charAt(infix.length()-2)=='-')
								infix = infix.substring(0, infix.length()-2);
						}
					}
				}
				else if (node.dataIsOr("-1", "-1.0"))
				{
					if (infix.length()>=2)
					{
						if (infix.charAt(infix.length()-2)=='+')
							infix = infix.substring(0, infix.length()-2);
					}
					infix += "- ";
				}
			}
		}
		else
		{
			boolean needParens=true;
			if (node.isRoot())
			{
				needParens=false;
			}
			else
			{
				if (node.comparePrec(node.getParent()) > 0)
				{
					needParens=false;
				}
				else if (node.comparePrec(node.getParent()) == 0)
				{
					if (node.getParent().getChild(node.getParent().getChildren().size()-1) == node)
					{
						needParens=false;
					}
					if (!node.getParent().dataIs("-"))
					{
						needParens=false;
					}
				}
			}
			
			if (needParens)
				infix += "( ";
			
			for (int i = node.getChildren().size() - 1; i >= 0; i--)
			{
				infix = toInfix(infix, node.getChild(i));
				
				// not first child
				if (i != 0)
				{
					if (!(node.getChild(i-1).dataIsOr("-1","-1.0") || (node.dataIs("*")&&node.getChild(1).dataIsIn(new String[]{"1", "1.0", "-1", "-1.0"}))))
						infix += node.getData().getData() + " ";
//					else if (node.getChild(i-1).dataIsOr("-1","-1.0"))
//						infix += "- ";
				}
			}
			
			if (needParens)
				infix += ") ";
		}
		return infix;
	}
	
	/*
	 * getPrec is an attempt at enforcing the rules
	 * of order of operations whilst reading the infix
	 */
	public static int getPrec(String op)
	{
		Map<String, Integer> ops = new HashMap<String, Integer>();
		ops.put("(", 100); ops.put(")", 0); ops.put("+", 2); ops.put("-", 2); ops.put("*", 3); ops.put("/", 3); ops.put("^", 4);
		
		if (ops.get(op) != null)
			return ops.get(op);
		else
			return -1;
	}
	
	/*
	 * printAST simply provides a visual representation of the
	 * AST, which is useful for debugging
	 */
	public static void printAST(Node node, String appender)
	{
		// default appender should be " "
		System.out.println(appender + node.getData().getData());
		for (Node child : node.getChildren())
		{
			printAST(child, appender+appender);
		}
	}
	
	public static String ASTtoInfix(Node node, String delim)
	{
		String s = toInfix("", node);
		String[] rev = s.split(" ");
		return String.join(delim, rev);
	}
	
	/*
	 * operate is an important part of the constSimp method
	 * in generating actual results from mathematical operations
	 * on two doubles, a and b.
	 */
	public static double operate(String op, String a, String b)
	{
		// returns result from operation op on operands a and b
		if (op.equals("+"))
			return toDouble(a) + toDouble(b);
		else if (op.equals("-"))
			return toDouble(a) - toDouble(b);
		else if (op.equals("*"))
			return toDouble(a) * toDouble(b);
		else if (op.equals("/"))
			return toDouble(a) / toDouble(b);
		else if (op.equals("^"))
			return Math.pow(toDouble(a), toDouble(b));
		else
			return -1;
	}

	/*
	 * This method allows the user to type in:
	 * a+b-(c+d)
	 * 
	 * The method will then transform this into:
	 * a + b - ( c + d )
	 * for use by the program
	 */
	public static String addSpaces(String infix)
	{
		String inf = String.join("", infix.split(" "));
		
		String newInfix="" + inf.charAt(0);
		if (inf.charAt(0)=='(')
		{
			newInfix+=" ";
		}
		for (int i = 1; i < inf.length(); i++)
		{
			if (isOperator(inf.charAt(i)))
			{
				if (!isOperator(inf.charAt(i-1)))
				{
					newInfix += " ";
				}
				newInfix += inf.charAt(i) + " ";
			}
			else
			{
				newInfix += inf.charAt(i);
			}
		}
		
		return newInfix;
	}
	
	/*
	 * Helper method to addSpaces
	 */
	public static boolean isOperator(char c)
	{
		return (c == '-') || (c == '+') || (c == '*') || (c == '/') || (c == '^') || (c == '(') || (c == ')');
	}
	
	/*
	 * This method is a useful helper method for converting
	 * the infix to AST, discussed above.
	 */
	public static String queueToString(Queue<String> q)
	{
		String s = "";
		for (String i : q)
			s += i + ", ";
		return s;
	}
}
