package nl.rug.ds.bpm.verification.modelcheck.nuxmv;

import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.modelcheck.nusmv2.NuSMVChecker;

import java.io.File;

public class NuXMVChecker extends NuSMVChecker {
    
    public NuXMVChecker(EventHandler eventHandler, File checker) {
        super(eventHandler, checker);
    }
}
