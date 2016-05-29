package org.organicdesign.fp.collections;

import org.organicdesign.fp.Option;
import org.organicdesign.fp.collections.interfaces.UnmodMap;
import org.organicdesign.fp.function.Function1;

import java.util.Map;

/** An immutable map with no guarantees about its ordering. */
public abstract class ImMap<K,V> implements UnmodMap<K,V> {
    public abstract Option<UnmodMap.UnEntry<K,V>> entry(K key);

//    Sequence<UnEntry<K,V>> seq();

    /** Returns a new map with the given key/value added */
    public abstract ImMap<K,V> assoc(K key, V val);

    /** Returns a new map with the given key/value removed */
    public abstract ImMap<K,V> without(K key);

    /**
     * Returns a view of the mappings contained in this map.  The set should actually contain UnmodMap.Entry items, but
     * that return signature is illegal in Java, so you'll just have to remember. */
    @Override public ImSet<Map.Entry<K,V>> entrySet() {
        return map(new Function1<UnEntry<K, V>, Entry<K, V>>() {
            @Override
            public Entry<K, V> applyEx(UnEntry<K, V> kvUnEntry) throws Exception {
                return kvUnEntry;
            }
        })
                .toImSet();
    }

    /** Returns a view of the keys contained in this map. */
    @Override public abstract ImSet<K> keySet();

    @SuppressWarnings("unchecked")
    @Override public boolean containsKey(Object key) { return entry((K) key).isSome(); }

    @SuppressWarnings("unchecked")
    @Override public V get(Object key) {
        Option<UnEntry<K,V>> entry = entry((K) key);
        return entry.isSome() ? entry.get().getValue() : null;
    }

    public V getOrElse(K key, V notFound) {
        Option<UnEntry<K,V>> entry = entry(key);
        return entry.isSome() ? entry.get().getValue() : notFound;
    }

//    @Override public UnmodCollection<V> values() { return map(e -> e.getValue()).toImSet(); }

//    @Override public UnmodIterator<UnEntry<K,V>> iterator() { return seq().iterator(); }

    /** Returns a new map with an immutable copy of the given entry added */
    public ImMap<K,V> assoc(Map.Entry<K,V> entry) { return assoc(entry.getKey(), entry.getValue()); }
}
