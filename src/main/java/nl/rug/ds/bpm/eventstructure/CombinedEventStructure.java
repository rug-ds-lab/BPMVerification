package nl.rug.ds.bpm.eventstructure;

import java.util.*;

public class CombinedEventStructure {
	private List<String> totalLabels;
	private Set<Integer> silents;
	private int source;
	private int sink;

	private List<PESPrefixUnfolding> pesPrefixUnfoldings;
	private int pesCount;

	// Integer = label index in totalLabels 
	// BitSet  = PESs that contain this label
	private Map<Integer, BitSet> labelmap;

	// all relations: 
	// BitSet1 = behavioral relation with fromEvent and toEvent 
	// BitSet2 = set of relation types that hold for this combination of events
	private Map<BitSet, BitSet> combinedPES;
		
	// mutual relations: 
	// BitSet = fromEvent, toEvent
	private Set<BitSet> directcausals;
	private Set<BitSet> invdirectcausals;
	private Set<BitSet> transcausals;
	private Set<BitSet> invtranscausals;
	private Set<BitSet> existcausals;
	private Set<BitSet> invexistcausals;
	private Set<BitSet> conflict;
	private Set<BitSet> concurrency;
	private Set<BitSet> loops;
	private Set<BitSet> directloops;
	private Set<BitSet> invdirectloops;
	
	private BitSet mslevents; // mutual self loop events
	
	private BitSet syncevents; 
	
	// int selfloop event, bitset PESs that have that selfloop
	private Map<Integer, BitSet> sleventmap; // self loop event map	
	
	// relations and their originating PES: 
	// BitSet1 = behavioral relation with fromEvent and toEvent
    // BitSet2 = PESs that have this relation
	private Map<BitSet, BitSet> dcmap;
	private Map<BitSet, BitSet> idcmap;
	private Map<BitSet, BitSet> tcmap;
	private Map<BitSet, BitSet> itcmap;
	private Map<BitSet, BitSet> cfmap;
	private Map<BitSet, BitSet> ccmap;
	private Map<BitSet, BitSet> lpmap;
	private Map<BitSet, BitSet> dlpmap;
	private Map<BitSet, BitSet> idlpmap;
	
	private Map<BitSet, BitSet> relmap;
	
	public CombinedEventStructure() {
		combinedPES = new HashMap<BitSet, BitSet>();
		totalLabels = new ArrayList<String>();
		silents = new HashSet<Integer>();
		labelmap = new HashMap<Integer, BitSet>();
		pesPrefixUnfoldings = new ArrayList<>();
		pesCount = 0;
		
		directcausals = new HashSet<BitSet>();
		invdirectcausals = new HashSet<BitSet>();
		transcausals = new HashSet<BitSet>();
		invtranscausals = new HashSet<BitSet>();
		existcausals = new HashSet<BitSet>();
		invexistcausals = new HashSet<BitSet>();
		conflict = new HashSet<BitSet>();
		concurrency = new HashSet<BitSet>();
		loops = new HashSet<BitSet>();
		directloops = new HashSet<BitSet>();
		invdirectloops = new HashSet<BitSet>();
		
		syncevents = new BitSet();
		
		mslevents = new BitSet();
		
		sleventmap = new HashMap<Integer, BitSet>();
		
		dcmap = new HashMap<BitSet, BitSet>();
		idcmap = new HashMap<BitSet, BitSet>();
		tcmap = new HashMap<BitSet, BitSet>();
		itcmap = new HashMap<BitSet, BitSet>();
		cfmap = new HashMap<BitSet, BitSet>();
		ccmap = new HashMap<BitSet, BitSet>();
		lpmap = new HashMap<BitSet, BitSet>();
		dlpmap = new HashMap<BitSet, BitSet>();
		idlpmap = new HashMap<BitSet, BitSet>();
		
		relmap = new HashMap<BitSet, BitSet>();
	}
	
