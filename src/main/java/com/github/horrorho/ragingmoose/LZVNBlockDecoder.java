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
import java.util.Objects;
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
final class LZVNBlockDecoder {

    private interface Op {

        boolean call(int opc) throws LZFSEDecoderException, IOException;
    }

    private final Op[] tbl = new Op[]{
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::eos,  this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::nop,  this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::nop,  this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::udef, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::udef, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::udef, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::udef, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::udef, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef,
        this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD,
        this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD,
        this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD,
        this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD, this::medD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::smlD, this::preD, this::lrgD,
        this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef,
        this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef, this::udef,
        this::lrgL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL,
        this::smlL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL, this::smlL,
        this::lrgM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM,
        this::smlM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM, this::smlM};

    @Nullable
    private InputStream is;
    @Nullable
    private MatchOutputStream maos;

    private int dPrev;

    LZVNBlockDecoder apply(@WillNotClose InputStream is, @WillNotClose MatchOutputStream maos)
            throws LZFSEDecoderException, IOException {
        return is(is).os(maos).decode();
    }

    LZVNBlockDecoder is(@WillNotClose InputStream is) {
        this.is = Objects.requireNonNull(is);
        return this;
    }

    LZVNBlockDecoder os(@WillNotClose MatchOutputStream maos) {
        this.maos = Objects.requireNonNull(maos);
        return this;
    }

    LZVNBlockDecoder decode() throws LZFSEDecoderException, IOException {
        boolean more;
        do {
            int opc = loadByte();
            more = tbl[opc].call(opc);
        } while (more);
        return this;
    }

    int loadByte() throws IOException {
        return is.read();
    }

    int loadShort() throws IOException {
        return is.read() + (is.read() << 8);
    }

    boolean copyLiteralMatch(int l, int m) throws LZFSEDecoderException, IOException {
        return copyLiteral(l) && copyMatch(m);
    }

    boolean copyLiteralMatch(int l, int m, int d) throws LZFSEDecoderException, IOException {
        return copyLiteral(l) && copyMatch(m, d);
    }

    boolean copyLiteral(int l) throws IOException {
        for (int i = 0; i < l; i++) {
            maos.write(is.read());
        }
        return true;
    }

    boolean copyMatch(int m) throws LZFSEDecoderException, IOException {
        return copyMatch(m, dPrev);
    }

    boolean copyMatch(int m, int d) throws LZFSEDecoderException, IOException {
        dPrev = d;
        try {
            maos.writeMatch(d, m);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new LZFSEDecoderException(ex);
        }
        return true;
    }

    boolean smlL(int opc) throws IOException {
        // 1110LLLL LITERAL
        int l = opc & 0x0F;
        return copyLiteral(l);
    }

    boolean lrgL(int opc) throws IOException {
        // 11100000 LLLLLLLL LITERAL
        int l = loadByte() + 16;
        return copyLiteral(l);
    }

    boolean smlM(int opc) throws LZFSEDecoderException, IOException {
        // 1111MMMM
        int m = opc & 0xF;
        return copyMatch(m);
    }

    boolean lrgM(int opc) throws LZFSEDecoderException, IOException {
        // 11110000 MMMMMMMM
        int m = loadByte() + 16;
        return copyMatch(m);
    }

    boolean preD(int opc) throws LZFSEDecoderException, IOException {
        // LLMMM110
        int l = opc >>> 6 & 0x03;
        int m = (opc >>> 3 & 0x07) + 3;
        return copyLiteralMatch(l, m);
    }

    boolean smlD(int opc) throws LZFSEDecoderException, IOException {
        // LLMMMDDD DDDDDDDD LITERAL
        int l = opc >>> 6 & 0x03;
        int m = (opc >>> 3 & 0x07) + 3;
        int d = (opc & 0x07) << 8 | loadByte();
        return copyLiteralMatch(l, m, d);
    }

    boolean medD(int opc) throws LZFSEDecoderException, IOException {
        // 101LLMMM DDDDDDMM DDDDDDDD LITERAL
        int s = loadShort();
        int l = opc >>> 3 & 0x03;
        int m = ((opc & 0x7) << 2 | (s & 0x03)) + 3;
        int d = s >>> 2;
        return copyLiteralMatch(l, m, d);
    }

    boolean lrgD(int opc) throws LZFSEDecoderException, IOException {
        // LLMMM111 DDDDDDDD DDDDDDDD LITERAL        
        int l = opc >>> 6 & 0x03;
        int m = (opc >>> 3 & 0x07) + 3;
        int d = loadShort();
        return copyLiteralMatch(l, m, d);
    }

    boolean eos(int opc) {
        return false;
    }

    boolean nop(int opc) {
        return true;
    }

    boolean udef(int opc) throws LZFSEDecoderException {
        throw new LZFSEDecoderException();
    }

    @Override
    public String toString() {
        return "LZVNBlockDecoder{"
                + "is=" + is
                + ", maos=" + maos
                + ", dPrev=" + dPrev
                + '}';
    }
}
