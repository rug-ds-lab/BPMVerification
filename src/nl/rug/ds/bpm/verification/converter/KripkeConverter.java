package nl.rug.ds.bpm.verification.converter;

import nl.rug.ds.bpm.petrinet.interfaces.element.T;
import nl.rug.ds.bpm.petrinet.interfaces.graph.TransitionGraph;
import nl.rug.ds.bpm.petrinet.interfaces.marking.ConditionalM;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataM;
import nl.rug.ds.bpm.petrinet.interfaces.marking.M;
import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class KripkeConverter {
	private TransitionGraph net;
    private Kripke kripke;
    private Set<String> conditions;
    private IDMap idMap;
	
	public KripkeConverter(TransitionGraph net, IDMap idMap, Set<String> conditions) {
        this.net = net;
        this.conditions = conditions;
        this.idMap = new IDMap("t", idMap.getIdToAp(), idMap.getApToId());
		State.resetStateId();
    }
	
	public Kripke convert() throws ConverterException {
        kripke = new Kripke();

        M marking = net.getInitialMarking();
        if (marking instanceof ConditionalM)
        	for (String condition: conditions)
        	    ((ConditionalM) marking).addCondition(condition);

        if (marking.getMarkedPlaces().isEmpty()) {
			throw new ConverterException("Initial marking empty, no tokens on any place.");
		}
		else for (Set<? extends T> enabled: net.getParallelEnabledTransitions(marking)) {
            State found = new State(marking.toString(), mapTransitionIds(enabled));

            if (marking instanceof DataM) {
            	Set<String> data = new HashSet<>();
	            for (String b : ((DataM) marking).getBindings().keySet())
		            if (!b.equalsIgnoreCase("nashorn.global"))
			            data.add(b + "=" + ((DataM) marking).getBindings().get(b));
	            found.addAP(data);
            }

            kripke.addInitial(found);
            
            for (T transition: enabled)
                for (M step : net.fireTransition(transition, marking)) {
					ConverterAction converterAction = new ConverterAction(kripke, net, idMap, step, found);
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

	private TreeSet<String> mapTransitionIds(Set<? extends T> transitions) {
        TreeSet<String> aps = new TreeSet<String>(new StringComparator());
        
        for (T transition: transitions)
	        aps.add(mapAP((transition.isTau() ? "tau" : (transition.getName().isEmpty() ? transition.getId() : transition.getName()))));
        
        return aps;
    }

    private String mapAP(String ap) {
	    boolean exist = idMap.getIdToAp().containsKey(ap);
	    idMap.addID(ap);
	    if(!exist)
		    Logger.log("Mapping " + ap + " to " + idMap.getAP(ap), LogEvent.VERBOSE);

	    return idMap.getAP(ap);
    }
}