	public void addPES(PESPrefixUnfolding pes) {
		int relation, e1, e2;
		BitSet br, causes, predecessors;
		Set<BitSet> visitedBr = new HashSet<BitSet>();

		Map<Integer, BitSet> correspondings = getCorrespondings(pes);

		pesPrefixUnfoldings.add(pes);
		pesCount = pesPrefixUnfoldings.size();

		// first add all labels
		int lbl;
		for (int i = 0; i < pes.getLabels().size(); i++) {
			if (!totalLabels.contains(pes.getLabel(i))) {
				totalLabels.add(pes.getLabel(i));
				if (pes.getLabel(i).equals("_0_")) source = totalLabels.size() - 1;
				if (pes.getLabel(i).equals("_1_")) sink = totalLabels.size() - 1;
			}
			
			lbl = totalLabels.indexOf(pes.getLabel(i));
			if (pes.getInvisibleEvents().get(i)) silents.add(lbl);
			if (!labelmap.containsKey(lbl)) labelmap.put(lbl, new BitSet());
			
			labelmap.get(lbl).set(pesCount);
		}

		// traverse cutoff traces and replace them with corresponding relations
		int corr;
		BitSet loopsucc;
		BitSet looppred;
				
		for (int cutoff = pes.getCutoffEvents().nextSetBit(0); cutoff >= 0; cutoff = pes.getCutoffEvents().nextSetBit(cutoff + 1)) {
			corr = pes.getCorrespondingEvent(cutoff);

			// if the corresponding event is before the cutoff, then we're dealing with a loop
			if ((pes.getTransitivePredecessors(cutoff).get(corr))) { // && (pes.getTransitivePredecessors(corr).get(cutoff))) {
				loopsucc = getRealSuccessors(pes, corr, new BitSet());
				if (pes.getInvisibleEvents().get(cutoff)) {
					looppred = getRealPredecessors(pes, cutoff, new BitSet());
				}
				else {
					looppred = new BitSet();
					looppred.set(cutoff);
				}
					
				for (int p = looppred.nextSetBit(0); p >= 0; p = looppred.nextSetBit(p + 1)) {
					for (int s = loopsucc.nextSetBit(0); s >= 0; s = loopsucc.nextSetBit(s + 1)) {
						e1 = totalLabels.indexOf(pes.getLabel(p));
						e2 = totalLabels.indexOf(pes.getLabel(s));
										
							// check for selfloops
						if (e1 == e2) {
							if (!sleventmap.containsKey(e1)) sleventmap.put(e1, new BitSet());
							sleventmap.get(e1).set(pesCount);
						}
						else {
							if (pes.getTransitivePredecessors(p).get(s)) addLoop(e1, e2);
						}
							
					}
				}
			}
			
			// fix direct causality of cutoff event
			pes.getDirectSuccessors(cutoff).or(pes.getDirectSuccessors(corr));
			
			// fix causality relations of cutoff event
			for (int i = 0; i < pes.getLabels().size(); i++) {
				if ((pes.getTransitiveSuccessors(corr).get(i)) && (!pes.getDirectSuccessors(cutoff).get(i))) {
					e1 = totalLabels.indexOf(pes.getLabel(cutoff));
					e2 = totalLabels.indexOf(pes.getLabel(i));
					if (e1 != e2) {
						br = hash(e1, e2);
						if (!combinedPES.containsKey(br))
							combinedPES.put(br, new BitSet(6));
						
						if (e1 < e2) {
							addRelation(br, 2);
						}
						else {
							addRelation(br, 3);
						}
						visitedBr.add(br);
					}
				}
			}
		}
		
		// fix causality relations of cutoff-preceding events
		for (int i = 0; i < pes.getLabels().size(); i++) {
			causes = getAllCausesOf(pes, correspondings, i, new BitSet());
			predecessors = getRealPredecessors(pes, i, new BitSet());
			causes.andNot(predecessors);
			for (int p = predecessors.nextSetBit(0); p >= 0; p = predecessors.nextSetBit(p + 1)) {
				if (correspondings.containsKey(p))
					causes.andNot(correspondings.get(p));
			}
			
			for (int cause = causes.nextSetBit(0); cause >= 0; cause = causes.nextSetBit(cause + 1)) {
				e1 = totalLabels.indexOf(pes.getLabel(cause));
				e2 = totalLabels.indexOf(pes.getLabel(i));
				if (e1 != e2) {
					br = hash(e1, e2);
					if (!combinedPES.containsKey(br))
						combinedPES.put(br, new BitSet(6));
					
					if (e1 < e2) {
						addRelation(br, 2);
					}
					else {
						addRelation(br, 3);
					}
					visitedBr.add(br);
				}
			}
		}

		// fill out all sets with behavioral relations 
		for (int x = 0; x < pes.getLabels().size(); x++) {
			for (int y = x + 1; y < pes.getLabels().size(); y++) {
				e1 = totalLabels.indexOf(pes.getLabel(x));
				e2 = totalLabels.indexOf(pes.getLabel(y));
				
				if (e1 != e2) {
					br = hash(e1, e2);

					if (!combinedPES.containsKey(br))
						combinedPES.put(br, new BitSet(6));
									
					if (pes.getDirectSuccessors(x).get(y)) {
						if (pes.getInvisibleEvents().get(y)) {
							BitSet realsucc = getRealSuccessors(pes, y, new BitSet());
							
							for (int yn = realsucc.nextSetBit(0); yn >= 0; yn = realsucc.nextSetBit(yn + 1)) {
								e2 = totalLabels.indexOf(pes.getLabel(yn));
								if (e2 != e1) {
									br = hash(e1, e2);
									if (!combinedPES.containsKey(br))
										combinedPES.put(br, new BitSet(6));
									
									fillInDirectCausals(e1, e2, br);
									visitedBr.add(br);
								}
							}
						}
						else {
							fillInDirectCausals(e1, e2, br);
							visitedBr.add(br);
						}					
					}
					//else
					if (pes.getDirectSuccessors(y).get(x)) {
						if (pes.getInvisibleEvents().get(x)) {
							BitSet realsucc = getRealSuccessors(pes, x, new BitSet());
							for (int yn = realsucc.nextSetBit(0); yn >= 0; yn = realsucc.nextSetBit(yn + 1)) {
								e1 = totalLabels.indexOf(pes.getLabel(yn));
								if (e2 != e1) {
									br = hash(e1, e2);
									if (!combinedPES.containsKey(br))
										combinedPES.put(br, new BitSet(6));
									
									fillInInvDirectCausals(e1, e2, br);
									visitedBr.add(br);
								}
							}
						}
						else {
							fillInInvDirectCausals(e1, e2, br);
							visitedBr.add(br);
						}
	
					}
					else {
						// CAUSALITY, INV_CAUSALITY, CONFLICT, CONCURRENCY
						if (!visitedBr.contains(br)) {
							if (e1 < e2) {
//								relation = pes.getBRelation(x, y).ordinal() + 2;
								relation = getRelation(pes, x, y);
							}
							else {
//								relation = pes.getBRelation(y, x).ordinal() + 2;
								relation = getRelation(pes, y, x);
							}
							
							if (relation < 6) {
								addRelation(br, relation);
							}
						}
					}
				}
			}
		}
		
		// fill loop map	
		for (int e = 0; e < totalLabels.size() - 1; e++) {
			for (int f = e + 1; f < totalLabels.size(); f++) {
				br = hash(e, f);
				if (((dcmap.containsKey(br) && dcmap.get(br).get(pesCount)) || (tcmap.containsKey(br) && tcmap.get(br).get(pesCount))) &&
					((idcmap.containsKey(br) && idcmap.get(br).get(pesCount)) || (itcmap.containsKey(br) && itcmap.get(br).get(pesCount)))) {
					if (!lpmap.containsKey(br)) lpmap.put(br, new BitSet());
					lpmap.get(br).set(pesCount);
				}
			}
		}
	}
	
