package org.organicdesign.fp.function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.organicdesign.fp.StaticImports;
import org.organicdesign.fp.collections.ImList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.organicdesign.fp.FunctionUtils.ordinal;
import static org.organicdesign.fp.StaticImports.vec;
import static org.organicdesign.fp.function.Function1.accept;

@RunWith(JUnit4.class)
public class Function1Test {
    @Test(expected = RuntimeException.class)
    public void applyIOException() {
        new Function1<Integer,Integer>() {
            @Override public Integer applyEx(Integer o) throws Exception {
                if (o < 10) {
                    throw new IOException("test exception");
                }
                return o;
            }
        }.call(3);
    }

    @Test(expected = IllegalStateException.class)
    public void applyIllegalStateException() {
        new Function1<Integer,Integer>() {
            @Override public Integer applyEx(Integer o) throws Exception {
                if (o < 10) {
                    throw new IllegalStateException("test exception");
                }
                return o;
            }
        }.call(3);
    }

    static Function1<Object,Boolean> NOT_PROCESSED = new Function1<Object, Boolean>() {
        @Override
        public Boolean applyEx(Object o) throws Exception {
            throw new IllegalStateException("Didn't short-circuit");
        }
    };

    @Test public void composePredicatesWithAnd() {
        assertEquals(Function1.ACCEPT, Function1.and(null));
        assertEquals(Function1.ACCEPT, Function1.and(StaticImports.<Function1<Object, Boolean>>vec()));
        assertEquals(Function1.ACCEPT, Function1.and(Collections.<Function1<Object, Boolean>>emptyList()));

        assertEquals(Function1.REJECT,
                     Function1.and(vec(Function1.reject(),
                                       NOT_PROCESSED)));

        assertEquals(Function1.REJECT,
                     Function1.and(vec(null, null, null, Function1.reject(),
                                       NOT_PROCESSED)));

        assertEquals(Function1.REJECT,
                     Function1.and(Arrays.asList(null, null, null, Function1.reject(),
                                                 NOT_PROCESSED)));

        assertEquals(Function1.REJECT,
                     Function1.and(vec(accept(),
                                       accept(),
                                       accept(),
                                       Function1.reject(),
                                       NOT_PROCESSED)));

        assertEquals(Function1.REJECT,
                     Function1.and(vec(Function1.reject(),
                                       accept(),
                                       accept(),
                                       accept())));

        assertEquals(Function1.ACCEPT,
                     Function1.and(vec(accept())));
    }

    @Test public void composePredicatesWithOr() {
        assertEquals(Function1.REJECT, Function1.or(null));

        assertEquals(Function1.ACCEPT,
                     Function1.or(vec(accept(),
                                      NOT_PROCESSED)));

        assertEquals(Function1.ACCEPT,
                     Function1.or(vec(null, null, null, accept(),
                                      NOT_PROCESSED)));

        assertEquals(Function1.ACCEPT,
                     Function1.or(Arrays.asList(null, null, null, accept(),
                                                NOT_PROCESSED)));

        assertEquals(Function1.ACCEPT,
                     Function1.or(vec(Function1.reject(),
                                      Function1.reject(),
                                      Function1.reject(),
                                      accept(),
                                      NOT_PROCESSED)));

        assertEquals(Function1.ACCEPT,
                     Function1.or(vec(accept(),
                                      Function1.reject(),
                                      Function1.reject(),
                                      Function1.reject())));

        assertEquals(Function1.REJECT,
                     Function1.or(vec(Function1.reject())));
    }

    @Test public void compose() {
        assertEquals(Function1.IDENTITY,
                     Function1.compose((Iterable<Function1<String,String>>) null));

        assertEquals(Function1.IDENTITY, Function1.compose(StaticImports.<Function1<Object, Object>>vec(null, null, null)));

        assertEquals(Function1.IDENTITY, Function1.compose(vec(null, Function1.identity(), null)));

        // FIXME Java 7 has trouble complying with this call
//        assertEquals(Function1.ACCEPT, Function1.compose(StaticImports.<Function1<Object, Object>>vec(null, Function1.<Boolean>identity(), null,
//                                                                    Function1.accept())));

        Function1<Integer,String> intToStr = new Function1<Integer, String>() {
            @Override
            public String applyEx(Integer i) throws Exception {
                return (i == 0) ? "zero" :
                       (i == 1) ? "one" :
                       (i == 2) ? "two" : "unknown";
            }
        };
        Function1<String,String> wordToOrdinal = new Function1<String, String>() {
            @Override
            public String applyEx(String s) throws Exception {
                return ("one".equals(s)) ? "first" :
                       ("two".equals(s)) ? "second" : s;
            }
        };
        Function1<Integer,String> f = wordToOrdinal.compose(intToStr);
        assertEquals("unknown", f.call(-1));
        assertEquals("zero", f.call(0));
        assertEquals("first", f.call(1));
        assertEquals("second", f.call(2));
        assertEquals("unknown", f.call(3));

        Function1<Integer,String> g = intToStr.compose(Function1.<Integer>identity());
        assertEquals("unknown", g.call(-1));
        assertEquals("zero", g.call(0));
        assertEquals("one", g.call(1));
        assertEquals("two", g.call(2));
        assertEquals("unknown", g.call(3));

        Function1<Integer,String> h = Function1.<String>identity().compose(intToStr);
        assertEquals("unknown", h.call(-1));
        assertEquals("zero", h.call(0));
        assertEquals("one", h.call(1));
        assertEquals("two", h.call(2));
        assertEquals("unknown", h.call(3));

        Function1<String,String> i = Function1.compose(vec(new Function1<String, String>() {
                                                               @Override
                                                               public String applyEx(String s) throws Exception {
                                                                   return s.substring(0, s.indexOf(" hundred"));
                                                               }
                                                           },
                wordToOrdinal));
        assertEquals("zillion", i.call("zillion hundred"));
        assertEquals("zero", i.call("zero hundred"));
        assertEquals("first", i.call("one hundred"));
        assertEquals("second", i.call("two hundred"));
        assertEquals("three", i.call("three hundred"));
    }

