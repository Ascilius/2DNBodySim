import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

public class NBody2D {
	
	// debugging
	private static final boolean debug = false;
	
	public static void main(String[] args) throws IOException {
		Manager manager = new Manager();
		
		if (debug) {
			manager.newWindow("Debug: Window 2"); // testing multi-window functionality
		}
	}
}

// -----------------------------------------------------------------------------------------------------------------------------
// class for managing windows and simulations
class Manager {
	
	// debugging
	private final boolean debug = true;
	
	// computer specifications
	private double screenWidth, screenHeight;
	
	// containers
	private ArrayList<JFrame> windows = new ArrayList<JFrame>(); // JFrames contain Windows
	private ArrayList<Simulation> sims = new ArrayList<Simulation>();
	
	public Manager() {
		// getting computer/monitor information
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screenWidth = screenSize.getWidth();
		screenHeight = screenSize.getHeight();
		if (debug)
			System.out.println("Debug: Screen dimensions: " + screenWidth + " x " + screenHeight);
		
		// creating first window/simulation
		newWindow("2D N-Body Simulation");
	}
	
	// creates a new window (and consequently a new simulation)
	public void newWindow(String title) {
		// creating new JFrame
		JFrame newFrame = new JFrame(title);
		Window newWindow = new Window(this, (int) screenWidth, (int) screenHeight); // also creates new sim
		newFrame.add(newWindow); // adding window to frame
		
		// additional frame adjustments
		newFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		newFrame.setVisible(true);
		newFrame.setMinimumSize(new Dimension(500, 500)); // setting minimum window size (500 x 500 pixels)
		
		// adding JFrame/Window to container
		windows.add(newFrame);
		if (debug)
			System.out.println("Debug: New window/simulation created.");
		
		// starting simulation
		// newWindow.repaint();
	}
	
	// creates a new simulation
	public void newSimulation() {
		Simulation newSim = new Simulation();
		sims.add(newSim);
	}
	
	// adds a simulation to manager
	public void addSim(Simulation newSim) {
		sims.add(newSim);
	}
	
	// reassigns a window with a different simulation
	public void reassignSim(Window window, int curr_i, int di) {
		// incrementing index
		curr_i += di;
		if (curr_i < 0)
			curr_i = sims.size() - 1;
		else if (curr_i >= sims.size())
			curr_i = 0;
		
		// reassigning
		window.assignSim(sims.get(curr_i), curr_i);
	}
	
}