	private int getRelation(PESPrefixUnfolding pes, int e1, int e2) {
		if (pes.getDirectSuccessors(e1).get(e2))       return 0;
		if (pes.getDirectPredecessors(e1).get(e2))     return 1;
		if (pes.getTransitiveSuccessors(e1).get(e2))   return 2;
		if (pes.getTransitivePredecessors(e1).get(e2)) return 3;
		if (pes.getConcurrency(e1).get(e2))            return 5;
		
		return 4;
	}
	
	private void addRelation(BitSet br, int relation) {
		combinedPES.get(br).set(relation);
		
		switch (relation) {
		case 2:
			if (!tcmap.containsKey(br)) tcmap.put(br, new BitSet());
			tcmap.get(br).set(pesCount);
			break;
		case 3:
			if (!itcmap.containsKey(br)) itcmap.put(br, new BitSet());
			itcmap.get(br).set(pesCount);
			break;
		case 4:
			if (!cfmap.containsKey(br)) cfmap.put(br, new BitSet());
			cfmap.get(br).set(pesCount);
			break;
		case 5:
			if (!ccmap.containsKey(br)) ccmap.put(br, new BitSet());
			ccmap.get(br).set(pesCount);
			break;
		}
		
		addRelMap(br);
	}
	
	private void addLoop(int e1, int e2) {
		BitSet br = hash(e1, e2);
		
		if (e1 < e2) {
			if (!dlpmap.containsKey(br)) dlpmap.put(br, new BitSet());
			dlpmap.get(br).set(pesCount);
		}
		else {
			if (!idlpmap.containsKey(br)) idlpmap.put(br, new BitSet());
			idlpmap.get(br).set(pesCount);
		}
	}
	
