package nl.rug.ds.bpm.verification.verifier.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.BPMSpecification;
import nl.rug.ds.bpm.specification.jaxb.Formula;
import nl.rug.ds.bpm.specification.jaxb.Specification;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.CheckerException;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.Checker;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.converter.ConverterAction;
import nl.rug.ds.bpm.verification.event.VerificationEvent;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.Structure;
import nl.rug.ds.bpm.verification.model.multi.MultiStructure;
import nl.rug.ds.bpm.verification.model.multi.SubStructure;
import nl.rug.ds.bpm.verification.model.multi.factory.MultiFactory;
import nl.rug.ds.bpm.verification.verifier.Verifier;
import nl.rug.ds.bpm.verification.verifier.generic.AbstractVerifier;

import java.util.List;

/**
 * Class implementing a Verifier that uses a MultiStructure.
 */
public class MultiVerifier extends AbstractVerifier implements Verifier {

    /**
     * Creates a MultiVerifier.
     *
     * @param net            The VerifiableNet that represents the model on which the given specification must be verified.
     * @param specification  The specification must be verified on the given model (i.e., net).
     * @param checkerFactory The factory that provides the model checker.
     * @throws ConfigurationException when the configuration fails to load.
     */
    public MultiVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
        super(net, specification, checkerFactory);
        structureFactory = new MultiFactory();
    }

    @Override
    public void verify() throws VerifierException {
        Logger.log("Verifying specification", LogEvent.INFO);

        MultiStructure structure = (MultiStructure) structureFactory.createStructure();

        for (SpecificationSet specificationSet : specification.getSpecificationSets()) {
            AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = getSpecificationPropositions(specificationSet);
            structureFactory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

            structure.addSubStructure(specificationSet, specificationSetPropositionMap.getAPKeys());
        }

        try {
            compute(structure);
            finalize(structure);
        } catch (Exception e) {
            Logger.log("Failed to compute multi structure.", LogEvent.CRITICAL);
            e.printStackTrace();
            throw new VerifierException("Failed to compute structure.");
        }

        for (SubStructure subStructure : structure.getSubStructures()) {
            Checker checker = checkerFactory.getChecker();

            try {
                convert(checker, subStructure);
                check(checker);
            } catch (Exception e) {
                Logger.log("Failed to verify set.", LogEvent.ERROR);
                e.printStackTrace();
                throw new VerifierException("Failed to verify set.");
            } finally {
                checkerFactory.release(checker);
            }
        }
    }

    /**
     * Computes the Structure of the Net and logs results.
     *
     * @param structure the Structure to populate.
     */
    protected void compute(Structure structure) {
        Logger.log("Calculating multi structure", LogEvent.INFO);
        double delta = compute(structureFactory.createConverter(net, net.getInitialMarking(), structure));
        Logger.log("Calculated multi structure with " + structure.stats() + " in " + formatComputationTime(delta), LogEvent.INFO);
        if (Logger.getLogLevel() <= LogEvent.DEBUG) {
            Logger.log("\n" + structure, LogEvent.DEBUG);
            for (SubStructure subStructure : ((MultiStructure) structure).getSubStructures())
                Logger.log("Substructure:\n" + subStructure, LogEvent.DEBUG);
        }
    }

    /**
     * Computes the Structure.
     *
     * @param converter the initial conversion step to start the computation from.
     * @return the time it took to compute the Structure in nanoseconds.
     */
    protected double compute(ConverterAction converter) {
        long t0 = System.nanoTime();
        converter.computeInitial();
        long t1 = System.nanoTime();

        return t1 - t0;
    }

    /**
     * Finalizes the given structure by adding a safety, 'ghost', state to each substructure for model
     * check safety. Prevents model checker from complaining about atomic propositions used in specifications
     * that are not in the model (i.e., Structure). In addition clears the state space of the full model.
     *
     * @param structure the Structure finalize.
     */
    protected void finalize(Structure structure) {
        ((MultiStructure) structure).finalizeStructure();
    }

    /**
     * Converts the Structure into the internal representation used by the given Checker.
     *
     * @param checker      the Checker to use for the conversion.
     * @param subStructure the SubStructure to convert.
     * @throws CheckerException when the conversion fails.
     */
    protected void convert(Checker checker, SubStructure subStructure) throws CheckerException {
        Logger.log("Collecting specifications", LogEvent.INFO);
        for (Specification specification : subStructure.getSpecificationSet().getSpecifications())
            for (Formula formula : specification.getSpecificationType().getFormulas())
                checker.addFormula(formula, specification, structureFactory.getAtomicPropositionMap());

        Logger.log("Generating model check input", LogEvent.VERBOSE);
        checker.createModel(subStructure);

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
                eventHandler.fireEvent(event);
                Logger.log("Specification " + event.getFormula().getSpecification().getId() + " evaluated " + event.getVerificationResult() + " for " + event.getFormula().getOriginalFormula(), LogEvent.INFO);
            }
        }

        if (!checker.getOutputChecker().isEmpty())
            throw new CheckerException("Model modelcheck error\n" + checker.getOutputChecker());
    }
}
