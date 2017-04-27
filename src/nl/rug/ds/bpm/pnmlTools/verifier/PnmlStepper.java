package nl.rug.ds.bpm.pnmlTools.verifier;

import nl.rug.ds.bpm.verification.stepper.Marking;
import nl.rug.ds.bpm.verification.stepper.Stepper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.JDOMException;

import com.google.common.collect.Sets;

import ee.ut.pnml.PNMLReader;
import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.petrinet.Transition;

/**
 * Created by Nick van Beest on 26-04-2017
 */
public class PnmlStepper extends Stepper {
	
	private PetriNet pn;
	private Map<String, Transition> transitionmap;
	private Map<String, Place> placemap;

	public PnmlStepper(File pnml) throws JDOMException, IOException {
		super(pnml);
		getPN();
		initializeTransitionMap();
		initializePlaceMap();
	}
	
	private void getPN() throws JDOMException, IOException {
		pn = PNMLReader.parse(net);
	}
	
	private void initializeTransitionMap() {
		transitionmap = new HashMap<String, Transition>();
		for (Transition t: pn.getTransitions()) {
			transitionmap.put(getId(t), t);
		}
	}
	
	private void initializePlaceMap() {
		placemap = new HashMap<String, Place>();
		
		for (Place p: pn.getPlaces()) {
			placemap.put(getId(p), p);
		}
	}
	
	// Create a map with all enabled transitions and their corresponding bitset presets
	private Map<String, BitSet> getEnabledPresets(Marking marking) {
		List<Place> filled = new ArrayList<Place>();
		Set<Transition> enabled = new HashSet<Transition>();
		Map<String, BitSet> enabledpresets = new HashMap<String, BitSet>();
		
		for (String place: marking.getMarkedPlaces()) {
			filled.add(placemap.get(place));
			enabled.addAll(placemap.get(place).getPostSet());
		}
		
		for (Transition t: new HashSet<Transition>(enabled)) {
			if (!filled.containsAll(t.getPreSet())) {
				enabled.remove(t);
			}
			else {
				enabledpresets.put(getId(t), getPresetBitSet(t, filled));
			}
		}
		
		return enabledpresets;
	}
	
	// Create a bitset that holds the positions in the list allplaces that are part of the preset of trans
	private BitSet getPresetBitSet(Transition trans, List<Place> allplaces) {
		BitSet b = new BitSet();
		
		for (Place p: trans.getPreSet()) {
			b.set(allplaces.indexOf(p));
		}
		
		return b;
	}
	
	private String getId(Place p) {
		return p.getName() + "(" + p.id + ")";
	}
	
	private String getId(Transition t) {
		return t.getName() + "(" + t.id + ")";
	}
	
	@Override
	public Marking initialMarking() {
		Marking initial = new Marking();
		
		// add all places with no incoming arcs to initial marking
		for (Place p: pn.getPlaces()) {
			if (p.getIncoming().size() == 0) {
				initial.addTokens(getId(p), 1);
			}
		}
		
		return initial;
	}
	
	public Set<String> getEnabledTransitions(Marking marking) {
		return getEnabledPresets(marking).keySet();
	}
	
	@Override
	public Set<Set<String>> parallelActivatedTransitions(Marking marking) {
		Set<Set<String>> ypar = new HashSet<Set<String>>();
		
		Map<String, BitSet> enabledpresets = getEnabledPresets(marking);

		// create a power set of all curently enabled transitions
		ypar = new HashSet<Set<String>>(Sets.powerSet(enabledpresets.keySet()));
		
		// remove empty set
		ypar.remove(new HashSet<String>());

		BitSet overlap;
		for (Set<String> sim: new HashSet<Set<String>>(ypar)) {
			overlap = new BitSet();
			for (String t: sim) {
				// check if presets overlap for the set of transitions
				// if yes, remove (i.e. they cannot fire simultaneously)
				if (!overlap.intersects(enabledpresets.get(t))) {
					overlap.or(enabledpresets.get(t));
				}
				else {
					ypar.remove(sim);
					break;
				}
			}
		}
		
		Set<Set<String>> subsets = new HashSet<Set<String>>();
		
		// remove subsets to obtain the largest sets
		for (Set<String> par1: ypar) {
			for (Set<String> par2: ypar) {
				if ((par1.containsAll(par2)) && (par1.size() != par2.size())) {
					subsets.add(par2);
				}
			}
		}
		
		ypar.removeAll(subsets);
		
		return ypar;
	}
	
	@Override
	public Set<Marking> fireTransition(Marking marking, String transitionId, Set<String> conditions) {
		Set<Marking> afterfire = new HashSet<Marking>();

		Boolean enabled = true;
		Marking currentfire = new Marking();
		currentfire.copyFromMarking(marking);

		Transition selected = transitionmap.get(transitionId);
		
		// check if selected transition is indeed enabled
		Set<String> placeIds = new HashSet<String>();
		for (Place p: selected.getPreSet()) {
			if (currentfire.hasTokens(getId(p))) {
				placeIds.add(getId(p));
			}
			else {
				enabled = false;
				break;
			}
		}
		
		// fire
		if (enabled) {
			// remove 1 token from each incoming place
			currentfire.consumeTokens(placeIds);
			
			// place 1 token in each outgoing place
			for (Place p: selected.getPostSet()) {
				currentfire.addTokens(getId(p), 1);
			}
		}
		
		afterfire.add(currentfire);			
		
		return afterfire;
	}
	
	public String getTransitionMap() {
		String str = "";
		for (String t: transitionmap.keySet()) {
			str += t + ": " + transitionmap.get(t).getName() + "\n";
		}
		
		return str;
	}
	
}
