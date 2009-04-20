package uni.netze1.simplefilesharing.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 * Haupt-Klasse des Filesharing-Clients.
 * Das erstellen einer Instanz reicht, um einen Client zu starten.
 */
public class Main {
	/**
	 * Wird genutzt, um Daten an den Server zu schicken.
	 */
	private PrintWriter toServer;

	/**
	 * Wird genutzt, um Daten vom Server zu empfangen.
	 */
	private BufferedReader fromServer;
	
	/**
	 * Socket f&uuml;r die TCP-Verbindung zum Server.
	 */
	private Socket serverConnectionSocket;

	/**
	 * Der letzte aktive DownloadThread oder null, falls noch keiner gestartet wurde.
	 * Wird <i>nicht</i> auf null zur&uuml;ckgesetzt, wenn der Download abgeschlossen wurde.
	 * @see DownloadThread
	 */
	private DownloadThread downloadThread = null;
	
	/**
	 * Der ListeningThread.
	 * @see ListeningThread.
	 */
	private ListeningThread listeningThread;

	/**
	 * Socket, auf dem nach eingehenden Verbindungen anderer Clients gelauscht wird.
	 */
	private ServerSocket listenSocket;

	/**
	 * Das User-Interface.
	 */
	private GUI gui;

	/**
	 * Ist true, falls der Server momentan l&auml;uft (und in der Hauptschleife ist)
	 */
	private boolean stayAlive;

	/**
	 * Enth&auml;lt alle momentan aktiven Upload-Threads.
	 */
	private Vector<UploadThread> uploadThreads;




	/**
	 * Konstruktor. Startet den Server.
	 * Blockiert, bis der Client geschlossen wird.
	 */
	public Main(){
		try{
			listenSocket = new ServerSocket();
			listenSocket.bind(null);

			joinNetwork();

			uploadThreads = new Vector<UploadThread>();
			gui = new GUI(this);

			System.out.printf("Listen-Port: %d\n", listenSocket.getLocalPort());
			listeningThread = new ListeningThread(listenSocket, this);
			listeningThread.start();

			sendFileList();

			mainLoop();

			for(UploadThread t : uploadThreads)
			{
				t.closeNow();
			}
			if(downloadThread != null)
				downloadThread.closeNow();

		}catch(Exception e){
			System.err.println("Top-Level-Error:");
			e.printStackTrace();
		}
		
		disconnectFromServer();
		
		try{
			listenSocket.close();
		}catch(IOException e){}
	}

//	============================== Grundsätzliche Netzwerk-Methoden ==============================
	/**
	 * Findet den Server und baut eine Verbindung auf.
	 */
	public void joinNetwork() throws IOException {
		ConnectionMessage conMsg = new ConnectionMessage();
		DatagramSocket udp_socket = new DatagramSocket();
		byte buffer_out[] = new byte[256];
		byte buffer_in[] = new byte[256];
		String command = "server_discovery\n";
		buffer_out = command.getBytes();
		InetAddress to_addr = InetAddress.getByName("224.0.0.42");
		DatagramPacket packet = new DatagramPacket(buffer_out, buffer_out.length, to_addr, 4950);
		udp_socket.send(packet);
		packet = new DatagramPacket(buffer_in, buffer_in.length);
		udp_socket.setSoTimeout(10000);
		try {
			udp_socket.receive(packet);
			udp_socket.close();
			printMsg(packet);
			String port_string = new String(packet.getData(), 0, packet.getLength());
			port_string = port_string.split(" ")[1];
			port_string = port_string.substring(0, port_string.length() - 1);
			serverConnectionSocket = new Socket(packet.getAddress(), Integer.parseInt(port_string));
			//tcp_socket = new Socket(InetAddress.getByName("127.0.0.1"), 4455);
			toServer = new PrintWriter(new OutputStreamWriter(serverConnectionSocket.getOutputStream(), "UTF-8"), true);
			fromServer = new BufferedReader(new InputStreamReader(serverConnectionSocket.getInputStream(), "UTF-8"));
			toServer.println(String.format("register %d\n", listenSocket.getLocalPort()));
			toServer.flush();
			conMsg.dispose();
		}
		catch (SocketTimeoutException e) {
			JOptionPane.showMessageDialog(null, "Timeout!", "Timeout!", JOptionPane.ERROR_MESSAGE);
			conMsg.dispose();
			throw new RuntimeException("Could not establish a connection to the server.");
		}
	}

	/**
	 * Schlie&szlig;t die Server-Verbindung.
	 */
	public void disconnectFromServer(){
		if(toServer != null){
			toServer.write("unregister\n");
			toServer.flush();
	
			try{
				serverConnectionSocket.close();
			}catch(IOException e){}
	
			try{
				listenSocket.close();
			}catch(IOException e){}
		}
	}

//	============================== "Aktionen" ==============================

