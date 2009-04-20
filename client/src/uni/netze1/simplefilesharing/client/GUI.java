package uni.netze1.simplefilesharing.client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

/**
 * Klasse f&uuml;r das Hauptfenster.
 */
class GUI implements ActionListener{
	/**
	 * Referenz auf das Main-Objekt.
	 */
	Main client;
	
	/**
	 * Das Fenster.
	 */
	private JFrame frame;
	
	/**
	 * Button zum Dateiliste senden.
	 */
	private JButton refresh;
	
	/**
	 * Button zum Herunterladen der ausgew&auml;hlten Datei.
	 */
	private JButton get;
	
	// Option zum filtern der globalen Liste.
	private JCheckBox filter;
		
	/**
	 * Siehe java.awt.event.ActionListener.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == refresh)
			client.sendFileList();
		else if(e.getSource() == get){
			if(!list.isSelectionEmpty())
				client.download(list.getSelectedValue().toString());
		}else
			JOptionPane.showMessageDialog(frame, "Received ActionEvent form unknown source!",  "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Die Dateiliste.
	 */
	private JList list;
	
	/**
	 * Das Listmodel zu {@link #client list}. 
	 */
	private DefaultListModel model;
	
	/**
	 * Die Progressbar.
	 */
	private JProgressBar progress;
	
	/**
	 * true, falls das User-Interface gerade im Download-Modus ist.
	 */
	private boolean downloadMode;

	/**
	 * Konstruktor.
	 * @param client Initialisiert {@link #client client}.
	 */
	protected GUI(Main client){
		this.client = client;
		downloadMode = false;
		
		frame = new JFrame("Simple Filesharing Client (" + client.getIDString() + ")");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new GUIWindowListener());
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		JTabbedPane tabs = new JTabbedPane();
	
		// start tab connection
		JPanel panel = new JPanel(new BorderLayout());
		tabs.addTab("Connection", panel);
		JTextArea console = new JTextArea();
		console.setFont(new Font("Monospaced", Font.PLAIN, 12));
		System.setOut(new PrintStream(new TextAreaOutputStream(new ByteArrayOutputStream(), console)));
		panel.add(console);
		// end tab connection

		// start tab available files
		panel = new JPanel();
		tabs.addTab("Available Files", panel);
		panel.setLayout(new BorderLayout());
		
		JPanel panel2 = new JPanel(new BorderLayout());
		panel.add(panel2, BorderLayout.NORTH);
		
		get = new JButton("Download selected file");
		get.addActionListener(this);
		panel2.add(get, BorderLayout.NORTH);		

		refresh = new JButton("Update file list");
		refresh.addActionListener(this);
		panel.add(refresh, BorderLayout.SOUTH);

		/*filter = new JCheckBox("Filter my own files from the global list.");
		filter.addActionListener(this);
		panel2.add(filter, BorderLayout.SOUTH);//*/

		progress = new JProgressBar();
		progress.setVisible(downloadMode);
		panel2.add(progress, BorderLayout.CENTER);
		
		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		panel.add(list, BorderLayout.CENTER);
		// end tab available files
		
		/*panel = new JPanel();
		tabs.addTab("Shared Files", panel);
		
		refresh = new JButton("Send refreshed list to Server");
		refresh.addActionListener(this);
		panel.add(refresh, BorderLayout.NORTH);//*/

		tabs.setSelectedIndex(1); // highlight "Available Files" on startup
		frame.getContentPane().add(tabs);		
		frame.setVisible(true);
	}
	
	/**
	 * L&ouml;scht alle Elemente aus der Dateiliste.
	 */
	public void resetList(){
		model.removeAllElements();
	}
	
	/**
	 * F&uuml;gt eine Zeile zu der Dateiliste hinzu.
	 * @param s Die hinzuzuf&uuml;gende Zeile.
	 */
	public void addToList(String s){
		model.addElement(s);
		list.repaint();
	}

	/**
	 * Wechselt in den Download-Modus.
	 * @param filesize Dateigr&ouml;&szlig;e der herunterzuladenden Datei in Bytes. 
	 */
	public synchronized void enterDownloadMode(int filesize){
		if(!downloadMode){
			downloadMode = true;
			progress.setMinimum(0);
			progress.setMaximum(filesize);
			progress.setValue(0);
			progress.setVisible(downloadMode);
			get.setEnabled(false);
		}
	}
	
	/**
	 * Beendet den Download-Modus.
	 */
	public synchronized void exitDownloadMode(){
		if(downloadMode){
			downloadMode = false;
			progress.setVisible(downloadMode);
			get.setEnabled(true);
		}
	}
	
	/**
	 * Aktualisiert den Download-Fortschritt.
	 * @param to Anzahl der bereits geruntergeladenen Bytes.
	 */
	public void setProgress(int to){
		progress.setValue(to);
		progress.repaint();
	}

	/**
	 * WindowListene f&uuml;r das Hauptfenster.
	 */
	class GUIWindowListener extends WindowAdapter{
		/**
		 * Konstruktor.
		 */
		GUIWindowListener(){}

		/**
		 * Siehe java.awt.event.WindowListener.
		 */
		@Override
		public void windowClosed(WindowEvent e) {
			client.close();
		}
	}
}
