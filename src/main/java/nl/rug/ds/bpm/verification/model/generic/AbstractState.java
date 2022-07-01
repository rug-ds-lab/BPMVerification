package nl.rug.ds.bpm.verification.model.generic;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.State;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An abstract state of a transition system.
 */
public abstract class AbstractState<S extends State<S>> implements State<S> {
    protected static ReentrantLock lock = new ReentrantLock();
    protected static long stateID = 0;
    protected String id;
    protected String hash;
    protected Set<String> atomicPropositions;
    protected Set<S> nextStates, previousStates;

    /**
     * Creates an abstract state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public AbstractState(Set<String> atomicPropositions) {
        this.atomicPropositions = new TreeSet<String>(new ComparableComparator<String>());
        this.atomicPropositions.addAll(atomicPropositions);

        hash = String.join("", atomicPropositions);

        nextStates = new HashSet<>();
        previousStates = new HashSet<>();
        setId();
    }

    /**
     * Sets the state ID to start counting from.
     *
     * @param start the ID to start counting from.
     */
    public static void setStartID(long start) {
        lock.lock();
        try {
            stateID = start;
        } catch (Exception e) {
            Logger.log("Failed to write state ID.", LogEvent.ERROR);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the state ID to start counting from to 0.
     */
    public static void resetStartID() {
        setStartID(0);
    }

    /**
     * Sets the unique id of the state.
     */
    public void setId() {
        lock.lock();
        try {
            this.id = "S" + stateID++;
        } catch (Exception e) {
            Logger.log("Failed to write state ID.", LogEvent.ERROR);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the unique id of this state.
     *
     * @return the unique id of this state.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the hash code representing this state.
     *
     * @return the hash code representing this state.
     */
    public String hash() {
        return hash;
    }

    /**
     * Returns the set of atomic proportions that hold in this state.
     *
     * @return the set of atomic proportions that hold in this state.
     */
    public Set<String> getAtomicPropositions() {
        return atomicPropositions;
    }

    /**
     * Removes a set of atomic propositions from holding in this state and recreates hashes.
     *
     * @param APs the set of atomic propositions to remove.
     */
    public void removeAtomicPropositions(Set<String> APs) {
        atomicPropositions.removeAll(APs);
        hash = String.join("", atomicPropositions);
    }

    /**
     * Adds a set of atomic propositions that hold in this state and recreates hashes.
     *
     * @param APs the set of atomic propositions to add.
     */
    public void addAtomicPropositions(Set<String> APs) {
        atomicPropositions.addAll(APs);
        hash = String.join("", atomicPropositions);
    }

    /**
     * Add a state as a next state that is accessible from this state.
     *
     * @param s the next state.
     * @return true if the set of next states did not already contain the given state.
     */
    public boolean addNext(S s) {
        return nextStates.add(s);
    }

    /**
     * Add a set of states as next states that are accessible from this state.
     *
     * @param s the set of next states.
     * @return true if the set of next states changed as a result of this call.
     */
    public boolean addNext(Set<S> s) {
        return nextStates.addAll(s);
    }

    /**
     * Returns the set of next states that are accessible from this state.
     *
     * @return the set of next states that are accessible from this state.
     */
    public Set<S> getNextStates() {
        return nextStates;
    }

    /**
     * Add a state as a previous state from which this state is accessible.
     *
     * @param s the previous state.
     * @return true if the set of previous states did not already contain the given state.
     */
    public boolean addPrevious(S s) {
        return previousStates.add(s);
    }

    /**
     * Add a set of states as previous states from which this state is accessible.
     *
     * @param s the set of previous states.
     * @return true if the set of previous states changed as a result of this call.
     */
    public boolean addPrevious(Set<S> s) {
        return previousStates.addAll(s);
    }

    /**
     * Returns the set of previous states from which this state is accessible.
     *
     * @return the set of previous states from which this state is accessible.
     */
    public Set<S> getPreviousStates() {
        return previousStates;
    }

    /**
     * Returns whether this state includes itself as a reachable next state.
     *
     * @return true iff this state includes itself as a reachable next state.
     */
    public boolean isReflexive() {
        return getNextStates().stream().anyMatch(s -> s == this);
    }

    @Override
    public String toString() {
        return id + ": {" + hash + "}";
    }

    @Override
    public int compareTo(S o) {
        if (this == o)
            return 0;
        if (o == null)
            return -1;
        if (this.getClass() != o.getClass())
            return -1;
        return hash.compareTo(o.hash());
    }

    @Override
    public boolean equals(Object arg0) {
        if (this == arg0)
            return true;
        if (arg0 == null)
            return false;
        if (this.getClass() != arg0.getClass())
            return false;
        return hash.equals(((S) arg0).hash());
    }
}
