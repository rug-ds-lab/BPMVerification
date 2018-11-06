package nl.rug.ds.bpm.verification.event;

import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFormula;

import java.util.HashSet;
import java.util.List;
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

	public void fireEvent(VerificationEvent event) {
		notify(event);
	}
	
	public void fireEvent(CheckerFormula formula, boolean eval) {
		VerificationEvent verificationEvent = new VerificationEvent(formula, eval);
		notify(verificationEvent);
	}

	public void fireEvent(CheckerFormula formula, boolean eval, List<List<String>> counterExample) {
		VerificationEvent verificationEvent = new VerificationEvent(formula, eval, counterExample);
		notify(verificationEvent);
	}

	private void notify(VerificationEvent event) {
		for (VerificationEventListener listener: verificationEventListenerSet)
			listener.verificationEvent(event);
	}
}
