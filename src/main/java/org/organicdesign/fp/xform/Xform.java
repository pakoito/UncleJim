// Copyright 2014-08-30 PlanBase Inc. & Glen Peterson
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

package org.organicdesign.fp.xform;

import org.organicdesign.fp.FunctionUtils;
import org.organicdesign.fp.Or;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodIterator;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 An immutable description of operations to be performed (a transformation, transform, or x-form).
 When foldLeft() is called, the Xform definition is "compiled" into a mutable transformation which
 is then carried out.  This allows certain performance shortcuts (such as doing a drop with index
 addition instead of iteration) and also hides the mutability otherwise inherent in a
 transformation.

 Xform is an abstract class.  Most of the methods on Xform produce immutable descriptions of actions
 to take at a later time.  These are represented by ___Desc classes.  When foldLeft() is called
 (or any of the helper methods that wrap it), that produces a result by first stringing together
 a bunch of Operations (____Op classes) and then "running" them.  This is analogous to compiling
 a program and running it.  The ____Desc classes are like the immutable source, the ____Op classes
 like the op-codes it's compiled into.

 Special thanks to Nathan Williams for pointing me toward separating the mutation from the
 description of a transformation.  Also to Paul Phillips (@extempore2) whose lectures provided
 an outline for what was ideal and also what was important.  All errors are my own.
 -Glen 2015-08-30
 */
public abstract class Xform<A> implements UnmodIterable<A> {

    enum OpStrategy { HANDLE_INTERNALLY, ASK_SUPPLIER, CANNOT_HANDLE }

    private static final Object TERMINATE = new Object();
    @SuppressWarnings("unchecked")
    private A terminate() { return (A) TERMINATE; }

    /**
     These are mutable operations that the transform carries out when it is run.  This is like the
     compiled "op codes" in contrast to the Xform is like the immutable "source code" of the
     transformation description.
     */
    static abstract class Operation {
        // Time using a linked list of ops instead of array, so that we can easily remove ops from
        // the list when they are used up.
        Function1<Object,Boolean> filter = null;
        Function1 map = null;
        Function1<Object,Iterable> flatMap = null;

        /**
         Drops as many items as the source can handle.
         @param num the number of items to drop
         @return  whether the source can handle the take, or pass-through (ask-supplier), or can't
         do either.
         */
        public Or<Long,OpStrategy> drop(long num) {
            return (num < 1) ? Or.<Long,OpStrategy>good(0L)
                             : Or.<Long,OpStrategy>bad(OpStrategy.CANNOT_HANDLE);
        }

        /**
         Takes as many items as the source can handle.
         @param num the number of items to take.
         @return whether the source can handle the take, or pass-through (ask-supplier), or can't
         do either.
         */
        public OpStrategy take(long num) { return OpStrategy.CANNOT_HANDLE; }

        /**
         We need to model this as a separate op for when the previous op is CANNOT_HANDLE.  It is
         coded as a filter, but still needs to be modeled separately so that subsequent drops can be
         combined into the earliest single explicit drop op.  Such combinations are additive,
         meaning that drop(3).drop(5) is equivalent to drop(8).
         */
        private static class DropOp extends Operation {
            private long leftToDrop;
            DropOp(long drop) {
                leftToDrop = drop;
                filter = new Function1<Object, Boolean>() {
                    @Override
                    public Boolean applyEx(Object o) throws Exception {
                        if (leftToDrop > 0) {
                            leftToDrop = leftToDrop - 1;
                            return Boolean.FALSE;
                        }
                        return Boolean.TRUE;
                    }
                };
            }
            @Override public Or<Long,OpStrategy> drop(long num) {
                leftToDrop = leftToDrop + num;
                return Or.good(num);
            }
        }

        private static class FilterOp extends Operation {
            FilterOp(Function1<Object,Boolean> func) { filter = func; }
        }

        private static class MapOp extends Operation {
            MapOp(Function1 func) { map = func; }
            @Override public Or<Long,OpStrategy> drop(long num) {
                return Or.bad(OpStrategy.ASK_SUPPLIER);
            }
            @Override public OpStrategy take(long num) { return OpStrategy.ASK_SUPPLIER; }
        }

        // TODO: FlatMap should drop and take internally using addition/subtraction on each output
        // TODO: list instead of testing each list item individually.
        private static class FlatMapOp extends Operation {
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;

