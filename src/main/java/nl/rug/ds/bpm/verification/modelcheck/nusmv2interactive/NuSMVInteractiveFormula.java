package nl.rug.ds.bpm.verification.modelcheck.nusmv2interactive;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.util.map.TreeSetMap;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.modelcheck.CheckerFormula;

/**
 * Created by p256867 on 13-4-2017.
 */
public class NuSMVInteractiveFormula extends CheckerFormula {

    public NuSMVInteractiveFormula(Formula formula, Specification specification, IDMap idMap, TreeSetMap<String, String> groupMap) {
        super(formula, specification, idMap, groupMap);
    }

    @Override
    public boolean equals(String outputFormula) {
        try {
            return trimFormula(super.getCheckerFormula()).equals(trimFormula(outputFormula));
        } catch (FormulaException e) {
            return false;
        }
    }

    private String trimFormula(String formula) {
        return formula.replaceAll("\\s+", "").trim();
    }
}
