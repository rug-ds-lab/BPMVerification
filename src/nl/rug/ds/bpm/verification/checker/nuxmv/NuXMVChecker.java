package nl.rug.ds.bpm.verification.checker.nuxmv;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.verification.checker.AbstractFormula;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVChecker;
import nl.rug.ds.bpm.verification.checker.nusmv2.NuSMVFormula;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;

import java.io.File;
import java.util.List;

/**
 * Created by Mark Kloosterhuis.
 */
public class NuXMVChecker extends NuSMVChecker {


    public NuXMVChecker(EventHandler eventHandler, File checker, Kripke kripke, List<AbstractFormula> formulas) {
        super(eventHandler, checker, kripke, formulas);
    }
}
