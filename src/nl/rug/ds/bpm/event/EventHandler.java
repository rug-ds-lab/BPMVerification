package nl.rug.ds.bpm.event;

import nl.rug.ds.bpm.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.event.listener.VerificationLogListener;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class EventHandler {
	private static int logLevel = 1;
	
	private Set<VerificationEventListener> verificationEventListenerSet;
	private Set<VerificationLogListener> verificationLogListenerSet;
	
	public EventHandler() {
		verificationEventListenerSet = new HashSet<>();
		verificationLogListenerSet = new HashSet<>();
	}
	
	public void addEventListener(VerificationEventListener verificationEventListener) {
		verificationEventListenerSet.add(verificationEventListener);
	}
	
	public void removeEventListener(VerificationEventListener verificationEventListener) {
		verificationEventListenerSet.remove(verificationEventListener);
	}
	
	public void addLogListener(VerificationLogListener verificationLogListener) {
		verificationLogListenerSet.add(verificationLogListener);
	}
	
	public void removeLogListener(VerificationLogListener verificationLogListener) {
		verificationLogListenerSet.remove(verificationLogListener);
	}
	
	public void fireEvent(CheckerFormula formula, boolean eval) {
		VerificationResult verificationResult = new VerificationResult(formula, eval);
		
		for (VerificationEventListener listener: verificationEventListenerSet)
			listener.verificationEvent(verificationResult);
	}
	
	public void logDebug(String message) {
		pushLog(new VerificationLog(VerificationLog.DEBUG, message));
	}
	
	public void logVerbose(String message) {
		pushLog(new VerificationLog(VerificationLog.VERBOSE, message));
	}
	
	public void logInfo(String message) {
		pushLog(new VerificationLog(VerificationLog.INFO, message));
	}
	
	public void logWarning(String message) {
		pushLog(new VerificationLog(VerificationLog.WARNING, message));
	}
	
	public void logError(String message) {
		pushLog(new VerificationLog(VerificationLog.ERROR, message));
	}
	
	public void logCritical(String message) {
		pushLog(new VerificationLog(VerificationLog.CRITICAL, message));
		
		System.exit(-1);
	}
	
	private void pushLog(VerificationLog e) {
		if(e.getLogLevel() >= EventHandler.logLevel)
			for(VerificationLogListener listener: verificationLogListenerSet)
				listener.verificationLogEvent(e);
	}
	
	public static void setLogLevel(int logLevel) { EventHandler.logLevel = logLevel; }
	
	public static int getLogLevel() { return logLevel; }
}
