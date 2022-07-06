package nl.rug.ds.bpm.verification.model.multi.postprocess.stutter;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.multi.Block;
import nl.rug.ds.bpm.verification.model.multi.MultiState;
import nl.rug.ds.bpm.verification.model.multi.SubStructure;

import java.util.*;
import java.util.concurrent.RecursiveAction;

/**
 * Class that creates and initiates stutter equivalence actions for a set of substructures in parallel.
 */
public class MultiStutterMergeSplitAction extends RecursiveAction {
    private SubStructure subStructure;

    /**
     * Creates a RecursiveAction that initiates stutter equivalence actions for a given set of substructures.
     *
     * @param subStructures the given set of substructures.
     */
    public MultiStutterMergeSplitAction(Set<SubStructure> subStructures) {
        computeInitial(subStructures);
    }

    /**
     * Creates a RecursiveAction that to calculate stutter equivalence for a given substructure.
     *
     * @param subStructure the given set of substructure.
     */
    public MultiStutterMergeSplitAction(SubStructure subStructure) {
        this.subStructure = subStructure;
    }

    private void computeInitial(Set<SubStructure> subStructures) {
        Set<MultiStutterMergeSplitAction> actions = new HashSet<>();

        for (SubStructure subStructure : subStructures)
            actions.add(new MultiStutterMergeSplitAction(subStructure));

        invokeAll(actions);
    }

    @Override
    protected void compute() {
        initialize();
        merge();
        split();
        connect();
    }

    /**
     * Initializes the blocks as per Groote's stutter equivalence algorithm.
     */
    private void initialize() {
        for (Block block : subStructure.getStates()) {
            block.initialize();
            Logger.log("Block " + block, LogEvent.DEBUG);
        }
    }

    /**
     * Merges and re-initializes any connected blocks that are equal.
     */
    private void merge() {
        Set<Block> remove = new TreeSet<>(new ComparableComparator<>());

        for (Block block : subStructure.getStates()) {
            if (block != null && !remove.contains(block)) {
                boolean merged = false;
                for (Block next : block.getNextParents()) {
                    if (block.canMerge(next)) {

                        Logger.log("Merging stutter blocks " + block + " and " + next, LogEvent.DEBUG);

                        merged = true;
                        block.merge(next);
                        block.initialize();
                        remove.add(next);
                    }
                }
                if (merged)
                    block.initialize();
            }
        }

        subStructure.getStates().removeAll(remove);
    }

    /**
     * Splits blocks according to Groote's stutter equivalence algorithm.
     */
    private void split() {
        List<Block> toBeProcessed = new LinkedList<>(subStructure.getStates());
        List<Block> stable = new LinkedList<>();
        List<Block> BL = new LinkedList<>();

        while (!toBeProcessed.isEmpty()) {
            Logger.log("Checking stutter block (" + (1 + stable.size()) + "/" + (toBeProcessed.size() + stable.size()) + ") for any required splits", LogEvent.VERBOSE);

            Block bAccent = toBeProcessed.get(0);
            // Scan incoming relations
            for (MultiState entryState : bAccent.getEntryStates()) {
                // Take start state and raise its flag
                entryState.setFlag(subStructure, true);
                // Test state's block flag, raise and add to BL if not raised
                Block parent = entryState.getParent(subStructure);
                if (!parent.getFlag()) {
                    parent.setFlag(true);
                    BL.add(parent);
                }
            }

            // Scan BL
            for (Block b : BL) {
                boolean isSplitter = false;
                Iterator<MultiState> i = b.getExitStates().iterator();
                while (i.hasNext() && !isSplitter)
                    isSplitter = !i.next().getFlag(subStructure);

                if (isSplitter) {
                    if (!toBeProcessed.remove(b))
                        stable.remove(b);

                    Block b2 = b.split();
                    toBeProcessed.add(b);
                    toBeProcessed.add(b2);

                    b.initialize();

                    //if additional bottom states are created, clear stable
                    if (b2.initialize()) {
                        toBeProcessed.addAll(stable);
                        stable.clear();
                    }

                    Logger.log("Split stutter block into block " + b + " and block " + b2 + " state(s))", LogEvent.DEBUG);
                }
            }
            BL.clear();

            //reset flags
            for (MultiState entryState : bAccent.getEntryStates()) {
                entryState.setFlag(subStructure, false);
                entryState.getParent(subStructure).setFlag(false);
            }

            //move to stable
            stable.add(bAccent);
            toBeProcessed.remove(bAccent);
        }
    }

    /**
     * Connects blocks using the connectivity of its substates as well as strongly connected components.
     */
    private void connect() {
        for (Block current : subStructure.getStates()) {
            for (Block next : current.getNextParents()) {
                current.addNext(next);
                next.addPrevious(current);
            }
            if (current.containsCycle()) {
                current.addNext(current);
                current.addPrevious(current);
            }
        }

        for (MultiState init : subStructure.getInitialSubStates()) {
            try {
                subStructure.addInitial(init.getParent(subStructure));
            } catch (ConverterException e) {
                Logger.log("Failed to add block as initial state.", LogEvent.ERROR);
            }
        }

        Block safety = subStructure.createParent(subStructure.getAtomicPropositions());
        safety.addNext(safety);
        safety.addPrevious(safety);
    }
}
