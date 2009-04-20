package uni.netze1.simplefilesharing.client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Fenster, dass die Nachricht "Connecting..." anzeigt.
 * Wenn das Fenster nicht mehr gebraucht wird, mmuss dispose() aufgerufen werden.
 */
public class ConnectionMessage extends JDialog{
	/**
	 * Serialisierungs-Version. 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Erstellt das Fenster und zeigt es an.
	 */
	public ConnectionMessage(){
		super((Frame) null, "Connecting...", false);
		addWindowListener(new ConnectionMessageWindowListener(this));
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());
		JLabel l = new JLabel("Connecting...");
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setVerticalAlignment(SwingConstants.CENTER);
		getContentPane().add(l);
		setAlwaysOnTop(true);
		setSize(400, 77);
		setLocationRelativeTo(null);
		setVisible(true);
		setLocationRelativeTo(null);
	}
	
	/**
	 * Private WindowListener-Klasse.
	 */
	private static class ConnectionMessageWindowListener extends WindowAdapter{
		/**
		 * ConnectionMessage, zu der this geh&ouml;rt.
		 */
		private ConnectionMessage parent;

		/**
		 * Konstruktor.
		 * @param parent ConnectionMessage, zu der <pre>this</pre> geh&ouml;rt. Darf nicht null sein
		 */
		public ConnectionMessageWindowListener(ConnectionMessage parent){
			this.parent = parent;
		}

		/**
		 * Siehe java.awt.event.WindowListener.windowClosing()
		 */
		@Override
		public void windowClosing(WindowEvent e){
			if(JOptionPane.showConfirmDialog(parent,
					"Do you really want to quit?",
					"Really Quit?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				System.exit(0);
		}
	}
}
