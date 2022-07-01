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
public class Block extends AbstractState<Block> {
    protected TreeSet<MultiState> states, exitStates;
    protected SubStructure subStructure;

    /**
     * Creates a stutter state.
     *
     * @param atomicPropositions the atomic propositions that hold in this state.
     * @param subStructure       the substructure this stutter state belongs to.
     */
    public Block(Set<String> atomicPropositions, SubStructure subStructure) {
        super(atomicPropositions);

        states = new TreeSet<>(new ComparableComparator<MultiState>());
        exitStates = new TreeSet<>(new ComparableComparator<MultiState>());

        this.subStructure = subStructure;
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
    public boolean addSubState(MultiState s) {
        return states.add(s);
    }


    /**
     * Add a set of states to this stutter state.
     *
     * @param s the set of states to add.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean addSubStates(Set<MultiState> s) {
        return states.addAll(s);
    }

    /**
     * Remove a state from this stutter state.
     *
     * @param s the state to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(MultiState s) {
        return states.remove(s) || exitStates.remove(s);
    }

    /**
     * Remove a set of states from this stutter state.
     *
     * @param s the set of states to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubStates(Set<MultiState> s) {
        return states.removeAll(s) || exitStates.removeAll(s);
    }

    /**
     * Returns the set of states that are part of this stutter state.
     *
     * @return the set of states that are part of this stutter state.
     */
    public Set<MultiState> getSubStates() {
        return states;
    }

    /**
     * Add an exit state to this stutter state.
     *
     * @param s the state to add.
     * @return true if this stutter state did not already contain the given state as an exit state.
     */
    public boolean addExitState(MultiState s) {
        return exitStates.add(s);
    }

    /**
     * Remove a state from the exit states of this stutter state.
     *
     * @param s the state to remove.
     * @return true if the state was removed from the exit states of this stutter state.
     */
    public boolean removeExitState(MultiState s) {
        return exitStates.remove(s);
    }

    /**
     * Returns the set of exit states that are part of this stutter state.
     *
     * @return the set of exit states that are part of this stutter state.
     */
    public TreeSet<MultiState> getExitStates() {
        return exitStates;
    }

    /**
     * Returns whether this block can merge with another block.
     *
     * @param other
     * @return whether this block can merge with the given block.
     */
    public boolean canMerge(Block other) {
        return other != this && this.equals(other);
    }

    /**
     * Merge this stutter state with a given other stutter state that equals this stutter state.
     *
     * @param other the given stutter state.
     */
    public void merge(Block other) {
        for (MultiState ms : other.getSubStates())
            ms.setParent(this.subStructure, this);

        other.getSubStates().clear();
    }

    public Block split() {
        MultiState splitter = exitStates.stream().filter(s -> (s).isSplitter(this.subStructure)).findFirst().orElse(null);

        if (splitter != null) {
            Block newparent = new Block(this.subStructure.createAtomicPropositions(this.atomicPropositions), this.subStructure);

            splitter.setParent(subStructure, newparent);
            for (MultiState prev : splitter.getPreviousStates())
                prev.updatePreviousParents(this.subStructure, this, newparent);

            for (MultiState s : getSubStates())
                for (MultiState next : s.getNextStates())
                    if (next.getParent(this.subStructure) == newparent) {
                        addExitState(s);
                        this.subStructure.addRelation(s, next);
                    }

            for (MultiState s : newparent.getSubStates())
                for (MultiState next : s.getNextStates())
                    if (next.getParent(this.subStructure) == this) {
                        newparent.addExitState(s);
                        this.subStructure.addRelation(s, next);
                    }
        }

        return (splitter == null ? this : splitter.getParent(this.subStructure));
    }

    @Override
    public int compareTo(Block o) {
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
