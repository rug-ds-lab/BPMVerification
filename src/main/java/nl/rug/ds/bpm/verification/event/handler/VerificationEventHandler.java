package nl.rug.ds.bpm.verification.event.handler;

import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class VerificationEventHandler extends AbstractEventHandler<VerificationEventListener, VerificationEvent> {

    public VerificationEventHandler() {
        super();
    }

    @Override
    protected void notify(VerificationEvent event) {
        for (VerificationEventListener listener : listenerSet)
            listener.verificationEvent(event);
    }
}
