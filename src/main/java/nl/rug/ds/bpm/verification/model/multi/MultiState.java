package nl.rug.ds.bpm.verification.model.multi;


import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements a Kripke state with multiple parent stutter states.
 */
public class MultiState extends KripkeState {
    private final Map<SubStructure, StutterState> parents;

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
     * @param stutterState the parent to assign.
     */
    public void setParent(SubStructure subStructure, StutterState stutterState) {
        stutterState.addSubState(this);

        StutterState current = parents.get(subStructure);
        if (current != null)
            current.removeSubState(this);

        parents.put(subStructure, stutterState);
    }

    /**
     * Returns the parentof this state within the given substructure.
     *
     * @param subStructure the SubStructure the returned parent belongs to.
     * @return the parent StutterState.
     */
    public StutterState getParent(SubStructure subStructure) {
        return parents.get(subStructure);
    }

    /**
     * Updates the parent of this and all next states within the given substructure while they belong to current.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param current      the currently assigned parent.
     * @param newparent    the parent to assign.
     */
    public void updateNextParents(SubStructure subStructure, StutterState current, StutterState newparent) {
        if (parents.get(subStructure) == current) {
            setParent(subStructure, newparent);

            for (State next : getNextStates())
                ((MultiState) next).updateNextParents(subStructure, current, newparent);
        }
    }

    /**
     * Updates the parent of this and all previous states within the given substructure while they belong to current.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param current      the currently assigned parent.
     * @param newparent    the parent to assign.
     */
    public void updatePreviousParents(SubStructure subStructure, StutterState current, StutterState newparent) {
        if (parents.get(subStructure) == current) {
            setParent(subStructure, newparent);

            for (State previous : getPreviousStates())
                ((MultiState) previous).updatePreviousParents(subStructure, current, newparent);
        }
    }

    /**
     * Returns whether this state exists in a loop within the same stutter state of the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff this state exists in a loop within the same stutter state of the given substructure.
     */
    public boolean isInLoop(SubStructure subStructure) {
        return isInLoop(subStructure, this);
    }

    /**
     * Recursive step for isInLoop(SubStructure subStructure)
     */
    protected boolean isInLoop(SubStructure subStructure, MultiState state) {
        boolean isLoop = false;

        Iterator<State> nextStates = getNextStates().iterator();
        while (!isLoop && nextStates.hasNext()) {
            MultiState n = (MultiState) nextStates.next();
            isLoop = n.getParent(subStructure) == state.getParent(subStructure) && (state == n || n.isInLoop(subStructure, state));
        }

        return isLoop;
    }

    /**
     * Returns the set of parents of the next states for the given substructure.
     *
     * @param subStructure the substructure the returned parents should belong to.
     * @return Set of StutterStates.
     */
    public Set<StutterState> getNextParents(SubStructure subStructure) {
        return nextStates.stream().map(s -> ((MultiState) s).getParent(subStructure)).collect(Collectors.toSet());
    }

    /**
     * Returns whether this state is a splitter of its parent within the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff this an exit state of its parent and its next parents are disjoint from the other exit states.
     */
    public boolean isSplitter(SubStructure subStructure) {
        StutterState parent = getParent(subStructure);
        Set<StutterState> otherParents = parent.getExitStates().stream().filter(s -> s != this).flatMap(s -> ((MultiState) s).getNextParents(subStructure).stream()).collect(Collectors.toSet());

        return parent.getExitStates().contains(this) && !otherParents.isEmpty() && Collections.disjoint(this.getNextParents(subStructure), otherParents);
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + parents.values().stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
    }
}
