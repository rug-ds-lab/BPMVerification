package nl.rug.ds.bpm.pnml;

import nl.rug.ds.bpm.jaxb.specification.Condition;
import nl.rug.ds.bpm.jaxb.specification.Specification;
import nl.rug.ds.bpm.jaxb.specification.SpecificationSet;
import nl.rug.ds.bpm.pnml.util.GroupMap;
import nl.rug.ds.bpm.pnml.util.IDMap;
import nl.rug.ds.bpm.verification.formulas.NuSMVFormula;
import nl.rug.ds.bpm.verification.modelCheckers.NuSMVChecker;
import nl.rug.ds.bpm.verification.modelConverters.Pnml2KripkeConverter;
import nl.rug.ds.bpm.verification.modelOptimizers.propositionOptimizer.PropositionOptimizer;
import nl.rug.ds.bpm.verification.modelOptimizers.stutterOptimizer.StutterOptimizer;
import nl.rug.ds.bpm.verification.models.kripke.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SpecificationSetVerifier {
	private Kripke kripke;
	private EventHandler eventHandler;
	private IDMap idMap;
	private GroupMap groupMap;
	private List<NuSMVFormula> formulas;
	private List<Specification> specifications;
	private List<Condition> conditions;
	
	public SpecificationSetVerifier(EventHandler eventHandler, SpecificationSet specificationSet, IDMap idMap, GroupMap groupMap) {
		this.eventHandler = eventHandler;
		this.idMap = idMap;
		this.groupMap = groupMap;
		specifications = specificationSet.getSpecifications();
		conditions = specificationSet.getConditions();
		formulas = new ArrayList<>();
	}

	public void buildKripke(File pnml) {
		Pnml2KripkeConverter converter = new Pnml2KripkeConverter();
		eventHandler.logInfo("Creating Kripke structure");
		kripke = converter.convert();
		eventHandler.logVerbose("\n" + kripke.toString(true));
		eventHandler.logInfo("\n" + kripke.toString(false));

		eventHandler.logInfo("Optimizing Kripke structure");
		eventHandler.logInfo("Removing unused atomic propositions");
		Set<String> unusedAP = new HashSet<>(kripke.getAtomicPropositions());
		unusedAP.removeAll(idMap.getAPKeys());
		PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke, unusedAP);
		eventHandler.logVerbose("\n" + propositionOptimizer.toString(true));

		eventHandler.logInfo("Shrinking state space");
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
				for (String key: idMap.getAPKeys())
					formula = formula.replaceAll(Matcher.quoteReplacement(key), idMap.getID(key));
				if(eval)
					eventHandler.logInfo("Failed to map " + formula + " to original specification while it evaluated true");
				else
					eventHandler.logError("Failed to map " + formula + " to original specification while it evaluated FALSE");
			}
			else {
				eventHandler.fireEvent(nuSMVFormula.getSpecification(), eval);
				if(eval)
					eventHandler.logInfo("Specification " + nuSMVFormula.getSpecification().getId() + " evaluated true for " + nuSMVFormula.getFormula());
				else
					eventHandler.logError("Specification " + nuSMVFormula.getSpecification().getId() + " evaluated FALSE for " + nuSMVFormula.getFormula());
			}
		}
	}

	private void mapFormulas() {
		for (Specification specification: specifications) {
			for(String formula: specification.getFormulas()) {
				String mappedFormula = formula;
				for (String key: idMap.getIDKeys())
					mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(key), idMap.getAP(key));
				for (String key: groupMap.keySet())
					mappedFormula = mappedFormula.replaceAll(Matcher.quoteReplacement(key), groupMap.toString(key));

				formulas.add(new NuSMVFormula(mappedFormula, specification));
			}
		}
	}
}
