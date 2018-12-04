package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.convert.KripkeConverter;
import nl.rug.ds.bpm.verification.map.GroupMap;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.optimize.proposition.PropositionOptimizer;
import nl.rug.ds.bpm.verification.optimize.stutter.StutterOptimizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SetVerifier {
	private Kripke kripke;
	private VerifiableNet net;
	private IDMap specIdMap;
	private GroupMap groupMap;
	private BPMSpecification specification;
	private SpecificationSet specificationSet;
	private List<Specification> specifications;
	private List<Condition> conditions;
	
	public SetVerifier(VerifiableNet net, BPMSpecification specification, SpecificationSet specificationSet) {
		this.net = net;
		this.specification = specification;
		this.specificationSet = specificationSet;
		
		specifications = specificationSet.getSpecifications();
		conditions = specificationSet.getConditions();

		Logger.log("Loading specification set", LogEvent.INFO);
		
		Logger.log("Conditions: ", LogEvent.VERBOSE);
		for(Condition condition: conditions)
			Logger.log("\t" + condition.getCondition(), LogEvent.VERBOSE);
		
		specIdMap = getIdMap();
		groupMap = getGroupMap(specIdMap);
	}
	
	public void buildKripke(boolean reduce) throws ConverterException {
		Set<String> conds = new HashSet<>();
		for (Condition condition : conditions)
			conds.add(condition.getCondition());

		KripkeConverter converter = new KripkeConverter(net, specIdMap, conds);
		
		Logger.log("Calculating Kripke structure", LogEvent.INFO);
		long t0 = System.nanoTime();
		kripke = converter.convert();
//				System.out.println(kripke);
		long t1 = System.nanoTime();

		double tdns = (t1 - t0);
		double tdms = tdns / 1000000;
		double tds = tdms / 1000;
		String delta = "";

		if(tds < 1 && tdms < 1)
			delta = tdns + " ns";
		else if (tds < 1)
			delta = tdms + " ms";
		else
			delta = tds + " s";

		Logger.log("Calculated Kripke structure with " + kripke.stats() + " in " + delta, LogEvent.INFO);
		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + kripke.toString(), LogEvent.DEBUG);
		
		Logger.log("Reducing Kripke structure", LogEvent.INFO);
		Logger.log("Removing unused atomic propositions", LogEvent.VERBOSE);
		Set<String> unusedAP = new HashSet<>(kripke.getAtomicPropositions());
		TreeSet<String> unknownAP = new TreeSet<>(new StringComparator());
		
		unusedAP.removeAll(specIdMap.getAPKeys());
		
		unknownAP.addAll(specIdMap.getAPKeys());
		unknownAP.removeAll(kripke.getAtomicPropositions());

		for (String id: converter.getIdMap().getIDKeys())
			specIdMap.addID(id, converter.getIdMap().getAP(id));

		if (reduce) {
			PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke, unusedAP);
			Logger.log("\n" + propositionOptimizer.toString(true), LogEvent.VERBOSE);
			
			Logger.log("Reducing state space", LogEvent.VERBOSE);
			t0 = System.currentTimeMillis();
			StutterOptimizer stutterOptimizer = new StutterOptimizer(kripke);
			Logger.log("Partitioning states into stutter blocks", LogEvent.VERBOSE);
			//stutter.linearPreProcess();
			stutterOptimizer.treeSearchPreProcess();
			stutterOptimizer.optimize();
			t1 = System.currentTimeMillis();
			
			Logger.log("Reduced Kripke structure to " + kripke.stats() + " in " + (t1 - t0) + " ms", LogEvent.INFO);
			if (Logger.getLogLevel() <= LogEvent.DEBUG) {
				Logger.log("\n" + stutterOptimizer.toString(), LogEvent.DEBUG);
				Logger.log("\n" + kripke.toString(), LogEvent.DEBUG);
			}
		}

		//Add ghost state with unknown AP for modelcheck safety
		State ghost = new State("ghost", unknownAP);
		ghost.addNext(ghost);
		ghost.addPrevious(ghost);
		
		kripke.addState(ghost);
	}
	
	public void verify(Checker checker) throws CheckerException {
		Logger.log("Collecting specifications", LogEvent.INFO);
		for (Specification specification: specifications)
			for (Formula formula: specification.getSpecificationType().getFormulas())
				checker.addFormula(formula, specification, specIdMap, groupMap);
		
		Logger.log("Generating model modelcheck input", LogEvent.VERBOSE);
		checker.createModel(kripke);
		
		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + checker.getInputChecker(), LogEvent.DEBUG);
		
		Logger.log("Calling Model Checker", LogEvent.INFO);
		checker.checkModel();
		if(!checker.getOutputChecker().isEmpty())
			throw new CheckerException("Model modelcheck error\n" + checker.getOutputChecker());
	}
	
	private IDMap getIdMap() {
		IDMap idMap = new IDMap();
		
		for (Specification s: specificationSet.getSpecifications())
			for (InputElement inputElement: s.getInputElements()) {
				idMap.addID(inputElement.getElement());
				Logger.log("Mapping " + inputElement.getElement() + " to " + idMap.getAP(inputElement.getElement()), LogEvent.VERBOSE);
				//inputElement.setElement(idMap.getAP(inputElement.getElement()));
			}
		
		for (Group group: specification.getGroups()) {
			//group.setId(idMap.getAP(group.getId()));
			for (Element element : group.getElements()) {
				idMap.addID(element.getId());
				Logger.log("Mapping " + element.getId() + " to " + idMap.getAP(element.getId()), LogEvent.VERBOSE);
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
			Logger.log("New group " + group.getId() + " as " + idMap.getAP(group.getId()), LogEvent.VERBOSE);
			for (Element element: group.getElements()) {
				groupMap.addToGroup(idMap.getAP(group.getId()), idMap.getAP(element.getId()));
				Logger.log("\t " + element.getId(), LogEvent.VERBOSE);
			}
		}
		return groupMap;
	}
	
	public Kripke getKripke() {
		return kripke;
	}
}
