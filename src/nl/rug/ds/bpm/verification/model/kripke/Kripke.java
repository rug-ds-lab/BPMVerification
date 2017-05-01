package nl.rug.ds.bpm.verification.model.kripke;

import nl.rug.ds.bpm.verification.comparator.StateComparator;
import nl.rug.ds.bpm.verification.comparator.StringComparator;

import java.util.*;
import java.util.stream.Collectors;

public class Kripke {
    private TreeSet<String> atomicPropositions;
    private TreeSet<State> states;
    private TreeSet<State> initial;


    private Set<String> propositionOptimized;
    private Set<State> stutterOptimizedStates;

    public Kripke() {
        atomicPropositions = new TreeSet<String>(new StringComparator());
        states = new TreeSet<State>(new StateComparator());
        initial = new TreeSet<State>(new StateComparator());

        propositionOptimized = new TreeSet<>(new StringComparator());
        stutterOptimizedStates = new TreeSet<State>(new StateComparator());
    }

    public boolean addAtomicProposition(String ap) {
        return atomicPropositions.add(ap);
    }

    public boolean addState(State s) {
        return states.add(s);
    }

    public boolean addInitial(State s) {
        return initial.add(s);
    }

    public boolean addAtomicPropositions(Set<String> ap) {
        return atomicPropositions.addAll(ap);
    }

    public boolean addStates(Set<State> s) {
        return states.addAll(s);
    }

    public boolean addInitials(Set<State> s) {
        return initial.addAll(s);
    }

    public void setPropositionOptimized(Set<String> propositionOptimized) {
        this.propositionOptimized = propositionOptimized;
    }

    public void setStutterOptimizedStates(Set<State> stutterOptimizedStates) {
        this.stutterOptimizedStates.addAll(stutterOptimizedStates);
    }

    public TreeSet<String> getAtomicPropositions() {
        return atomicPropositions;
    }

    public TreeSet<State> getStates() {
        return states;
    }

    public TreeSet<State> getInitial() {
        return initial;
    }

    public String[] getAtomicPropositionsArray() {
        String[] apArray = new String[atomicPropositions.size()];
        return atomicPropositions.toArray(apArray);
    }

    public State[] getStatesArray() {
        State[] sArray = new State[states.size()];
        return states.toArray(sArray);
    }

    public State[] getInitialArray() {
        State[] sArray = new State[initial.size()];
        return initial.toArray(sArray);
    }
    
    public Set<State> getSinkStates() {
        return states.stream().filter(s -> s.getNextStates().isEmpty()).collect(Collectors.toSet());
    }
    
    public int getStateCount() {
        return states.size();
    }

    public int getRelationCount() {
        int relCount = 0;
        for (State s : states)
            relCount += s.getNextStates().size();

        return relCount;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean fullOutput) {
        String ret = "";
        if (fullOutput) {
            StringBuilder ap = new StringBuilder("Atomic Propositions: {");
            Iterator<String> k = atomicPropositions.iterator();
            while (k.hasNext()) {
                ap.append(k.next());
                if (k.hasNext()) ap.append(", ");
            }
            ap.append("}\n\n");

            StringBuilder st = new StringBuilder("States:\n");
            for (State s : states)
                st.append(s.toFriendlyString() + "\n");

            StringBuilder rel = new StringBuilder("\nRelations:\n");
            double relCount = 0;
            for (State s : states) {
                relCount += s.getNextStates().size();
                if (!s.getNextStates().isEmpty()) {
                    rel.append(s.toFriendlyString() + " -> ");
                    for (State t : s.getNextStates())
                        rel.append(t.toFriendlyString() + " ");
                    rel.append("\n");
                }
            }
            StringBuilder stutterOp = new StringBuilder("\nstutter Optimized States:\n");
            for (State s : stutterOptimizedStates) {
                stutterOp.append(s.toFriendlyString() + "\n");
            }

            StringBuilder propositionOp = new StringBuilder("\nproposition Optimized Transitions:\n");
            for (String s : propositionOptimized) {
                propositionOp.append(s.toString() + "\n");
            }
    
            StringBuilder count = new StringBuilder("\nNumber of states: " + states.size() + "\n");
            count.append("Number of relations: " + getRelationCount() + "\n");
            count.append("Number of AP: " + atomicPropositions.size() + "\n");
    
            count.append("Number of proposition Optimized transitions: " + propositionOptimized.size() + "\n");
            count.append("Number of stutter Optimized States: " + stutterOptimizedStates.size() + "\n");

            ret = ap.toString() + st.toString() + rel.toString() + count.toString() + stutterOp.toString() + propositionOp.toString() + "\n";
        }

        return ret;
    }
    
    public String stats() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("|S| = " + states.size() + ", ");
        sb.append("|R| = " + states.stream().map(State::getNextStates).mapToInt(Set::size).sum() +", ");
        sb.append("|AP| = " + atomicPropositions.size());
        
        return sb.toString();
    }
}
