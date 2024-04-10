import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

public class NBody2DDriver {
	public static void main(String[] args) throws IOException {
		// simulation 1
		JFrame frame1 = new JFrame("2D N-Body Simulation"); // frame and title

		// creating panel
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		NBody2DPanel panel1 = new NBody2DPanel(screenSize.getWidth(), screenSize.getHeight());
		frame1.add(panel1);

		// when the simulation is alt+tabbed out of (https://gamedev.stackexchange.com/questions/59229/how-to-detect-whether-my-java-application-is-active)
		frame1.addWindowFocusListener(new WindowAdapter() {
			public void windowLostFocus(WindowEvent e) {
				// panel1.pause();
				panel1.clear_held_keys();
			}
		});

		// frame1.setSize(1080, 720);

		// frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// window settings
		frame1.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame1.setUndecorated(true);
		// frame1.setOpacity((float) 0.5);
		frame1.setVisible(true);

		// simulation 2
		/*
		JFrame frame2 = new JFrame("2D N-Body Simulation"); // frame and title
		
		// creating panel
		NBody2DPanel panel2 = new NBody2DPanel(screenSize.getWidth(), screenSize.getHeight());
		frame2.add(panel2);
		
		// frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// window settings
		// frame2.setExtendedState(JFrame.MAXIMIZED_BOTH);
		// frame2.setUndecorated(true);
		frame2.setVisible(true);
		*/
	}
}