package nl.rug.ds.bpm.verification.event;

import nl.rug.ds.bpm.specification.jaxb.Input;
import nl.rug.ds.bpm.specification.jaxb.InputElement;
import nl.rug.ds.bpm.specification.jaxb.Message;
import nl.rug.ds.bpm.verification.checker.CheckerFormula;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Created by Heerko Groefsema on 10-Apr-17.
 */
public class VerificationEvent {
	private boolean eval;
	private List<List<String>> counterExample = null;
	private CheckerFormula formula;
	
	public VerificationEvent(CheckerFormula formula, boolean eval) {
		this.formula = formula;
		this.eval = eval;
	}

	public VerificationEvent(CheckerFormula formula, boolean eval, List<List<String>> counterExample) {
		this.formula = formula;
		this.eval = eval;
		this.counterExample = counterExample;
	}
	
	public String getId() {
		return formula.getSpecification().getId();
	}
	
	public String getType() {
		return formula.getSpecification().getType();
	}

	public boolean getVerificationResult() {
		return eval;
	}

	public CheckerFormula getFormula() {
		return formula;
	}

	public List<List<String>> getCounterExample() { return counterExample; }

	public void setCounterExample(List<List<String>> counterExample) {
		this.counterExample = counterExample;
	}

	public String getMessage() {
		Message message = formula.getSpecification().getSpecificationType().getMessage();
		return (message == null ? "unknown reasons" : (eval ? message.getHold() : message.getFail()) + ".");
	}

	public String getCounterString() {
		StringBuilder ce = new StringBuilder();

		Iterator<List<String>> states = counterExample.iterator();
		while (states.hasNext()) {
			ce.append("{");
			Iterator<String> aps = states.next().iterator();
			while (aps.hasNext()) {
				ce.append(aps.next());
				if (aps.hasNext())
					ce.append(",");
			}
			if (states.hasNext())
				ce.append("} -> ");
			else
				ce.append("}");
		}

		return ce.toString();
	}

	public String toString() {
		return "Specification " + formula.getOriginalFormula() + " with id " + formula.getSpecification().getId() + (eval ? " holds because " + getMessage() : " failed because " + getMessage() + " given the following counter example: " + getCounterString());
	}

	private String mapInputs(String s) {
		String r = s;
		
		for (Input input: formula.getSpecification().getSpecificationType().getInputs()) {
			List<InputElement> elements = formula.getSpecification().getInputElements().stream().filter(element -> element.getTarget().equals(input.getValue())).collect(Collectors.toList());
			
			String APBuilder = "";
			if (elements.size() == 1) {
				APBuilder = elements.get(0).getElement();
			} else if (elements.size() > 1) {
				Iterator<InputElement> inputElementIterator = elements.iterator();
				APBuilder = inputElementIterator.next().getElement();
				while (inputElementIterator.hasNext()) {
					APBuilder = APBuilder + (input.getType().equalsIgnoreCase("and") ? " and " : " or ") + inputElementIterator.next().getElement();
				}
			}
			r = r.replaceAll(Matcher.quoteReplacement(input.getValue()), APBuilder);
		}
		return r;
	}
	
}
