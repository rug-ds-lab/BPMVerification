package nl.rug.ds.bpm.verification.formula;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;

/**
 * Created by p256867 on 13-4-2017.
 */
public class NuSMVFormula {
    private String nusmvFormula;
    private Specification parent;
    private Formula formula;

    public NuSMVFormula(String nusmvFormula, Formula formula, Specification parent) {
        this.nusmvFormula = nusmvFormula;
        this.formula = formula;
        this.parent = parent;
    }

    public String getNusmvFormula() { return nusmvFormula; }

    public Specification getSpecification() { return parent; }

    public Formula getFormula() { return formula; }

    public boolean equals(String outputFormula) {
        return trimFormula(nusmvFormula).equals(trimFormula(outputFormula));
    }

    private String trimFormula(String formula) {
        String f = formula.replace(this.formula.getLanguage(), "");
        return f.replaceAll("\\s+", "").trim();
    }
}
