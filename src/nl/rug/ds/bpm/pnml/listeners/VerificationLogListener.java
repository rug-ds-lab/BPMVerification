package nl.rug.ds.bpm.pnml.listeners;

import nl.rug.ds.bpm.pnml.events.VerificationLogEvent;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public interface VerificationLogListener {
	void verificationLogEvent(VerificationLogEvent event);
}
