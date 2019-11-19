package nl.rug.ds.bpm.verification;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.convert.net.KripkeConverter;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;
import nl.rug.ds.bpm.verification.modelcheck.Checker;
import nl.rug.ds.bpm.verification.optimize.proposition.PropositionOptimizer;
import nl.rug.ds.bpm.verification.optimize.stutter.StutterOptimizer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class SetVerifier {
	private Kripke kripke;
	private VerifiableNet net;
	private EventHandler eventHandler;
	private AtomicPropositionMap<CompositeExpression> apMap;
	private BPMSpecification specification;
	private SpecificationSet specificationSet;
	private List<Specification> specifications;
	private List<Condition> conditions;
	
	public SetVerifier(VerifiableNet net, BPMSpecification specification, SpecificationSet specificationSet, EventHandler eventHandler) {
		this.net = net;
		this.specification = specification;
		this.specificationSet = specificationSet;
		this.eventHandler = eventHandler;
		
		specifications = specificationSet.getSpecifications();
		conditions = specificationSet.getConditions();

		Logger.log("Loading specification set", LogEvent.INFO);
		
		Logger.log("Conditions: ", LogEvent.VERBOSE);
		for(Condition condition: conditions)
			Logger.log("\t" + condition.getCondition(), LogEvent.VERBOSE);
		
		apMap = getAPMap();
	}
	
	public void buildKripke(boolean reduce) throws ConverterException {
		Set<String> conds = new HashSet<>();
		for (Condition condition : conditions)
			conds.add(condition.getCondition());

		KripkeConverter converter = new KripkeConverter(net, apMap, conds);
		
		Logger.log("Calculating Kripke structure", LogEvent.INFO);
		long t0 = System.nanoTime();
		kripke = converter.convert();
//				System.out.println(kripke);
		long t1 = System.nanoTime();

		String delta = "";
		double tdns = (t1 - t0);
		double tdms = tdns / 1000000;
		double tds = tdms / 1000;

		NumberFormat nf = NumberFormat.getNumberInstance(Locale.UK);
		DecimalFormat df = (DecimalFormat) nf;
		df.applyPattern("#.###");

		if(tds < 1 && tdms < 1)
			delta = df.format(tdns) + " ns";
		else if (tds < 1)
			delta = df.format(tdms) + " ms";
		else
			delta = df.format(tds) + " s";

		Logger.log("Calculated Kripke structure with " + kripke.stats() + " in " + delta, LogEvent.INFO);
		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + kripke.toString(), LogEvent.DEBUG);
		
		Logger.log("Reducing Kripke structure", LogEvent.INFO);
		Logger.log("Removing unused atomic propositions", LogEvent.VERBOSE);
		Set<String> unusedAP = new HashSet<>(kripke.getAtomicPropositions());
		TreeSet<String> unknownAP = new TreeSet<>(new ComparableComparator<String>());
		
		unusedAP.removeAll(apMap.getAPKeys());
		
		unknownAP.addAll(apMap.getAPKeys());
		unknownAP.removeAll(kripke.getAtomicPropositions());

		for (CompositeExpression id: converter.getAtomicPropositionMap().getIDKeys())
			apMap.addID(id, converter.getAtomicPropositionMap().getAP(id));

		if (reduce) {
			PropositionOptimizer propositionOptimizer = new PropositionOptimizer(kripke, unusedAP);
			Logger.log("\n" + propositionOptimizer.toString(true), LogEvent.VERBOSE);
			
			Logger.log("Reducing state space", LogEvent.VERBOSE);
			t0 = System.nanoTime();
			StutterOptimizer stutterOptimizer = new StutterOptimizer(kripke);
			Logger.log("Partitioning states into stutter blocks", LogEvent.VERBOSE);
			stutterOptimizer.linearPreProcess();
			//stutterOptimizer.treeSearchPreProcess();
			stutterOptimizer.optimize();
			t1 = System.nanoTime();

			tdns = (t1 - t0);
			tdms = tdns / 1000000;
			tds = tdms / 1000;

			if(tds < 1 && tdms < 1)
				delta = df.format(tdns) + " ns";
			else if (tds < 1)
				delta = df.format(tdms) + " ms";
			else
				delta = df.format(tds) + " s";

			Logger.log("Reduced Kripke structure to " + kripke.stats() + " in " + delta, LogEvent.INFO);
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
				checker.addFormula(formula, specification, apMap);
		
		Logger.log("Generating model check input", LogEvent.VERBOSE);
		checker.createModel(kripke);
		
		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + checker.getInputChecker(), LogEvent.DEBUG);
		
		Logger.log("Calling Model Checker", LogEvent.INFO);
		List<VerificationEvent> events = checker.checkModel();

		for (VerificationEvent event: events) {
			if (event.getFormula() == null)
				Logger.log("Failed to map formula to original specification", LogEvent.ERROR);
			else {
				eventHandler.fireEvent(event);
				Logger.log("Specification " + event.getFormula().getSpecification().getId() + " evaluated " + event.getVerificationResult() + " for " + event.getFormula().getOriginalFormula(), LogEvent.INFO);
			}
		}

		if(!checker.getOutputChecker().isEmpty())
			throw new CheckerException("Model modelcheck error\n" + checker.getOutputChecker());
	}
	
	private AtomicPropositionMap<CompositeExpression> getAPMap() {
		AtomicPropositionMap<CompositeExpression> atomicPropositionMap = new AtomicPropositionMap<>();
		
		for (Specification s: specificationSet.getSpecifications())
			for (InputElement inputElement: s.getInputElements())
				atomicPropositionMap.addID(ExpressionBuilder.parseExpression(inputElement.getElement()));
		
		for (Group group: specification.getGroups()) {
			CompositeExpression groupExpression = new CompositeExpression(LogicalType.OR);
			for (Element element : group.getElements())
				groupExpression.addArgument(ExpressionBuilder.parseExpression(element.getId()));
			String ap = atomicPropositionMap.addID(groupExpression);
			atomicPropositionMap.addID(ExpressionBuilder.parseExpression(group.getId()), ap);
		}
		
		return atomicPropositionMap;
	}

	public Kripke getKripke() {
		return kripke;
	}
}
