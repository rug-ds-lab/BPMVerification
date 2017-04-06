package nl.rug.ds.bpm.pnml.util;

import java.util.*;

/**
 * Created by p256867 on 4-4-2017.
 */
public class TransitionMap {
    private int n;
    private HashMap<String, Set<String>> idToAp, apToId;

    public TransitionMap() {
        n = 0;
        idToAp = new HashMap<>();
        apToId = new HashMap<>();
    }

    public void addTransition(String id) {
        String nid = "n" + n++;
        Set<String> apValues = new HashSet<>();
        apValues.add(nid);
        idToAp.put(id, apValues);

        Set<String> idValues = new HashSet<>();
        idValues.add(id);
        apToId.put(nid, idValues);
    }

    public void addAP(String id, String ap) {
        Set<String> apValues = idToAp.get(id);
        if(apValues == null) {
            addTransition(id);
            apValues = idToAp.get(id);
        }
        apValues.add(ap);

        Set<String> idValues = apToId.get(ap);
        if(idValues == null) {
            idValues = new HashSet<>();
            apToId.put(ap, idValues);
        }
        idValues.add(id);
    }

    public Set<String> getAPs(String id) {
        return idToAp.get(id);
    }

    public Set<String> getIDs(String ap) {
        return apToId.get(ap);
    }
}
