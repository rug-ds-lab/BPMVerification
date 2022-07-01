package nl.rug.ds.bpm.verification.model.kripke;

import nl.rug.ds.bpm.verification.model.generic.MarkedState;
import nl.rug.ds.bpm.verification.model.kripke.optimizer.stutter.Block;

import java.util.Set;

/**
 * Class that implements a state of a Kripke structure transition system.
 */
public class KripkeState extends MarkedState<KripkeState> {
    private boolean flag = false;
    private Block block;


    /**
     * Creates a Kripke structure state.
     *
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     */
    public KripkeState(Set<String> atomicPropositions) {
        super(atomicPropositions);
    }

    /**
     * Creates a Kripke structure state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions the set of atomic propositions that hold in this state.
     */
    public KripkeState(String marking, Set<String> atomicPropositions) {
        super(marking, atomicPropositions);
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
