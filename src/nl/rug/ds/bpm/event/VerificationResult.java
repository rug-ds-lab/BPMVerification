package nl.rug.ds.bpm.event;

import nl.rug.ds.bpm.verification.checker.CheckerFormula;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class VerificationResult {
	private boolean eval;
	private CheckerFormula formula;
	
	public VerificationResult(CheckerFormula formula, boolean eval) {
		this.formula = formula;
		this.eval = eval;
	}
	
	public String getId() {
		return formula.getSpecification().getId();
	}
	
	public String getType() {
		return formula.getSpecification().getType();
	}
	
	public String getFormulaString() {
		return formula.getoriginalFormula();
	}
	
	public boolean getVerificationResult() {
		return eval;
	}

	public CheckerFormula getFormula() {
		return formula;
	}

	public String toString() {
		return "Specification " + formula.getSpecification().getId() + " evaluated " + eval + " for " + formula.getoriginalFormula() + " (mapped to " + formula.getCheckerFormula() + ")";
	}
}
