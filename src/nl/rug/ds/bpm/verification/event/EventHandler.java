package nl.rug.ds.bpm.verification.event;

import nl.rug.ds.bpm.verification.checker.CheckerFormula;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class EventHandler {
	private Set<VerificationEventListener> verificationEventListenerSet;
	
	public EventHandler() {
		verificationEventListenerSet = new HashSet<>();
	}
	
	public void addEventListener(VerificationEventListener verificationEventListener) {
		verificationEventListenerSet.add(verificationEventListener);
	}
	
	public void removeEventListener(VerificationEventListener verificationEventListener) {
		verificationEventListenerSet.remove(verificationEventListener);
	}
	
	public void fireEvent(CheckerFormula formula, boolean eval) {
		VerificationEvent verificationEvent = new VerificationEvent(formula, eval);
		
		for (VerificationEventListener listener: verificationEventListenerSet)
			listener.verificationEvent(verificationEvent);
	}
}
