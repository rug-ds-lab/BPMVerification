package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.verification.stepper.Stepper;
import nl.rug.ds.bpm.verification.util.EventHandler;
import nl.rug.ds.bpm.verification.util.GroupMap;
import nl.rug.ds.bpm.verification.util.IDMap;
import nl.rug.ds.bpm.verification.formula.NuSMVFormula;
import nl.rug.ds.bpm.verification.checker.NuSMVChecker;
import nl.rug.ds.bpm.verification.converter.KripkeConverter;
import nl.rug.ds.bpm.verification.optimizer.propositionOptimizer.PropositionOptimizer;
import nl.rug.ds.bpm.verification.optimizer.stutterOptimizer.StutterOptimizer;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SetVerifier {
	private Kripke kripke;
	private EventHandler eventHandler;
	private Stepper stepper;
	private IDMap specIdMap;
	private GroupMap groupMap;
	private List<NuSMVFormula> formulas;
	private BPMSpecification specification;
	private SpecificationSet specificationSet;
	private List<Specification> specifications;
	private List<Condition> conditions;
	
	public SetVerifier(EventHandler eventHandler, Stepper stepper, BPMSpecification specification, SpecificationSet specificationSet) {
		this.stepper = stepper;
		this.eventHandler = eventHandler;
		this.specification = specification;
		this.specificationSet = specificationSet;
		
		specifications = specificationSet.getSpecifications();
		conditions = specificationSet.getConditions();
		formulas = new ArrayList<>();
		
		eventHandler.logInfo("Creating new conditional set and model");
		
		eventHandler.logVerbose("Conditions: ");
		for(Condition condition: conditions)
			eventHandler.logVerbose("\t" + condition.getCondition());
		
		eventHandler.logVerbose("Specifications:");
		for (Specification s: specifications)
			for(String formula: s.getFormulas())
				eventHandler.logVerbose("\t" + formula);
		
		specIdMap = getIdMap();
		groupMap = getGroupMap(specIdMap);
	}

	public void buildKripke() {
		KripkeConverter converter = new KripkeConverter(eventHandler, stepper, conditions, specIdMap);
		eventHandler.logInfo("Creating Kripke structure");
		kripke = converter.convert();
		eventHandler.logVerbose("\n" + kripke.toString(true));
		eventHandler.logInfo("\n" + kripke.toString(false));

		eventHandler.logInfo("Optimizing Kripke structure");
		eventHandler.logInfo("Removing unused atomic propositions");
		Set<String> unusedAP = new HashSet<>(kripke.getAtomicPropositions());
		
		unusedAP.removeAll(specIdMap.getAPKeys());
		PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke, unusedAP);
		eventHandler.logVerbose("\n" + propositionOptimizer.toString(true));

		eventHandler.logInfo("Reducing state space");
		StutterOptimizer stutterOptimizer = new StutterOptimizer(kripke);
		stutterOptimizer.optimize();
		eventHandler.logVerbose("\n" + stutterOptimizer.toString(true));
	}

	public void verify(File nusmv2) {
		eventHandler.logInfo("Collecting specifications");
		mapFormulas();

		eventHandler.logInfo("Calling Model Checker");
		NuSMVChecker nuSMVChecker = new NuSMVChecker(eventHandler, nusmv2, kripke, formulas);

		nuSMVChecker.createInputData();
		eventHandler.logVerbose("Model checker input\n" + nuSMVChecker.getInputChecker());

		List<String> resultLines = nuSMVChecker.callModelChecker();
		eventHandler.logVerbose("Model checker output\n" + nuSMVChecker.getOutputChecker());

		eventHandler.logInfo("Collecting results");
		for (String result: resultLines) {
			String formula = result;
			boolean eval = false;
			if (formula.contains("is false")) {
				formula = formula.replace("is false", "");
			} else {
				formula = formula.replace("is true", "");
				eval = true;
			}

			NuSMVFormula nuSMVFormula = null;
			boolean found = false;
			Iterator<NuSMVFormula> nuSMVFormulaIterator = formulas.iterator();
			while (nuSMVFormulaIterator.hasNext() && !found) {
				NuSMVFormula f = nuSMVFormulaIterator.next();
				if(f.equals(formula)) {
					found = true;
					nuSMVFormula = f;
				}
			}

			if(!found) {
				for (String key: specIdMap.getAPKeys())
					formula = formula.replaceAll(Matcher.quoteReplacement(key), specIdMap.getID(key));
				if(eval)
					eventHandler.logWarning("Failed to map " + formula + " to original specification while it evaluated true");
				else
					eventHandler.logError("Failed to map " + formula + " to original specification while it evaluated FALSE");
			}
			else {
				eventHandler.fireEvent(nuSMVFormula.getSpecification(), eval);
				if(eval)
					eventHandler.logInfo("Specification " + nuSMVFormula.getSpecification().getId() + " evaluated true for " + nuSMVFormula.getFormula());
				else
					eventHandler.logError("Specification " + nuSMVFormula.getSpecification().getId() + " evaluated FALSE for " + nuSMVFormula.getFormula());
				formulas.remove(nuSMVFormula);
			}
		}
	}

	private void mapFormulas() {
		for (Specification specification: specifications) {
			for(String formula: specification.getFormulas()) {
				String mappedFormula = formula;
				for (String key: specIdMap.getIDKeys())
					mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(key), specIdMap.getAP(key));
				for (String key: groupMap.keySet())
					mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(key), groupMap.toString(key));

				formulas.add(new NuSMVFormula(mappedFormula, specification));
			}
		}
	}
	
	private IDMap getIdMap() {
		IDMap idMap = new IDMap();
		
		for (Specification s: specificationSet.getSpecifications())
			for (InputElement inputElement: s.getInputElements()) {
				idMap.addID(inputElement.getElement());
				eventHandler.logVerbose("Mapping " + inputElement.getElement() + " to " + idMap.getAP(inputElement.getElement()));
				//inputElement.setElement(idMap.getAP(inputElement.getElement()));
			}
		
		for (Group group: specification.getGroups()) {
			//group.setId(idMap.getAP(group.getId()));
			for (Element element : group.getElements()) {
				idMap.addID(element.getId());
				eventHandler.logVerbose("Mapping " + element.getId() + " to " + idMap.getAP(element.getId()));
				//element.setId(idMap.getAP(element.getId()));
			}
		}
		
		return idMap;
	}
	
	public GroupMap getGroupMap(IDMap idMap) {
		GroupMap groupMap = new GroupMap();
		
		for (Group group: specification.getGroups()) {
			groupMap.addGroup(idMap.getAP(group.getId()));
			eventHandler.logVerbose("New group " + group.getId());
			for (Element element: group.getElements()) {
				groupMap.addToGroup(idMap.getAP(group.getId()), idMap.getAP(element.getId()));
				eventHandler.logVerbose("\t " + element.getId());
			}
		}
		return groupMap;
	}
}
