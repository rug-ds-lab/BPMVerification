package nl.rug.ds.bpm.pnml.verifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jdom.JDOMException;

import com.google.common.collect.Sets;

import hub.top.petrinet.Arc;
import hub.top.petrinet.Node;
import hub.top.petrinet.PetriNet;
import hub.top.petrinet.Place;
import hub.top.petrinet.Transition;
import nl.rug.ds.bpm.expression.Expression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.extpetrinet.ExtPetriNet;
import nl.rug.ds.bpm.pnml.reader.ExtPNMLReader;
import nl.rug.ds.bpm.verification.comparator.StringComparator;
import nl.rug.ds.bpm.verification.stepper.Marking;
import nl.rug.ds.bpm.verification.stepper.Stepper;

/**
 * Created by Nick van Beest on 26-04-2017
 */
public class ExtPnmlStepper extends Stepper {
	private ScriptEngine se;
	private ExtPetriNet pn;
	private Map<String, Transition> transitionmap;
	private Map<String, Place> placemap;
	private Map<String, Set<String>> transitionIdmap;
	
	// these are the guards on transitions
	private Map<Transition, Set<Expression<?>>> conditionmap; 
	
	// these are the global conditions that hold for the ctl spec to be evaluated (and hence apply to the entire process)
	private Set<Expression<?>> globalconditions; 
	
	public ExtPnmlStepper(File pnml) throws JDOMException, IOException {
		super(pnml);
		getPN();
		initializeTransitionMaps();
		initializePlaceMap();
		
		ScriptEngineManager sem = new ScriptEngineManager();
		se = sem.getEngineByName("JavaScript");
	}

	public ExtPnmlStepper(PetriNet pn) throws JDOMException, IOException {
		super();
		this.pn = getExtPN(pn);
		initializeTransitionMaps();
		initializePlaceMap();
		
		ScriptEngineManager sem = new ScriptEngineManager();
		se = sem.getEngineByName("JavaScript");
	}
	
	public ExtPnmlStepper(ExtPetriNet pn, Map<String, Transition> transitionmap, Map<String, Place> placemap, Map<String, Set<String>> transitionIdmap) {
		this.pn = pn;
		this.transitionmap = transitionmap;
		this.placemap = placemap;
		this.transitionIdmap = transitionIdmap;
		
		ScriptEngineManager sem = new ScriptEngineManager();
		se = sem.getEngineByName("JavaScript");
	}
	
	private void getPN() throws JDOMException, IOException {
		pn = ExtPNMLReader.parse(net);
	}
	
	private ExtPetriNet getExtPN(PetriNet pn) {
		ExtPetriNet epn = new ExtPetriNet();
		HashMap<Node, Object> map = new HashMap<>();

		for (Transition t: pn.getTransitions()) {
			Object tNew  = epn.addTransition(t.getName());
			map.put(t, tNew);
		}
		
		for (Place p: pn.getPlaces()) {
			Object pNew = epn.addPlace(p.getName());
			map.put(p, pNew);
		}
		
		for (Arc a: pn.getArcs()) {
			if (a.getSource() instanceof Place)
				epn.addArc((Place)map.get(a.getSource()), (Transition)map.get(a.getTarget()));
			else
				epn.addArc((Transition)map.get(a.getSource()), (Place)map.get(a.getTarget()));
		}
		
		return epn;
	}
	
	private void initializeTransitionMaps() {
		transitionmap = new TreeMap<String, Transition>(new StringComparator());
		transitionIdmap = new TreeMap<String, Set<String>>(new StringComparator());
		
		for (Transition t: pn.getTransitions()) {
			transitionmap.put(getId(t), t);
			
			if (!transitionIdmap.containsKey(t.getName()))
				transitionIdmap.put(t.getName(), new HashSet<String>());
			
			transitionIdmap.get(t.getName()).add(getId(t));
		}
	}
	
