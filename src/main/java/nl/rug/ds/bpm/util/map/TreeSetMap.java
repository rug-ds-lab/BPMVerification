package nl.rug.ds.bpm.util.map;

import java.util.*;

public class TreeSetMap<K,V> implements Map<K,Set<V>> {
	private Map<K, Set<V>> map;
	private Comparator<V> valueComparator;

	public TreeSetMap() {
		map = new TreeMap<>();
	}

	public TreeSetMap(Comparator<K> keyCcomparator) {
		map = new TreeMap<>(keyCcomparator);
	}

	public TreeSetMap(Comparator<K> keyComparator, Comparator<V> valueComparator) {
		map = new TreeMap<>(keyComparator);
		this.valueComparator = valueComparator;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<V> get(Object key) {
		return map.get(key);
	}

	public Set<V> add(K key, V value) {
		if (map.containsKey(key)) {
			Set<V> existingValues = map.get(key);
			existingValues.add(value);
			return existingValues;
		}
		else {
			Set<V> values;
			if (valueComparator == null)
				values = new TreeSet<>();
			else
				values = new TreeSet<>(valueComparator);
			values.add(value);
			return map.put(key, values);
		}
	}

	@Override
	public Set<V> put(K key, Set<V> value) {
		if (map.containsKey(key)) {
			Set<V> existingValues = map.get(key);
			existingValues.addAll(value);
			return existingValues;
		}
		else {
			Set<V> values;
			if (valueComparator == null)
				values = new TreeSet<>();
			else
				values = new TreeSet<>(valueComparator);
			values.addAll(value);
			return map.put(key, values);
		}
	}

	@Override
	public Set<V> remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends Set<V>> m) {
		for (K key: m.keySet())
			put(key, m.get(key));
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<Set<V>> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K, Set<V>>> entrySet() {
		return map.entrySet();
	}

	@Override
	public String toString() {
		String sb = "";
		Iterator<K> keyIterator = map.keySet().iterator();
		while (keyIterator.hasNext()) {
			sb = sb + toString(keyIterator.next());
			if (keyIterator.hasNext())
				sb = sb + "\n";
		}
		return sb.toString();
	}

	public String toString(K key) {
		String sb = "";
		Set<V> members = map.get(key);
		if(members.size() == 1) {
			sb = members.iterator().next().toString();
		}
		else if(members.size() > 1) {
			Iterator<V> iterator = members.iterator();
			sb = iterator.next().toString();
			while (iterator.hasNext()) {
				sb = "(" + sb + " | " + iterator.next() + ")";
			}
		}
		//else empty
		return sb.toString();
	}
}