	private void fillInDirectCausals(int e1, int e2, BitSet br) {
		Map<BitSet, BitSet> curmap;
		
		if (e1 < e2) {
			combinedPES.get(br).set(0);
			curmap = dcmap;
		}
		else {
			combinedPES.get(br).set(1);
			curmap = idcmap;
		}
		
		if (!curmap.containsKey(br)) curmap.put(br, new BitSet());
		curmap.get(br).set(pesCount);
		
		addRelMap(br);
	}
	
	private void fillInInvDirectCausals(int e1, int e2, BitSet br) {
		Map<BitSet, BitSet> curmap;

		if (e1 < e2) {
			combinedPES.get(br).set(1);
			curmap = idcmap;
		}
		else {
			combinedPES.get(br).set(0);
			curmap = dcmap;
		}
		
		if (!curmap.containsKey(br)) curmap.put(br, new BitSet());
		curmap.get(br).set(pesCount);
		
		addRelMap(br);
	}
	
	private void fillInSyncEvents() {
		BitSet findcaus = new BitSet();
		
		for (BitSet conc: ccmap.keySet()) {
			for (BitSet caus: directcausals) {
				findcaus.clear();

				if (caus.nextSetBit(0) == conc.nextSetBit(0)) {
					findcaus.set(conc.previousSetBit(conc.length()));
					findcaus.set(caus.previousSetBit(caus.length()));

					if (caus.previousSetBit(caus.length()) > conc.previousSetBit(conc.length())) {
						if (directcausals.contains(findcaus)) syncevents.set(caus.previousSetBit(caus.length()));
					}
					else {
						if (invdirectcausals.contains(findcaus)) syncevents.set(caus.previousSetBit(caus.length()));
					}
				}
				else if (caus.nextSetBit(0) == conc.previousSetBit(conc.length())) {
					findcaus.set(conc.nextSetBit(0));
					findcaus.set(caus.previousSetBit(caus.length()));

					if (caus.previousSetBit(caus.length()) > conc.nextSetBit(conc.length())) {
						if (directcausals.contains(findcaus)) syncevents.set(caus.previousSetBit(caus.length()));
					}
					else {
						if (invdirectcausals.contains(findcaus)) syncevents.set(caus.previousSetBit(caus.length()));
					}
				}
				
			}
			for (BitSet invcaus: invdirectcausals) {
				findcaus.clear();
				
				if (invcaus.previousSetBit(invcaus.length()) == conc.nextSetBit(0)) {
					findcaus.set(conc.previousSetBit(conc.length()));
					findcaus.set(invcaus.nextSetBit(0));

					if (invcaus.nextSetBit(0) > conc.previousSetBit(conc.length())) {
						if (directcausals.contains(findcaus)) syncevents.set(invcaus.nextSetBit(0));
					}
					else {
						if (invdirectcausals.contains(findcaus)) syncevents.set(invcaus.nextSetBit(0));
					}
				}
				else if (invcaus.previousSetBit(invcaus.length()) == conc.previousSetBit(conc.length())) {
					findcaus.set(conc.nextSetBit(0));
					findcaus.set(invcaus.nextSetBit(0));

					if (invcaus.nextSetBit(0) > conc.previousSetBit(conc.length())) {
						if (directcausals.contains(findcaus)) syncevents.set(invcaus.nextSetBit(0));
					}
					else {
						if (invdirectcausals.contains(findcaus)) syncevents.set(invcaus.nextSetBit(0));
					}
				}
			}
		}
		
	}
		
