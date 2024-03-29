package nl.rug.ds.bpm.verification.map;

import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.map.TreeBiMap;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by p256867 on 4-4-2017.
 */
public class AtomicPropositionMap<T extends Comparable<T>> {
    private int n;
    private String ap;
    private TreeBiMap<T, String> map;
    private Set<T> specificationIds;

    public AtomicPropositionMap() {
        ap = "n";
        n = 0;
        map = new TreeBiMap<T, String>(new ComparableComparator<T>(), new StringComparator());
        specificationIds = new TreeSet<T>(new ComparableComparator<T>());
    }

    public AtomicPropositionMap(String apIdentifier) {
        this();
        ap = apIdentifier;
    }

    public AtomicPropositionMap(String apIdentifier, Map<T, String> map) {
        this(apIdentifier);
        this.map.putAll(map);
    }

    public synchronized String addSpecificationId(T id) {
        specificationIds.add(id);
        return addID(id);
    }

    public synchronized String addID(T id) {
        if (!map.containsKey(id)) {
            String nid = ap + n++;
            map.put(id, nid);
            Logger.log("Mapping " + id.toString() + " to " + nid, LogEvent.DEBUG);
        }
        return map.get(id);
    }

    public synchronized void addID(T id, String ap) {
        map.put(id, ap);
        Logger.log("Remapping " + id.toString() + " to " + ap, LogEvent.DEBUG);
    }

    public synchronized void merge(AtomicPropositionMap<T> atomicPropositionMap) {
        map.putAll(atomicPropositionMap.getMap());
        specificationIds.addAll(atomicPropositionMap.getSpecificationIds());
    }

    public boolean contains(T id) {
        return map.containsKey(id);
    }

    public String getAP(T id) {
        return map.get(id);
    }

    public T getID(String ap) {
        return map.getKey(ap);
    }

    public Set<T> getIDKeys() {
        return map.keySet();
    }

    public Set<String> getAPKeys() {
        return (Set<String>) map.values();
    }

    public Set<T> getSpecificationIds() {
        return specificationIds;
    }

    public Map<T, String> getMap() {
        return map;
    }
}
