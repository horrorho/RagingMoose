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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
class LiteralDecoder {

    static class Entry {

        int nBits;
        int nBase;
        byte symbol;
    }

    private final Entry[] table;

    @Nullable
    private ByteBuffer bb;

    private int nLiteralPayloadBytes;
    private int nLiterals;
    private int literalBits;
    private int state0;
    private int state1;
    private int state2;
    private int state3;

    LiteralDecoder(int nStates) {
        this.table = new Entry[nStates];
        for (int i = 0; i < table.length; i++) {
            this.table[i] = new Entry();
        }
    }

    @Nonnull
    LiteralDecoder load(short[] weights) throws LZFSEDecoderException {
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

                for (int j = 0; j < f; j++) {
                    Entry e = table[t++];
                    e.symbol = (byte) i;

                    if (j < x) {
                        e.nBits = k;
                        e.nBase = (f + j << k) - nStates;
                    } else {
                        e.nBits = k - 1;
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
    LiteralDecoder state(int state0, int state1, int state2, int state3) {
        this.state0 = state0;
        this.state1 = state1;
        this.state2 = state2;
        this.state3 = state3;
        return this;
    }

    @Nonnull
    LiteralDecoder nLiteralPayloadBytes(int nLiteralPayloadBytes) {
        this.nLiteralPayloadBytes = nLiteralPayloadBytes;
        return this;
    }

    @Nonnull
    LiteralDecoder nLiterals(int nLiterals) {
        this.nLiterals = nLiterals;
        return this;
    }

    @Nonnull
    LiteralDecoder literalBits(int literalBits) {
        this.literalBits = literalBits;
        return this;
    }

    @Nonnull
    LiteralDecoder decode(@WillNotClose InputStream is, byte[] literals) throws IOException, LZFSEDecoderException {
        initBuffer();
        IO.readFully(is, bb);
        BitInStream in = new BitInStream(bb)
                .init(literalBits);

        for (int i = 0; i < nLiterals; i += 4) {
            in.fill();

            Entry e = table[state0];
            state0 = e.nBase + (int) in.read(e.nBits);
            literals[i + 0] = e.symbol;

            e = table[state1];
            state1 = e.nBase + (int) in.read(e.nBits);
            literals[i + 1] = e.symbol;

            e = table[state2];
            state2 = e.nBase + (int) in.read(e.nBits);
            literals[i + 2] = e.symbol;

            e = table[state3];
            state3 = e.nBase + (int) in.read(e.nBits);
            literals[i + 3] = e.symbol;
        }
        return this;
    }

    ByteBuffer initBuffer() {
        int capacity = 8 + nLiteralPayloadBytes;
        if (bb == null || bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(capacity).order(LITTLE_ENDIAN);
        } else {
            bb.limit(capacity);
        }
        bb.position(8);
        return bb;
    }

    @Override
    public String toString() {
        return "LiteralDecoder{"
                + "entries=" + table.length
                + ", nLiteralPayloadBytes=" + nLiteralPayloadBytes
                + ", nLiterals=" + nLiterals
                + ", literalBits=" + literalBits
                + ", state0=" + state0
                + ", state1=" + state1
                + ", state2=" + state2
                + ", state3=" + state3
                + '}';
    }
}
