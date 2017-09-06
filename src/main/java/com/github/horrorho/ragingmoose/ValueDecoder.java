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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
class ValueDecoder {

    static class Entry {

        int nBits;
        int nBase;
        int vBits;
        int vBase;
    }

    private final Entry[] table;
    private int state;

    ValueDecoder(int nStates) {
        this.table = new Entry[nStates];
        for (int i = 0; i < table.length; i++) {
            this.table[i] = new Entry();
        }
        state = -1;
    }

    @Nonnull
    ValueDecoder load(short[] weights, byte[] symbolVBits, int[] symbolVBase) throws LZFSEDecoderException {
        try {
            int nSymbols = weights.length;
            int nStates = table.length;
            int nZero = Integer.numberOfLeadingZeros(nStates);

            int t = 0;
            for (int i = 0; i < nSymbols; i++) {
                int f = weights[i];
                if (f == 0) {
                    continue;
                }

                int k = Integer.numberOfLeadingZeros(f) - nZero;
                int x = (2 * nStates >>> k) - f;

                byte vBits = symbolVBits[i];
                int vBase = symbolVBase[i];

                for (int j = 0; j < f; j++) {
                    Entry e = table[t++];
                    e.vBase = vBase;
                    e.vBits = vBits;

                    if (j < x) {
                        e.nBits = k + vBits;
                        e.nBase = (f + j << k) - nStates;
                    } else {
                        e.nBits = k - 1 + vBits;
                        e.nBase = j - x << k - 1;
                    }
                }
            }
            return this;
            
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new LZFSEDecoderException(ex);
        }
    }

    @Nonnull
    ValueDecoder state(int state) {
        this.state = state;
        return this;
    }

    int decode(BitInStream is) throws LZFSEDecoderException {
        Entry e = table[state];
        int bits = (int) is.read(e.nBits);
        int vBits = e.vBits;
        state = e.nBase + (bits >>> vBits);
        return e.vBase + (bits & (1 << vBits) - 1);
    }

    @Override
    public String toString() {
        return "ValueDecoder{"
                + "table.length=" + table.length
                + ", state=" + state
                + '}';
    }
}
