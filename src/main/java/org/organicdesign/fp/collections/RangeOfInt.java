// Copyright (c) 2014-03-08 PlanBase Inc. & Glen Peterson
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

import org.organicdesign.fp.collections.interfaces.UnmodIterable;
import org.organicdesign.fp.collections.interfaces.UnmodList;
import org.organicdesign.fp.collections.interfaces.UnmodListIterator;
import org.organicdesign.fp.collections.interfaces.UnmodSortedIterable;

import java.math.BigInteger;
import java.util.List;

/**
 An efficient (in both time and memory) implementation of List.  If you want to compare a RangeOfInt
 to generic List&lt;Integer&gt;, use RangeOfInt.LIST_EQUATOR so that the hashCodes will be
 compatible.

 A RangeOfInt is an indexed sequence of integers.  It currently assumes a step of 1.
 Like everything in Java and similar classes in Clojure, Python, and Scala, it is inclusive of the
 start value but exclusive of the end.

 In theory, a class like this could be made for anything that can provide it's next() and previous()
 item and a size.  To do that, Integer would need to implement something that defined what the
 next() and previous() values.  Currently limited to Integer.MIN_VALUE to Integer.MAX_VALUE.
 */
// Note: In theory, this could implement both List<Integer> and Set<Integer>
public class RangeOfInt implements UnmodList<Integer> {

    public static final Equator<List<Integer>> LIST_EQUATOR = new Equator<List<Integer>>() {
        /**
         This is an *inefficient* hash code for RangeOfInt, but it is compatible with List.
         @param integers the list
         @return a HashCode which is compatible with java.util.List of the same integers as
         a range.
         */
        @Override public int hash(List<Integer> integers) {
            return UnmodIterable.Helpers.hashCode(integers);
        }

        @Override
        public boolean eq(List<Integer> o1, List<Integer> o2) {
            if (o1 == o2) { return true; }
            if ( (o1 == null) || (o2 == null) ) { return false; }
            if ((o1 instanceof RangeOfInt) && (o2 instanceof RangeOfInt)) {
                return o1.equals(o2);
            }
            return o1.size() == o2.size() &&
                   UnmodSortedIterable.equals(Helpers.castFromList(o1),
                                              Helpers.castFromList(o2));
        }
    };

    private final int start;
    private final int end;
    private final int size;

    private RangeOfInt(int s, int e) { start = s; end = e; size = (end - start); }

//    public static RangeOfInt of(int s, int e) {
//        if (e < s) {
//            throw new IllegalArgumentException("end of range must be >= start of range");
//        }
//        return new RangeOfInt(s, e);
//    }

    public static RangeOfInt of(Number s, Number e) {
        if ((s == null) || (e == null)) {
            throw new IllegalArgumentException("Nulls not allowed");
        }
        if (e.longValue() < s.longValue()) {
            throw new IllegalArgumentException("end of range must be >= start of range");
        }
        return new RangeOfInt(s.intValue(), e.intValue());
    }

    //
    public static RangeOfInt of(Number e) {
        if (e == null) {
            throw new IllegalArgumentException("Nulls not allowed");
        }
        if (e.longValue() < 0) {
            throw new IllegalArgumentException("Single argument factory can't accept a negative" +
                                               " endpoint (it assumes start is zero and positive" +
                                               " unit step).");
        }
        return new RangeOfInt(0, e.intValue());
    }

//    public static RangeOfInt of(int s, int e) { return of((int) s, (int) e); }

//    public int start() { return start; }
//    public int end() { return end; }

    /**
     Returns true if the number is within the bounds of this range (low end incluive, high end
     exclusive).  In math terms, returns true if the argument is [low, high).  False
     otherwise.  This is an efficient method when the compiler can see that it's being passed a
     primitive int, but contains(Object o) delegates to this method when it's passed a reasonable
     and unambiguous argument.
     */
    public boolean contains(int i) {
        return (i >= start) && (i < end);
    }

