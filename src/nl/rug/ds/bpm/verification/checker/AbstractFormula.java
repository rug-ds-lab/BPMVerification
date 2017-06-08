package nl.rug.ds.bpm.verification.checker;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;

/**
 * Created by p256867 on 8-6-2017.
 */
public abstract class AbstractFormula {
	protected String formulaString;
	protected Specification parent;
	protected Formula formula;

	public String getFormulaString() { return formulaString; }

	public Specification getSpecification() { return parent; }

	public Formula getFormula() { return formula; }

	public abstract boolean equals(String outputFormula);
}
