package nl.rug.ds.bpm.verification.model.kripke.optimizer.stutter;

import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;

import java.util.*;

/**
 * Created by Heerko Groefsema on 06-Mar-17.
 */
public class StutterOptimizer {
	private int count, eventCount;
	private KripkeStructure kripke;
	private Set<KripkeState> stutterStates;
	private List<Block> toBeProcessed, stable, BL;

	public StutterOptimizer(KripkeStructure kripke) {
		this.kripke = kripke;

		toBeProcessed = new LinkedList<>();
		stable = new LinkedList<>();
		BL = new LinkedList<>();

		count = 0;
		eventCount = 160000;
		stutterStates = new HashSet<KripkeState>();
	}
	
	public int optimize() {
		while(!toBeProcessed.isEmpty()) {
			Logger.log("Checking stutter block (" + (1 + BL.size() + stable.size()) + "/" + (toBeProcessed.size() + BL.size() + stable.size()) + ") for any required splits", LogEvent.VERBOSE);
			Block bAccent = toBeProcessed.get(0);
			// Scan incoming relations
			for (KripkeState entryState : bAccent.getEntry()) {
				// Take start state and raise its flag
				entryState.setFlag(true);
				// Test state's block flag, raise and add to BL if not raised
				if (!entryState.getBlock().getFlag()) {
					BL.add(entryState.getBlock());
					entryState.getBlock().setFlag(true);
				}
			}
			
			// Scan BL
			for(Block b: BL) {
				boolean isSplitter = false;
				Iterator<KripkeState> i = b.getBottom().iterator();
				while (i.hasNext() && !isSplitter)
					isSplitter = !i.next().getFlag();
				
				if(isSplitter) {
					Logger.log("Splitting stutter block " + b + " with " + b.size() + " state(s))", LogEvent.DEBUG);

					toBeProcessed.remove(b);
					stable.remove(b);
					
					Block b2 = b.split();
					toBeProcessed.add(b);
					toBeProcessed.add(b2);

					b.reinit();


					//if additional bottom states are created, clear stable
					if(b2.reinit()) {
						toBeProcessed.addAll(stable);
						stable.clear();
					}

					Logger.log("Split stutter block into block " + b + " and block " + b2 + " state(s))", LogEvent.DEBUG);
				}
			}
			BL.clear();
			
			//reset flags
			for (KripkeState entryState : bAccent.getEntry()) {
				entryState.setFlag(false);
				entryState.getBlock().setFlag(false);
			}
			
			//move to stable
			stable.add(bAccent);
			toBeProcessed.remove(bAccent);
		}

		//merge blocks with size > 1
		for(Block b: stable) {
			if(b.size() > 1) {
				Logger.log("Merging states in stutter block with size " + (b.getBottom().size() + b.getNonbottom().size()), LogEvent.VERBOSE);

				Set<KripkeState> previous = new HashSet<KripkeState>();
				Set<KripkeState> next = new HashSet<KripkeState>();
				KripkeState s = null;

				for (KripkeState n : b.getBottom()) {
					for (State state : n.getPreviousStates()) {
						KripkeState prev = (KripkeState) state;
						if (prev.getBlock() != b) {
							previous.add(prev);
							prev.getNextStates().remove(n);
						}
					}

					for (State state : n.getNextStates()) {
						KripkeState ne = (KripkeState) state;
						if (ne.getBlock() != b) {
							next.add(ne);
							ne.getPreviousStates().remove(n);
						}
					}

					if (s == null)
						s = n;
					else
						stutterStates.add(n);
				}

				for (KripkeState n : b.getNonbottom()) {
					for (State state : n.getPreviousStates()) {
						KripkeState prev = (KripkeState) state;
						if (prev.getBlock() != b) {
							previous.add(prev);
							prev.getNextStates().remove(n);
						}
					}

					for (State state : n.getNextStates()) {
						KripkeState ne = (KripkeState) state;
						if (ne.getBlock() != b) {
							next.add(ne);
							ne.getPreviousStates().remove(n);
						}
					}

					if (s == null)
						s = n;
					else
						stutterStates.add(n);
				}

				if(s != null) {
					for (KripkeState p : previous)
						p.addNext(s);

					for (KripkeState n : next)
						n.addPrevious(s);

					s.getNextStates().clear();
					s.getNextStates().addAll(next);

					s.getPreviousStates().clear();
					s.getPreviousStates().addAll(previous);

					//if block contains initial states, remove them and add the stutter state
					Set<KripkeState> initRem = new HashSet<>();
					for (State state : kripke.getInitial()) {
						KripkeState initial = (KripkeState) state;
						if (initial.getBlock() == b)
							initRem.add(initial);
					}

					if (!initRem.isEmpty()) {
						kripke.getInitial().removeAll(initRem);
						kripke.getInitial().add(s);
					}
				}
			}
		}

		kripke.getStates().removeAll(stutterStates);

		for (State z: kripke.getStates())
			((KripkeState) z).resetBlock();

		return stutterStates.size();
	}



