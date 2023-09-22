package nl.rug.ds.bpm.verification.model.kripke.postprocess.stutter;

import nl.rug.ds.bpm.verification.model.kripke.KripkeState;

import java.util.*;

/**
 * Class that represents a partition.
 */
public class Block {
    private boolean flag;
    private final List<KripkeState> entry;
    private List<KripkeState> bottom;
    private List<KripkeState> nonbottom;

    /**
     * Creates an empty Block partition.
     */
    public Block() {
        flag = false;
        bottom = new ArrayList<>();
        nonbottom = new ArrayList<>();
        entry = new ArrayList<>();
    }

    /**
     * Creates a Block partition with the given bottom and nonbottom states.
     *
     * @param bottom    the bottom states assigned to the block.
     * @param nonbottom the nonbottom states assigned to the block.
     */
    public Block(List<KripkeState> bottom, List<KripkeState> nonbottom) {
        flag = false;
        this.bottom = bottom;
        this.nonbottom = nonbottom;
        entry = new ArrayList<>();
    }

    /**
     * Splits the partition into two partitions according to the flags raised on states, and returns the newly created partition.
     *
     * @return the newly created partition.
     */
    public Block split() {
        List<KripkeState> thisBottom = new ArrayList<>();
        List<KripkeState> thisNonBottom = new ArrayList<>();
        List<KripkeState> otherBottom = new ArrayList<>();
        List<KripkeState> otherNonBottom = new ArrayList<>();

        for (KripkeState b : bottom) {
            if (b.getFlag())
                thisBottom.add(b);
            else
                otherBottom.add(b);
        }

        //if flag down and next in bot or nonbot, add to nonbot
        //BSF added, so iterate back to front
        ListIterator<KripkeState> iterator = nonbottom.listIterator(nonbottom.size());
        while (iterator.hasPrevious()) {
            KripkeState nb = iterator.previous();
            if (!nb.getFlag()) {
                boolean isB2 = true;
                Iterator<KripkeState> next = nb.getNextStates().iterator();
                while (next.hasNext() && isB2) {
                    KripkeState n = next.next();
                    isB2 = otherBottom.contains(n) || otherNonBottom.contains(n);
                }

                if (isB2)
                    otherNonBottom.add(nb);
                else
                    thisNonBottom.add(nb);
            }
        }

        //split lists
        bottom = thisBottom;
        nonbottom = thisNonBottom;

        //nonbot was filled in reverse
        thisNonBottom.sort(Collections.reverseOrder());
        otherNonBottom.sort(Collections.reverseOrder());

        //keep only B1 entries
        entry.clear();
        for (KripkeState s : nonbottom) {
            for (KripkeState previous : s.getPreviousStates())
                if (previous.getBlock() != this)
                    entry.add(previous);
        }
        for (KripkeState s : bottom) {
            for (KripkeState previous : s.getPreviousStates())
                if (previous.getBlock() != this)
                    entry.add(previous);
        }

        //make B2
        Block block = new Block(otherBottom, otherNonBottom);

        for (KripkeState state : otherBottom)
            state.setBlock(block);
        for (KripkeState state : otherNonBottom)
            state.setBlock(block);

        return block;
    }

    /**
     * Merges the given partition into this partition.
     *
     * @param b the partition to be merged into this one.
     */
    public void merge(Block b) {
        for (KripkeState s : b.getNonbottom())
            s.setBlock(this);

        for (KripkeState s : b.getBottom())
            s.setBlock(this);

        nonbottom.addAll(b.getNonbottom());
        bottom.addAll(b.getBottom());
        entry.addAll(b.getEntry());

        flag = flag && b.getFlag();
        b = null;
    }

    /**
     * Initializes this partition by dividing its states into entry, bottom, and non-bottom states.
     */
    public void init() {
        Iterator<KripkeState> iterator = nonbottom.iterator();
        while (iterator.hasNext()) {
            KripkeState s = iterator.next();
            boolean isBottom = true;

            Iterator<KripkeState> i = s.getNextStates().iterator();
            while (i.hasNext() && isBottom) {
                KripkeState state = i.next();
                if (state != s && state.getBlock() == this)
                    //if(nonbottom.contains(state) || bottom.contains(state))
                    isBottom = false;
            }

            for (KripkeState previous : s.getPreviousStates())
                if (previous.getBlock() != this)
                    entry.add(previous);

            if (isBottom) {
                bottom.add(s);
                iterator.remove();
            }
        }
    }

    /**
     * Re-initializes this partition by dividing its states into entry, bottom, and non-bottom states.
     *
     * @return true iff additional bottom states were discovered.
     */
    public boolean reinit() {
        List<KripkeState> newBottom = new ArrayList<>();
        entry.clear();

        Iterator<KripkeState> iterator = nonbottom.iterator();
        while (iterator.hasNext()) {
            KripkeState s = iterator.next();
            boolean isBottom = true;

            Iterator<KripkeState> i = s.getNextStates().iterator();
            while (i.hasNext() && isBottom) {
                KripkeState state = i.next();
                if (state != s && state.getBlock() == this)
                    //if(nonbottom.contains(state) || bottom.contains(state))
                    isBottom = false;
            }

            if (isBottom) {
                newBottom.add(s);
                iterator.remove();
            }
        }

        bottom.addAll(newBottom);

        for (KripkeState s : bottom)
            for (KripkeState previous : s.getPreviousStates())
                if (previous.getBlock() != this)
                    entry.add(previous);

        //return true if new bottom states were found
        return !newBottom.isEmpty();
    }

    /**
     * Returns the list of bottom states of this partition.
     *
     * @return the list of bottom states of this partition.
     */
    public List<KripkeState> getBottom() {
        return bottom;
    }

    /**
     * Returns the list of non-bottom states of this partition.
     *
     * @return the list of non-bottom states of this partition.
     */
    public List<KripkeState> getNonbottom() {
        return nonbottom;
    }

    /**
     * Returns the list of entry states of this partition.
     *
     * @return the list of entry states of this partition.
     */
    public List<KripkeState> getEntry() {
        return entry;
    }

    /**
     * Adds a state to the partition.
     *
     * @param state the state to add.
     */
    public void addState(KripkeState state) {
        nonbottom.add(state);
    }

    /**
     * Returns the status of this partition's flag.
     *
     * @return true iff the flag is raised.
     */
    public boolean getFlag() {
        return flag;
    }

    /**
     * Sets the status of the flag of this partition.
     *
     * @param flag the status that flag is to be given.
     */
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    /**
     * Returns the number of states within th partition.
     *
     * @return the number of states within th partition.
     */
    public int size() {
        return nonbottom.size() + bottom.size();
    }

    /**
     * Returns a String representation of the partition.
     *
     * @return a String representation of the partition.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("{b: ");

        Iterator<KripkeState> bi = bottom.iterator();
        while (bi.hasNext()) {
            sb.append(bi.next().toString());
            if (bi.hasNext())
                sb.append(", ");
        }

        Iterator<KripkeState> nbi = nonbottom.iterator();
        if (nbi.hasNext())
            sb.append(" | nb: ");
        while (nbi.hasNext()) {
            sb.append(nbi.next().toString());
            if (nbi.hasNext())
                sb.append(", ");
        }

        sb.append("}");
        return sb.toString();
    }
}
