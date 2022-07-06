package nl.rug.ds.bpm.verification.converter.kripke;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.ConditionalMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.converter.generic.AbstractConverterAction;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;
import nl.rug.ds.bpm.verification.model.kripke.factory.KripkeFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

/**
 * Class that converts a given VerifiableNet into a KripkeStructure using RecursiveActions.
 */
public class KripkeStructureConverterAction extends AbstractConverterAction<KripkeState> {
	private final KripkeFactory kripkeFactory;
	private final KripkeStructure kripkeStructure;
	private KripkeState previous;

	/**
	 * Creates a KripkeStructureConverterAction to compute the initial States and start the RecursiveAction computation.
	 *
	 * @param net             a VerifiableNet
	 * @param marking         the initial Marking of the VerifiableNet.
	 * @param factory         the StructureFactory used.
	 * @param kripkeStructure the KripkeStructure to populate.
	 */
	public KripkeStructureConverterAction(VerifiableNet net, MarkingI marking, KripkeFactory factory, KripkeStructure kripkeStructure) {
		super(net, marking);
		kripkeFactory = factory;
		this.kripkeStructure = kripkeStructure;
	}

	/**
	 * Creates a KripkeStructureConverterAction to compute subsequent States of the RecursiveAction computation.
	 *
	 * @param net             a VerifiableNet
	 * @param marking         the subsequent Marking of the VerifiableNet obtained after firing the Transition fired.
	 * @param fired           the Transition fired to obtain marking.
	 * @param factory         the StructureFactory used.
	 * @param kripkeStructure the KripkeStructure to populate.
	 * @param previous        the State obtained in the previous computation step.
	 */
	public KripkeStructureConverterAction(VerifiableNet net, MarkingI marking, TransitionI fired, KripkeFactory factory, KripkeStructure kripkeStructure, KripkeState previous) {
		super(net, marking, fired);
		this.kripkeFactory = factory;
		this.kripkeStructure = kripkeStructure;
		this.previous = previous;
	}

	/**
	 * Computes the initial States and starts the RecursiveAction computation.
	 */
	@Override
	public void computeInitial() {
		if (marking instanceof ConditionalMarkingI)
			for (String condition : kripkeStructure.getConditions())
				((ConditionalMarkingI) marking).addCondition(condition);

		for (Set<? extends TransitionI> enabled : net.getParallelEnabledTransitions(marking)) {
			KripkeState created = kripkeFactory.createState(marking, enabled);

			try {
				KripkeState found = kripkeStructure.addInitial(created);

				if (isNew(created, found)) {
					if (!isSink(enabled))
						invokeAll(nextActions(found, enabled));
					else {
						makeSink(created);
						Logger.log("Encountered empty initial marking, setting sink state.", LogEvent.WARNING);
					}
				}
			} catch (ConverterException e) {
				Logger.log("Encountered issue adding initial marking.", LogEvent.ERROR);
			}
		}
	}

	/**
	 * Overriden method from RecursiveAction that computes a step to calculate the Kripke structure.
	 * <p>
	 * TODO evaluate the need to check if all previous atomic propositions, minus those of the fired Transition, are in the computed State.
	 */
	@Override
	public void compute() {
		if (marking.getMarkedPlaces().isEmpty()) {
			makeSink(previous);
			Logger.log("Encountered empty marking, adding sink state.", LogEvent.WARNING);
		} else for (Set<? extends TransitionI> enabled : net.getParallelEnabledTransitions(marking)) {
			KripkeState created = (KripkeState) kripkeFactory.createState(marking, enabled);

			try {
				KripkeState found = (KripkeState) kripkeStructure.addNext(previous, created);

				if (isNew(created, found)) {
					if (isSink(enabled))
						makeSink(found);

					invokeAll(nextActions(found, enabled));
				}
			} catch (ConverterException e) {
				Logger.log("Maximum state space reached", LogEvent.CRITICAL);
			}
		}
	}

	/**
	 * Obtains the set of next ConverterActions from the State created by this ConverterAction and the obtained set of enabled Transitions.
	 *
	 * @param created the State created by this ConverterAction.
	 * @param enabled the obtained set of enabled Transitions.
	 * @return a set of ConverterActions.
	 */
	@Override
	public Set<? extends RecursiveAction> nextActions(KripkeState created, Set<? extends TransitionI> enabled) {
		Set<KripkeStructureConverterAction> nextActions = new HashSet<>();
		for (TransitionI transition : enabled)
			for (MarkingI step : net.fireTransition(transition, marking))
				nextActions.add(new KripkeStructureConverterAction(this.net, step, transition, this.kripkeFactory, this.kripkeStructure, created));

		return nextActions;
	}
}