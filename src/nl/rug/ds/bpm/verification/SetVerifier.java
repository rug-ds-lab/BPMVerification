package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.event.VerificationLog;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.comparator.StringComparator;
import nl.rug.ds.bpm.verification.converter.KripkeConverter;
import nl.rug.ds.bpm.verification.map.GroupMap;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;
import nl.rug.ds.bpm.verification.optimizer.propositionOptimizer.PropositionOptimizer;
import nl.rug.ds.bpm.verification.optimizer.stutterOptimizer.StutterOptimizer;
import nl.rug.ds.bpm.verification.stepper.Stepper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SetVerifier {
	private Kripke kripke;
	private EventHandler eventHandler;
	private Stepper stepper;
	private IDMap specIdMap;
	private GroupMap groupMap;
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
		
//		Set<String> conds = new HashSet<>();
//		for (Condition condition : conditions)
//			conds.add(condition.getCondition());
//		stepper.setConditions(conds);
		
		eventHandler.logInfo("Loading specification set");
		
		eventHandler.logVerbose("Conditions: ");
		for(Condition condition: conditions)
			eventHandler.logVerbose("\t" + condition.getCondition());
		
		specIdMap = getIdMap();
		groupMap = getGroupMap(specIdMap);
	}

	public void buildKripke(boolean reduce) {
		KripkeConverter converter = new KripkeConverter(eventHandler, stepper, specIdMap);
		
		eventHandler.logInfo("Calculating Kripke structure");
		long t0 = System.currentTimeMillis();
		kripke = converter.convert();
//		System.out.println(kripke);
		long t1 = System.currentTimeMillis();
		
		eventHandler.logInfo("Calculated Kripke structure with " +kripke.stats() + " in " + (t1 - t0) + " ms");
		if(EventHandler.getLogLevel() <= VerificationLog.DEBUG)
			eventHandler.logDebug("\n" + kripke.toString());

		eventHandler.logInfo("Reducing Kripke structure");
		eventHandler.logVerbose("Removing unused atomic propositions");
		Set<String> unusedAP = new HashSet<>(kripke.getAtomicPropositions());
		TreeSet<String> unknownAP = new TreeSet<>(new StringComparator());
		
		unusedAP.removeAll(specIdMap.getAPKeys());
		
		unknownAP.addAll(specIdMap.getAPKeys());
		unknownAP.removeAll(kripke.getAtomicPropositions());
		
		if(reduce) {
			PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke, unusedAP);
			eventHandler.logVerbose("\n" + propositionOptimizer.toString(true));
			
			eventHandler.logVerbose("Reducing state space");
			t0 = System.currentTimeMillis();
			StutterOptimizer stutterOptimizer = new StutterOptimizer(eventHandler, kripke);
			eventHandler.logVerbose("Partitioning states into stutter blocks");
			//stutterOptimizer.linearPreProcess();
			stutterOptimizer.treeSearchPreProcess();
			stutterOptimizer.optimize();
			t1 = System.currentTimeMillis();
			
			eventHandler.logInfo("Reduced Kripke structure to " + kripke.stats() + " in " + (t1 - t0) + " ms");
			if (EventHandler.getLogLevel() <= VerificationLog.DEBUG) {
				eventHandler.logDebug("\n" + stutterOptimizer.toString());
				eventHandler.logDebug("\n" + kripke.toString());
			}
		}
		
		//Add ghost state with unknown AP for checker safety
		State ghost = new State("ghost", unknownAP);
		ghost.addNext(ghost);
		ghost.addPrevious(ghost);

		kripke.addState(ghost);
	}

	public void verify(Checker checker) {
		eventHandler.logInfo("Collecting specifications");
		for (Specification specification: specifications)
			for (Formula formula: specification.getSpecificationType().getFormulas())
				checker.addFormula(formula, specification, specIdMap, groupMap);
		
		eventHandler.logVerbose("Generating model checker input");
		checker.createModel(kripke);
		
		if(EventHandler.getLogLevel() <= VerificationLog.DEBUG)
			eventHandler.logDebug("\n" + checker.getInputChecker());
		
		eventHandler.logInfo("Calling Model Checker");
		checker.checkModel();
		if(!checker.getOutputChecker().isEmpty())
			eventHandler.logCritical("Model checker error\n" + checker.getOutputChecker());
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
			idMap.addID(group.getId());
			groupMap.addGroup(idMap.getAP(group.getId()));
			eventHandler.logVerbose("New group " + group.getId() + " as " + idMap.getAP(group.getId()));
			for (Element element: group.getElements()) {
				groupMap.addToGroup(idMap.getAP(group.getId()), idMap.getAP(element.getId()));
				eventHandler.logVerbose("\t " + element.getId());
			}
		}
		return groupMap;
	}
}
