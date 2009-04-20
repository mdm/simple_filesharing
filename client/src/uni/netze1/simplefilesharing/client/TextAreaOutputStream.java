package uni.netze1.simplefilesharing.client;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

public class TextAreaOutputStream extends FilterOutputStream {

	private JTextArea textArea;
	public TextAreaOutputStream(OutputStream out, JTextArea textArea) {
		super(out);
		
		this.textArea = textArea;
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		String newString = new String(b, off, len);
		textArea.append(newString);
	}
	@Override
	public void write(byte[] b) throws IOException {
		String newString = new String(b);
		textArea.append(newString);
	}
	@Override
	public void write(int b) throws IOException {
		byte[] newByte= new byte[1];
		newByte[0] = (byte) b;
		this.write(newByte);
	}

}
