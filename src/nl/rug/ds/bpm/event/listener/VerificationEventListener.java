package nl.rug.ds.bpm.event.listener;

import nl.rug.ds.bpm.event.VerificationResult;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public interface VerificationEventListener {
	void verificationEvent(VerificationResult event);
}
