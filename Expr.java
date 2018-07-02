import java.util.HashMap;
import java.util.Map;

/* 
 * This is the abstract Expression class. Expressions 
 * are the basic form of representative text and input 
 * in the program.
 * 
 * The class encompasses the three types of accepted expressions:
 * constants (e.g. 1, -1, 1.0, etc.)
 * operators (e.g. +, -, /, *, etc.)
 * variables (e.g. a, b, c, x, etc.)
 * 
 * The usefulness of utilizing such an abstract
 * class is that I don't have to specify the type
 * of expression initially. This can be decided
 * later at declaration. An alternative approach
 * could have been to make an abstract class for
 * Nodes, with the three different types. This would
 * be fine, but I prefer my approach as it is easy to
 * add more expressions and such. It is also more
 * organized compared to multiple node-types.
 * 
 * All the methods are quite standard with simple
 * "get" methods and such. The getPrec() method
 * deals with precedence. This is a means of
 * implementing order of operations.
 */
abstract public class Expr {
	private String data;
	
	public Expr (String s)
	{
		data=s;
	}
	
	public String getData()
	{
		return data;
	}
	
	public void setData(String s)
	{
		data=s;
	}
	
	public int getPrec()
	{
		Map<String, Integer> ops = new HashMap<String, Integer>();
		ops.put("+", 2); ops.put("-", 2); ops.put("*", 3); ops.put("/", 3); ops.put("^", 4);
		
		if (ops.get(data) != null)
			return ops.get(data);
		else
			return -1;
	}
	
	abstract public String getType();
}

class ConstExpr extends Expr
{
	public ConstExpr (String n)
	{
		super(n);
	}
	
	public String getType()
	{
		return "const";
	}
}

class OperatorExpr extends Expr
{
	public OperatorExpr (String s)
	{
		super(s);
	}
	
	public String getType()
	{
		return "operator";
	}
}

class VariableExpr extends Expr
{
	public VariableExpr (String x)
	{
		super(x);
	}
	
	public String getType()
	{
		return "variable";
	}
}
