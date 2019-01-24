package nl.rug.ds.bpm.verification.convert.net;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.ConditionalMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.Set;
import java.util.TreeSet;

public class KripkeConverter {
	public final static CompositeExpression tau = ExpressionBuilder.parseExpression("tau");

	private VerifiableNet net;
    private Kripke kripke;
    private Set<String> conditions;
    private AtomicPropositionMap<CompositeExpression> atomicPropositionMap;
	
	public KripkeConverter(VerifiableNet net, AtomicPropositionMap<CompositeExpression> atomicPropositionMap, Set<String> conditions) {
        this.net = net;
        this.conditions = conditions;
        this.atomicPropositionMap = new AtomicPropositionMap("t", atomicPropositionMap.getMap());

        atomicPropositionMap.addID(tau);
		State.resetStateId();
    }
	
	public Kripke convert() throws ConverterException {
        kripke = new Kripke();

        MarkingI marking = net.getInitialMarking();
        if (marking instanceof ConditionalMarkingI)
        	for (String condition: conditions)
        	    ((ConditionalMarkingI) marking).addCondition(condition);

        if (marking.getMarkedPlaces().isEmpty()) {
			throw new ConverterException("Initial marking empty, no tokens on any place.");
		}
		else for (Set<? extends TransitionI> enabled: net.getParallelEnabledTransitions(marking)) {
            State found = new State(marking.toString(), getAtomicPropositions(marking, enabled, atomicPropositionMap));
            kripke.addInitial(found);
            
            for (TransitionI transition: enabled)
                for (MarkingI step : net.fireTransition(transition, marking)) {
					ConverterAction converterAction = new ConverterAction(kripke, net, atomicPropositionMap, step, transition, found);
                    converterAction.compute();
                }
        }
		
		if (kripke.getStateCount() >= Kripke.getMaximumStates()) {
			throw new ConverterException("Maximum state space reached (at " + Kripke.getMaximumStates() + " states)");
		}
		
        return kripke;
    }

	public AtomicPropositionMap<CompositeExpression> getAtomicPropositionMap() {
		return atomicPropositionMap;
	}

	public synchronized static TreeSet<String> getAtomicPropositions(MarkingI marking, Set<? extends TransitionI> transitions, AtomicPropositionMap<CompositeExpression> atomicPropositionMap) {
        TreeSet<String> aps = new TreeSet<String>(new ComparableComparator<String>());

        CompositeExpression stateExpression = new CompositeExpression(LogicalType.XOR);
		for (TransitionI transition: transitions) {
			if (transition.isTau())
				stateExpression.addArgument(tau);
			else {
				CompositeExpression transitionExpression = ExpressionBuilder.parseExpression((transition.getName().isEmpty() ? transition.getId() : transition.getName()));
				atomicPropositionMap.addID(transitionExpression);
				stateExpression.addArgument(transitionExpression);
			}
		}

		if (marking instanceof DataMarkingI)
			for (String b : ((DataMarkingI) marking).getBindings().keySet())
				if (!b.equalsIgnoreCase("nashorn.global"))
					stateExpression.addArgument(ExpressionBuilder.parseExpression((b + " == " + ((DataMarkingI) marking).getBindings().get(b))));

		for (CompositeExpression expression: atomicPropositionMap.getIDKeys())
			if (expression.isFulfilledBy(stateExpression))
				aps.add(atomicPropositionMap.getAP(expression));

		return aps;
    }
}
