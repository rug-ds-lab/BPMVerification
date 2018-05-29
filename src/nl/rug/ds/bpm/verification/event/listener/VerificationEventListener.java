package nl.rug.ds.bpm.verification.event.listener;

import nl.rug.ds.bpm.verification.event.VerificationEvent;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public interface VerificationEventListener {
	void verificationEvent(VerificationEvent event);
}
