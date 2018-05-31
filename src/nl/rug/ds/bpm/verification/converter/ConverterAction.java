package nl.rug.ds.bpm.verification.converter;

import nl.rug.ds.bpm.petrinet.interfaces.element.T;
import nl.rug.ds.bpm.petrinet.interfaces.graph.TransitionGraph;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataM;
import nl.rug.ds.bpm.petrinet.interfaces.marking.M;
import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.map.IDMap;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;

/**
 * Created by Heerko Groefsema on 20-May-17.
 */
public class ConverterAction extends RecursiveAction {
	private Kripke kripke;
	private TransitionGraph net;
	private IDMap idMap;
	private M marking;
	private State previous;
	
	public ConverterAction(Kripke kripke, TransitionGraph net, IDMap idMap, M marking, State previous) {
		this.kripke = kripke;
		this.net = net;
		this.idMap = idMap;
		this.marking = marking;
		this.previous = previous;
	}
	
	@Override
	protected void compute() {
		if(kripke.getStateCount() >= Kripke.getMaximumStates()) {
			return;
		}
		for (Set<? extends T> enabled: net.getParallelEnabledTransitions(marking)) {
			State found = new State(marking.toString(), mapTransitionIds(enabled));

			if (marking instanceof DataM) {
				Set<String> data = new HashSet<>();
				for (String b : ((DataM) marking).getBindings().keySet())
					if (!b.equalsIgnoreCase("nashorn.global"))
						data.add(b + "=" + ((DataM) marking).getBindings().get(b));
				found.addAP(data);
			}

			State existing = kripke.addNext(previous, found);
			
			if (found == existing) { //if found is a new state
				if (enabled.isEmpty()) { //if state is a sink
					found.addNext(found);
					found.addPrevious(found);
				}

				Set<ConverterAction> nextActions = new HashSet<>();
				for (T transition: enabled)
					for (M step : net.fireTransition(transition, marking))
						nextActions.add(new ConverterAction(kripke, net, idMap, step, found));

				invokeAll(nextActions);
			}
		}
	}

	private TreeSet<String> mapTransitionIds(Set<? extends T> transitions) {
		TreeSet<String> aps = new TreeSet<String>(new StringComparator());

		for (T transition: transitions)
			aps.add(mapAP((transition.isTau() ? "tau" : transition.getId())));

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