	private void addRelMap(BitSet br) {
		if (!relmap.containsKey(br)) relmap.put(br, new BitSet());
		relmap.get(br).set(pesCount);
	}
	
	private BitSet getRealPredecessors(PESPrefixUnfolding pes, int event, BitSet visited) {
		BitSet pred = new BitSet();
		pred.or(pes.getDirectPredecessors(event));
		BitSet cleanpred = new BitSet();
		BitSet silents = new BitSet();
		silents.or(pes.getInvisibleEvents());
		
		BitSet nvisited = (BitSet)visited.clone();
		nvisited.set(event);
		
		cleanpred.or(pred);
		cleanpred.andNot(silents); // remove all silents
		pred.andNot(cleanpred); // remove all clean predecessors
		pred.andNot(nvisited); // remove all predecessors that already have been visited, in order to prevent endless loops with silents
		
		for (int e = pred.nextSetBit(0); e >= 0; e = pred.nextSetBit(e + 1)) {
			cleanpred.or(getRealPredecessors(pes, e, nvisited));
		}
		
		return cleanpred;
	}
	
	private BitSet getRealSuccessors(PESPrefixUnfolding pes, int event, BitSet visited) {
		BitSet succ;
		
		if (pes.getCutoffEvents().get(event)) {
			succ = new BitSet();
			succ.or(pes.getDirectSuccessors(getRealCorresponding(pes, event)));
		}
		else {
			succ = new BitSet();
			succ.or(pes.getDirectSuccessors(event));
		}
		
		BitSet cleansucc = new BitSet();
		BitSet silents = new BitSet();
		silents.or(pes.getInvisibleEvents());
		
		BitSet nvisited = (BitSet)visited.clone();
		nvisited.set(event);
		
		cleansucc.or(succ);
		cleansucc.andNot(silents); // remove all silents
		succ.andNot(cleansucc); // remove all clean successors
		succ.andNot(nvisited); // remove all successors that already have been visited, in order to prevent endless loops with silents
		
		for (int e = succ.nextSetBit(0); e >= 0; e = succ.nextSetBit(e + 1)) {
			cleansucc.or(getRealSuccessors(pes, e, nvisited));
		}
		
		return cleansucc;
	}
	
	private BitSet getAllCausesOf(PESPrefixUnfolding pes, Map<Integer, BitSet> correspondings, int event, BitSet visited) {
		BitSet pred = new BitSet();
		BitSet cutoffs = new BitSet();
		BitSet realpred = new BitSet();
		
		pred.or(pes.getTransitivePredecessors(event));
					
		realpred.or(pred);
		
		BitSet nvisited = (BitSet)visited.clone();
		nvisited.set(event);
		
		pred.andNot(nvisited);
		if (pred.cardinality() == 0) return realpred;
		
		for (int corr: correspondings.keySet()) {
			if (pred.get(corr)) cutoffs.or(correspondings.get(corr));
		}
		realpred.or(cutoffs);

		nvisited.or(pred);
		
		for (int e = cutoffs.nextSetBit(0); e >= 0; e = cutoffs.nextSetBit(e + 1)) {
			realpred.or(getAllCausesOf(pes, correspondings, e, nvisited));
		}
		
		return realpred;
	}
	
	private int getRealCorresponding(PESPrefixUnfolding pes, int event) {
		int corr = event;
		
		while (pes.getCutoffEvents().get(corr)) {
			corr = pes.getCorrespondingEvent(corr);
		}
		return corr;
	}
	
	private Map<Integer, BitSet> getCorrespondings(PESPrefixUnfolding pes) {
		Map<Integer, BitSet> corresponding = new HashMap<Integer, BitSet>();
		
		for (int c = pes.getCutoffEvents().nextSetBit(0); c >= 0; c = pes.getCutoffEvents().nextSetBit(c + 1)) {
			if (!corresponding.containsKey(pes.getCorrespondingEvent(c)))
				corresponding.put(pes.getCorrespondingEvent(c), new BitSet());
			
			corresponding.get(pes.getCorrespondingEvent(c)).set(c);
		}
		
		return corresponding;
	}
	