	public void linearPreProcess() {
		SortedMap<String, Block> blocks = new TreeMap<>(new StringComparator());

		for (State state : kripke.getStates()) {
			KripkeState s = (KripkeState) state;

			Block b = blocks.get(s.APHash());
			if (b == null) {
				b = new Block();
				blocks.put(s.APHash(), b);
				toBeProcessed.add(b);
			}
			b.addState(s);
			s.setBlock(b);
		}

		for (State state : kripke.getSinkStates()) {
			KripkeState sink = (KripkeState) state;
			Block b = new Block();
			sink.getBlock().getNonbottom().remove(sink);
			sink.setBlock(b);
			b.addState(sink);

			toBeProcessed.add(b);
		}

		for (Block b : toBeProcessed) {
			if (!b.getNonbottom().isEmpty()) {
				b.init();
				Logger.log("Created stutter block with " + b.size() + " state(s))", LogEvent.VERBOSE);
			}
		}
	}
	
	public void treeSearchPreProcess() {
		for (State state : kripke.getInitial()) {
			KripkeState s = (KripkeState) state;
			Block b = new Block();
			b.addState(s);
			s.setBlock(b);
			toBeProcessed.add(b);

			count++;
			treeSearchPreProcess(s);
		}

		for (State state : kripke.getSinkStates()) {
			KripkeState sink = (KripkeState) state;
			Block b = new Block();
			sink.getBlock().getNonbottom().remove(sink);
			sink.setBlock(b);
			b.addState(sink);
			toBeProcessed.add(b);
		}

		for (Block b : toBeProcessed) {
			b.init();
			Logger.log("Created stutter block with " + b.size() + " state(s))", LogEvent.DEBUG);
		}
	}

	private void treeSearchPreProcess(KripkeState s) {
		//Set<State> toPartion = new HashSet<>();

		for (State state : s.getNextStates()) {
			KripkeState next = (KripkeState) state;
			if (next.getBlock() == null) {
				if (s.atomicPropositionsEquals(next)) {
					s.getBlock().addState(next);
					next.setBlock(s.getBlock());
				} else {
					Block b = new Block();
					b.addState(next);
					next.setBlock(b);
					toBeProcessed.add(b);
				}
				//toPartion.add(next);
				count++;
				if (count >= eventCount) {
					Logger.log("Partitioning states into stutter blocks (at " + count + " states)", LogEvent.VERBOSE);
					eventCount += 160000;
				}
				
				treeSearchPreProcess(next);
			}
			else {
				if (s.atomicPropositionsEquals(next) && s.getBlock() != next.getBlock()) {
					Block merge = next.getBlock();
					toBeProcessed.remove(merge);
					s.getBlock().merge(merge);
				}
				//else already preprocessed correctly
			}
		}
		//for (State next: toPartion)
			//treeSearchPreProcess(next);
	}
	
	public String toString(boolean fullOutput) {
		StringBuilder sb = new StringBuilder();
		sb.append("Reduction of " + stutterStates.size() + " states");
		if (fullOutput) {
			sb.append("\nUnstable Block Partitions:\n");
			for (Block b: toBeProcessed)
				sb.append(b.toString() + "\n");
			sb.append("Stable Block Partitions:\n");
			for (Block b: stable)
				sb.append(b.toString() + "\n");
		}
		
		return sb.toString();
	}

	public Set<KripkeState> getStutterStates() {
		return stutterStates;
	}
	
	public String toString() {
		return toString(true);
	}
}
