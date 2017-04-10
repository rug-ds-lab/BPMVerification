package nl.rug.ds.bpm.pnml.util;

import java.util.*;

/**
 * Created by p256867 on 4-4-2017.
 */
public class IDMap {
    private int n;
    private HashMap<String, String> idToAp, apToId;

    public IDMap() {
        n = 0;
        idToAp = new HashMap<>();
        apToId = new HashMap<>();
    }

    public void addID(String id) {
        if(!idToAp.containsKey(id)) {
            String nid = "n" + n++;
            idToAp.put(id, nid);
            apToId.put(nid, id);
        }
    }

    public String getAP(String id) {
        return idToAp.get(id);
    }

    public String getID(String ap) {
        return apToId.get(ap);
    }
}
