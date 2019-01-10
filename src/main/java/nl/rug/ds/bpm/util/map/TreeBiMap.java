package nl.rug.ds.bpm.util.map;

import java.util.*;

public class TreeBiMap<K,V> implements Map<K,V> {
	private TreeMap<K,V> kvTreeMap;
	private TreeMap<V,K> vkTreeMap;

	public TreeBiMap() {
		kvTreeMap = new TreeMap<>();
		vkTreeMap = new TreeMap<>();
	}

	public TreeBiMap(Comparator<K> kComparator, Comparator<V> vComparator) {
		kvTreeMap = new TreeMap<>(kComparator);
		vkTreeMap = new TreeMap<>(vComparator);
	}

	@Override
	public int size() {
		return kvTreeMap.size();
	}

	@Override
	public boolean isEmpty() {
		return kvTreeMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return kvTreeMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return vkTreeMap.containsKey(value);
	}

	@Override
	public V get(Object key) {
		return kvTreeMap.get(key);
	}

	public K getKey(Object value) {
		return vkTreeMap.get(value);
	}

	@Override
	public synchronized V put(K key, V value) {
		if (kvTreeMap.containsKey(key))
			vkTreeMap.remove(value);
		vkTreeMap.put(value,key);
		return kvTreeMap.put(key, value);
	}

	@Override
	public synchronized V remove(Object key) {
		if (kvTreeMap.containsKey(key))
			vkTreeMap.remove(kvTreeMap.get(key));
		return kvTreeMap.remove(key);
	}

	public synchronized V removeValue(Object value) {
		if (vkTreeMap.containsKey(value))
			kvTreeMap.remove(vkTreeMap.get(value));
		return kvTreeMap.remove(value);
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		for (K key: m.keySet())
			put(key, m.get(key));
	}

	@Override
	public synchronized void clear() {
		kvTreeMap.clear();
		vkTreeMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return kvTreeMap.keySet();
	}

	@Override
	public Collection<V> values() {
		return vkTreeMap.keySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return kvTreeMap.entrySet();
	}
}
