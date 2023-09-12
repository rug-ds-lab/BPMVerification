package nl.rug.ds.bpm.verification.checker;

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
import java.util.regex.Pattern;

/**
 * Class that maps formulas from input to model checker formats and vice versa.
 * <p>
 * Created by Heerko Groefsema on 09-Jun-17.
 */
public abstract class CheckerFormula {
	protected String inputFormula;
	protected String outputFormula;
	protected String checkerFormula;

	protected Pattern formulaRegEx;
	protected Formula formula;
	protected Specification specification;
	protected AtomicPropositionMap<CompositeExpression> atomicPropositionMap;

	/**
	 * Creates a new CheckerFormula.
	 *
	 * @param formula the input formula in its original XML Formula class format.
	 * @param specification the specification, in its original XML Specification class format.
	 * @param atomicPropositionMap the AtomicPropositionMap that is to be used to map between input propositions and output propositions used during model checking.
	 */
	public CheckerFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) throws FormulaException {
		this.formula = formula;
		this.specification = specification;
		this.atomicPropositionMap = atomicPropositionMap;

		this.inputFormula = parseInputFormula(formula, specification);
		this.outputFormula = parseOutputFormula(formula, specification, atomicPropositionMap);
		this.checkerFormula = outputFormula;

		this.formulaRegEx = Pattern.compile(Pattern.quote(outputFormula));
	}

	/**
	 * Returns the String representation of the input Formula.
	 *
	 * @return the String representation of the input Formula.
	 */
	public String getInputFormula() {
		return inputFormula;
	}

	/**
	 * Returns the String representation of the Formula with mapped output propositions used during model checking.
	 *
	 * @return the String representation of the Formula with mapped output propositions used during model checking.
	 */
	public String getOutputFormula() {
		return outputFormula;
	}

	/**
	 * Returns the String representation of the Formula with mapped output propositions and format used by the model checker.
	 *
	 * @return the String representation of the Formula with mapped output propositions and format used by the model checker.
	 */
	public String getCheckerFormula() {
		return checkerFormula;
	}


	/**
	 * Returns the input formula in its original XML Formula class format.
	 *
	 * @return the input formula in its original XML Formula class format.
	 */
	public Formula getFormula() {
		return formula;
	}

	/**
	 * Returns the specification, in its original XML Specification class format.
	 *
	 * @return the specification, in its original XML Specification class format.
	 */
	public Specification getSpecification() {
		return specification;
	}

	/**
	 * Returns the AtomicPropositionMap that is used to map between input propositions and output propositions used during model checking.
	 *
	 * @return the AtomicPropositionMap that is used to map between input propositions and output propositions used during model checking.
	 */
	public AtomicPropositionMap<CompositeExpression> getAtomicPropositionMap() {
		return atomicPropositionMap;
	}

	/**
	 * Returns true if the given formula matches the outputFormula.
	 *
	 * @param formula the given formula.
	 * @return true if and only if the given String matches the outputFormula.
	 */
	public boolean equals(String formula) {
		return formulaRegEx.matcher(formula).matches();
	}

	/**
	 * Parses the input Formula, in its XML class format, and returns the outputFormula by mapping the input propositions to the output propositions used during model checking.
	 *
	 * @param formula the input Formula in its XML class format.
	 * @param specification the Specification that the given Formula is part of.
	 * @param atomicPropositionMap the AtomicPropositionMap that is to be used to map between input propositions and output propositions used during model checking.
	 *
	 * @return a String representing the outputFormula.
	 * @throws FormulaException when parsing or mapping fails.
	 */
	private String parseOutputFormula(Formula formula, Specification specification, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) throws FormulaException {
		String mappedFormula = formula.getFormula();

		for (Input input : specification.getSpecificationType().getInputs()) {
			List<InputElement> elements = specification.getInputElements().stream().filter(element -> element.getTarget().equals(input.getValue())).toList();

			String APBuilder = "";
			if (elements.size() == 0) {
				throw new FormulaException("Input " + input.getValue() + " has no matching elements in formula " + inputFormula);
			} else if (elements.size() == 1) {
				APBuilder = atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(elements.get(0).getElement()));
			} else {
				Iterator<InputElement> inputElementIterator = elements.iterator();
				APBuilder = atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(inputElementIterator.next().getElement()));
				while (inputElementIterator.hasNext())
					APBuilder = "(" + APBuilder + (input.getType().equalsIgnoreCase("and") ? " & " : " | ") + atomicPropositionMap.getAP(ExpressionBuilder.parseExpression(inputElementIterator.next().getElement())) + ")";
			}

			if (APBuilder == null)
				throw new FormulaException("Formula " + input.getValue() + " could not be mapped " + mappedFormula + " -> " + inputFormula);

			mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(input.getValue()), APBuilder);
		}

		return mappedFormula;
	}


	/**
	 * Parses the input Formula, in its XML class format, and returns the inputFormula.
	 *
	 * @param formula       the input Formula in its XML class format.
	 * @param specification the Specification that the given Formula is part of.
	 * @return a String representing the inputFormula.
	 */
	private String parseInputFormula(Formula formula, Specification specification) {
		String mappedFormula = formula.getFormula();

		for (Input input : specification.getSpecificationType().getInputs()) {
			List<InputElement> elements = specification.getInputElements().stream().filter(element -> element.getTarget().equals(input.getValue())).toList();

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
}