	private void initializePlaceMap() {
		placemap = new TreeMap<String, Place>(new StringComparator());
		
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
			if ((!filled.containsAll(t.getPreSet())) || (contradictsConditions(t))) {  // NEW: CONTRADICTSCONDITIONS
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
	
	private Boolean contradictsConditions(Transition t) {
		if ((globalconditions.size() == 0) || (!conditionmap.containsKey(t))) return false;
		
		for (Expression<?> global: globalconditions) {
			for (Expression<?> guard: conditionmap.get(t)) {
				if (guard.contradicts(global)) return true;
			}
		}
		
		return false;
	}
	
	private Boolean haveContradiction(String transition1, String transition2) {
		Transition t1 = transitionmap.get(transition1);
		Transition t2 = transitionmap.get(transition2);
		
		for (Expression<?> e1: conditionmap.get(t1)) {
			for (Expression<?> e2: conditionmap.get(t2)) {
				if (e1.contradicts(e2)) return true;
			}
		}
		
		return false;
	}
	
	private Boolean haveContradiction(Set<String> tset1, Set<String> tset2) {
		for (String t1: tset1) {
			for (String t2: tset2) {
				if (haveContradiction(t1, t2)) return true;
			}
		}
		
		return false;
	}
	
	private String getId(Place p) {
		return p.getName() + "(" + p.id + ")";
	}
	
	private String getId(Transition t) {
//		return t.getName() + "(" + t.id + ")";
//		return t.getUniqueIdentifier();
		return t.getName();
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
	
	public Map<String, Set<String>> getTransitionIdMap() {
		return transitionIdmap;
	}
	
	public void setConditions(Set<String> conditions) {
		globalconditions = new HashSet<Expression<?>>();
		
		for (String c: conditions) {
			globalconditions.add(ExpressionBuilder.parseExpression(c));
		}
	}
	
	public void setTransitionGuards(Map<Transition, Set<String>> guardmap) {
		conditionmap = new HashMap<Transition, Set<Expression<?>>>();
		
		for (Transition t: guardmap.keySet()) {
			if (!conditionmap.containsKey(t)) conditionmap.put(t, new HashSet<Expression<?>>());
			
			for (String c: guardmap.get(t)) {
				conditionmap.get(t).add(ExpressionBuilder.parseExpression(c));
			}
		}
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
		List<String> simlist;
		Boolean removed;
		for (Set<String> sim: new HashSet<Set<String>>(ypar)) {
			overlap = new BitSet();
			removed = false;
			for (String t: sim) {
				// check if presets overlap for the set of transitions
				// if yes, remove (i.e. they cannot fire simultaneously)
				if (!overlap.intersects(enabledpresets.get(t))) {
					overlap.or(enabledpresets.get(t));
				}
				else {
					ypar.remove(sim);
					removed = true;
					break;
				}
			}
			
			// NEW
			// check if any of the elements contradicts with any of the others
			// if so, the entire subset sim can be removed from ypar
			if (!removed) {
				simlist = new ArrayList<String>(sim);
				for (int i = 0; i < simlist.size() - 1; i++) {
					if (removed) break;
					for (int j = i + 1; j < simlist.size(); j++) {
						if (haveContradiction(simlist.get(i), simlist.get(j))) {
							removed = true;
							ypar.remove(sim);
							break;
						}
					}
				}
			}
		}
		
		Set<Set<String>> subsets = new HashSet<Set<String>>();
		Set<String> additional;
		// remove subsets to obtain the largest sets
		for (Set<String> par1: ypar) {
			for (Set<String> par2: ypar) {
				if ((par1.containsAll(par2)) && (par1.size() != par2.size())) {
					// NEW: 
					// check if any of the additional elements (i.e. par1 \ par2) can contradict with par2
					// if not, subset par2 is redundant and can be removed. 
					additional = new HashSet<String>(par1);
					additional.removeAll(par2);
					if (haveContradiction(additional, par2)) subsets.add(par2);
				}
			}
		}
		
		ypar.removeAll(subsets);
		
		return ypar;
	}
	
	@Override
	public Set<Marking> fireTransition(Marking marking, String transitionId) {
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
	
	public boolean evalGuard(List<String> expressions) {
		String expression = "(";
		Iterator<String> iterator = expressions.iterator();
		while (iterator.hasNext()) {
			expression += iterator.next();
			if (iterator.hasNext())
				expression += " && ";
		}
		expression += ")";
		return evalGuard(expression);
	}
	
	public boolean evalGuard(String expression) {
		boolean ret = true;
		try {
			Object o = se.eval(expression);
			//System.out.println(o.toString());
			ret = Boolean.parseBoolean(o.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	@Override
	public Stepper clone() {
		return new ExtPnmlStepper(pn, transitionmap, placemap, transitionIdmap);
	}
	
}