	public void findMutualRelations() {
		BitSet relation;
		
		directcausals.clear();
		invdirectcausals.clear();
		transcausals.clear();
		invtranscausals.clear();
		existcausals.clear();
		invexistcausals.clear();
		conflict.clear();
		concurrency.clear();
		loops.clear();
				
		for (BitSet key: combinedPES.keySet()) {
			relation = combinedPES.get(key);
			if (relation.cardinality() == 1) {
				switch (relation.nextSetBit(0)) {
					case 0: 
//						if (dcmap.get(key).cardinality() == pesCount) {
							directcausals.add(key); 
//						}
						break;
					case 1: 
//						if (idcmap.get(key).cardinality() == pesCount) {
							invdirectcausals.add(key); 
//						}
						break;
					case 2: 
//						if (tcmap.get(key).cardinality() == pesCount) {
							transcausals.add(key); 
//						}
						break;
					case 3: 
//						if (itcmap.get(key).cardinality() == pesCount) {
							invtranscausals.add(key); 
//						}
						break;
					case 4: 
//						if (cfmap.get(key).cardinality() == pesCount) {
							conflict.add(key); 
//						}
						break;
					case 5: 
						if (ccmap.get(key).cardinality() == pesCount) {
							concurrency.add(key); 
						}
						break;
				}
			}
			else if (relation.cardinality() > 1) {
				
				// if there is a concurrency relation, check whether there are other relations that come from the same PES. If so: remove that other relation
				if (relation.get(5)) {					
					if (dcmap.containsKey(key) && (dcmap.get(key).equals(ccmap.get(key)))) {
						relation.clear(0);
						dcmap.remove(key);
					}
					if (idcmap.containsKey(key) && (idcmap.get(key).equals(ccmap.get(key)))) {
						relation.clear(1);
						idcmap.remove(key);
					}
					if (tcmap.containsKey(key) && (tcmap.get(key).equals(ccmap.get(key)))) {
						relation.clear(2);
						tcmap.remove(key);
					}
					if (itcmap.containsKey(key) && (itcmap.get(key).equals(ccmap.get(key)))) {
						relation.clear(3);
						itcmap.remove(key);
					}
					if (cfmap.containsKey(key) && (cfmap.get(key).equals(ccmap.get(key)))) {
						relation.clear(4);
						cfmap.remove(key);
					}
					
					// if after removing the other relations the concurrency relation is the only relation, it can be safely added to the concurrency set
					if (relation.cardinality() == 1) {
						concurrency.add(key);
					}
				}
				
				// if there is no PES for which the conflict relation is the only relation for [key], then the conflict relation can be removed
				if (relation.get(4)) {					
					if ((!(dcmap.containsKey(key) && (!dcmap.get(key).intersects(cfmap.get(key))))) && 
							(!(idcmap.containsKey(key) && (!idcmap.get(key).intersects(cfmap.get(key))))) &&
							(!(tcmap.containsKey(key) && (!tcmap.get(key).intersects(cfmap.get(key))))) &&
							(!(itcmap.containsKey(key) && (!itcmap.get(key).intersects(cfmap.get(key))))) &&
							(!(ccmap.containsKey(key) && (!ccmap.get(key).intersects(cfmap.get(key)))))) { 
					
						relation.clear(4);
						cfmap.remove(key);
						conflict.remove(key);
					}
				}
				
				if (relation.get(0)) {
					if (dcmap.get(key).equals(relmap.get(key))) directcausals.add(key);
				}
				else if (relation.get(1)) {
					if (idcmap.get(key).equals(relmap.get(key))) invdirectcausals.add(key);
				}
				else if (relation.get(2)) {
					if (tcmap.get(key).equals(relmap.get(key))) transcausals.add(key);
				}
				else if (relation.get(3)) {
					if (itcmap.get(key).equals(relmap.get(key))) invtranscausals.add(key);
				}
				
				if (!(relation.get(4)) && !(relation.get(5))) { // no conflict and no concurrency, so causality 
					if (relation.get(2)) {
						existcausals.add(key);
					}
					if (relation.get(3)) {
						invexistcausals.add(key);
					}
				}
			}
			
			if (lpmap.containsKey(key) && lpmap.get(key).cardinality() == pesCount) {
				loops.add(key);
			}
		}
				
		for (BitSet dl: dlpmap.keySet()) {
			if ((dlpmap.get(dl).cardinality() == relmap.get(dl).cardinality()) && (checkEventOccForAllPES(dl))) {
				directloops.add(dl);
			}
		}

		for (BitSet idl: idlpmap.keySet()) {
			if ((idlpmap.get(idl).cardinality() == relmap.get(idl).cardinality()) && (checkEventOccForAllPES(idl))) {
				invdirectloops.add(idl);
			}
		}

		for (int sl: sleventmap.keySet()) {
			System.out.println(sl + " " + labelmap.get(sl) + " " + sleventmap.get(sl));
			if ((sleventmap.get(sl).cardinality() == pesCount) || (sleventmap.get(sl).equals(labelmap.get(sl)))) {
				mslevents.set(sl);
			}
		}
		
		existcausals.addAll(transcausals);
		invexistcausals.addAll(invtranscausals);
	}
	
