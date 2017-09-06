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
class LZFSEBlockDecoder {

    private static final byte[] L_EXTRA_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 5, 8
    };

    private static final int[] L_BASE_VALUE = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 28, 60
    };

    private static final byte[] M_EXTRA_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5, 8, 11
    };

    private static final int[] M_BASE_VALUE = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 56, 312
    };

    private static final byte[] D_EXTRA_BITS = {
        0,  0,  0,  0,  1,  1,  1,  1,  2,  2,  2,  2,  3,  3,  3,  3,
        4,  4,  4,  4,  5,  5,  5,  5,  6,  6,  6,  6,  7,  7,  7,  7,
        8,  8,  8,  8,  9,  9,  9,  9,  10, 10, 10, 10, 11, 11, 11, 11,
        12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15
    };

    private static final int[] D_BASE_VALUE = {
        0,      1,      2,      3,     4,     6,     8,     10,    12,    16,
        20,     24,     28,     36,    44,    52,    60,    76,    92,    108,
        124,    156,    188,    220,   252,   316,   380,   444,   508,   636,
        764,    892,    1020,   1276,  1532,  1788,  2044,  2556,  3068,  3580,
        4092,   5116,   6140,   7164,  8188,  10236, 12284, 14332, 16380, 20476,
        24572,  28668,  32764,  40956, 49148, 57340, 65532, 81916, 98300, 114684,
        131068, 163836, 196604, 229372
    };

    private final ValueDecoder lValueDecoder = new ValueDecoder(LZFSE.ENCODE_L_STATES);
    private final ValueDecoder mValueDecoder = new ValueDecoder(LZFSE.ENCODE_M_STATES);
    private final ValueDecoder dValueDecoder = new ValueDecoder(LZFSE.ENCODE_D_STATES);

    private final LiteralDecoder literalDecoder = new LiteralDecoder(LZFSE.ENCODE_LITERAL_STATES);

    private final byte[] literals = new byte[LZFSE.LITERALS_PER_BLOCK + 64];

    @Nullable
    private ByteBuffer bb;

    private int rawBytes;
    private int nLmdPayloadBytes;
    private int lmdBits;
    private int symbols;

    @Nonnull
    LZFSEBlockDecoder init(LZFSEBlockHeader bh) throws LZFSEDecoderException {
        lValueDecoder.load(bh.lFreq(), L_EXTRA_BITS, L_BASE_VALUE)
                .state(bh.lState());
        mValueDecoder.load(bh.mFreq(), M_EXTRA_BITS, M_BASE_VALUE)
                .state(bh.mState());
        dValueDecoder.load(bh.dFreq(), D_EXTRA_BITS, D_BASE_VALUE)
                .state(bh.dState());
        literalDecoder.load(bh.literalFreq())
                .state(bh.literalState0(), bh.literalState1(), bh.literalState2(), bh.literalState3())
                .nLiteralPayloadBytes(bh.nLiteralPayloadBytes())
                .nLiterals(bh.nLiterals())
                .literalBits(bh.literalBits());

        rawBytes = bh.nRawBytes();
        nLmdPayloadBytes = bh.nLmdPayloadBytes();
        lmdBits = bh.lmdBits();
        symbols = bh.nMatches();

        return this;
    }

    public int rawBytes() {
        return rawBytes;
    }

    @Nonnull
    LZFSEBlockDecoder apply(@WillNotClose InputStream is, @WillNotClose MatchOutputStream maos)
            throws IOException, LZFSEDecoderException {
        try {
            literalDecoder.decode(is, literals);
            int literal = 0;

            initBuffer();
            IO.readFully(is, bb);
            BitInStream in = new BitInStream(bb)
                    .init(lmdBits);

            int d = 0;
            while (symbols > 0) {
                in.fill();
                int l = lValueDecoder.decode(in);
                int m = mValueDecoder.decode(in);
                int _d = dValueDecoder.decode(in);
                d = _d == 0 ? d : _d;

                maos.write(literals, literal, l);
                literal += l;
                maos.writeMatch(d, m);
                symbols--;
            }

            return this;

        } catch (IllegalArgumentException ex) {
            throw new LZFSEDecoderException(ex);
        }
    }

    ByteBuffer initBuffer() {
        int capacity = 32 + nLmdPayloadBytes;
        if (bb == null || bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(capacity).order(LITTLE_ENDIAN);
        } else {
            bb.limit(capacity);
        }
        bb.position(32);
        return bb;
    }

    @Override
    public String toString() {
        return "LZFSEBlockDecoder{"
                + "lValueDecoder=" + lValueDecoder
                + ", mValueDecoder=" + mValueDecoder
                + ", dValueDecoder=" + dValueDecoder
                + ", literalDecoder=" + literalDecoder
                + ", literalsOff=" + literals.length
                + ", bb=" + bb
                + ", rawBytes=" + rawBytes
                + ", nLmdPayloadBytes=" + nLmdPayloadBytes
                + ", lmdBits=" + lmdBits
                + ", symbols=" + symbols
                + '}';
    }
}