            FlatMapOp(Function1<Object,Iterable> func) { flatMap = func; }
        }

        /**
         We need to model this as a separate op for when the previous op is CANNOT_HANDLE.  It is
         coded as a map, but still needs to be modeled separately so that subsequent takes can be
         combined into the earliest single explicit take op.  Such combination is a pick-least of
         all the takes, meaning that take(5).take(3) is equivalent to take(3).
         */
        private static class TakeOp extends Operation {
            private long numToTake;
            TakeOp(long take) {
                numToTake = take;
                map = new Function1() {
                    @Override
                    public Object applyEx(Object a) throws Exception {
                        if (numToTake > 0) {
                            numToTake = numToTake - 1;
                            return a;
                        }
                        return TERMINATE;
                    }
                };
            }

            @Override public OpStrategy take(long num) {
                // This data condition is prevented in Xform.take()
//                if (num < 0) {
//                    throw new IllegalArgumentException("Can't take less than 0 items.");
//                }
                if (num < numToTake) {
                    numToTake = num;
                }
                return OpStrategy.HANDLE_INTERNALLY;
            }
        }
    } // end class Operation

    /**
     A RunList is a list of Operations "compiled" from an Xform.  It contains an Iterable data
     source (or some day and array source or List source) and a List of Operation op-codes.

     A RunList is also a SourceProvider, since the output of one transform can be the input to
     another.  FlatMap is implemented that way.  Notice that there are almost no generic types used
     here: Since the input could be one type, and each map or flatmap operation could change that to
     another type.

     For speed, we ignore all that in the "compiled" version and just use Objects and avoid any
     wrapping or casting.
     */
    protected static class RunList implements Iterable {
        Iterable source;
        List<Operation> list = new ArrayList<>();
        RunList next = null;
        RunList prev = null;

        private RunList(RunList prv, Iterable src) { prev = prv; source = src; }

        public static RunList of(RunList prv, Iterable src) {
            RunList ret = new RunList(prv, src);
            if (prv != null) { prv.next = ret; }
            return ret;
        }

        Operation[] opArray() {
            return list.toArray(new Operation[list.size()]);
        }
        @Override public Iterator iterator() { return source.iterator(); }
    }

    /**
     When iterator() is called, the AppendOp processes the previous source and operation into
     an ArrayList.  Then yields an iterator that yield the result of that operation until it runs
     out.  Then continues to yield the appended items until they run out, at which point hasNext()
     returns false;
     */
    private static class AppendOp extends RunList {
        private AppendOp(RunList prv, Iterable src) { super(prv, src); }

        @Override public Iterator iterator() {
            final ArrayList prevSrc = _foldLeft(prev, prev.opArray(), 0, new ArrayList(),
                                          new Function2<ArrayList,Object,ArrayList>() {
                                              @SuppressWarnings("unchecked")
                                              @Override
                                              public ArrayList applyEx(ArrayList res, Object item) throws Exception {
                                                  res.add(item);
                                                  return res;
                                              }
                                          }

            );
            //noinspection unchecked
            return new Iterator() {
                Iterator innerIter = prevSrc.iterator();
                boolean usingPrevSrc = true;
                /** {@inheritDoc} */
                @Override public boolean hasNext() {
                    if (innerIter.hasNext()) {
                        return true;
                    } else if (usingPrevSrc) {
                        usingPrevSrc = false;
                        innerIter = source.iterator();
                    }
                    return innerIter.hasNext();
                }

                @Override public Object next() {
                    return innerIter.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } // end iterator()
    }

    /** Describes an concat() operation, but does not perform it. */
    private static class AppendIterDesc<T> extends Xform<T> {
        final Xform<T> src;

        AppendIterDesc(Xform<T> prev, Xform<T> s) { super(prev); src = s; }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
            return new AppendOp(prevOp.toRunList(), src);
        }
    }