	public BitSet getBRrel(String e1, String e2) {
		return getBRrel(totalLabels.indexOf(e1), totalLabels.indexOf(e2));
	}
	
	public BitSet getBRrel(int e1, int e2) {
		return combinedPES.get(hash(e1, e2));
	}
	
	public String getLabel(int e) {
		return totalLabels.get(e);
	}
	
	public List<String> getAllLabels() {
		return totalLabels;
	}
	
	public Set<Integer> getSilents() {
		return silents;
	}
	
	public int getSource() {
		return source;
	}
	
	public int getSink() {
		return sink;
	}
	
	public Boolean occursInAllPESs(int label) {
		return (labelmap.get(label).cardinality() == pesCount);
	}
	
	public BitSet getSyncEvents() {
		if (syncevents.cardinality() == 0) fillInSyncEvents();
		return syncevents;
	}
	
	public Set<BitSet> getDirectCausals()    {return dcmap.keySet();}
	public Set<BitSet> getInvDirectCausals() {return idcmap.keySet();}
	public Set<BitSet> getTransCausals()     {return tcmap.keySet();}
	public Set<BitSet> getInvTransCausals()  {return itcmap.keySet();}
	public Set<BitSet> getConflicts()        {return cfmap.keySet();}
	public Set<BitSet> getConcurrents()      {return ccmap.keySet();}
	public Set<BitSet> getLoops()			 {return lpmap.keySet();}
	public Set<BitSet> getDirectLoops()		 {return dlpmap.keySet();}
	public Set<BitSet> getInvDirectLoops()	 {return idlpmap.keySet();}
		
	public Set<BitSet> getMutualDirectCausals()    {return directcausals;}
	public Set<BitSet> getMutualInvDirectCausals() {return invdirectcausals;}
	public Set<BitSet> getMutualTransCausals() 	   {return transcausals;}
	public Set<BitSet> getMutualInvTransCausals()  {return invtranscausals;}
	public Set<BitSet> getMutualExistCausals()	   {return existcausals;}
	public Set<BitSet> getMutualInvExistCausals()  {return invexistcausals;}
	public Set<BitSet> getMutualConflicts() 	   {return conflict;}
	public Set<BitSet> getMutualConcurrents() 	   {return concurrency;}
	public Set<BitSet> getMutualLoops()			   {return loops;}
	public Set<BitSet> getMutualDirectLoops()	   {return directloops;}
	public Set<BitSet> getMutualInvDirectLoops()   {return invdirectloops;}
	
	public BitSet getMutualSelfLoopEvents() {return mslevents;}

