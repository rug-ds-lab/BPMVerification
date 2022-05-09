package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.generic.AbstractState;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class that implements a stutter state that contains multiple similar states.
 */
public class StutterState extends AbstractState {
    protected TreeSet<State> states;
    protected SubStructure parent;

    /**
     * Creates a stutter state.
     *
     * @param atomicPropositions the atomic propositions that hold in this state.
     * @param parent             the substructure this stutter state belongs to.
     */
    public StutterState(Set<String> atomicPropositions, SubStructure parent) {
        super(atomicPropositions);
        states = new TreeSet<State>(new ComparableComparator<State>());
        this.parent = parent;
    }

    @Override
    public void setId() {
        lock.lock();
        try {
            this.id = "B" + stateID++;
        } catch (Exception e) {
            Logger.log("Failed to write state ID.", LogEvent.ERROR);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a state to this stutter state.
     *
     * @param s the state to add.
     * @return true if this stutter state did not already contain the given state.
     */
    public boolean addSubState(State s) {
        return states.add(s);
    }

    /**
     * Add a set of states to this stutter state.
     *
     * @param s the set of states to add.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean addSubState(Set<State> s) {
        return states.addAll(s);
    }

    /**
     * Remove a state from this stutter state.
     *
     * @param s the state to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(State s) {
        return states.remove(s);
    }

    /**
     * Remove a set of states from this stutter state.
     *
     * @param s the set of states to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(Set<State> s) {
        return states.removeAll(s);
    }

    /**
     * Returns the set of states that are part of this stutter state.
     *
     * @return the set of states that are part of this stutter state.
     */
    public Set<State> getSubStates() {
        return states;
    }

    /**
     * Merge this stutter state with a given other stutter state that equals this stutter state.
     *
     * @param other the given stutter state.
     * @return true if the merge was successful.
     */
    public boolean merge(StutterState other) {
        boolean equals = other.equals(this) && other != this;

        if (equals) {
            states.addAll(other.getSubStates());
            for (State ms : other.getSubStates())
                ((MultiState) ms).setParent(this.parent, this);
        }

        return equals;
    }


    @Override
    public int compareTo(State o) {
        if (this == o)
            return 0;
        if (o == null)
            return -1;
        if (this.getClass() != o.getClass())
            return -1;
        return id.compareTo(o.getId());
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + states.stream().map(State::getId).collect(Collectors.joining(",")) + " }";
    }
}
