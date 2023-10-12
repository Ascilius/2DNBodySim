import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.JFrame;

public class NBody2DDriver {
	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("2D N-Body Simulation"); // frame and title
		
		// creating panel
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		NBody2DPanel panel = new NBody2DPanel(screenSize.getWidth(), screenSize.getHeight());
		frame.add(panel);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// window settings
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		frame.setVisible(true);
		
	}
}