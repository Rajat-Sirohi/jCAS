import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class jGUI extends JFrame implements ActionListener {

	JPanel[] row = new JPanel[6];
    
    char[] buttonString = {'7', '8', '9', '+',
            '4', '5', '6', '-',
            '1', '2', '3', '*',
            '.', '/', 'C', '√',
            '+', '=', '0'};
    
    JButton[] button = new JButton[buttonString.length];
    JTextArea display = new JTextArea(5,20);
    
    JTextArea output = new JTextArea(5,20);
    
    int[] dimW = {300,45,100,90};
    int[] dimH = {35, 40};
    
    /* 
     * This is the constructor function for the
     * Graphical User Interface (GUI).
     */
	public jGUI() {
		super("jCAS"); // Define GUI Title
		
		Dimension displayDimension = new Dimension(dimW[0], 500);
		Dimension outputDimension = new Dimension(300, 99999);
	    Dimension regularDimension = new Dimension(dimW[1], dimH[1]);
	    Dimension rColumnDimension = new Dimension(dimW[2], dimH[1]);
	    Dimension zeroButDimension = new Dimension(dimW[3], dimH[1]);
	    
	    Font font = new Font("Verdana", Font.BOLD, 14);

        setSize(380, 400); 
        setResizable(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        GridLayout grid = new GridLayout(7,5);
        
        setLayout(grid);

        FlowLayout f1 = new FlowLayout(FlowLayout.CENTER);
        FlowLayout f2 = new FlowLayout(FlowLayout.CENTER,1,1);
        
        for(int i = 0; i < 6; i++)
            row[i] = new JPanel();
        row[0].setLayout(f1);
        row[5].setLayout(f1);
        for(int i = 1; i < 5; i++)
            row[i].setLayout(f2);
        
        for(int i = 0; i < 19; i++) {
            button[i] = new JButton();
            button[i].setText(String.valueOf(buttonString[i]));
            button[i].setFont(font);
            button[i].addActionListener(this);
        }
        
        display.setFont(font);
        output.setFont(font);
        display.setWrapStyleWord(true);
        display.setLineWrap(true);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        display.setEditable(true);
        output.setEditable(false);
       //.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        display.setPreferredSize(displayDimension);
        output.setPreferredSize(outputDimension);
        //System.out.println(f1. + " : " + output.getSize().height);
        
        for(int i = 0; i < 14; i++)
            button[i].setPreferredSize(regularDimension);
        for(int i = 14; i < 18; i++)
            button[i].setPreferredSize(rColumnDimension);
        button[18].setPreferredSize(zeroButDimension);
        
        row[0].add(display);
        add(row[0]);
        
        for(int i = 0; i < 4; i++)
            row[1].add(button[i]);
        
        row[1].add(button[14]);
        
        
        for(int i = 4; i < 8; i++)
            row[2].add(button[i]);
        row[2].add(button[15]);
        
        
        for(int i = 8; i < 12; i++)
            row[3].add(button[i]);
        row[3].add(button[16]);
        
        
        row[4].add(button[18]);
        for(int i = 12; i < 14; i++)
            row[4].add(button[i]);
        row[4].add(button[17]);
        
        row[5].add(output);
        //System.out.println(row[5].getSize().width + " : " + row[5].getSize().height);
        this.add(row[1]);
        this.add(row[2]);
        this.add(row[3]);
        this.add(row[4]);
        this.add(row[5]);
        setVisible(true);
    }
    
	/* 
	 * Self-explanatory "clear" method. 
	 */
    public void clear() {
        display.setText("");
    }
    
    /* 
     * Takes in the infix and returns a
     * String concatenated with all variables.
     */
    public String variableParser(String input) {
    	String variables = "";
    	
    	for(int curChar = 0; curChar < input.length(); curChar++) {
    		if(Character.isLetter(input.charAt(curChar)) && variables.indexOf(input.charAt(curChar)) == -1) {
    			variables += input.charAt(curChar);
    		}
    	}
    	
    	return variables;
    }
    
    /* 
     * Takes the input and parses it to
     * get and return the infix. This is
     * based on input of "simplify(a)"
     * where "a" is the infix.
     */
    public String simplifyParser(String input) {
    	int firstPar = input.indexOf("(");
    	int lastPar = input.lastIndexOf(")");
    	
    	String infix = input.substring(firstPar + 1, lastPar);
    	
    	
    	return infix;
    }

    /* 
     * Takes the input and parses it to
     * get and return "a-b". This is
     * based on input of "solve(a=b)"
     * The reason for returning a-b
     * is so that it can be easily solved
     * using Newton's Method.
     */
    public String solveParser(String input) {
    	int firstPar = input.indexOf("(");
    	int equals = input.indexOf("=");
    	int lastPar = input.lastIndexOf(")");
    	
    	String infix = input.substring(firstPar + 1, equals);
    	infix += "-(" + input.substring(equals+1, lastPar) + ")";
    	
    	return infix;
    }
    
    /* 
     * Takes the input and returns if it
     * is calling the simplify() command.
     */
    public boolean isSimplify(String input) {
    	if(input.indexOf("simplify(") != -1)
    		return true;
    	
    	return false;
    }
    
    /* 
     * Takes the input and returns if it
     * is calling the solve() command.
     */
    public boolean isSolve(String input) {
    	if (input.indexOf("solve(") != -1)
    		return true;
    	return false;
    }
    
    /* 
     * Takes a keyboard input from the user
     * and analyzes it to create a jCAS object
     * which will then respectively solve or
     * simplify the input.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
    	boolean add = true;
        String actionCommand = event.getActionCommand();
        if(actionCommand.compareToIgnoreCase("c") == 0) {
        	clear();
        	return;
        }
        
        switch(actionCommand) {
        case "=":
        	add = false;
        	break;
        }
        
        if (isSimplify(display.getText())) {
        	String parsed = simplifyParser(display.getText());
        	String variables = variableParser(parsed);
        	
        	jCAS newEquation = new jCAS(variables, parsed, false, false);
        	output.setText(newEquation.getOutput());
        	add = false;
        }
        else if (isSolve(display.getText())) {
        	String parsed = solveParser(display.getText());
        	String variables = variableParser(parsed);
        	
        	jCAS newEquation = new jCAS(variables, parsed, false, true);
        	output.setText(newEquation.getOutput());
        	add = false;
        }
        
        if(add)
        	display.setText((display.getText() + actionCommand));
    }
}