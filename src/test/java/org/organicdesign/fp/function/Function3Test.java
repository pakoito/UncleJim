package org.organicdesign.fp.function;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class Function3Test {
    @Test(expected = RuntimeException.class)
    public void applyIOException() {
        new Function3<Integer,Integer,Integer,Integer>() {
            @Override public Integer applyEx(Integer a, Integer b, Integer c) throws Exception {
                if (a < b) {
                    throw new IOException("test exception");
                }
                return a;
            }
        }.call(1, 2, 3);
    }

    @Test(expected = IllegalStateException.class)
    public void applyIllegalStateException() {
        new Function3<Integer,Integer,Integer,Integer>() {
            @Override public Integer applyEx(Integer a, Integer b, Integer c) throws Exception {
                if (a < b) {
                    throw new IllegalStateException("test exception");
                }
                return a;
            }
        }.call(3, 4, 5);
    }

    @Test public void memoize() {
        final AtomicInteger counter = new AtomicInteger(0);
        Function3<Boolean,Integer,Double,String> f = new Function3<Boolean, Integer, Double, String>() {
            @Override
            public String applyEx(Boolean b, Integer l, Double d) throws Exception {
                counter.getAndIncrement();
                return (b ? "+" : "-") + String.valueOf(l) + "~" + String.valueOf(d);
            }
        };
        Function3<Boolean,Integer,Double,String> g = Function3.memoize(f);
        assertEquals("+3~2.5", g.call(true, 3, 2.5));
        assertEquals(1, counter.get());
        assertEquals("+3~2.5", g.call(true, 3, 2.5));
        assertEquals(1, counter.get());

        assertEquals("+3~2.5", f.call(true, 3, 2.5));
        assertEquals(2, counter.get());

        assertEquals("+3~2.5", g.call(true, 3, 2.5));
        assertEquals(2, counter.get());

        assertEquals("-5~4.3", g.call(false, 5, 4.3));
        assertEquals(3, counter.get());
        assertEquals("+3~2.5", g.call(true, 3, 2.5));
        assertEquals(3, counter.get());
        assertEquals("-5~4.3", g.call(false, 5, 4.3));
        assertEquals(3, counter.get());
        assertEquals("+3~2.5", g.call(true, 3, 2.5));
        assertEquals(3, counter.get());
    }

}
