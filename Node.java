import java.util.ArrayList;
import java.util.List;

/* 
 * The Node class is the basic structural unit of
 * the AST (Abstract Syntax Tree). This was my chosen
 * method of representing the mathematical expressions.
 * Each node is arranged such that it is either an operator
 * or operand. It is binary so that each node has either
 * 0 or 2 children, seen in the children ArrayList.
 * The AST is arranged such that "(x+2)*(x-3)+1" is:
 * +
 * 		1
 * 		*
 * 			+
 * 				x
 * 				2
 * 			+
 * 				x
 * 				-3
 * 
 * Most methods in this class are just "get" methods
 * or other similar methods for the convenience of programming.
 */
public class Node {
	private List<Node> children = new ArrayList<Node>();
	private Node parent;
	private Expr data;
	
	public Node()
	{
	}
	
	public Node(Expr e)
	{
		data=e;
	}
	
	public Expr getData()
	{
		return data;
	}
	
	public void setData(Expr e)
	{
		data=e;
	}
	
	public List<Node> getChildren()
	{
		return children;
	}
	
	public Node getChild(int pos)
	{
		return children.get(pos);
	}
	
	public void setChildren(List<Node> c)
	{
		children = new ArrayList<Node>();
		for (Node child : c)
		{
			children.add(child);
		}
	}
	
	public Node getParent()
	{
		return parent;
	}
	
	public void setParent(Node p)
	{
		parent=p;
	}
	
	public Node setNode(Node node)
	{
		parent=node.getParent();
		data=node.getData();
		this.setChildren(node.getChildren());
		
		return this;
	}
	
	public Node addChild(Node node)
	{
		this.getChildren().add(node);
		node.setParent(this);
		return node;
	}
	
	public boolean typeIs(String type)
	{
		return this.getData().getType().equals(type);
	}
	
	public boolean typeIsOr(String a, String b)
	{
		return (this.typeIs(a) || this.typeIs(b));
	}
	
	public boolean sameType(Node node)
	{
		return this.getData().getType().equals(node.getData().getType());
	}
	
	public boolean dataIs(String data)
	{
		return this.getData().getData().equals(data);
	}
	
	public boolean dataIsOr(String a, String b)
	{
		return (this.dataIs(a) || this.dataIs(b));
	}
	
	public boolean dataIsIn(String[] s)
	{
		for (String i : s)
		{
			if (i.equals(data.getData()))
				return true;
		}
		return false;
	}
	
	public boolean sameData(Node node)
	{
		return this.dataIs(node.getData().getData());
	}
	
	public int comparePrec(Node node)
	{
		return this.getData().getPrec() - node.getData().getPrec();
	}
	
	public boolean hasChildOfType(String type)
	{
		for (Node node : children)
		{
			if (node.typeIs(type))
				return true;
		}
		return false;
	}
	
	public int getChildOfTypeInd(String type)
	{
		// returns index of child of type "type"
		for (int i = 0; i < children.size(); i++)
		{
			if (children.get(i).typeIs(type))
				return i;
		}
		return -1;
	}
	
	public boolean hasChildOfData(String s)
	{
		for (Node node : children)
		{
			if (node.dataIs(s))
				return true;
		}
		return false;
	}
	
	public int getChildOfDataInd(String s)
	{
		for (int i = 0; i < children.size(); i++)
		{
			if (children.get(i).dataIs(s))
				return i;
		}
		return -1;
	}
	
	public int getOtherChildInd()
	{
		List<Node> Children = this.getParent().getChildren();
		for (int i = 0; i < Children.size(); i++)
		{
			if (Children.get(i) != this)
				return i;
		}
		return -1;
	}
	
	public Node getChildOfType(String type)
	{
		// returns index of child of type "type"
		for (int i = 0; i < children.size(); i++)
		{
			if (children.get(i).typeIs(type))
				return children.get(i);
		}
		return null;
	}
	
	public Node getChildOfData(String s)
	{
		for (int i = 0; i < children.size(); i++)
		{
			if (children.get(i).dataIs(s))
				return children.get(i);
		}
		return null;
	}
	
	public Node getOtherChild()
	{
		if (this.isRoot())
		{
			return this;
		}
		else
		{
			List<Node> Children = this.getParent().getChildren();
			for (int i = 0; i < Children.size(); i++)
			{
				if (Children.get(i).isSame(this))
					return Children.get((i+1)%2);
			}
		}
		return null;
	}
	
	public boolean isSame(Node node)
	{
		if (node.sameData(this))
		{
			if (children.size() == 0 && node.getChildren().size() == 0)
			{
				return true;
			}
			else
			{
				return node.getChild(0).isSame(this.getChild(0)) && node.getChild(1).isSame(this.getChild(1));
			}
		}
		else
		{
			return false;
		}
	}
	
	public boolean isRoot()
	{
		return parent==null;
	}
	
	public boolean hasChildren()
	{
		return children.size()!=0;
	}
}
