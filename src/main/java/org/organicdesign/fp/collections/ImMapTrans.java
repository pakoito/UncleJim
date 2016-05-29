package org.organicdesign.fp.collections;

/**
 Allows a map to be taken from transient to persistent.  This is NOT inherently thread-safe and calls to the
 asTransient() and persistent() methods need to be wrapped in a thread-safe manner.
 This bridges that gap to let PersistentHashSet know about the asTransient() method for accessing PersistentHashMap's
 inner transient class, which in turn, allows this class to implement keySet for PersistentHashMap.  Also allows
 */
public abstract class ImMapTrans<K,V> extends ImMap<K,V> {
    public abstract ImMapTrans<K,V> asTransient();

    /** Returns the Equator used by this map for equals comparisons and hashCodes */
    public abstract Equator<K> equator();

    public abstract ImMapTrans<K,V> persistent();

    /** {@inheritDoc} */
    @Override
    public ImSet<K> keySet() { return PersistentHashSet.ofMap(this); }

    /** {@inheritDoc} */
    @Override
    public abstract ImMapTrans<K,V> assoc(K key, V val);

    /** {@inheritDoc} */
    @Override
    public abstract ImMapTrans<K,V> without(K key);
}