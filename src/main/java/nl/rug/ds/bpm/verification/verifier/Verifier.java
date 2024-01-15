package nl.rug.ds.bpm.verification.verifier;

import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.verification.event.listener.PerformanceEventListener;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;


public interface Verifier {

    /**
     * Returns the maximum number of states the Verifier may consider.
     *
     * @return the maximum number of states.
     */
    static long getMaximumStates() {
        return AbstractStructure.getMaximum();
    }

    /**
     * Sets the maximum number of states the Verifier may consider.
     */
    static void setMaximumStates(long max) {
        AbstractStructure.setMaximum(max);
    }

    /**
     * Starts the verification process.
     *
     * @throws VerifierException when the verification process fails.
     */
    void verify() throws VerifierException;

    /**
     * Adds a listener that is notified of verification results.
     *
     * @param verificationEventListener the listener to add.
     */
    void addVerificationEventListener(VerificationEventListener verificationEventListener);

    /**
     * Adds a listener that is notified of performance results.
     *
     * @param performanceEventListener the listener to add.
     */
    void addPerformanceEventListener(PerformanceEventListener performanceEventListener);

    /**
     * Removes the listener, preventing further notifications.
     *
     * @param verificationEventListener the listener to remove.
     */
    void removeEventListener(VerificationEventListener verificationEventListener);
}
