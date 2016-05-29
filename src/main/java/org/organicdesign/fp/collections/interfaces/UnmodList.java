// Copyright 2015 PlanBase Inc. & Glen Peterson
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.organicdesign.fp.collections.interfaces;

import org.organicdesign.fp.function.Function2;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 An unmodifiable version of {@link java.util.List} which formalizes the return type of
 Collections.unmodifiableList()
 */
public interface UnmodList<E> extends List<E>, UnmodSortedCollection<E> {

    // ========================================== Static ==========================================

    final class Helpers {
        private Helpers() {
            // No instances
        }

        /**
         Apply the given function against all unique pairings of items in the list.  Does this belong on
         Function2 instead of List?
         */
        public static <T> void permutations(List<T> items, Function2<? super T, ? super T, ?> f) {
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    f.call(items.get(i), items.get(j));
                }
            }
        }
    }

    // ========================================= Instance =========================================

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean add(E e) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default void add(int index, E element) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default void clear() {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /**
     This method is deprecated because implementing it on a List has O(n) performance.  It will
     never go away because it's declared on java.util.Collection which List extends.  It still
     shouldn't be used.

     If you need repeated or fast contains() tests, use a Set instead instead of a List.
     SortedSet.contains() has O(log2 n) performance.  HashSet.contains() has O(1) performance!
     If you truly need a one-shot contains test, iterate the list manually, or override the
     deprecation warning, but include a description of why you need to use a List instead of some
     kind of Set or Map!
     */
    @Deprecated
    @Override default boolean contains(Object o) {
        for (Object item : this) {
            if (Objects.equals(item, o)) { return true; }
        }
        return false;
    }

    /**
     The default implementation of this method has O(this.size() + that.size()) or O(n) performance.
     So even though contains() is impossible to implement efficiently for Lists, containsAll()
     has a decent implementation (brute force would be O(this.size() * that.size()) or O(n^2) ).
     */
    @Override default boolean containsAll(Collection<?> c) {
        return UnmodSortedCollection.super.containsAll(c);
    }

//boolean	equals(Object o)
//E	get(int index)
//int	hashCode()

    /**
     The default implementation of this method has O(this.size()) performance.  If you call this
     much, you probably want to use a Map&lt;Integer,T&gt; instead for O(1) performance.
     */
    @Override default int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            if (Objects.equals(get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    /** A convenience method to check if size is 0 */
    @Override default boolean isEmpty() { return size() == 0; }

    /** A convenience method to get a listIterator. */
    @Override default UnmodSortedIterator<E> iterator() { return listIterator(0); }

    /** The default implementation of this method has O(this.size()) performance. */
    @Override default int lastIndexOf(Object o) {
        for (int i = size() - 1; i > -1; i--) {
            if (Objects.equals(get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override default UnmodListIterator<E> listIterator() { return listIterator(0); }

    /** {@inheritDoc}  Subclasses should override this when they can do so more efficiently. */
    @Override default UnmodListIterator<E> listIterator(final int index) {
        if ( (index < 0) || (index >= size()) ) {
            throw new IndexOutOfBoundsException("Expected an index between 0 and " + size() +
                                                " but found: " + index);
        }
        return new UnmodListIterator<E>() {
            private final int sz = size();
            private int idx = index;
            @Override public boolean hasNext() { return (idx < sz); }

            @Override public E next() {
                // I think this temporary variable i gets compiled to a register access
                // Load memory value from idx to register.  This is the index we will use against
                // our internal data.
                int i = idx;
                // Throw based on value in register
                if (i >= size()) { throw new NoSuchElementException(); }
                // Store incremented register value back to memory.  Note that this is the
                // next index value we will access.
                idx = i + 1;
                // call get() using the old value of idx (before our increment).
                // i should still be in the register, not in memory.
                return get(i);
            }

            @Override public boolean hasPrevious() { return idx > 0; }

            @Override public E previous() {
                // I think this temporary variable i gets compiled to a register access
                // retrieve idx, subtract 1, leaving result in register.  The JVM only has one
                // register.
                int i = idx - 1;
                // throw if item in register is < 0
                if (i < 0) { throw new NoSuchElementException(); }
                // Write register to memory location
                idx = i;
                // retrieve item at the index in the register.
                return get(i);
            }

            @Override public int nextIndex() { return idx; }
        };
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default E remove(int index) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean remove(Object o) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Modification attempted");
    }

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default E set(int index, E element) {
        throw new UnsupportedOperationException("Modification attempted");
    }

//int	size()

    /** Not allowed - this is supposed to be unmodifiable */
    @Override @Deprecated default void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException("Modification attempted");
    }

//default Spliterator<E> spliterator()

    /** {@inheritDoc} */
    @Override default UnmodList<E> subList(int fromIndex, int toIndex) {
        if ( (fromIndex == 0) && (toIndex == size()) ) {
            return this;
        }
        // Note that this is an IllegalArgumentException, not IndexOutOfBoundsException in order to
        // match ArrayList.
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex +
                                               ")");
        }
        // The text of this matches ArrayList
        if (fromIndex < 0) { throw new IndexOutOfBoundsException("fromIndex = " + fromIndex); }
        if (toIndex > size()) { throw new IndexOutOfBoundsException("toIndex = " + toIndex); }

        final UnmodList<E> parent = this;
        return new UnmodList<E>() {
            private final int size = toIndex - fromIndex;

            @Override public int size() { return size; }

            @Override public E get(int index) { return parent.get(index + fromIndex); }
        };
    }

    /**
     This method goes against Josh Bloch's Item 25: "Prefer Lists to Arrays", but is provided for
     backwards compatibility in some performance-critical situations.  If you really need an array,
     consider using the somewhat type-safe version of this method instead, but read the caveats
     first.

     {@inheritDoc}
     */
    @Override default Object[] toArray() { return UnmodSortedCollection.super.toArray(); }

    /**
     This method goes against Josh Bloch's Item 25: "Prefer Lists to Arrays", but is provided for
     backwards compatibility in some performance-critical situations.  If you need to create an
     array (you almost always do) then the best way to use this method is:

     <code>MyThing[] things = col.toArray(new MyThing[coll.size()]);</code>

     Calling this method any other way causes unnecessary work to be done - an extra memory
     allocation and potential garbage collection if the passed array is too small, extra effort to
     fill the end of the array with nulls if it is too large.

     {@inheritDoc}
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    @Override default <T> T[] toArray(T[] as) { return UnmodSortedCollection.super.toArray(as); }

//Methods inherited from interface java.util.Collection
//parallelStream, removeIf, stream

    /** Not allowed - this is supposed to be unmodifiable */
    @SuppressWarnings("deprecation")
    @Override @Deprecated default boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException("Modification attempted");
    }

//Methods inherited from interface java.lang.Iterable
//forEach

}
