package nl.rug.ds.bpm.verification.verifier.generic;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.specification.jaxb.*;
import nl.rug.ds.bpm.specification.marshaller.SpecificationUnmarshaller;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConfigurationException;
import nl.rug.ds.bpm.util.exception.VerifierException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.checker.CheckerFactory;
import nl.rug.ds.bpm.verification.event.EventHandler;
import nl.rug.ds.bpm.verification.event.listener.VerificationEventListener;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;
import nl.rug.ds.bpm.verification.model.StructureFactory;
import nl.rug.ds.bpm.verification.verifier.Verifier;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract class representing a verifier.
 */
public abstract class AbstractVerifier<F extends StructureFactory<? extends State<?>, ? extends Structure<?>>> implements Verifier {
    protected F structureFactory;
    protected EventHandler eventHandler;

    protected VerifiableNet net;
    protected BPMSpecification specification;
    protected CheckerFactory checkerFactory;

    protected HashMap<String, SpecificationType> specificationTypes;

    /**
     * Creates the abstract verifier.
	 *
	 * @param net            The VerifiableNet that represents the model on which the given specification must be verified.
	 * @param specification  The specification must be verified on the given model (i.e., net).
	 * @param checkerFactory The factory that provides the model checker.
	 * @throws ConfigurationException when the configuration fails to load.
	 */
	public AbstractVerifier(VerifiableNet net, BPMSpecification specification, CheckerFactory checkerFactory) throws ConfigurationException {
		eventHandler = new EventHandler();
		specificationTypes = new HashMap<>();

		this.net = net;
		this.specification = specification;
		this.checkerFactory = checkerFactory;

		BPMSpecification configuration = loadConfiguration();
		addSpecificationTypes(configuration);
		addSpecificationTypes(specification);

		mapSpecificationSets(specification);
	}

	/**
	 * Starts the verification process.
	 *
	 * @throws VerifierException when the verification process fails.
	 */
	public abstract void verify() throws VerifierException;

	/**
	 * Adds a listener that is notified of verification results.
	 *
	 * @param verificationEventListener the listener to add.
	 */
	public void addEventListener(VerificationEventListener verificationEventListener) {
		eventHandler.addEventListener(verificationEventListener);
	}

	/**
	 * Removes the listener, preventing further notifications.
	 *
	 * @param verificationEventListener the listener to remove.
	 */
	public void removeEventListener(VerificationEventListener verificationEventListener) {
		eventHandler.removeEventListener(verificationEventListener);
	}

	/**
	 * Formats the given computation time into a String.
	 *
	 * @param nanos the computation time in nanoseconds.
	 * @return formatted String of the computation time in (nano/mili) seconds.
	 */
	protected String formatComputationTime(double nanos) {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.UK);
		DecimalFormat df = (DecimalFormat) nf;
		df.applyPattern("#.###");

		String delta = "";
		double milis = nanos / 1000000;
		double secs = milis / 1000;

		if (secs < 1 && milis < 1)
			delta = df.format(nanos) + " ns";
		else if (secs < 1)
			delta = df.format(milis) + " ms";
		else
			delta = df.format(secs) + " s";

		return delta;
	}

	/**
	 * Add the atomic propositions contained in a specification set to an AtomicPropositionMap.
	 *
	 * @param atomicPropositionMap the AtomicPropositionMap to fill.
	 * @param specificationSet     the subset of specifications to use.
	 */
	protected void getSpecificationSetPropositions(AtomicPropositionMap<CompositeExpression> atomicPropositionMap, SpecificationSet specificationSet) {
		for (CompositeExpression expression : getSpecificationSetExpressions(specificationSet))
			atomicPropositionMap.addID(expression);
	}

	/**
	 * Obtain a set of CompositeExpressions of the atomic propositions used by the given subset of specifications.
	 *
	 * @param specificationSet the subset of specifications to use.
	 * @return a set of CompositeExpression
	 */
	protected Set<CompositeExpression> getSpecificationSetExpressions(SpecificationSet specificationSet) {
		Set<CompositeExpression> ap = new TreeSet<>(new ComparableComparator<>());

		for (Specification s : specificationSet.getSpecifications())
			for (InputElement inputElement : s.getInputElements())
				ap.add(ExpressionBuilder.parseExpression(inputElement.getElement()));

		return ap;
	}

	/**
	 * Addthe atomic propositions defined by groups to an AtomicPropositionMap.
	 *
	 * @param atomicPropositionMap the AtomicPropositionMap to fill.
	 */
	protected void getGroupPropositions(AtomicPropositionMap<CompositeExpression> atomicPropositionMap) {
		for (Group group : specification.getGroups()) {
			CompositeExpression groupExpression = new CompositeExpression(LogicalType.OR);
			for (Element element : group.getElements())
				groupExpression.addArgument(ExpressionBuilder.parseExpression(element.getId()));
			String ap = atomicPropositionMap.addID(groupExpression);
			atomicPropositionMap.addID(ExpressionBuilder.parseExpression(group.getId()), ap);
		}
	}

	private BPMSpecification loadConfiguration() throws ConfigurationException {
		Logger.log("Loading configuration file", LogEvent.INFO);
		try {
			SpecificationUnmarshaller unmarshaller = new SpecificationUnmarshaller(this.getClass().getResourceAsStream("/specificationTypes.xml"));
			return unmarshaller.getSpecification();
		} catch (Exception e) {
			throw new ConfigurationException("Failed to load configuration file");
		}
	}

	private void addSpecificationTypes(BPMSpecification specification) {
		for (SpecificationType specificationType : specification.getSpecificationTypes())
			addSpecificationType(specificationType);
	}

	private void addSpecificationType(SpecificationType specificationType) {
		if (specificationTypes.containsKey(specificationType.getId()))
			Logger.log("Duplicate specification type: " + specificationType.getId() + ". Overwriting!", LogEvent.WARNING);
		else
			Logger.log("Adding specification type " + specificationType.getId(), LogEvent.VERBOSE);

		specificationTypes.put(specificationType.getId(), specificationType);
	}

	private void mapSpecificationSets(BPMSpecification specification) {
		for (SpecificationSet set : specification.getSpecificationSets())
			mapSpecificationSet(set);
	}

	private void mapSpecificationSet(SpecificationSet set) {
		for (Specification specification : set.getSpecifications())
			mapSpecification(specification);
	}

	private void mapSpecification(Specification specification) {
		if (specificationTypes.containsKey(specification.getType()))
			specification.setSpecificationType(specificationTypes.get(specification.getType()));
		else
			Logger.log("No such specification type: " + specification.getType(), LogEvent.WARNING);
	}
}
