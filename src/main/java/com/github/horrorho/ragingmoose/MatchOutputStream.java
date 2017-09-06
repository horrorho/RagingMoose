/*
 * The MIT License
 *
 * Copyright 2017 Ayesha.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.ragingmoose;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
class MatchOutputStream extends FilterOutputStream {

    private final OutputStream os;
    private final byte[] bs;
    private long n;
    private int p;
    private int mask;

    MatchOutputStream(@WillNotClose OutputStream os, byte[] bs, int mask, long n, int p) {
        super(os);
        if (mask != bs.length - 1) {
            throw new IllegalArgumentException();
        }
        this.os = Objects.requireNonNull(os);
        this.bs = Objects.requireNonNull(bs);
        this.mask = mask;
        this.n = n;
        this.p = p;
    }

    MatchOutputStream(@WillNotClose OutputStream os, byte[] bs, int mask) {
        this(os, bs, mask, 0, 0);
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
        bs[p++] = (byte) b;
        p &= mask;
        n++;
    }

    void writeMatch(int d, int m) throws IOException {
        if (d <= 0) {
            throw new IllegalArgumentException();
        } else if (m < 0) {
            throw new IllegalArgumentException();
        } else if (d > n) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < m; i++) {
            write(bs[(p - d) & mask]);
        }
    }

    long count() {
        return n;
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
