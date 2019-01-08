package nl.rug.ds.bpm.verification.convert.net;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
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
	private VerifiableNet net;
	private IDMap idMap;
	private MarkingI marking;
	private TransitionI fired;
	private State previous;
	
	public ConverterAction(Kripke kripke, VerifiableNet net, IDMap idMap, MarkingI marking, TransitionI fired, State previous) {
		this.kripke = kripke;
		this.net = net;
		this.idMap = idMap;
		this.marking = marking;
		this.fired = fired;
		this.previous = previous;
	}
	
	@Override
	protected void compute() {
		if(kripke.getStateCount() >= Kripke.getMaximumStates()) {
			return;
		}
		if (marking.getMarkedPlaces().isEmpty()) {
			previous.addNext(previous);
			previous.addPrevious(previous);
			Logger.log("Encountered empty marking, adding sink state.", LogEvent.WARNING);
		}
		else for (Set<? extends TransitionI> enabled: net.getParallelEnabledTransitions(marking)) {
			TreeSet<String> ap = mapTransitionIds(enabled);
			TreeSet<String> previousAp = new TreeSet<>(new ComparableComparator());
			previousAp.addAll(previous.getAtomicPropositions());
			previousAp.remove(idMap.addID((fired.isTau() ? "tau" : (fired.getName().isEmpty() ? fired.getId() : fired.getName()))));
			//previousAp.remove(idMap.addID((fired.getName().isEmpty() ? fired.getId() : fired.getName())));

			if(ap.containsAll(previousAp)) {
				State found = new State(marking.toString(), ap);

				if (marking instanceof DataMarkingI) {
					Set<String> data = new HashSet<>();
					for (String b : ((DataMarkingI) marking).getBindings().keySet())
						if (!b.equalsIgnoreCase("nashorn.global"))
							data.add(b + "=" + ((DataMarkingI) marking).getBindings().get(b));
					found.addAP(data);
				}

				State existing = kripke.addNext(previous, found);

				if (found == existing) { //if found is a new state
					if (enabled.isEmpty()) { //if state is a sink
						found.addNext(found);
						found.addPrevious(found);
					}

					Set<ConverterAction> nextActions = new HashSet<>();
					for (TransitionI transition : enabled)
						for (MarkingI step : net.fireTransition(transition, marking))
							nextActions.add(new ConverterAction(kripke, net, idMap, step, transition, found));

					invokeAll(nextActions);
				}
			}
		}
	}

	private TreeSet<String> mapTransitionIds(Set<? extends TransitionI> transitions) {
		TreeSet<String> aps = new TreeSet<String>(new ComparableComparator());

		for (TransitionI transition: transitions) {
			aps.add(idMap.addID((transition.isTau() ? "tau" : (transition.getName().isEmpty() ? transition.getId() : transition.getName()))));
			//aps.add(idMap.addID((transition.getName().isEmpty() ? transition.getId() : transition.getName())));
		}

		return aps;
	}
}
