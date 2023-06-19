package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.comparator.ReverseStateIdComparator;
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
    protected TreeSet<MultiState> states, entryStates, exitStates; //aka nonbottom, entry, and bottom states.
    protected Partition partition;
    protected boolean flag;
    protected int scc = -1;
    protected int sccid = 0;


    /**
     * Creates a block.
     *
     * @param atomicPropositions the atomic propositions that hold in this state.
     * @param partition          the substructure this block belongs to.
     */
    public Block(Set<String> atomicPropositions, Partition partition) {
        super(atomicPropositions);

        flag = false;

        states = new TreeSet<>(new ReverseStateIdComparator());
        entryStates = new TreeSet<>(new ReverseStateIdComparator());
        exitStates = new TreeSet<>(new ReverseStateIdComparator());

        this.partition = partition;
    }

    @Override
    public void setId() {
        lock.lock();
        try {
            this.idNumber = stateID++;
            this.id = "B" + idNumber;
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
    public Set<MultiState> getSubStates() {
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
    public Set<MultiState> getExitStates() {
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
    public Set<MultiState> getEntryStates() {
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
        scc = -1;
        entryStates.clear();
        boolean foundNew = false;

        Iterator<MultiState> iterator = states.iterator();
        while (iterator.hasNext()) {
            MultiState s = iterator.next();
            s.setFlag(partition, false);
            boolean isBottom = true;

            Iterator<MultiState> nexts = s.getNextStates(partition).iterator();
            while (nexts.hasNext() && isBottom) {
                MultiState next = nexts.next();
                Block nextBlock = next.getParent(partition);
                if (s != next && nextBlock == this)
                    isBottom = false;
            }

            for (MultiState previous : s.getPreviousStates(partition))
                if (previous.getParent(partition) != this)
                    entryStates.add(previous);

            if (isBottom) {
                if (!exitStates.contains(s)) {
                    foundNew = true;
                    exitStates.add(s);
                }
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
            for (MultiState ms : other.getSubStates()) {
                ms.setParent(this.partition, this);
                states.add(ms);
            }
            for (MultiState ms : other.getExitStates()) {
                ms.setParent(this.partition, this);
                states.add(ms);
            }

            states.addAll(exitStates);

            exitStates.clear();
            entryStates.clear();

            // reset the scc count
            scc = -1;
        }
    }

    /**
     * Splits off the states without raised flags and forms them into a new block, using Groote's algorithm.
     *
     * @return the new block with split off states.
     */
    public Block split() {
        Set<MultiState> thisBottom = new TreeSet<>(new ReverseStateIdComparator());
        Set<MultiState> thisNonBottom = new TreeSet<>(new ReverseStateIdComparator());
        Set<MultiState> otherBottom = new TreeSet<>(new ReverseStateIdComparator());
        Set<MultiState> otherNonBottom = new TreeSet<>(new ReverseStateIdComparator());

        for (MultiState b : exitStates) {
            if (b.getFlag(partition))
                thisBottom.add(b);
            else
                otherBottom.add(b);
        }

        //if flag down and next in bot or nonbot, add to nonbot
        //The set is reverse ordered, making sure that states are split, beginning at exit states with raised flags, in reverse order.
        for (MultiState nb : states) {
            if (!nb.getFlag(partition)) {
                boolean isB2 = true;
                Iterator<MultiState> next = nb.getNextStates(partition).iterator();
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
        Block other = partition.createParent(atomicPropositions);

        for (MultiState state : otherBottom) {
            state.setParent(partition, other);
            other.addExitState(state);
        }

        for (MultiState state : otherNonBottom) {
            state.setParent(partition, other);
            other.addSubState(state);
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
        return exitStates.stream().anyMatch(state -> state.getNextStates(partition).contains(state))
                || states.stream().anyMatch(state -> state.getNextStates(partition).contains(state))
                || getScc() < (states.size() + exitStates.size());
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

        int[] ids = new int[states.size() + exitStates.size()];
        int[] low = new int[states.size() + exitStates.size()];

        sccid = 0;
        scc = 0;

        for (int i = 0; i < states.size() + exitStates.size(); i++) {
            ids[i] = -1; //-1 equals unvisited
            low[i] = -1;
        }

        Iterator<MultiState> statesIterator = states.iterator();
        Iterator<MultiState> exitIterator = exitStates.iterator();

        // To prevent having to copy the set into a list to have access to get and indexOf methods, we pass both a state and an index to dfsSCC.
        // We obtain the state from the concatenation of the states and exitstates iterators.
        for (int i = 0; i < states.size() + exitStates.size(); i++) {
            MultiState state = (statesIterator.hasNext() ? statesIterator.next() : exitIterator.next());
            if (ids[i] == -1)
                dfsSCC(state, i, ids, low, stack);
        }

        // Reset flags
        for (MultiState state : states)
            state.setFlag(partition, false);
        for (MultiState state : exitStates)
            state.setFlag(partition, false);

        if (Logger.getLogLevel() == LogEvent.DEBUG)
            Logger.log("Block " + this.getId() + " SCC ids = " + Arrays.toString(ids) + " low = " + Arrays.toString(low), LogEvent.DEBUG);
    }

    private void dfsSCC(MultiState state, int at, int[] ids, int[] low, Stack<MultiState> stack) {
        ids[at] = sccid;
        low[at] = sccid;
        sccid++;

        stack.push(state);
        state.setFlag(partition, true);

        for (MultiState next : state.getNextStates(partition)) {
            if (next.getParent(partition) == this) { //only include states within this block
                int to = joinedIndexOf(next);
                if (ids[to] == -1) {
                    dfsSCC(next, to, ids, low, stack);
                    low[at] = Math.min(low[at], low[to]);
                } else if (next.getFlag(partition))
                    low[at] = Math.min(low[at], ids[to]);
            }
        }

        if (ids[at] == low[at]) {
            MultiState p = null;
            while (!stack.isEmpty() && p != state) {
                p = stack.pop();
                p.setFlag(partition, false);
            }
            scc++;
        }
    }

    /**
     * Method to obtain the index of a given state within the concatenation of the set states and the set exitstates.
     * Sets don't have an indexOf method. To not have to copy the set to a list to calculate SCC, we use the headset
     * method.
     *
     * @param state the given state of which we would like the index.
     * @return the index of the state.
     */
    private int joinedIndexOf(MultiState state) {
        int index = -1;

        SortedSet<MultiState> stateshead = states.headSet(state, true);
        if (!stateshead.isEmpty() && stateshead.last() == state)
            index = stateshead.size() - 1;
        else {
            SortedSet<MultiState> exithead = exitStates.headSet(state, true);
            if (!exithead.isEmpty() && exithead.last() == state)
                index = states.size() + exithead.size() - 1;
        }

        return index;
    }

    /**
     * Returns the set of blocks that are reachable from the states of this block, excluding this.
     *
     * @return a set of blocks.
     */
    public Set<Block> getNextParents() {
        Set<Block> next = new TreeSet<>(new ComparableComparator<>());
        next.addAll(exitStates.stream().flatMap(state -> state.getNextStates(partition).stream()).map(state -> state.getParent(partition)).filter(block -> block != this).collect(Collectors.toSet()));
        next.addAll(states.stream().flatMap(state -> state.getNextStates(partition).stream()).map(state -> state.getParent(partition)).filter(block -> block != this).collect(Collectors.toSet()));
        return next;
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
        return partition.getId() + " " + id + ": {" + hash + " | " + states.stream().map(State::getId).collect(Collectors.joining(",")) + " + " + exitStates.stream().map(State::getId).collect(Collectors.joining(",")) + " }";
    }
}
