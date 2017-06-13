package nl.rug.ds.bpm.event;

import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.InputElement;
import nl.rug.ds.bpm.specification.jaxb.Specification;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class VerificationEvent {
	private boolean eval;
	private String formulaString;
	private Specification specification;
	private Formula formula;
	
	public VerificationEvent(Specification specification, Formula formula, String formulaString, boolean eval) {
		this.specification = specification;
		this.formula = formula;
		this.formulaString = formulaString;
		this.eval = eval;
	}
	
	public String getId() {
		return specification.getId();
	}
	
	public String getType() {
		return specification.getType();
	}
	
	public String getFormulaString() { return formulaString; }
	
	public boolean getVerificationResult() {
		return eval;
	}
	
	public Specification getSpecification() {
		return specification;
	}

	public Formula getFormula() { return formula; }

	public String getUserFriendlyFeedback() {
		String feedback;

		feedback = "Specification " + specification.getId() + " evaluated " + eval + " for " + specification.getType() + "(";
		for (InputElement e: specification.getInputElements()) {
			feedback += e.getElement() + ", ";
		}
		feedback = feedback.substring(0, feedback.length() - 2) + ")";
		
		return feedback;
	}
	
	public String toString() {
		return "Specification " + specification.getId() + " evaluated " + eval + " for " + formulaString;
	}
}