	/**
	 * Erstellt einen neuen UploadThread.
	 * Der Thread wird erstellt, aber nicht gestartet.
	 * @param socket Socket, &uuml;ber die Anfrage eingeht/die Datei gesendet wird.
	 * Sollte bereits ge&ouml;ffnet sein, wird vom Thread geschlossen.
	 * @return den neuen Thread.
	 */
	UploadThread addUploadThread(Socket socket){
		UploadThread u = new UploadThread(socket);
		uploadThreads.add(u);
		return u;
	}

	/**
	 * Sendet die aktueller Dateiliste zum Server.
	 */
	void sendFileList()
	{
		File dir = new File("./shared_files");
		if(!dir.exists() || ! dir.isDirectory())
			System.err.println("Error: the directory \"./shared_files\" does not exist!");
		else
		{
			Vector<File> list = new Vector<File>();
			for(File f : dir.listFiles())
			{
				if(f.isFile())
					list.add(f);
			}
			System.out.println("Sending filelist...");
			toServer.write(String.format("send_filelist %d\n", list.size()));
			toServer.flush();
			//try{Thread.sleep(50);}catch(Exception e){}
			String line;
			for(File f : list)
			{
				line = String.format("%d %s\n", f.length(), f.getName());
				System.out.print("\t" + line);
				toServer.write(line);
				toServer.flush();
				//try{Thread.sleep(50);}catch(Exception e){}
			}
		}
	}

	/**
	 * L&auml;dt eine Datei herunter.
	 * @param file Gibt die Datei an (Format wie vom Server bekommen).
	 */
	void download(String file){
		if(downloadThread != null && downloadThread.isAlive())
			throw new RuntimeException("Do NOT start a download while one is still running.");

		Matcher m = Pattern.compile("^(\\d+) (\\d+\\.\\d+\\.\\d+\\.\\d+) (\\d+) (.+)$").matcher(file);
		if(m.matches()){
			System.out.println("Starting download:");
			System.out.println("\tHost: " + m.group(2));
			System.out.println("\tPort: " + m.group(3));
			System.out.println("\tFile: " + m.group(4));
			System.out.println("\tSize: " + m.group(1));

			try{
				downloadThread = new DownloadThread(InetAddress.getByName(m.group(2)),
						Integer.parseInt(m.group(3)),
						m.group(4),
						Integer.parseInt(m.group(1)),
						gui);
				downloadThread.start();
			}catch(IOException e){
				System.err.println("Download could not be started:");
				e.printStackTrace();
			}
		}else{
			System.err.println("Error while trying to initiate a download: ivalid format.");
		}

	}

//	============================== Kontrollfluß ==============================

	/**
	 * Die Main-Loop (k&uuml;mmert sich um das Empfangen der Dateiliste.
	 */
	private void mainLoop(){
		stayAlive = true;

		String line;
		Matcher m;
		Pattern incomingFilelist = Pattern.compile("^update_filelist (\\d+)$");
		while(stayAlive){
			try{
				line = fromServer.readLine();
				m = incomingFilelist.matcher(line);
				if(m.matches()){
					int n = Integer.parseInt(m.group(1));
					System.out.println("Receiving new filelist from server (" + n + " entries)...");
					gui.resetList();
					for(int i = 0; i < n; i++){
						line = fromServer.readLine();
						System.out.println("\t" + line);
						gui.addToList(line);
					}
				}else{
					System.out.println("Unrecognized message from server: " + line);
				}
			}catch(IOException e){
				if(stayAlive)
				{
					System.err.println("Error while receiving from server:");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Schlie&szlig;t den Server.
	 */
	public void close()
	{
		if(stayAlive){
			stayAlive = false;
			disconnectFromServer();
		}
	}

//	============================== Getter/Setter ==============================

	/**
	 * Gibt zur&uuml;ck, ob der Server momentan l&auml;uft (und in der Hauptschleife ist)
	 * @return true, falls der Server momentan l&auml;uft (und in der Hauptschleife ist).
	 */
	public boolean isAlive()
	{
		return stayAlive;
	}
	
	public String getIDString()
	{
		return (serverConnectionSocket.getLocalAddress().getHostAddress() + ":" + String.valueOf(serverConnectionSocket.getLocalPort())); 
	}

//	============================== static-Methoden ==============================

	/**
	 * Main-Methode.
	 */
	public static void main(String[] args){
		// TODO Auto-generated method stub
		new Main();
	}

	/**
	 * Gibt den Inhalt eines UDP-Pakets auf den STDOUT aus.
	 * @param packet
	 */
	public static void printMsg(DatagramPacket packet) {
		System.out.print("Message: " + new String(packet.getData(), 0, packet.getLength()));
		System.out.println("from " + packet.getAddress().toString() + ":" + String.valueOf(packet.getPort()));
	}
}
