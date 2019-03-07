package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
		input = inputStream.next().split("\\R");
		index = 0;
	}

	public void writeln(String message) {
		try {
			outputStream.println(message);
			outputStream.flush();
			read();
		}
		catch (Exception e) {
			Logger.log("Failed to read NuSMV2 feedback for message " + message, LogEvent.ERROR);
		}
	}

	public boolean hasNext() {
		return index < input.length;
	}

	public String next() {
		return (input.length > index ? input[index++] : "");
	}

	public List<String> getErrors() {
		List<String> results = new ArrayList<>();

		//errorstream
		InputStream stderr = proc.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);

		try {
			if (br.ready()) {
				String line = null;
				while ((line = br.readLine()) != null) {
					results.add(line);
				}
			}
			br.close();
		}
		catch (Exception e) {}

		return results;
	}

	public void close() {
		writeln("quit");

		outputStream.close();
		inputStream.close();
	}
}
