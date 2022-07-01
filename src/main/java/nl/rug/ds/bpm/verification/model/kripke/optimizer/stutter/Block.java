package nl.rug.ds.bpm.verification.model.kripke.optimizer.stutter;

import nl.rug.ds.bpm.verification.model.kripke.KripkeState;

import java.util.*;

/**
 * Created by Heerko Groefsema on 09-Mar-17.
 */
public class Block {
    private boolean flag;
    private List<KripkeState> bottom, nonbottom, entry;

    public Block() {
        flag = false;
        bottom = new ArrayList<>();
        nonbottom = new ArrayList<>();
        entry = new ArrayList<>();
    }

    public Block(List<KripkeState> bottom, List<KripkeState> nonbottom) {
        flag = false;
        this.bottom = bottom;
        this.nonbottom = nonbottom;
        entry = new ArrayList<>();
    }

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

    public List<KripkeState> getBottom() {
        return bottom;
    }

    public List<KripkeState> getNonbottom() {
        return nonbottom;
    }

    public List<KripkeState> getEntry() {
        return entry;
    }

    public void addState(KripkeState state) {
        nonbottom.add(state);
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public int size() {
        return nonbottom.size() + bottom.size();
    }

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