	public Set<BitSet> getImmediateResponses() {
		Set<BitSet> immresp = new HashSet<BitSet>();

		Set<BitSet> allrel = relmap.keySet();
		BitSet notpes = new BitSet();
		BitSet relpes = new BitSet();
		boolean exists;

		for (BitSet eir: allrel) {
			relpes.clear();
			if (dcmap.containsKey(eir)) relpes.or(dcmap.get(eir));
			if (dlpmap.containsKey(eir)) relpes.or(dlpmap.get(eir));
			
			if (relpes.cardinality() == pesCount) {
				immresp.add(eir);
			}
			else {
				notpes.set(0, pesCount);
				notpes.andNot(relpes);
				exists = false;
				for (int pes = notpes.nextSetBit(0); pes >= 0; pes = notpes.nextSetBit(pes + 1)) {
					if (labelmap.get(eir.nextSetBit(0)).get(pes)) exists = true;
				}
				
				if ((!exists) && (notpes.cardinality() > 0)) immresp.add(eir);
			}
		}
		
		return immresp;
	}
	
	public Set<BitSet> getInvImmediateResponses() {
		Set<BitSet> invimmresp = new HashSet<BitSet>();

		Set<BitSet> allrel = relmap.keySet();
		BitSet notpes = new BitSet();
		BitSet relpes = new BitSet();
		boolean exists;
		
		for (BitSet eir: allrel) {
			relpes.clear();
			if (idcmap.containsKey(eir)) relpes.or(idcmap.get(eir));
			if (idlpmap.containsKey(eir)) relpes.or(idlpmap.get(eir));
			
			if (relpes.cardinality() == pesCount) {
				invimmresp.add(eir);
			}
			else {
				notpes.set(0, pesCount);
				notpes.andNot(relpes);
				exists = false;
				for (int pes = notpes.nextSetBit(0); pes >= 0; pes = notpes.nextSetBit(pes + 1)) {
					if (labelmap.get(eir.previousSetBit(eir.length())).get(pes)) exists = true;
				}
				
				if ((!exists) && (notpes.cardinality() > 0)) invimmresp.add(eir);
			}
		}
		
		return invimmresp;
	}
	
	public Boolean containsRelation(Set<BitSet> relations, int e1, int e2) {
		BitSet r = new BitSet();
		r.set(e1);
		r.set(e2);
		
		return relations.contains(r);
	}
	
	public String getBRmatrix() {
		return getBRmatrix(false);
	}
	
	public String getBRmatrix(Boolean viewLabels) {
		String matrix = "";
		String br = "";
		String spaces = "                  ";
		String lbl;
		
		for (int y = 0; y < totalLabels.size(); y++) {
			if (viewLabels) {
				lbl = totalLabels.get(y) + spaces;
				matrix += lbl.substring(0, spaces.length()) + ": ";
			}
			for (int x = 0; x < totalLabels.size(); x++) {
				if (combinedPES.containsKey(hash(x, y))) {
					br = combinedPES.get(hash(x, y)).toString();
					matrix += br + spaces.substring(0, spaces.length() - br.length()) + " ";
				}
				else {
					matrix += spaces + " ";
				}
			}
			matrix += "\n";
		}
		
		return matrix;
	}
	
	public String getLabelString() {
		return getLabelString(false);
	}
	
	public String getLabelString(Boolean viewFirstLabel) {
		String spaces = "                  ";
		String labels = "";
		String lbl;
		
		if (viewFirstLabel) {
			labels = spaces + "  ";
		}

		for (int i = 0; i < totalLabels.size(); i++) {
			lbl = totalLabels.get(i) + spaces;
			labels += lbl.substring(0, spaces.length()) + " ";
		}

		return labels;
	}

	/**
	 * Returns the PESPrefixUnfoldings currently included in this CombinedEventStructure.
	 *
	 * @return a List of PESPrefixUnfoldings currently included in this CombinedEventStructure.
	 */
	public List<PESPrefixUnfolding> getSourcePesPrefixUnfoldings() {
		return pesPrefixUnfoldings;
	}

	public int getPEScount() {
		return pesCount;
	}

	// Checks whether all events in the specified relation occur in all PESs
	private Boolean checkEventOccForAllPES(BitSet relation) {
		for (int b = relation.nextSetBit(0); b >= 0; b = relation.nextSetBit(b + 1)) {
			if (labelmap.containsKey(b)) {
				if (labelmap.get(b).cardinality() < pesCount) return false;
			}
			else {
				return false;
			}
		}

		return true;
	}
	
	private BitSet hash(int x, int y) {
		BitSet relation = new BitSet();
		relation.set(x);
		relation.set(y);
		return relation;
	}
}
