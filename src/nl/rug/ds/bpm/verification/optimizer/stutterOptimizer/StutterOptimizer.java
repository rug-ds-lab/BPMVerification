package nl.rug.ds.bpm.verification.optimizer.stutterOptimizer;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.*;

/**
 * Created by Heerko Groefsema on 06-Mar-17.
 */
public class StutterOptimizer {
	private EventHandler eventHandler;
	private Kripke kripke;
	private Set<State> stutterStates;
	private List<Block> toBeProcessed, stable, BL;
	
	public StutterOptimizer(EventHandler eventHandler, Kripke kripke) {
		this.eventHandler = eventHandler;
		this.kripke = kripke;
		
		toBeProcessed = new LinkedList<>();
		stable = new LinkedList<>();
		BL = new LinkedList<>();

		stutterStates = new HashSet<State>();
	}
	
	public int optimize() {
		while(!toBeProcessed.isEmpty()) {
			Block bAccent = toBeProcessed.get(0);
			// Scan incoming relations
			for(State entryState: bAccent.getEntry()) {
				// Take start state and raise its flag
				entryState.setFlag(true);
				// Test state's block flag, raise and add to BL if not raised
				if(!entryState.getBlock().getFlag()) {
					BL.add(entryState.getBlock());
					entryState.getBlock().setFlag(true);
				}
			}
			
			// Scan BL
			for(Block b: BL) {
				boolean isSplitter = false;
				Iterator<State> i = b.getBottom().iterator();
				while (i.hasNext() && !isSplitter)
					isSplitter = !i.next().getFlag();
				
				if(isSplitter) {
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
					//System.out.println("Split: " + b.toString() + " & " + b2.toString());
				}
			}
			BL.clear();
			
			//reset flags
			for(State entryState: bAccent.getEntry()) {
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
				Iterator<State> i = b.getBottom().iterator();
				State s = i.next();  //there shouldn't exist empty blocks
				Set<State> previous = new HashSet<State>(s.getPreviousStates());
				Set<State> next = new HashSet<State>(s.getNextStates());
				while (i.hasNext()) {
					State n = i.next();
					stutterStates.add(n);
					previous.addAll(n.getPreviousStates());
					next.addAll(n.getNextStates());
				}

				Iterator<State> j = b.getNonbottom().iterator();
				while (j.hasNext()) {
					State n = j.next();
					stutterStates.add(n);
					previous.addAll(n.getPreviousStates());
					next.addAll(n.getNextStates());
				}

				previous.removeAll(b.getBottom());
				previous.removeAll(b.getNonbottom());
				next.removeAll(b.getBottom());
				next.removeAll(b.getNonbottom());

				for(State p: previous) {
					p.getNextStates().removeAll(b.getBottom());
					p.getNextStates().removeAll(b.getNonbottom());
					p.addNext(s);
				}

				for(State n: next) {
					n.getPreviousStates().removeAll(b.getBottom());
					n.getPreviousStates().removeAll(b.getNonbottom());
					n.addPrevious(s);
				}

				s.getNextStates().clear();
				s.getPreviousStates().clear();

				s.addNext(next);
				s.addPrevious(previous);
			}
		}

		kripke.getStates().removeAll(stutterStates);
		
		return stutterStates.size();
	}
	
	public int preProcess() {
		for(State s: kripke.getInitial()) {
			Block b = new Block();
			b.addState(s);
			s.setBlock(b);
			toBeProcessed.add(b);
			
			preProcessBSF(s);
		}
		
		eventHandler.logVerbose("Block init");
		for(Block b: toBeProcessed)
			b.init();
		
		return toBeProcessed.size();
	}
	
	private void preProcessBSF(State s) {
		for(State next: s.getNextStates()) {
			if(s.APequals(next) && s.getBlock().size() < 10000) {
				if(next.getBlock() == null) {
					s.getBlock().addState(next);
					next.setBlock(s.getBlock());
					
					preProcessBSF(next);
				}
				else {
					if(s.getBlock() != next.getBlock()) {
						Block merge = next.getBlock();
						toBeProcessed.remove(merge);
						s.getBlock().merge(merge);
					}
					//else both already in same block
				}
			}
			else {
				if(next.getBlock() == null) {
					Block b = new Block();
					b.addState(next);
					next.setBlock(b);
					toBeProcessed.add(b);

					preProcessBSF(next);
				}
				//else already preprocessed correctly
			}
		}
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
	
	public Set<State> getStutterStates() { return stutterStates; }
	
	public String toString() {
		return toString(true);
	}
}
