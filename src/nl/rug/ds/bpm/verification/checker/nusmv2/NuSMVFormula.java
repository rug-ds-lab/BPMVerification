package nl.rug.ds.bpm.verification.checker.nusmv2;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.verification.checker.AbstractFormula;

/**
 * Created by p256867 on 13-4-2017.
 */
public class NuSMVFormula extends AbstractFormula {

    public NuSMVFormula(String nusmvFormula, Formula formula, Specification parent) {
        this.formulaString = nusmvFormula;
        this.formula = formula;
        this.parent = parent;
    }

    @Override
    public String getFormulaString() {
        return formula.getLanguage() + " " + formulaString;
    }

    public boolean equals(String outputFormula) {
        return trimFormula(formulaString).equals(trimFormula(outputFormula));
    }

    private String trimFormula(String formula) {
        String f = formula.replace(this.formula.getLanguage(), "");
        return f.replaceAll("\\s+", "").trim();
    }
}
