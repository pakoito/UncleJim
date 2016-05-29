// Copyright 2015-04-13 PlanBase Inc. & Glen Peterson
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

import org.organicdesign.fp.collections.interfaces.UnmodSet;

/** An immutable set interface */
public abstract class ImSet<E> implements UnmodSet<E> {
    /**
     Adds an element, returning a modified version of the set (leaving the original set unchanged).
     If the element already exists in this set, the new value overwrites the old one.  If the new
     element is the same as an old element (based on the address of that item in memory, not an
     equals test), the old set is returned unchanged.

     @param e the element to add to this set
     @return a new set with the element added (see note above about adding duplicate elements).
     */
    public abstract ImSet<E> put(E e);

    /**
     Removes the given item, returning a modified version of the set (leaving the original set
     unchanged).
     */
    public abstract ImSet<E> without(E key);

//    /**
//     A sequence of the items contained in this set.  Note that for some implementations, multiple
//     calls to seq() will yield sequences with different ordering of the same elements.
//     */
//    Sequence<E> seq();

    public ImSet<E> union(Iterable<? extends E> iter) {
        if (iter == null) { return this; }
        ImSet<E> ret = this;
        for (E e : iter) { ret = ret.put(e); }
        return ret;
    }

//    /** {@inheritDoc} */
//    @Override UnmodIterator<E> iterator();

//    /**
//     This method goes against Josh Bloch's Item 25: "Prefer Lists to Arrays", but is provided for
//     backwards compatibility in some performance-critical situations.  If you really need an array,
//     consider using the somewhat type-safe version of this method instead, but read the caveats
//     first.
//     {@inheritDoc}
//     */
//    @Override default Object[] toArray() { return UnmodSet.super.toArray(); }
}
