package nl.rug.ds.bpm.verification.model.kripke;

import nl.rug.ds.bpm.verification.model.generic.AbstractState;
import nl.rug.ds.bpm.verification.model.kripke.optimizer.stutter.Block;

import java.util.Set;

/**
 * Class that implements a state of a Kripke structure transition system.
 */
public class KripkeState extends AbstractState {
    private String marking;
    private String APHash;

    //stutter variables
    private boolean flag = false;
    private Block block;


    /**
     * Creates a Kripke structure state.
     *
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     */
    public KripkeState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        APHash = hash;
    }

    /**
     * Creates a Kripke structure state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     */
    public KripkeState(String marking, Set<String> atomicPropositions) {
        this(atomicPropositions);
        this.marking = marking;
        hash = marking + "=" + APHash;
    }

    /**
     * Returns the hash of the atomic propositions.
     *
     * @return the hash of the atomic propositions.
     */
    public String APHash() {
        return APHash;
    }

    @Override
    public void removeAtomicPropositions(Set<String> APs) {
        atomicPropositions.removeAll(APs);

        APHash = String.join("", atomicPropositions);
        hash = marking + "=" + APHash;
    }

    @Override
    public void addAtomicPropositions(Set<String> APs) {
        atomicPropositions.addAll(APs);

        APHash = String.join("", atomicPropositions);
        hash = marking + "=" + APHash;
    }

    /**
     * Returns true if the same set of atomic propositions hold in this state and the given Kripke state.
     *
     * @param o the given Kripke state.
     * @return true if the same set of atomic propositions hold in this state and the given Kripke state.
     */
    public boolean atomicPropositionsEquals(KripkeState o) {
        return APHash.equals(o.APHash());
    }

    /**
     * Returns the stutter flag of the KripkeState.
     *
     * @return true if the flag is set.
     */
    public boolean getFlag() {
        return flag;
    }

    /**
     * Sets the stutter flag of the KripkeState.
     *
     * @param flag boolean signifying whether the flag is set or not.
     */
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    /**
     * Resets the stutter Block this state belongs to.
     */
    public void resetBlock() {
        block = null;
    }

    /**
     * Gets the stutter Block this state belongs to
     *
     * @return the Block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Sets the stutter Block this state belongs to.
     *
     * @param block the Block.
     */
    public void setBlock(Block block) {
        this.block = block;
    }
}
