package nl.rug.ds.bpm.verification.verifier.stutter;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.event.PerformanceEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.generic.optimizer.proposition.PropositionOptimizer;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;
import nl.rug.ds.bpm.verification.model.kripke.postprocess.stutter.StutterOptimizer;
import nl.rug.ds.bpm.verification.verifier.Verifier;
import nl.rug.ds.bpm.verification.verifier.kripke.KripkeVerifier;

import java.util.TreeSet;

/**
 * Class implementing a Verifier that uses a stutter equivalent Kripke structure.
 */
public class StutterVerifier extends KripkeVerifier implements Verifier {

    /**
     * Creates a StutterVerifier.
     *
     * @param net            The VerifiableNet that represents the model on which the given specification must be verified.
     * @param specification  The specification must be verified on the given model (i.e., net).
     * @param checkerFactory The factory that provides the model checker.
     * @throws ConfigurationException when the configuration fails to load.
     */
    public StutterVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        super(net, specification, checkerFactory);
    }

    /**
     * Starts the verification process for a subset of the specification.
     * Overwrites super to include stutter optimization steps.
     *
     * @param specificationSet the subset of the specification to verify.
     * @throws VerifierException when the verification process fails.
     */
    @Override
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

            double optimizationTime = optimize(structure, specificationPropositions);

            performanceEvent.addMetric("ReductionComputationMs", optimizationTime / 1000000);
            performanceEvent.addMetric("ReducedStructureStateCount", structure.getStateCount());
            performanceEvent.addMetric("ReducedStructureRelationCount", structure.getRelationCount());
            performanceEvent.addMetric("ReducedStructureAtomicPropositionCount", structure.getAtomicPropositionCount());

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
     * Optimizes the given Structure by removing unused atomic propositions from its states and reducing its state
     * space to a stutter equivalent model.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the relevant atomic propositions.
     * @return the time it took to optimize in nanoseconds.
     */
    protected double optimize(KripkeStructure structure, AtomicPropositionMap<CompositeExpression> ap) {
        Logger.log("Reducing Kripke structure", LogEvent.INFO);

        double optimizationTime = optimizeAtomicPropositions(structure, ap);
        optimizationTime += optimizeStutterStates(structure);

        return optimizationTime;
    }

    /**
     * Optimizes the given Structure by removing unused atomic propositions from its states while logging events.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the relevant atomic propositions.
     *
     * @return the time it took to optimize in nanoseconds.
     */
    protected double optimizeAtomicPropositions(KripkeStructure structure, AtomicPropositionMap<CompositeExpression> ap) {
        Logger.log("Removing unused atomic propositions", LogEvent.VERBOSE);

        TreeSet<String> unusedAP = new TreeSet<>(new ComparableComparator<String>());
        unusedAP.addAll(structure.getAtomicPropositions());
        unusedAP.removeAll(ap.getAPKeys());

        double delta = optimizeAtomicPropositions(structure, unusedAP);

        Logger.log("Removed unused atomic propositions in " + formatComputationTime(delta) + ".", LogEvent.VERBOSE);

        return delta;
    }

    /**
     * Optimizes the given Structure by removing unused atomic propositions from its states.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the unused atomic propositions.
     *
     * @return the time it took to optimize in nanoseconds.
     */
    protected double optimizeAtomicPropositions(KripkeStructure structure, TreeSet<String> ap) {
        long t0 = System.nanoTime();
        PropositionOptimizer propositionOptimizer = new PropositionOptimizer(structure, ap);
        long t1 = System.nanoTime();

        Logger.log("\n" + propositionOptimizer, LogEvent.VERBOSE);
        return t1 - t0;
    }

    /**
     * Optimizes the given Structure by reducing its state space to a stutter equivalent model.
     *
     * @param structure the Structure to optimize.
     *
     * @return the time it took to optimize in nanoseconds.
     */
    protected double optimizeStutterStates(KripkeStructure structure) {
        Logger.log("Reducing state space of " + structure.stats(), LogEvent.VERBOSE);
        double delta = stutterOptimize(structure);
        Logger.log("Reduced state space to " + structure.stats() + " in " + formatComputationTime(delta), LogEvent.INFO);

        return delta;
    }

    /**
     * Optimizes the given Structure by reducing its state space to a stutter equivalent model.
     *
     * @param structure the Structure to optimize.
     *
     * @return the time it took to optimize in nanoseconds.
     */
    protected double stutterOptimize(KripkeStructure structure) {
        long t0 = System.nanoTime();
        StutterOptimizer stutterOptimizer = new StutterOptimizer(structure);
        stutterOptimizer.preprocess();
        stutterOptimizer.partition();
        stutterOptimizer.reduce();
        long t1 = System.nanoTime();

        if (Logger.getLogLevel() <= LogEvent.DEBUG) {
            Logger.log("\n" + stutterOptimizer, LogEvent.DEBUG);
            Logger.log("\n" + structure.toString(), LogEvent.DEBUG);
        }

        return t1 - t0;
    }
}
