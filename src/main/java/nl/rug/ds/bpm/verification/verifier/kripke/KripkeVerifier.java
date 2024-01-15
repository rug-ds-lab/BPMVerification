package nl.rug.ds.bpm.verification.verifier.kripke;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.converter.kripke.KripkeStructureConverterAction;
import nl.rug.ds.bpm.verification.event.PerformanceEvent;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.ConditionalStructure;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;
import nl.rug.ds.bpm.verification.model.kripke.factory.KripkeFactory;
import nl.rug.ds.bpm.verification.verifier.Verifier;
import nl.rug.ds.bpm.verification.verifier.generic.AbstractVerifier;

import java.util.List;

/**
 * Class implementing a Verifier that uses a Kripke structure.
 */
public class KripkeVerifier extends AbstractVerifier<KripkeFactory> implements Verifier {

	/**
	 * Creates a KripkeVerifier.
	 *
	 * @param net            The VerifiableNet that represents the model on which the given specification must be verified.
	 * @param specification  The specification must be verified on the given model (i.e., net).
	 * @param checkerFactory The factory that provides the model checker.
	 * @throws ConfigurationException when the configuration fails to load.
	 */
	public KripkeVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
		super(net, specification, checkerFactory);
		structureFactory = new KripkeFactory();
	}

	@Override
	public void verify() throws VerifierException {
		Logger.log("Verifying specification sets", LogEvent.INFO);

		for (SpecificationSet specificationSet : specification.getSpecificationSets())
			verifySet(specificationSet);
	}

	/**
	 * Starts the verification process for a subset of the specification.
	 *
	 * @param specificationSet the subset of the specification to verify.
	 * @throws VerifierException when the verification process fails.
	 */
	protected void verifySet(SpecificationSet specificationSet) throws VerifierException {
		Logger.log("Verifying set.", LogEvent.INFO);

		PerformanceEvent performanceEvent = new PerformanceEvent(this.net, specificationSet);

		AtomicPropositionMap<CompositeExpression> specificationPropositions = new AtomicPropositionMap<>("p");
		getGroupPropositions(specificationPropositions);
		getSpecificationSetPropositions(specificationPropositions, specificationSet);
		structureFactory.getAtomicPropositionMap().merge(specificationPropositions);

		Checker checker = checkerFactory.getChecker();
		KripkeStructure structure = structureFactory.createStructure();
		addConditions(structure, specificationSet.getConditions());

		try {
			double computationTime = compute(structure);

			performanceEvent.addMetric("StructureComputationMs", computationTime / 1000000);
			performanceEvent.addMetric("StructureStateCount", structure.getStateCount());
			performanceEvent.addMetric("StructureRelationCount", structure.getRelationCount());
			performanceEvent.addMetric("StructureAtomicPropositionCount", structure.getAtomicPropositionCount());

			finalize(structure, specificationPropositions);
			convert(checker, structure, specificationSet);
			check(checker);
		} catch (Exception e) {
			Logger.log("Failed to verify set.", LogEvent.ERROR);
			throw new VerifierException("Failed to verify set.");
		} finally {
			checkerFactory.release(checker);
		}

		performanceEventHandler.fireEvent(performanceEvent);
	}

	/**
	 * Adds the given List of Conditions to the given Structure.
	 *
	 * @param structure  the Structure to add the Conditions to.
	 * @param conditions the List of Conditions to add.
	 */
	protected void addConditions(ConditionalStructure structure, List<Condition> conditions) {
		structure.addConditions(conditions);

		Logger.log("Conditions: ", LogEvent.VERBOSE);
		for (Condition condition : conditions)
			Logger.log("\t" + condition.getCondition(), LogEvent.VERBOSE);
	}

	/**
	 * Computes the Structure of the Net and logs results.
	 *
	 * @param structure the Structure to populate.
	 * @return the time it took to compute the Structure in nanoseconds.
	 */
	protected double compute(KripkeStructure structure) {
		Logger.log("Calculating Kripke structure", LogEvent.INFO);
		double delta = compute(structureFactory.createConverter(net, net.getInitialMarking(), structure));
		Logger.log("Calculated Kripke structure with " + structure.stats() + " in " + formatComputationTime(delta), LogEvent.INFO);
		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + structure, LogEvent.DEBUG);

		return delta;
	}

	/**
	 * Computes the Structure.
	 *
	 * @param converter the initial conversion step to start the computation from.
	 * @return the time it took to compute the Structure in nanoseconds.
	 */
	protected double compute(KripkeStructureConverterAction converter) {
		long t0 = System.nanoTime();
		converter.computeInitial();
		long t1 = System.nanoTime();

		return t1 - t0;
	}

	/**
	 * Finalizes the given structure by adding a safety, 'ghost', state with given atomic propositions for model
	 * check safety. Prevents model checker from complaining about atomic propositions used in specifications
	 * that are not in the model (i.e., Structure).
	 *
	 * @param structure the Structure finalize.
	 * @param ap        the atomic propositions used.
	 * @throws ConverterException when the State con not be added.
	 */
	protected void finalize(KripkeStructure structure, AtomicPropositionMap<CompositeExpression> ap) throws ConverterException {
		KripkeState ghost = structureFactory.createState("ghost", ap.getAPKeys());
		ghost.addNext(ghost);
		ghost.addPrevious(ghost);

		structure.addState(ghost);
	}

	/**
	 * Converts the Structure into the internal representation used by the given Checker.
	 *
	 * @param checker        the Checker to use for the conversion.
	 * @param structure      the Structure to convert.
	 * @param specifications the (sub)set of Specifications to include.
	 * @throws CheckerException when the conversion fails.
	 */
	protected void convert(Checker checker, KripkeStructure structure, SpecificationSet specifications) throws CheckerException {
		Logger.log("Collecting specifications", LogEvent.INFO);
		for (Specification specification : specifications.getSpecifications())
			for (Formula formula : specification.getSpecificationType().getFormulas())
				checker.addFormula(formula, specification, structureFactory.getAtomicPropositionMap());

		Logger.log("Generating model check input", LogEvent.VERBOSE);
		checker.createModel(structure);

		if (Logger.getLogLevel() <= LogEvent.DEBUG)
			Logger.log("\n" + checker.getInputChecker(), LogEvent.DEBUG);
	}

	/**
	 * Model checks the converted structure using the given Checker.
	 *
	 * @param checker the Checker used to model check.
	 * @throws CheckerException when the model checking fails.
	 */
	protected void check(Checker checker) throws CheckerException {
		Logger.log("Calling Model Checker", LogEvent.INFO);
		List<VerificationEvent> events = checker.checkModel();

		for (VerificationEvent event : events) {
			if (event.getFormula() == null)
				Logger.log("Failed to map formula to original specification", LogEvent.ERROR);
			else {
				verificationEventHandler.fireEvent(event);
				Logger.log("Specification " + event.getFormula().getSpecification().getId() + " evaluated " + event.getVerificationResult() + " for " + event.getFormula().getInputFormula(), LogEvent.INFO);
			}
		}

		if (!checker.getOutputChecker().isEmpty())
			throw new CheckerException("Model modelcheck error\n" + checker.getOutputChecker());
	}
}
