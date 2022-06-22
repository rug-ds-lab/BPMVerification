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
    protected TreeSet<State> entryStates;
    protected TreeSet<State> exitStates;
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
        entryStates = new TreeSet<State>(new ComparableComparator<State>());
        exitStates = new TreeSet<State>(new ComparableComparator<State>());
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
    public boolean addSubStates(Set<State> s) {
        return states.addAll(s);
    }

    /**
     * Remove a state from this stutter state.
     *
     * @param s the state to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(State s) {
        return states.remove(s) || entryStates.remove(s) || exitStates.remove(s);
    }

    /**
     * Remove a set of states from this stutter state.
     *
     * @param s the set of states to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubStates(Set<State> s) {
        return states.removeAll(s) || entryStates.removeAll(s) || exitStates.removeAll(s);
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
     * Add an entry state to this stutter state.
     *
     * @param s the state to add.
     * @return true if this stutter state did not already contain the given state as an entry state.
     */
    public boolean addEntryState(State s) {
        return entryStates.add(s);
    }

    /**
     * Remove a state from the entry states of this stutter state.
     *
     * @param s the state to remove.
     * @return true if the state was removed from the entry states of this stutter state.
     */
    public boolean removeEntryState(State s) {
        return entryStates.remove(s);
    }

    /**
     * Returns the set of entry states that are part of this stutter state.
     *
     * @return the set of entry states that are part of this stutter state.
     */
    public TreeSet<State> getEntryStates() {
        return entryStates;
    }

    /**
     * Add an exit state to this stutter state.
     *
     * @param s the state to add.
     * @return true if this stutter state did not already contain the given state as an exit state.
     */
    public boolean addExitState(State s) {
        return exitStates.add(s);
    }

    /**
     * Remove a state from the exit states of this stutter state.
     *
     * @param s the state to remove.
     * @return true if the state was removed from the exit states of this stutter state.
     */
    public boolean removeExitState(State s) {
        return exitStates.remove(s);
    }

    /**
     * Returns the set of exit states that are part of this stutter state.
     *
     * @return the set of exit states that are part of this stutter state.
     */
    public TreeSet<State> getExitStates() {
        return exitStates;
    }

    /**
     * Returns whether this block can merge with another block.
     *
     * @param other
     * @return whether this block can merge with the given block.
     */
    public boolean canMerge(StutterState other) {
        return other != this && this.equals(other);
    }

    /**
     * Merge this stutter state with a given other stutter state that equals this stutter state.
     *
     * @param other the given stutter state.
     * @return true if the merge was successful.
     */
    public boolean merge(StutterState other) {
        boolean equals = canMerge(other);

        if (equals) {
            states.addAll(other.getSubStates());
            for (State ms : other.getSubStates())
                ((MultiState) ms).setParent(this.parent, this);
            for (State entry : other.getEntryStates())
                if (!entry.getPreviousStates().stream().allMatch(e -> ((MultiState) e).getParent(this.parent) == this))
                    entryStates.add(entry);
            for (State exit : other.getEntryStates())
                if (!exit.getNextStates().stream().allMatch(e -> ((MultiState) e).getParent(this.parent) == this))
                    entryStates.add(exit);
        }

        return equals;
    }

    public StutterState split() {
        MultiState splitter = (MultiState) exitStates.stream().filter(s -> ((MultiState) s).isSplitter(this.parent)).findFirst().orElse(null);
        StutterState newparent = null;

        if (splitter != null) {
            TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());
            ap.addAll(this.atomicPropositions);
            newparent = new StutterState(ap, this.parent);

            splitter.updatePreviousParents(this.parent, this, newparent);
            newparent.addExitState(splitter);

            for (State s : newparent.getSubStates())
                if (s.getPreviousStates().stream().anyMatch(state -> ((MultiState) s).getParent(this.parent) != this))
                    newparent.addEntryState(s);
        }

        return newparent;
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
