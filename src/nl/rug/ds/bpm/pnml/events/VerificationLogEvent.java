package nl.rug.ds.bpm.pnml.events;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */

public class VerificationLogEvent {
	public static enum eventType {
		VERBOSE,
		INFO,
		ERROR,
		CRITICAL;
	}
	
	private eventType type;
	private String message;
	
	public VerificationLogEvent(eventType type, String message) {
		this.type = type;
		this.message = message;
	}
	
	public eventType getType() {
		return type;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(type == eventType.VERBOSE)
			sb.append("VERBOSE\t: ");
		if(type == eventType.INFO)
			sb.append("INFO\t: ");
		else if(type == eventType.ERROR)
			sb.append("ERROR\t: ");
		else
			sb.append("CRITICAL: ");
		sb.append(message);
		return sb.toString();
	}
}
