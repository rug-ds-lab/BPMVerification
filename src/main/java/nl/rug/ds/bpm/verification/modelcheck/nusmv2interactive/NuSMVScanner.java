package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class NuSMVScanner {
	private Process proc;
	private Scanner inputStream;
	private PrintStream outputStream;
	private String[] input;
	private int index;

	public NuSMVScanner(Process proc) {
		input = new String[0];
		index = 0;

		this.proc = proc;

		OutputStream out = proc.getOutputStream();
		outputStream = new PrintStream(out);

		InputStream stdin = proc.getInputStream();
		InputStreamReader in = new InputStreamReader(stdin);
		inputStream = new Scanner(in);

		inputStream.useDelimiter("\\s*NuSMV\\s>");

		read();
	}

	private void read() {
		while (!inputStream.hasNext()) ;
		String l = inputStream.next();
		input = l.split("\\R");
		index = 0;
	}

	public void writeln(String message) {
		outputStream.println(message);
		outputStream.flush();
		read();
	}

	public boolean hasNext() {
		return index < input.length;
	}

	public String next() {
		return (input.length > index ? input[index++] : "");
	}

	public void close() {
		outputStream.println("quit");
		outputStream.flush();

		outputStream.close();
		inputStream.close();
	}
}
