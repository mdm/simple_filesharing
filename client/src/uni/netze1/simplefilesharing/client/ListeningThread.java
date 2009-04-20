package uni.netze1.simplefilesharing.client;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Lauscht nach eingehenden TCP-Verbindungen anderer Clients und startet gegebenenfalls einen
 * FileUploadThread.
 */
class ListeningThread extends Thread {
	/**
	 * Main, zu der dieser ListeningThread geh&ouml;rt.
	 */
	private Main parent;
	
	/**
	 * Socket, auf der gelauscht wird.
	 */
	private ServerSocket socket;
	
	/**
	 * Beendet sich selbst, wenn die Socket geschlossen wird.
	 * @param socket Socket, auf der gelauscht werden soll.
	 * Muss bereit sein, um accept() auf zu rufen, also insbesondere bereits im Listening-Modus sein.
	 * @param parent Main, zu der dieser ListeningThread geh&ouml;rt. 
	 */
	public ListeningThread(ServerSocket socket, Main parent)
	{
		this.socket = socket;
		this.parent = parent;
	}
	
	/**
	 * Siehe java.lang.Thread.run().
	 */
	@Override
	public void run(){
		while(!socket.isClosed())
		{
			try{
				parent.addUploadThread(socket.accept()).start();//Neuen Thread erstellen und starten
			}catch(IOException e){
				if(!socket.isClosed()){
					System.err.println("Error while trying to accept a file-request:");
					e.printStackTrace();
				}
			}
		}
	}
}
