package uni.netze1.simplefilesharing.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Ein Thread, der eine Datei von einem anderen Client runterl&auml;dt.
 */
class DownloadThread extends Thread {
	/**
	 * Referenz auf das GUI-Objekt.
	 */
	private GUI gui;
	
	/**
	 * TCP-Socket f&uuml;r die Peer-to-peer-Kommunikation.
	 */
	private Socket socket;
			
	/**
	 * Name der zu empfangenden Datei.
	 */
	private String file;
	
	/**
	 * Gr&ouml;&szlig;e der zu empfangenden Datei.
	 */
	private int size;
	

	/**
	 * Sollte nicht direkt aufgerufen werden, statt dessen: Main.addUploadThread().
	 * @param addr Adresse des Clients, von dem heruntergeladen werden soll.
	 * @param port Port des Clients, von dem heruntergeladen werden soll.
	 * @param file Name der zu empfangenden Datei.
	 * @param size Gr&ouml;&szlig;e der zu empfangenden Datei.
	 * @param gui Referenz auf das GUI-Objekt.
	 */
	DownloadThread(InetAddress addr, int port, String file, int size, GUI gui) throws IOException{
		super("DownloadThread");
		this.file = file;
		this.size = size;
		this.gui = gui;
		
		socket = new Socket(addr, port);
	}

	/**
	 * Siehe java.lang.Thread.run().
	 */
	@Override
	public void run() {
		gui.enterDownloadMode(size);
		try{
			PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
			out.write("get " + file + "\n");
			out.flush();
			
			File f = new File("downloaded_files/" + file);
			try{
				f.createNewFile();
				FileOutputStream fos = new FileOutputStream(f);
				byte[] buffer = new byte[1024];
				int read;
				int readSoFar = 0;
				for(read = socket.getInputStream().read(buffer); read > 0; read = socket.getInputStream().read(buffer)){
					fos.write(buffer, 0, read);
					readSoFar += read;
					gui.setProgress(readSoFar);
					fos.flush();		
				}
				fos.close();
			}catch(IOException e){
				System.err.println("Error while downloading:");
				e.printStackTrace();
			}
			closeNow();
		}catch(IOException e){
			System.err.println("Error while downloading:");
			e.printStackTrace();
		}
		gui.exitDownloadMode();
	}

	
	/**
	 * Veranlasst den Thread, sich zu schlie&szlig;en, ohne den aktuellen Vorgang
	 * abzuschlie&szlig;en. Darf aufgerufen werden, auch wenn der Thread nicht mehr l&auml;uft.
	 * Der Aufrufer muss sicher stellen, dass der so geschlossene Thread auch aus
	 * main.uploadThreads entfernt wird!
	 */
	public void closeNow(){
		try{
			if(socket.isConnected())
				socket.close();
		}catch(IOException e){
			System.err.println("Download : Error while closing the thread:");
			e.printStackTrace();
		}
	}
}