    /**
     Though this overrides List.contains(Object o), it is effectively a convenience method for
     calling contains(int i).  Therefore, it only accepts Integers, Longs, BigIntegers, and
     Strings that parse as signed decimal Integers.  It does not accept Numbers since they can't
     easily be checked for truncation and floating-point might not round properly with respect to
     bounds, or might not make sense if you are using your range to define a set of integers.
     Handles truncation (returns false) for the types it accepts.  Throws exceptions for types it
     does not accept.

     Thanks for all the help from codereview.stackexchange:
     http://codereview.stackexchange.com/questions/100846/rounding-and-truncation-in-intrange-containsobject-o

     @param o an Integer, Long, BigInteger, or String that parses as a signed decimal Integer.
     @return true if the number is within the bounds of this range (high end is exclusive).  False
     otherwise.
     @throws IllegalArgumentException if the argument is not an Integer, Long, BigInteger, or
     String.
     @throws NumberFormatException if a String argument cannot be parsed using Integer.valueOf().
     */
    @SuppressWarnings("deprecation")
    @Override public boolean contains(Object o) {
        // Only accept classes where we can convert to an integer while preventing
        // overflow/underflow.  If there were a way to reliably test for overflow/underflow in
        // Numbers, we could accept them too, but with the rounding errors of floats and doubles
        // that's impractical.
        if (o instanceof Integer) {
            return contains(((Integer) o).intValue());
        } else if (o instanceof Long) {
            long l = (Long) o;
            return (l <= ((long) Integer.MAX_VALUE)) &&
                   (l >= ((long) Integer.MIN_VALUE)) &&
                   contains((int) l);
        } else if (o instanceof BigInteger) {
            try {
                // Throws an exception if it's more than 32 bits.
                return contains(((BigInteger) o).intValueExact());
            } catch (ArithmeticException ignore) {
                return false;
            }
        } else if (o instanceof String) {
            return contains(Integer.valueOf((String) o));
        } else {
            throw new IllegalArgumentException("Don't know how to convert to a primitive int" +
                                               " without risking accidental truncation.  Pass an" +
                                               " Integer, Long, BigInteger, or String that parses" +
                                               " within Integer bounds instead.");
        }
    }

    @Override public Integer get(int idx) {
        if ( (idx >= 0) && (idx < size) ) { return start + idx; }
        throw new IndexOutOfBoundsException("Index " + idx +
                                            " was outside the size of this range: " + start +
                                            " to " + end);
    }

    /**
     Unlike most implementations of List, this method has excellent O(1) performance!
     {@inheritDoc}
     */
    @Override public int indexOf(Object o) {
        if (o instanceof Number) {
            int i = ((Number) o).intValue();
            if ( (i >= start) && (i < end) ) {
                return i - start;
            }
        }
        return -1;
    }

    /**
     Unlike most implementations of List, this method has excellent O(1) performance!
     {@inheritDoc}
     */
    @Override public int lastIndexOf(Object o) { return indexOf(o); }

    @Override public int size() { return size; }

//    @Override
//    public int hashCode() {
//        // My theory is that we can compute a hashCode compatible with ArrayList using
//        // sum of a series.  Maybe the summing hashCode truncates differently on overflow, but I
//        // thought this was worth a try.
//
//        // size * (start + end) / 2
//        // But convert to long to avoid premature overflow
//        long tmp = ((long) start) + ((long) end);
//        tmp = tmp * (long) size;
//        return (int) (tmp / 2L);
//    }
//    @Override
//    public boolean equals(Object other) {
//        if (this == other) { return true; }
//        if ( !(other instanceof List) ) { return false; }
//
//        // Fast compare for other int ranges:
//        if (other instanceof RangeOfInt) {
//            final RangeOfInt that = (RangeOfInt) other;
//            return (this.start == that.start) &&
//                   (this.end == that.end);
//        }
//
//        // Slower compare for other lists:
//        final List that = (List) other;
//        // This is not a database object; compare "significant" fields here.
//        return UnmodSortedIterable.equals(this, UnmodSortedIterable.castFromList(that));
//    }

    @Override
    public int hashCode() { return start + end; }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if ( !(other instanceof RangeOfInt) ) { return false; }

        // Details...
        final RangeOfInt that = (RangeOfInt) other;
        // This is not a database object; compare "significant" fields here.
        return (this.start == that.start) &&
               (this.end == that.end);
    }

    /**
     {@inheritDoc}
     Iterates from start of range (inclusive) up-to, but excluding, the end of the range.
     I'm not sure this is a good idea, but Python, Clojure, Scala, and just about everything in
     Java expects similar behavior.
     */
    @Override public UnmodListIterator<Integer> listIterator(final int startIdx) {
        return new UnmodListIterator<Integer>() {
            int val = start + startIdx;
            @Override public boolean hasNext() { return val < end; }
            @Override public Integer next() {
                Integer t = val;
                val = val + 1;
                return t;
            }
            @Override public boolean hasPrevious() { return val > start; }
            @Override public Integer previous() {
                val = val - 1;
                return val;
            }
            @Override public int nextIndex() { return val - start; }
        };
    }

    /** {@inheritDoc} */
    @Override public RangeOfInt subList(int fromIndex, int toIndex) {
        if ( (fromIndex == 0) && (toIndex == size) ) {
            return this;
        }
        // Note that this is an IllegalArgumentException, not IndexOutOfBoundsException in order to
        // match ArrayList.
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex +
                                               ")");
        }
        // The text of this matches ArrayList
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }

        // Look very closely at the second parameter because the bounds checking is *different*
        // from the get() method.  get(toIndex) can throw an exception if toIndex >= start+size.
        // But since a range is exclusive of it's right-bound, we can create a new sub-range
        // with a right bound index of size, as opposed to size minus 1.  I spent hours
        // understanding this before fixing a bug with it.  In the end, subList should do the same
        // thing on a Range that it does on the equivalent ArrayList.  I made tests for the same.
        return RangeOfInt.of(start + fromIndex, start + toIndex);
    }
}