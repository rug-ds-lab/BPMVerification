package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.generic.AbstractState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements a block that contains multiple similar states.
 */
public class Block extends AbstractState<Block> {
    protected List<MultiState> states, entryStates, exitStates; //aka nonbottom, entry, and bottom states
    protected SubStructure subStructure;
    protected boolean flag;
    protected int scc = -1;
    protected int sccid = 0;


    /**
     * Creates a block.
     *
     * @param atomicPropositions the atomic propositions that hold in this state.
     * @param subStructure       the substructure this block belongs to.
     */
    public Block(Set<String> atomicPropositions, SubStructure subStructure) {
        super(atomicPropositions);

        flag = false;

        states = new ArrayList<>();
        entryStates = new ArrayList<>();
        exitStates = new ArrayList<>();

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
     * Add a state to this block.
     *
     * @param s the state to add.
     * @return true if this block did not already contain the given state.
     */
    public boolean addSubState(MultiState s) {
        return states.add(s);
    }


    /**
     * Add a set of states to this block.
     *
     * @param s the set of states to add.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean addSubState(Set<MultiState> s) {
        return states.addAll(s);
    }

    /**
     * Remove a state from this block.
     *
     * @param s the state to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(MultiState s) {
        return states.remove(s);
    }

    /**
     * Remove a set of states from this block.
     *
     * @param s the set of states to remove.
     * @return true if the set of sub-states changed as a result of this call.
     */
    public boolean removeSubState(Set<MultiState> s) {
        return states.removeAll(s);
    }

    /**
     * Returns the set of states that are part of this block.
     *
     * @return the set of states that are part of this block.
     */
    public List<MultiState> getSubStates() {
        return states;
    }

    /**
     * Add an exit state to this block.
     *
     * @param s the state to add.
     * @return true if this block did not already contain the given state as an exit state.
     */
    public boolean addExitState(MultiState s) {
        return exitStates.add(s);
    }

    /**
     * Remove a state from the exit states of this block.
     *
     * @param s the state to remove.
     * @return true if the state was removed from the exit states of this block.
     */
    public boolean removeExitState(MultiState s) {
        return exitStates.remove(s);
    }

    /**
     * Returns the set of exit states that are part of this block.
     *
     * @return the set of exit states that are part of this block.
     */
    public List<MultiState> getExitStates() {
        return exitStates;
    }

    /**
     * Add an entry state to this block.
     *
     * @param s the state to add.
     * @return true if this block did not already contain the given state as an entry state.
     */
    public boolean addEntryState(MultiState s) {
        return entryStates.add(s);
    }

    /**
     * Remove a state from the entry states of this block.
     *
     * @param s the state to remove.
     * @return true if the state was removed from the entry states of this block.
     */
    public boolean removeEntryState(MultiState s) {
        return entryStates.remove(s);
    }

    /**
     * Returns the set of entry states that are part of this block.
     *
     * @return the set of entry states that are part of this block.
     */
    public List<MultiState> getEntryStates() {
        return entryStates;
    }

    /**
     * Returns whether the flag of the stutter algorithm was raised or not.
     *
     * @return true iff the flag of the stutter algorithm was raised.
     */
    public boolean getFlag() {
        return flag;
    }

    /**
     * Sets the flag of the stutter algorithm.
     *
     * @param flag true to raise the flag, false to lower the flag.
     */
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    /**
     * Initializes the lists of entry and exit states for this block.
     *
     * @return true iff exit states were found.
     */
    public boolean initialize() {
        flag = false;
        entryStates.clear();
        boolean foundNew = false;

        Iterator<MultiState> iterator = states.iterator();
        while (iterator.hasNext()) {
            MultiState s = iterator.next();
            s.setFlag(subStructure, false);
            boolean isBottom = true;

            Iterator<MultiState> nexts = s.getNextStates(subStructure).iterator();
            while (nexts.hasNext() && isBottom) {
                MultiState next = nexts.next();
                if (s != next && next.getParent(subStructure) == this)
                    isBottom = false;
            }

            for (MultiState previous : s.getPreviousStates(subStructure))
                if (previous.getParent(subStructure) != this)
                    entryStates.add(previous);

            if (isBottom && !exitStates.contains(s)) {
                foundNew = true;
                exitStates.add(s);
                iterator.remove();
            }
        }

        return foundNew;
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
     * Merge this block with a given other block.
     *
     * @param other the given block.
     */
    public void merge(Block other) {
        if (canMerge(other)) {
            for (MultiState ms : other.getSubStates())
                ms.setParent(this.subStructure, this);
            for (MultiState ms : other.getExitStates())
                ms.setParent(this.subStructure, this);

            states.addAll(other.getSubStates());
            states.addAll(other.getExitStates());
            states.addAll(exitStates);
            exitStates.clear();
        }
    }

    /**
     * Splits off the states without raised flags and forms them into a new block, using Groote's algorithm.
     *
     * @return the new block with split off states.
     */
    public Block split() {
        List<MultiState> thisBottom = new ArrayList<>();
        List<MultiState> thisNonBottom = new ArrayList<>();
        List<MultiState> otherBottom = new ArrayList<>();
        List<MultiState> otherNonBottom = new ArrayList<>();

        for (MultiState b : exitStates) {
            if (b.getFlag(subStructure))
                thisBottom.add(b);
            else
                otherBottom.add(b);
        }

        //if flag down and next in bot or nonbot, add to nonbot
        //BSF added, so iterate back to front
        ListIterator<MultiState> iterator = states.listIterator(states.size());
        while (iterator.hasPrevious()) {
            MultiState nb = iterator.previous();
            if (!nb.getFlag(subStructure)) {
                boolean isB2 = true;
                Iterator<MultiState> next = nb.getNextStates(subStructure).iterator();
                while (next.hasNext() && isB2) {
                    MultiState n = next.next();
                    isB2 = otherBottom.contains(n) || otherNonBottom.contains(n);
                }

                if (isB2)
                    otherNonBottom.add(nb);
                else
                    thisNonBottom.add(nb);
            } else
                thisNonBottom.add(nb);
        }

        //split lists
        exitStates.retainAll(thisBottom);
        states.retainAll(thisNonBottom);

        //make the new block
        Block other = subStructure.createParent(atomicPropositions);

        for (MultiState state : otherBottom) {
            state.setParent(subStructure, other);
            other.addExitState(state);
        }

        //nonbot was filled in reverse
        ListIterator<MultiState> nbiterator = otherNonBottom.listIterator(otherNonBottom.size());
        while (nbiterator.hasPrevious()) {
            MultiState previous = nbiterator.previous();
            previous.setParent(subStructure, other);
            other.addSubState(previous);
        }

        this.setFlag(false);
        other.setFlag(false);

        return other;
    }

    /**
     * Returns whether the number of strongly connected components is smaller than the number of states,
     * indicating a loop, or whether any state is a sink.
     *
     * @return true iff this block contains a loop.
     */
    public boolean containsCycle() {
        return getScc() < (states.size() + exitStates.size()) ||
                states.stream().anyMatch(state -> state.getNextStates(subStructure).contains(state)) ||
                exitStates.stream().anyMatch(state -> state.getNextStates(subStructure).contains(state));
    }

    /**
     * Gets the number of strongly connected components within this block.
     *
     * @return the number of strongly connected components within this block
     */
    public int getScc() {
        if (scc == -1)
            tarjanSCC();
        return scc;
    }

    /**
     * Finds strongly connected components within this block using Tarjan's algorithm.
     */
    private void tarjanSCC() {
        Stack<MultiState> stack = new Stack<>();

        List<MultiState> list = new ArrayList<>();
        list.addAll(states);
        list.addAll(exitStates);

        int[] ids = new int[list.size()];
        int[] low = new int[list.size()];

        sccid = 0;
        scc = 0;

        for (int i = 0; i < list.size(); i++)
            ids[i] = -1; //-1 equals unvisited

        for (int i = 0; i < list.size(); i++)
            if (ids[i] == -1)
                dfsSCC(i, ids, low, list, stack);

        //reset flags
        for (MultiState state : list)
            state.setFlag(subStructure, false);

        Logger.log("Block " + this.getId() + " SCC ids = " + Arrays.toString(ids) + " low = " + Arrays.toString(low), LogEvent.DEBUG);
    }

    private void dfsSCC(int at, int[] ids, int[] low, List<MultiState> list, Stack<MultiState> stack) {
        MultiState state = list.get(at);

        ids[at] = sccid;
        low[at] = sccid;
        sccid++;

        stack.push(state);
        state.setFlag(subStructure, true); //re-use the stutter flag to track whether states are on the stack

        for (MultiState next : state.getNextStates(subStructure)) {
            if (next.getParent(subStructure) == this) { //only include states within this block
                int to = list.indexOf(next);
                if (ids[to] == -1)
                    dfsSCC(to, ids, low, list, stack);
                else if (next.getFlag(subStructure))
                    low[at] = Math.min(low[at], low[to]);
            }
        }

        if (ids[at] == low[at]) {
            MultiState p = null;
            while (!stack.isEmpty() && p != state) {
                p = stack.pop();
                p.setFlag(subStructure, false);
            }
            scc++;
        }
    }

    /**
     * Returns the set of blocks that are reachable from the exit states of this block, excluding this.
     *
     * @return a set of blocks.
     */
    public Set<Block> getNextParents() {
        return exitStates.stream().flatMap(state -> state.getNextStates(subStructure).stream()).map(state -> state.getParent(subStructure)).filter(block -> block != this).collect(Collectors.toSet());
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
        return id + ": {" + hash + " | " + states.stream().map(State::getId).collect(Collectors.joining(",")) + " + " + exitStates.stream().map(State::getId).collect(Collectors.joining(",")) + " }";
    }
}