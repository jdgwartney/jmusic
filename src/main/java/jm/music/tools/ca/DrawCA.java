/* --------------------
* A tool for displaying Cellular Automata in
* a window.
* @author Andrew Troedson and Andrew Brown
* ---------------------
*/
package jm.music.tools.ca;

import java.awt.*;
import java.awt.event.*;

public class DrawCA extends Frame implements WindowListener {
	int xPos;
	int yPos;
	int width;
	int height;
	int cellSize;
	CellDrawArea da;
	
	//--------------
	//constructors
	//--------------
	public DrawCA(AutomataMusic AM) {
		this(AM, "Cellular Automata");
	}
	
	public DrawCA(AutomataMusic AM, String title) {
		this(AM, title, 5); //5 is the default cellSize
	}
	
	public DrawCA(AutomataMusic AM, String title, int cellSize) {
		this(AM, title, cellSize, 10, 10); // default window position
	}
	
	public DrawCA(AutomataMusic AM, String title, int cellSize, int xPos, int yPos) {
		super(title);			
		this.width = AM.cells.length;
		this.height = AM.cells[0].length;
		this.cellSize = cellSize;
		this.xPos = xPos;
		this.yPos = yPos;
		
		//register the closebox event
		this.addWindowListener(this);
		
		//add a Panel called pan with a Canvas called da inside it
		Panel pan = new Panel();
		pan.setSize(width*cellSize, height*cellSize);
		da = new CellDrawArea(AM, cellSize);
		pan.add(da);
		this.add(pan);
		
		//construct and display			
		this.setSize(width*cellSize, height*cellSize);
		this.setResizable(false);
		this.setLocation(xPos,yPos);			
		this.pack();					
		this.show();
		this.toFront();
	}
	
	//--------------
	// Class Methods
	//--------------
	
	public void repaint() {
		da.repaint();
	}
	
	// Deal with the window closebox
	public void windowClosing(WindowEvent we) {
		this.dispose(); //System.exit(0);
	}
	//other WindowListener interface methods
	//They do nothing but are required to be present
	public void windowActivated(WindowEvent we) {};
	public void windowClosed(WindowEvent we) {};
	public void windowDeactivated(WindowEvent we) {};
	public void windowIconified(WindowEvent we) {};
	public void windowDeiconified(WindowEvent we) {};
	public void windowOpened(WindowEvent we) {};
	
}

//--------------
//second class!!
//--------------
class CellDrawArea extends Canvas {
	AutomataMusic AM;
	int cellSize;
	
	public CellDrawArea(AutomataMusic AM, int cellSize){
		super();		
		this.AM = AM;
		this.cellSize = cellSize;
		this.setSize(AM.cells.length*cellSize,AM.cells[0].length*cellSize);
	}	
	
	public void paint(Graphics g) {
		g.setColor(Color.black);
		for(int i=0;i<AM.cells.length;i++) {
			for(int j=0;j<AM.cells[0].length;j++) {
			if(AM.cells[i][j] == true) g.fillRect(cellSize*i,cellSize*j,cellSize-1,cellSize-1);
			}
		}
	}
}
