import java.awt.event.*;
import javax.swing.*;

/*
 * Created on 06.06.2007
 *
 */

/**
 * @author Daniel Enke
 */

public class SudokuApplication extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String APP_VERSION = "2.4";

	public static void main (String[] args) {
		
		try {
			UIManager.setLookAndFeel(
				UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Couldn't use the system "
						 + "look and feel: " + e);
		}
		SudokuMainframe s = new SudokuMainframe(APP_VERSION);

		WindowListener l = new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				System.exit(0);
			}
		};

		s.addWindowListener (l);
		s.pack ();

		s.setVisible (true);
	}
	
}
