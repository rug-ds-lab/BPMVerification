package nl.rug.ds.bpm.verification.model.kripke;

import nl.rug.ds.bpm.verification.optimizer.stutterOptimizer.Block;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class State implements Comparable<State> {
    private static int stateID = 0;
    private String id;
    private String AP;
    private String marking;
    private Set<String> atomicPropositions;
    private HashSet<State> nextStates, previousStates;
    //stutter variables
    private boolean flag = false;
    private Block block;

    public State(String marking, TreeSet<String> atomicPropositions) {
        this.id = "S" + stateID++;
        this.marking = marking;
        this.atomicPropositions = atomicPropositions;
        nextStates = new HashSet<State>();
        previousStates = new HashSet<State>();

        AP = marking + "=";
        Iterator<String> api = atomicPropositions.iterator();
        while (api.hasNext()) {
            AP = AP + api.next();
            if (api.hasNext()) AP = AP + " ";
        }
    }
    public static void resetStateId(){
        State.stateID = 0;
    }
    public Set<String> getAtomicPropositions() {
        return atomicPropositions;
    }

    public String getAPString() {
        return AP;
    }

    public String getID() {
        return id;
    }

    public boolean addNext(State s) {
        return nextStates.add(s);
    }

    public boolean addNext(Set<State> s) {
        return nextStates.addAll(s);
    }

    public Set<State> getNextStates() {
        return nextStates;
    }

    public void setNextStates(HashSet<State> nextStates) { this.nextStates = nextStates; }

    public boolean addPrevious(State s) {
        return previousStates.add(s);
    }

    public boolean addPrevious(Set<State> s) {
        return previousStates.addAll(s);
    }

    public Set<State> getPreviousStates() {
        return previousStates;
    }

    public void setPreviousStates(HashSet<State> previousStates) { this.previousStates = previousStates; }

    public String toFriendlyString() {
        StringBuilder st = new StringBuilder(getID() + ": {" + marking + " = ");
        Iterator<String> api = getAtomicPropositions().iterator();
        while (api.hasNext()) {
            st.append(api.next());
            if (api.hasNext()) st.append(" ");
        }
        st.append("}");
        return st.toString();
    }

    @Override
    public String toString() {
        return id + ": {" + getAPString() + "}";
    }

    @Override
    public int compareTo(State o) {
        return (o == null ? 0 : getAPString().compareTo(o.getAPString()));
    }

    @Override
    public boolean equals(Object arg0) {
        return (arg0 != null && getAPString().equals(((State) arg0).getAPString()));
    }

    @Override
    public int hashCode() {
        return getAPString().hashCode();
    }

    public void removeAP(Set<String> APs) {
        atomicPropositions.removeAll(APs);
    }

    public boolean APequals(State n) {
        Iterator<String> i = atomicPropositions.iterator();
        Iterator<String> j = n.getAtomicPropositions().iterator();
        boolean equal = true;

        while (i.hasNext() && j.hasNext() && equal)
            equal = i.next().equals(j.next());

        return equal && (i.hasNext() == j.hasNext());
    }

    public void clearMarking() {
        marking = "";
    }
    
    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    
    public boolean getFlag() {
        return flag;
    }
    
    public void setBlock(Block block) { this.block = block; }
    
    public void resetBlock() { block = null; }
    
    public Block getBlock() { return block; }
}
