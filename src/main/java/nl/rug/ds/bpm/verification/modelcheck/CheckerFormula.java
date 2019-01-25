package nl.rug.ds.bpm.verification.modelcheck;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Input;
import nl.rug.ds.bpm.specification.jaxb.InputElement;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.util.exception.FormulaException;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public abstract class CheckerFormula {
	protected AtomicPropositionMap<CompositeExpression> atomicPropositionMap;
	protected Formula formula;
	protected Specification specification;
	
	public CheckerFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) {
		this.formula = formula;
		this.specification = specification;
		this.atomicPropositionMap = atomicPropositionMap;
	}
	
	public Formula getFormula() {
		return formula;
	}
	
	public Specification getSpecification() {
		return specification;
	}
	
	public boolean equals(String outputFormula) {
		try {
			return getCheckerFormula().equals(outputFormula);
		} catch (FormulaException e) {
			return false;
		}
	}
	
	public String getCheckerFormula() throws FormulaException {
		String mappedFormula = formula.getFormula();
		
		for (Input input: specification.getSpecificationType().getInputs()) {
			List<InputElement> elements = specification.getInputElements().stream().filter(element -> element.getTarget().equals(input.getValue())).collect(Collectors.toList());

			String APBuilder = "";
			if(elements.size() == 0) {
				throw new FormulaException("Input " + input.getValue() + " has no matching elements in formula " + getOriginalFormula());
			}
			else if(elements.size() == 1) {
				APBuilder = atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(elements.get(0).getElement()));
			}
			else {
				Iterator<InputElement> inputElementIterator = elements.iterator();
				APBuilder = atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(inputElementIterator.next().getElement()));
				while (inputElementIterator.hasNext())
					APBuilder = "(" + APBuilder + (input.getType().equalsIgnoreCase("and") ? " & " : " | ") + atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(inputElementIterator.next().getElement())) + ")";
			}
			mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(input.getValue()), APBuilder);
		}

		return mappedFormula;
	}
	
	public String getOriginalFormula() {
		String mappedFormula = formula.getFormula();
		
		for (Input input: specification.getSpecificationType().getInputs()) {
			List<InputElement> elements = specification.getInputElements().stream().filter(element -> element.getTarget().equals(input.getValue())).collect(Collectors.toList());
			
			String APBuilder = "";
			if (elements.size() == 0) {
				APBuilder = "true";
			} else if (elements.size() == 1) {
				APBuilder = elements.get(0).getElement();
			} else {
				Iterator<InputElement> inputElementIterator = elements.iterator();
				APBuilder = inputElementIterator.next().getElement();
				while (inputElementIterator.hasNext()) {
					APBuilder = "(" + APBuilder + (input.getType().equalsIgnoreCase("and") ? " & " : " | ") + inputElementIterator.next().getElement() + ")";
				}
			}
			mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(input.getValue()), APBuilder);
		}

		return mappedFormula;
	}

	public AtomicPropositionMap<CompositeExpression> getAtomicPropositionMap() {
		return atomicPropositionMap;
	}
}