    /**
     Describes a "drop" operation.  Drops will be pushed as early in the operation-list as possible,
     ideally being done using one-time pointer addition on the source.

     I have working source-pointer-addition code, but it added a fair amount of complexity to
     implement it for Lists and arrays, but not for Iterables in general, so it is not currently
     (2015-08-21) part of this implementation.

     When source-pointer-addition is not possible, a Drop op-code is created (implemented as a
     filter function).  Subsequent drop ops will be combined into the earliest drop (for speed).
     @param <T> the expected input type to drop.
     */
    private static class DropDesc<T> extends Xform<T> {
        private final long dropAmt;
        DropDesc(Xform<T> prev, long d) { super(prev); dropAmt = d; }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
//                System.out.println("in toRunList() for drop");
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a drop...");
            Or<Long,OpStrategy> earlierDs = null;
            for (; i >= 0; i--) {
                Operation op = ret.list.get(i);
                earlierDs = op.drop(dropAmt);
                if (earlierDs.isBad() && (earlierDs.bad() == OpStrategy.CANNOT_HANDLE) ) {
//                        System.out.println("\tNone can handle a drop...");
                    break;
                } else if (earlierDs.isGood()) {
//                        System.out.println("\tHandled internally by " + opRun);
                    return ret;
                }
            }
//            if ( !Or.bad(OpStrategy.CANNOT_HANDLE).equals(earlierDs) && (i <= 0) ) {
//                Or<Long,OpStrategy> srcDs = ret.source.drop(dropAmt);
//                if (srcDs.isGood()) {
//                    if (srcDs.good() == dropAmt) {
////                        System.out.println("\tHandled internally by source: " + ret.source);
//                        return ret;
//                    } else {
//                        // TODO: Think about this and implement!
//                        throw new UnsupportedOperationException("Not implemented yet!");
//                    }
//                }
//            }
//                System.out.println("\tSource could not handle drop.");
//                System.out.println("\tMake a drop for " + dropAmt + " items.");
            ret.list.add(new Operation.DropOp(dropAmt));
            return ret;
        }
    }

    /** Describes a filter() operation, but does not perform it. */
    private static class FilterDesc<T> extends Xform<T> {
        final Function1<? super T,Boolean> f;

        FilterDesc(Xform<T> prev, Function1<? super T,Boolean> func) { super(prev); f = func; }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new Operation.FilterOp((Function1<Object,Boolean>) f));
            return ret;
        }
    }

    /** Describes a map() operation, but does not perform it. */
    private static class MapDesc<T,U> extends Xform<U> {
        final Function1<? super T,? extends U> f;

        MapDesc(Xform<T> prev, Function1<? super T,? extends U> func) { super(prev); f = func; }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new Operation.MapOp(f));
            return ret;
        }
    }

    /** Describes a flatMap() operation, but does not perform it. */
    private static class FlatMapDesc<T,U> extends Xform<U> {
        final Function1<? super T,Iterable<U>> f;
        FlatMapDesc(Xform<T> prev, Function1<? super T,Iterable<U>> func) {
            super(prev); f = func;
        }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new Operation.FlatMapOp((Function1) f));
            return ret;
        }
    }

    /**
     Describes a "take" operation, but does not perform it.  Takes will be pushed as early in the
     operation-list as possible, ideally being done using one-time pointer addition on the source.
     When source pointer addition is not possible, a Take op-code is created (implemented as a
     filter function).  Subsequent take ops will be combined into the earliest take (for speed).
     @param <T> the expected input type to take.
     */
    private static class TakeDesc<T> extends Xform<T> {
        private final long take;
        TakeDesc(Xform<T> prev, long t) { super(prev); take = t; }

        @SuppressWarnings("unchecked")
        @Override protected RunList toRunList() {
//                System.out.println("in toRunList() for take");
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a take...");
            OpStrategy earlierTs = null;
            for (; i >= 0; i--) {
                Operation op = ret.list.get(i);
                earlierTs = op.take(take);
                if (earlierTs == OpStrategy.CANNOT_HANDLE) {
//                        System.out.println("\tNone can handle a take...");
                    break;
                } else if (earlierTs == OpStrategy.HANDLE_INTERNALLY) {
//                        System.out.println("\tHandled internally by " + opRun);
                    return ret;
                }
            }
//            if ( (earlierTs != OpStrategy.CANNOT_HANDLE) && (i <= 0) ) {
//                OpStrategy srcDs = ret.source.take(take);
//                if (srcDs == OpStrategy.HANDLE_INTERNALLY) {
////                        System.out.println("\tHandled internally by source: " + ret.source);
//                    return ret;
//                }
//            }
//                System.out.println("\tSource could not handle take.");
//                System.out.println("\tMake a take for " + take + " items.");
            ret.list.add(new Operation.TakeOp(take));
            return ret;
        }
    }

    static class SourceProviderIterableDesc<T> extends Xform<T> {
        private final Iterable<? extends T> list;
        SourceProviderIterableDesc(Iterable<? extends T> l) { super(null); list = l; }
        @Override protected RunList toRunList() {
            return RunList.of(null, list);
        }

        @Override public int hashCode() { return UnmodIterable.hashCode(this); }
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( !(other instanceof SourceProviderIterableDesc) ) { return false; }
            return Objects.equals(this.list, ((SourceProviderIterableDesc) other).list);
        }
    }

