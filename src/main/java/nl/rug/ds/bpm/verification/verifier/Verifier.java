package nl.rug.ds.bpm.verification.verifier;

import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;


public interface Verifier {

    static long getMaximumStates() {
        return AbstractStructure.getMaximum();
    }

    static void setMaximumStates(long max) {
        AbstractStructure.setMaximum(max);
    }

    void verify() throws VerifierException;

    void addEventListener(VerificationEventListener verificationEventListener);

    void removeEventListener(VerificationEventListener verificationEventListener);
}
