package nl.rug.ds.bpm.log;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */

public class LogEvent {
	public static final int DEBUG = 0;
	public static final int VERBOSE = 1;
	public static final int INFO = 2;
	public static final int WARNING = 3;
	public static final int ERROR = 4;
	public static final int CRITICAL = 5;
	
	private int logLevel;
	private String message;
	
	public LogEvent(int logLevel, String message) {
		this.logLevel = logLevel;
		this.message = message;
	}
	
	public int getLogLevel() {
		return logLevel;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(logLevel == DEBUG)
			sb.append("DEBUG\t: ");
		if(logLevel == VERBOSE)
			sb.append("VERBOSE\t: ");
		if(logLevel == INFO)
			sb.append("INFO\t\t: ");
		if(logLevel == WARNING)
			sb.append("WARNING\t\t: ");
		else if(logLevel == ERROR)
			sb.append("ERROR\t\t: ");
		else if(logLevel == CRITICAL)
			sb.append("CRITICAL\t: ");
		sb.append(message);
		return sb.toString();
	}
}