    @Test
    public void filtersOfPredicates() {
        Integer[] oneToNineArray = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ImList<Integer> oneToNine = vec(oneToNineArray);

        assertEquals(Function1.ACCEPT, Function1.or(Function1.<Integer>accept(), lowerThan(6)));
        assertEquals(Function1.ACCEPT, Function1.or(lowerThan(6), Function1.<Integer>accept()));

        assertArrayEquals(oneToNineArray,
                          oneToNine.filter(Function1.or(lowerThan(3),
                                  Function1.<Integer>accept()))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(oneToNineArray,
                          oneToNine.filter(Function1.or(Function1.<Integer>accept(),
                                                        largerThan(5)))
                                   .toMutableList()
                                   .toArray());


        assertArrayEquals(new Integer[]{6, 7, 8, 9},
                          oneToNine.filter(Function1.or(Function1.<Integer>reject(),
                                                        largerThan(5)))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{1, 2},
                          oneToNine.filter(Function1.or(lowerThan(3),
                                                        Function1.<Integer>reject()))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{1, 2, 6, 7, 8, 9},
                          oneToNine.filter(Function1.or(lowerThan(3),
                                                        largerThan(5)))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{1, 2, 4, 6, 7, 8, 9},
                          oneToNine.filter(Function1.or(vec(lowerThan(3),
                                  equalTo(4),
                                  largerThan(5))))
                                   .toMutableList()
                                   .toArray());

        // and(a, b)
        assertEquals(Function1.REJECT, Function1.and(Function1.<Integer>reject(), lowerThan(6)));
        assertEquals(Function1.REJECT, Function1.and(lowerThan(6), Function1.<Integer>reject()));

        assertArrayEquals(new Integer[]{},
                          oneToNine.filter(Function1.and(largerThan(2),
                                                         Function1.<Integer>reject()))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{},
                          oneToNine.filter(Function1.and(Function1.<Integer>reject(),
                                                         largerThan(2)))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{3, 4, 5, 6, 7, 8, 9},
                          oneToNine.filter(Function1.and(largerThan(2),
                                                         Function1.<Integer>accept()))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5},
                          oneToNine.filter(Function1.and(Function1.<Integer>accept(),
                                                         lowerThan(6)))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{3, 4, 5},
                          oneToNine.filter(Function1.and(largerThan(2),
                                                         lowerThan(6)))
                                   .toMutableList()
                                   .toArray());

        assertArrayEquals(new Integer[]{4, 5},
                          oneToNine.filter(Function1.and(vec(largerThan(2),
                                                             largerThan(3),
                                                             lowerThan(3))))
                                   .toMutableList()
                                   .toArray());

        assertEquals(Function1.REJECT, Function1.negate(accept()));
        assertEquals(Function1.ACCEPT, Function1.negate(Function1.reject()));

        assertArrayEquals(new Integer[]{1, 2},
                          oneToNine.filter(Function1.negate(largerThan(2)))
                                   .toMutableList()
                                   .toArray());
    }

    private Function1<Integer, Boolean> equalTo(final int number) {
        return new Function1<Integer, Boolean>() {
            @Override
            public Boolean applyEx(Integer integer) throws Exception {
                return integer == number;
            }
        };
    }

    private Function1<Integer, Boolean> largerThan(final int number) {
        return new Function1<Integer, Boolean>() {
            @Override
            public Boolean applyEx(Integer i) throws Exception {
                return i > number;
            }
        };
    }

    private Function1<Integer, Boolean> lowerThan(final int number) {
        return new Function1<Integer, Boolean>() {
            @Override
            public Boolean applyEx(Integer i) throws Exception {
                return i < number;
            }
        };
    }

    @Test public void testMemoize() {
        final int MAX_INT = 1000;
        final AtomicInteger counter = new AtomicInteger(0);
        Function1<Integer,String> f = Function1.memoize(new Function1<Integer, String>() {
            @Override
            public String applyEx(Integer i) throws Exception {
                counter.getAndIncrement();
                return ordinal(i);
            }
        });

        assertEquals(0, counter.get());

        // Call function a bunch of times, memoizing the results.
        for (int i = 0; i < MAX_INT; i++) {
            assertEquals(ordinal(i), f.call(i));
        }
        // Assert count of calls equals the actual number.
        assertEquals(MAX_INT, counter.get());

        // Make all those calls again.
        for (int i = 0; i < MAX_INT; i++) {
            assertEquals(ordinal(i), f.call(i));
            // this is for compatibility with Consumer.
            f.call(i);
        }

        // Assert that function has not actually been called again.
        assertEquals(MAX_INT, counter.get());
    }
}
