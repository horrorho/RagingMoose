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
import java.io.OutputStream;
import static java.lang.Integer.toHexString;
import java.nio.ByteBuffer;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
public class LZFSEDecoder {

    private final ByteBuffer mb = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);

    private final LZFSEBlockHeader lzfseBlockHeader;
    private final LZVNBlockHeader lzvnBlockHeader;
    private final RawBlockHeader rawBlockHeader;
    private final LZFSEBlockDecoder decoder;
    private final LZVNBlockDecoder lzvnDecoder;

    private final byte[] dBuffer;
    private final int dBufferMask;

    LZFSEDecoder(LZFSEBlockHeader lzfseBlockHeader,
            LZVNBlockHeader lzvnBlockHeader,
            RawBlockHeader rawBlockHeader,
            LZFSEBlockDecoder decoder,
            LZVNBlockDecoder lzvnDecoder,
            byte[] dBuffer,
            int dBufferMask) {
        this.lzfseBlockHeader = Objects.requireNonNull(lzfseBlockHeader);
        this.lzvnBlockHeader = Objects.requireNonNull(lzvnBlockHeader);
        this.rawBlockHeader = Objects.requireNonNull(rawBlockHeader);
        this.decoder = Objects.requireNonNull(decoder);
        this.lzvnDecoder = Objects.requireNonNull(lzvnDecoder);
        this.dBuffer = Objects.requireNonNull(dBuffer);
        this.dBufferMask = dBufferMask;
    }

    public LZFSEDecoder() {
        this(new LZFSEBlockHeader(),
                new LZVNBlockHeader(),
                new RawBlockHeader(),
                new LZFSEBlockDecoder(),
                new LZVNBlockDecoder(),
                new byte[LZFSE.D_BUFFER_SIZE],
                LZFSE.D_BUFFER_MASK);
    }

    @Nonnull
    public LZFSEDecoder decode(@WillNotClose InputStream is, @WillNotClose OutputStream os)
            throws IOException, LZFSEDecoderException {
        MatchOutputStream maos = new MatchOutputStream(os, dBuffer, dBufferMask);
        while (true) {
            int magic = magic(is);
            switch (magic) {
                case LZFSE.COMPRESSEDV2_BLOCK_MAGIC:
                    v2(is, maos);
                    break;
                case LZFSE.COMPRESSEDV1_BLOCK_MAGIC:
                    v1(is, maos);
                    break;
                case LZFSE.COMPRESSEDLZVN_BLOCK_MAGIC:
                    vn(is, maos);
                    break;
                case LZFSE.UNCOMPRESSED_BLOCK_MAGIC:
                    raw(is, maos);
                    break;
                case LZFSE.ENDOFSTREAM_BLOCK_MAGIC:
                    maos.flush();
                    return this;
                default:
                    throw new LZFSEDecoderException("bad block: 0x" + toHexString(magic));
            }
        }
    }

    void v1(@WillNotClose InputStream is, @WillNotClose MatchOutputStream maos)
            throws IOException, LZFSEDecoderException {
        lzfseBlockHeader.loadV1(is);
        decoder.init(lzfseBlockHeader)
                .apply(is, maos);
    }

    void v2(@WillNotClose InputStream is, @WillNotClose MatchOutputStream maos)
            throws IOException, LZFSEDecoderException {
        lzfseBlockHeader.loadV2(is);
        decoder.init(lzfseBlockHeader)
                .apply(is, maos);
    }

    void vn(@WillNotClose InputStream is, @WillNotClose MatchOutputStream maos)
            throws IOException, LZFSEDecoderException {
        lzvnBlockHeader.load(is);
        lzvnDecoder.apply(is, maos);
    }

    void raw(@WillNotClose InputStream is, @WillNotClose OutputStream os)
            throws IOException, LZFSEDecoderException {
        rawBlockHeader.load(is);
        IO.copy(is, os, rawBlockHeader.nRawBytes());
    }

    int magic(@WillNotClose InputStream is) throws IOException {
        mb.rewind();
        IO.readFully(is, mb).flip();
        return mb.getInt();
    }

    @Override
    public String toString() {
        return "LZFSEDecoder{"
                + "mb=" + mb
                + ", lzfseBlockHeader=" + lzfseBlockHeader
                + ", lzvnBlockHeader=" + lzvnBlockHeader
                + ", rawBlockHeader=" + rawBlockHeader
                + ", decoder=" + decoder
                + ", lzvnDecoder=" + lzvnDecoder
                + ", dBuffer=" + dBuffer.length
                + ", dBufferMask=" + dBufferMask
                + '}';
    }
}
