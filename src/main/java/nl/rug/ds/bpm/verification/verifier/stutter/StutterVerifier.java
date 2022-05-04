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
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.ConditionalStructure;
import nl.rug.ds.bpm.verification.model.Structure;
import nl.rug.ds.bpm.verification.model.generic.optimizer.proposition.PropositionOptimizer;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;
import nl.rug.ds.bpm.verification.model.kripke.optimizer.stutter.StutterOptimizer;
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

        AtomicPropositionMap<CompositeExpression> specificationPropositions = getSpecificationPropositions(specificationSet);
        structureFactory.getAtomicPropositionMap().merge(specificationPropositions);

        Checker checker = checkerFactory.getChecker();
        Structure structure = structureFactory.createStructure();
        addConditions((ConditionalStructure) structure, specificationSet.getConditions());

        try {
            compute(structure);
            optimize(structure, specificationPropositions);
            finalize(structure, specificationPropositions);
            convert(checker, structure, specificationSet);
            check(checker);
        } catch (Exception e) {
            Logger.log("Failed to verify set.", LogEvent.ERROR);
            e.printStackTrace();
            throw new VerifierException("Failed to verify set.");
        } finally {
            checkerFactory.release(checker);
        }
    }

    /**
     * Optimizes the given Structure by removing unused atomic propositions from its states and reducing its state
     * space to a stutter equivalent model.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the relevant atomic propositions.
     */
    protected void optimize(Structure structure, AtomicPropositionMap<CompositeExpression> ap) {
        Logger.log("Reducing Kripke structure", LogEvent.INFO);

        optimizeAtomicPropositions(structure, ap);
        optimizeStutterStates(structure);
    }

    /**
     * Optimizes the given Structure by removing unused atomic propositions from its states while logging events.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the relevant atomic propositions.
     */
    protected void optimizeAtomicPropositions(Structure structure, AtomicPropositionMap<CompositeExpression> ap) {
        Logger.log("Removing unused atomic propositions", LogEvent.VERBOSE);

        TreeSet<String> unusedAP = new TreeSet<>(new ComparableComparator<String>());
        unusedAP.addAll(structure.getAtomicPropositions());
        unusedAP.removeAll(ap.getAPKeys());

        double delta = optimizeAtomicPropositions(structure, unusedAP);

        Logger.log("Removed unused atomic propositions in " + formatComputationTime(delta) + ".", LogEvent.VERBOSE);
    }

    /**
     * Optimizes the given Structure by removing unused atomic propositions from its states.
     *
     * @param structure the Structure to optimize.
     * @param ap        the AtomicPropositionMap that contains the unused atomic propositions.
     * @return the time it took to optimize in nanoseconds.
     */
    protected double optimizeAtomicPropositions(Structure structure, TreeSet<String> ap) {
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
     */
    protected void optimizeStutterStates(Structure structure) {
        Logger.log("Reducing state space of " + structure.stats(), LogEvent.VERBOSE);

        double delta = stutterOptimize(structure);

        Logger.log("Reduced state space to " + structure.stats() + " in " + formatComputationTime(delta), LogEvent.INFO);
    }

    /**
     * Optimizes the given Structure by reducing its state space to a stutter equivalent model.
     *
     * @param structure the Structure to optimize.
     * @return the time it took to optimize in nanoseconds.
     */
    protected double stutterOptimize(Structure structure) {
        long t0 = System.nanoTime();
        StutterOptimizer stutterOptimizer = new StutterOptimizer((KripkeStructure) structure);
        stutterOptimizer.linearPreProcess();
        ///stutterOptimizer.treeSearchPreProcess();
        stutterOptimizer.optimize();
        long t1 = System.nanoTime();

        if (Logger.getLogLevel() <= LogEvent.DEBUG) {
            Logger.log("\n" + stutterOptimizer, LogEvent.DEBUG);
            Logger.log("\n" + structure.toString(), LogEvent.DEBUG);
        }

        return t1 - t0;
    }
}
