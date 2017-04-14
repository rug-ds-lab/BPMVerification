package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.pnml.EventHandler;
import nl.rug.ds.bpm.verification.formulas.NuSMVFormula;
import nl.rug.ds.bpm.verification.models.kripke.Kripke;

import java.io.File;
import java.util.List;

/**
 * Created by Mark Kloosterhuis.
 */
public class NuXMVChecker extends NuSMVChecker {


    public NuXMVChecker(EventHandler eventHandler, File checker, Kripke kripke, List<NuSMVFormula> formulas) {
        super(eventHandler, checker, kripke, formulas);
    }
}
