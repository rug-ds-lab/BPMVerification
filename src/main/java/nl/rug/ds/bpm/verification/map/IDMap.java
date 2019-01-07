package nl.rug.ds.bpm.verification.map;

import nl.rug.ds.bpm.util.comparator.StringComparator;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.util.map.TreeBiMap;

import java.util.Map;
import java.util.Set;

/**
 * Created by p256867 on 4-4-2017.
 */
public class IDMap {
    private int n;
    private String ap;
    private TreeBiMap<String, String> map;

    public IDMap() {
        ap = "n";
        n = 0;
        map = new TreeBiMap<>(new StringComparator(), new StringComparator());
    }

    public IDMap(String apIdentifier) {
        this();
        ap = apIdentifier;
    }

    public IDMap(String apIdentifier, Map<String, String> map) {
        this(apIdentifier);
        this.map.putAll(map);
    }

    public synchronized String addID(String id) {
        if(!map.containsKey(id)) {
            String nid = ap + n++;
            map.put(id, nid);
            Logger.log("Mapping " + id + " to " + nid, LogEvent.VERBOSE);
        }
        return map.get(id);
    }

    public synchronized void addID(String id, String ap) {
        map.put(id, ap);
    }

    public String getAP(String id) {
        return map.get(id);
    }

    public String getID(String ap) {
        return map.getKey(ap);
    }

    public Set<String> getIDKeys() {
        return map.keySet();
    }

    public Set<String> getAPKeys() {
        return (Set<String>) map.values();
    }

    public Map<String, String> getMap() { return map; }
}
