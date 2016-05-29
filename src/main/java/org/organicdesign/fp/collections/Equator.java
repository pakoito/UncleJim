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

package org.organicdesign.fp.collections;

import java.util.Comparator;

/**
 An Equator represents an equality context in a way that is analgous to the java.util.Comparator
 interface.
 <a href="http://glenpeterson.blogspot.com/2013/09/object-equality-is-context-relative.html" target="_blank">Comparing Objects is Relative</a>
 This will need to be passed to Hash-based collections the way a Comparator is passed to tree-based
 ones.

 The method names hash() and eq() are intentionally elisions of hashCode() and equals() so that your
 IDE will suggest the shorter name as you start typing which is almost always what you want.
 You want the hash() and eq() methods because that's how Equators compare things.  You don't want
 an equator's .hashCode() or .equals() methods because those are for comparing *Equators* and are
 inherited from java.lang.Object.  I'd deprecate those methods, but you can't do that on an
 interface.
 */
public abstract class Equator<T> {

    // ============================================= Static ========================================

    public static final Equator<Object> DEFAULT_EQUATOR = new Equator<Object>() {
        @Override public int hash(Object o) {
            return (o == null) ? 0 : o.hashCode();
        }

        @Override public boolean eq(Object o1, Object o2) {
            if (o1 == null) { return (o2 == null); }
            return o1.equals(o2);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Equator<T> defaultEquator() { return (Equator<T>) DEFAULT_EQUATOR; }

    @SuppressWarnings("ConstantConditions")
    public static final
    Comparator<Comparable<Object>> DEFAULT_COMPARATOR = new Comparator<Comparable<Object>>() {
        @Override
        public int compare(Comparable<Object> o1, Comparable<Object> o2) {
            if (o1 == o2) { return 0; }
            if (o1 == null) {
                return - (o2.compareTo(o1));
            }
            return o1.compareTo(o2);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> defaultComparator() { return (Comparator<T>) DEFAULT_COMPARATOR; }

    /**
     Implement compare() and hash() and you get a 100% compatible eq() for free.
     */
    public static abstract class ComparisonContext<T> extends Equator<T> implements Comparator<T> {
        /** Returns true if the first object is less than the second. */
        public boolean lt(T o1, T o2) { return compare(o1, o2) < 0; }

        /** Returns true if the first object is less than or equal to the second. */
        public boolean lte(T o1, T o2) { return compare(o1, o2) <= 0; }

        /** Returns true if the first object is greater than the second. */
        public boolean gt(T o1, T o2) { return compare(o1, o2) > 0; }

        /** Returns true if the first object is greater than or equal to the second. */
        public boolean gte(T o1, T o2) { return compare(o1, o2) >= 0; }

        @Override public boolean eq(T o1, T o2) { return compare(o1, o2) == 0; }

        static final ComparisonContext<Comparable<Object>> DEFAULT_CONTEXT =
                new ComparisonContext<Comparable<Object>>() {
                    @Override public int hash(Comparable<Object> o) {
                        return (o == null) ? 0 : o.hashCode();
                    }
                    @SuppressWarnings("ConstantConditions")
                    @Override public int compare(Comparable<Object> o1, Comparable<Object> o2) {
                        if (o1 == o2) { return 0; }
                        if (o1 == null) {
                            return - (o2.compareTo(o1));
                        }
                        return o1.compareTo(o2);
                    }
                };

        @SuppressWarnings("unchecked")
        static <T> ComparisonContext<T> defCompCtx() {
            return (ComparisonContext<T>) DEFAULT_CONTEXT;
        }
    }

    // ========================================= Instance =========================================
    /**
     An integer digest used for very quick "can-equal" testing.
     This method MUST return equal hash codes for equal objects.
     It should USUALLY return unequal hash codes for unequal objects.
     You should not change mutable objects while you rely on their hash codes.
     That said, if a mutable object's internal state changes, the hash code generally must change to
     reflect the new state.
     The name of this method is short so that auto-complete can offer it before hashCode().
     */
    public abstract int hash(T t);

    /**
     Determines whether two objects are equal.  The name of this method is short so that
     auto-complete can offer it before equals().
     @return true if this Equator considers the two objects to be equal.
     */
    public abstract boolean eq(T o1, T o2);

}