//    /** Static factory methods */
//    @SafeVarargs
//    public static <T> Xform<T> ofArray(T... list) {
//        return new SourceProviderIterableDesc<>(Arrays.asList(list));
//    }

    public static Xform EMPTY = new SourceProviderIterableDesc<>(Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <T> Xform<T> empty() { return (Xform<T>) EMPTY; }

    /** Static factory methods */
    public static <T> Xform<T> of(Iterable<? extends T> list) {
        if (list == null) { return empty(); }
        return new SourceProviderIterableDesc<>(list);
    }

    // ========================================= Instance =========================================

    // Fields
    /** This is the previous operation or source. */
    final Xform prevOp;

    // Constructor
    Xform(Xform pre) { prevOp = pre; }

    // This is the main method of this whole file.  Everything else lives to serve this.
    // We used a linked-list to build the type-safe operations so if that code compiles, the types
    // should work out here too.  However, for performance, we don't want to be stuck creating and
    // passing Options around, nor do we want a telescoping stack of hasNext() and next() calls.
    // So abandon type safety, store all the intermediate results as Objects, and use loops and
    // sentinel values to break out or skip processing as appropriate.  Initial tests indicate this
    // is 2.6 times faster than wrapping items type-safely in Options and 10 to 100 times faster
    // than lazily evaluated and cached linked-list, Sequence model.
    @SuppressWarnings("unchecked")
    private static <H> H _foldLeft(Iterable source, Operation[] ops, int opIdx, H ident, Function2 reducer) {
        Object ret = ident;

        // This is a label - the first one I have used in Java in years, or maybe ever.
        // I'm assuming this is fast, but will have to test to confirm it.
        sourceLoop:
        for (Object o : source) {
            for (int j = opIdx; j < ops.length; j++) {
                Operation op = ops[j];
                if ( (op.filter != null) && !op.filter.call(o) ) {
                    // stop processing this source item and go to the next one.
                    continue sourceLoop;
                }
                if (op.map != null) {
                    o = op.map.call(o);
                    // This is how map can handle takeWhile, take, and other termination marker
                    // roles.  Remember, the fewer functions we have to check for, the faster this
                    // will execute.
                    if (o == TERMINATE) {
                        return (H) ret;
                    }
                } else if (op.flatMap != null) {
                    ret = _foldLeft(op.flatMap.call(o), ops, j + 1, (H) ret, reducer);
                    // stop processing this source item and go to the next one.
                    continue sourceLoop;
                }
//                    if ( (op.terminate != null) && op.terminate.apply(o) ) {
//                        return (G) ret;
//                    }
            }
            // Here, the item made it through all the operations.  Combine it with the result.
            ret = reducer.call(ret, o);
        }
        return (H) ret;
    } // end _foldLeft();

    @Override public UnmodIterator<A> iterator() {
        // TODO: I had a really fast array-list implementation that I could probably hack into this for performance (assuming it actually works).
        return FunctionUtils.unmodIterable(toMutableList()).iterator();
    }

    // =============================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

//    public Xform<A> concatList(List<? extends A> list) {
//        if ( (list == null) || (list.size() < 1) ) { return this; }
//        return concat(list);
//    }

    @Override public Xform<A> concat(Iterable<? extends A> list) {
        if (list == null) { throw new IllegalArgumentException("Can't concat a null iterable"); }
        return new AppendIterDesc<>(this, new SourceProviderIterableDesc<>(list));
    }

//    @SafeVarargs
//    public final Xform<A> concatArray(A... list) {
//        if ( (list == null) || (list.length < 1) ) { return this; }
//        return concat(Arrays.asList(list));
//    }

//    public Xform<A> precatList(List<? extends A> list) {
//        if ( (list == null) || (list.size() < 1) ) { return this; }
//        return precat(list);
//    }

    @Override public Xform<A> precat(Iterable<? extends A> list) {
        if (list == null) { throw new IllegalArgumentException("Can't precat a null iterable"); }
        return new AppendIterDesc<>(of(list), this);
    }

//    @SafeVarargs
//    public final Xform<A> precatArray(A... list) {
//        if ( (list == null) || (list.length < 1) ) { return this; }
//        return precat(Arrays.asList(list));
//    }

    /** The number of items to drop from the beginning of the output. */
    @Override public Xform<A> drop(long n) {
        if (n < 0) { throw new IllegalArgumentException("Can't drop less than one item."); }
        return new DropDesc<>(this, n);
    }

    // Do we need a dropWhile???

    /** Provides a way to collect the results of the transformation. */
    @Override public <B> B foldLeft(B ident, Function2<B,? super A,B> reducer) {
        if (reducer == null) {
            throw new IllegalArgumentException("Can't foldLeft with a null reduction function.");
        }

        // Construct an optimized array of OpRuns (mutable operations for this run)
        RunList runList = toRunList();
        return _foldLeft(runList, runList.opArray(), 0, ident, reducer);
    }

    // TODO: Is this worth keeping over takeWhile(f).foldLeft(...)?
    /**
     Thit implementation should be correct, but could be slow in the case where previous operations
     are slow and the terminateWhen operation is fast and terminates early.  It actually renders
     items to a mutable List, then runs through the list performing the requested reduction,
     checking for early termination on the result.  If you can to a takeWhile() or take() earlier
     in the transform chain instead of doing it here, always do that.  If you really need early
     termination based on the *result* of a fold, and the operations are expensive or the input
     is huge, try using a View instead.  If you don't care about those things, then this method is
     perfect for you.

     {@inheritDoc}
     */
    @Override public <B> B foldLeft(B ident, Function2<B,? super A,B> reducer,
                                    Function1<? super B,Boolean> terminateWhen) {
        if (reducer == null) {
            throw new IllegalArgumentException("Can't foldLeft with a null reduction function.");
        }

        if ( (terminateWhen == null) || (Function1.reject() == terminateWhen) ) {
            return foldLeft(ident, reducer);
        }

        // Yes, this is a cheap plastic imitation of what you'd hope for if you really need this
        // method.  The trouble is that when I implemented it correctly in _foldLeft, I found
        // it was going to be incredibly difficult, or more likely impossible to implement
        // when the previous operation was flatMap, since you don't have the right result type to
        // check against when you recurse in to the flat mapping function, and if you check the
        // return from the recursion, it may have too many elements already.
        // In XformTest.java, there's something marked "Early termination test" that illustrates
        // this exact problem.
        List<A> as = this.toMutableList();
        for (A a : as) {
            ident = reducer.call(ident, a);
            if (terminateWhen.call(ident)) {
                return ident;
            }
        }
        return ident;
    }

    @Override public Xform<A> filter(Function1<? super A,Boolean> f) {
        if (f == null) { throw new IllegalArgumentException("Can't filter with a null function."); }
        return new FilterDesc<>(this, f);
    }

    @Override public <B> Xform<B> flatMap(Function1<? super A,Iterable<B>> f) {
        if (f == null) { throw new IllegalArgumentException("Can't flatmap with a null function."); }
        return new FlatMapDesc<>(this, f);
    }

    @Override public <B> Xform<B> map(Function1<? super A, ? extends B> f) {
        if (f == null) { throw new IllegalArgumentException("Can't map with a null function."); }
        return new MapDesc<>(this, f);
    }

    protected abstract RunList toRunList();

    @Override public Xform<A> take(long numItems) {
        if (numItems < 0) { throw new IllegalArgumentException("Num items must be >= 0"); }
        return new TakeDesc<>(this, numItems);
    }

    @Override public Xform<A> takeWhile(final Function1<? super A,Boolean> f) {
        if (f == null) {
            throw new IllegalArgumentException("Can't takeWhile with a null function.");
        }
        // I'm coding this as a map operation that either returns the source, or a TERMINATE
        // sentinel value.
        return new MapDesc<>(this, new Function1<A, A>() {
            @Override
            public A applyEx(A a) throws Exception {
                return f.call(a) ? a : terminate();
            }
        });
    }
}
