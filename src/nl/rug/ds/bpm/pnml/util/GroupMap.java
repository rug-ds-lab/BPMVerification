package nl.rug.ds.bpm.pnml.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Heerko Groefsema on 07-Apr-17.
 */
public class GroupMap {
	private HashMap<String, Set<String>> groups;
	
	public GroupMap() {
		groups = new HashMap<>();
	}
	
	public void addGroup(String group) {
		Set<String> members = new HashSet<>();
		groups.put(group, members);
	}
	
	public void addToGroup(String group, String member) {
		groups.get(group).add(member);
	}
	
	public Set<String> getMembers(String group) {
		return groups.get(group);
	}
	
	public String toString(String group) {
		StringBuilder sb = new StringBuilder();
		Set<String> members = groups.get(group);
		if(members.size() == 1) {
			sb.append(members.iterator().next());
		}
		else if(members.size() > 1) {
			Iterator<String> iterator = members.iterator();
			sb.append("(");
			while (iterator.hasNext()) {
				sb.append(iterator.next());
				if(iterator.hasNext())
					sb.append(" | ");
			}
			sb.append(")");
		}
		//else empty
		return sb.toString();
	}
}
