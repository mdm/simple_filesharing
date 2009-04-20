package uni.netze1.simplefilesharing.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UploadThread extends Thread {
	/**
	 * N&auml;chste freie ID.
	 */
	private static int nextID = 0;

	/**
	 * Identifiziert einen Thread eindeutige.
	 */
	private int id;
	
	/**
	 * Socket, &uuml;ber die Anfrage eingeht/die Datei gesendet wird.
	 */
	private Socket socket;
	

	/**
	 * Sollte nicht direkt aufgerufen werden, statt dessen: Main.addUploadThread().
	 * @param socket Socket, &uuml;ber die Anfrage eingeht/die Datei gesendet wird.
	 * Sollte bereits ge&oml;ffnet sein, wird vom Thread geschlossen.
	 */
	UploadThread(Socket socket) {
		super("UploadThread " + Integer.toString(nextID));
		this.id = nextID++;
		this.socket = socket;
		
	}

	/**
	 * Siehe Thread.
	 * @see Thread.run()
	 */
	@Override
	public void run() {
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			String request = in.readLine();
			Matcher m  = Pattern.compile("^get (.+)$").matcher(request);
			if(m.matches())
			{			
				System.out.println(String.format("Upload %d: %s requests file: %s",
						id,
						socket.getRemoteSocketAddress().toString(),
						m.group(1)));
				File file = new File("shared_files/" + m.group(1));
				if(file.exists() && file.isFile()){
					InputStream fis = new FileInputStream(file);
					byte[] buffer = new byte[1024];
					int read;
					int readSoFar = 0;
					try{
						for(read = fis.read(buffer); read > 0; read = fis.read(buffer)){
							socket.getOutputStream().write(buffer, 0, read);
							readSoFar += read;
						}
					}catch(IOException e){
						System.err.println("Error during upload:");
						e.printStackTrace();
					}					
					fis.close();
				}else{
					System.err.println("Upload " + id + ": File does not exist.");
				}
			}else{
				System.err.println("Upload " + id + ": Invalid request.");
			}
		}catch(IOException e){
			System.err.println("Upload " + id + ": Error while uploading file:");
			e.printStackTrace();
		}

		closeNow();
	}
	
	/**
	 * Veranlasst den Thread, sich zu schlie&szlig;en, ohne den aktuellen Vorgang
	 * abzuschlie&szlig;en. Darf aufgerufen werden, auch wenn der Thread nicht mehr l&aml;uft.
	 */
	public void closeNow(){
		try{
			if(socket.isConnected())
				socket.close();
		}catch(IOException e){
			System.err.println("Upload " + id + ": Error while closing the thread:");
			e.printStackTrace();
		}
	}
}







