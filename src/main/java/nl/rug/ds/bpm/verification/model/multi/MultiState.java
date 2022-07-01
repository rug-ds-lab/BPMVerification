package nl.rug.ds.bpm.verification.model.multi;


import nl.rug.ds.bpm.verification.model.generic.MarkedState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements a Kripke state with multiple parent stutter states.
 */
public class MultiState extends MarkedState<MultiState> {
    private final Map<SubStructure, Block> parents;
    private boolean inLoop = false;

    /**
     * Creates a multi state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        parents = new HashMap<>();
    }

    /**
     * Creates a multi state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(String marking, Set<String> atomicPropositions) {
        super(marking, atomicPropositions);
        parents = new HashMap<>();
    }

    /**
     * Assigns the given StutterState as the parent of this state within the given substructure.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param block        the parent to assign.
     */
    public void setParent(SubStructure subStructure, Block block) {
        block.addSubState(this);

        parents.put(subStructure, block);
    }

    /**
     * Returns the parentof this state within the given substructure.
     *
     * @param subStructure the SubStructure the returned parent belongs to.
     * @return the parent StutterState.
     */
    public Block getParent(SubStructure subStructure) {
        return parents.get(subStructure);
    }

    /**
     * Updates the parent of this and all previous states within the given substructure while
     * they belong to current and have no other reachable parents.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param current      the currently assigned parent.
     * @param newparent    the parent to assign.
     */
    public void updatePreviousParents(SubStructure subStructure, Block current, Block newparent) {
        if (parents.get(subStructure) == current && getNextParents(subStructure).stream().allMatch(p -> p == newparent)) {
            setParent(subStructure, newparent);
            current.removeSubState(this);

            for (MultiState previous : getPreviousStates())
                previous.updatePreviousParents(subStructure, current, newparent);
        }
    }

    /**
     * Returns
     *
     * @return
     */
    public boolean isInLoop() {
        return inLoop;
    }

    /**
     * Returns whether this state exists in a loop within the same stutter state of the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff this state exists in a loop within the same stutter state of the given substructure.
     */
    public synchronized boolean isInLoop(SubStructure subStructure) {
        return inLoop || isInLoop(subStructure, this);
    }

    /**
     * Recursive step for isInLoop(SubStructure subStructure)
     */
    protected synchronized boolean isInLoop(SubStructure subStructure, MultiState state) {
        Iterator<MultiState> nextStates = getNextStates().iterator();
        while (!inLoop && nextStates.hasNext()) {
            MultiState n = nextStates.next();
            inLoop = n.getParent(subStructure) == state.getParent(subStructure) && (state == n || n.isInLoop(subStructure, state));
        }

        return inLoop;
    }

    /**
     * Returns the set of parents of the next states for the given substructure.
     *
     * @param subStructure the substructure the returned parents should belong to.
     * @return Set of StutterStates.
     */
    public synchronized Set<Block> getNextParents(SubStructure subStructure) {
        return nextStates.stream().filter(s -> s != this).map(s -> ((MultiState) s).getParent(subStructure)).collect(Collectors.toSet());
    }

    /**
     * Returns whether this state is a splitter of its parent within the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff this an exit state of its parent and its next parents are disjoint from the other exit states.
     */
    public boolean isSplitter(SubStructure subStructure) {
        Block parent = getParent(subStructure);
        Set<Block> otherParents = parent.getExitStates().stream().filter(s -> s != this).flatMap(s -> ((MultiState) s).getNextParents(subStructure).stream()).collect(Collectors.toSet());

        return parent.getExitStates().contains(this) && !otherParents.isEmpty() && Collections.disjoint(this.getNextParents(subStructure), otherParents);
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + parents.values().stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
    }
}
