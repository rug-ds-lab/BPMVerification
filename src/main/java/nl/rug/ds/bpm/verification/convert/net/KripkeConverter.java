package nl.rug.ds.bpm.verification.convert.net;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.ConditionalMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class KripkeConverter {
	private VerifiableNet net;
    private Kripke kripke;
    private Set<String> conditions;
    private IDMap idMap;
	
	public KripkeConverter(VerifiableNet net, IDMap idMap, Set<String> conditions) {
        this.net = net;
        this.conditions = conditions;
        this.idMap = new IDMap("t", idMap.getMap());
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
            State found = new State(marking.toString(), mapTransitionIds(enabled));

            if (marking instanceof DataMarkingI) {
            	Set<String> data = new HashSet<>();
	            for (String b : ((DataMarkingI) marking).getBindings().keySet())
		            if (!b.equalsIgnoreCase("nashorn.global"))
			            data.add(b + "=" + ((DataMarkingI) marking).getBindings().get(b));
	            found.addAP(data);
            }

            kripke.addInitial(found);
            
            for (TransitionI transition: enabled)
                for (MarkingI step : net.fireTransition(transition, marking)) {
					ConverterAction converterAction = new ConverterAction(kripke, net, idMap, step, transition, found);
                    converterAction.compute();
                }
        }
		
		if (kripke.getStateCount() >= Kripke.getMaximumStates()) {
			throw new ConverterException("Maximum state space reached (at " + Kripke.getMaximumStates() + " states)");
		}
		
        return kripke;
    }

	public IDMap getIdMap() {
		return idMap;
	}

	private TreeSet<String> mapTransitionIds(Set<? extends TransitionI> transitions) {
        TreeSet<String> aps = new TreeSet<String>(new StringComparator());

		for (TransitionI transition: transitions) {
			aps.add(idMap.addID((transition.isTau() ? "tau" : (transition.getName().isEmpty() ? transition.getId() : transition.getName()))));
			//aps.add(idMap.addID((transition.getName().isEmpty() ? transition.getId() : transition.getName())));
		}

		return aps;
    }

}
