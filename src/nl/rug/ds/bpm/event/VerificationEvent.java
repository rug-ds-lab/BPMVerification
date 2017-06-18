package nl.rug.ds.bpm.event;

import java.util.Map;

import nl.rug.ds.bpm.specification.jaxb.Element;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Group;
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
	
	public String getUserFriendlyFeedback(Map<String, Group> groupMap) {
		String feedback;

		feedback = "Specification " + specification.getId() + " evaluated " + eval + " for " + specification.getType() + "(";
		for (InputElement e: specification.getInputElements()) {
			if (groupMap.containsKey(e.getElement())) {
				if (specification.getType().equals("AlwaysParallel")) {
					feedback += getGroupString(groupMap.get(e.getElement()), "&") + ", ";
				}
				else {
					feedback += getGroupString(groupMap.get(e.getElement()), "|") + ", ";
				}
			}
			else {
				feedback += e.getElement() + ", ";
			}
		}
		feedback = feedback.substring(0, feedback.length() - 2) + ")";
		
		return feedback;
	}

	private String getGroupString(Group group, String separator) {
		String grpstr = "(";
		
		for (Element e: group.getElements()) {
			grpstr += e.getId() + " " + separator + " ";
		}
		
		grpstr = grpstr.substring(0, grpstr.length() - 2 - separator.length()) + ")";
		
		return grpstr;
	}
	
	public String toString() {
		return "Specification " + specification.getId() + " evaluated " + eval + " for " + formulaString;
	}
}
