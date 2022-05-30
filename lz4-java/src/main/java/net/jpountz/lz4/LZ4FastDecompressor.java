package net.jpountz.lz4;

import java.nio.ByteBuffer;

/*
 * Copyright 2020 Adrien Grand and the lz4-java contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * LZ4 decompressor that requires the size of the original input to be known.
 * Use {@link LZ4SafeDecompressor} if you only know the size of the
 * compressed stream.
 * <p>
 * From lz4-java 1.6.0, it is deprecated to use a JNI-binding instance
 * of this class; i.e., an instasnce returned by
 * {@link LZ4Factory#fastDecompressor()} of {@link LZ4Factory#nativeInstance()}.
 * Please see {@link LZ4Factory#nativeInstance()} for details.
 * <p>
 * Instances of this class are thread-safe.
 *
 * @see LZ4Factory#nativeInstance()
 */
public abstract class LZ4FastDecompressor implements LZ4Decompressor {

  /** Decompresses <code>src[srcOff:]</code> into <code>dest[destOff:destOff+destLen]</code>
   * and returns the number of bytes read from <code>src</code>.
   * <code>destLen</code> must be exactly the size of the decompressed data.
   *
   * @param src the compressed data
   * @param srcOff the start offset in src
   * @param dest the destination buffer to store the decompressed data
   * @param destOff the start offset in dest
   * @param destLen the <b>exact</b> size of the original input
   * @return the number of bytes read to restore the original input
   */
  public abstract int decompress(byte[] src, int srcOff, byte[] dest, int destOff, int destLen);

  /** Decompresses <code>src[srcOff:]</code> into <code>dest[destOff:destOff+destLen]</code>
   * and returns the number of bytes read from <code>src</code>.
   * <code>destLen</code> must be exactly the size of the decompressed data.
   * The positions and limits of the {@link ByteBuffer}s remain unchanged.
   *
   * @param src the compressed data
   * @param srcOff the start offset in src
   * @param dest the destination buffer to store the decompressed data
   * @param destOff the start offset in dest
   * @param destLen the <b>exact</b> size of the original input
   * @return the number of bytes read to restore the original input
   */
  public abstract int decompress(ByteBuffer src, int srcOff, ByteBuffer dest, int destOff, int destLen);

  /**
   * Convenience method, equivalent to calling
   * {@link #decompress(byte[], int, byte[], int, int) decompress(src, 0, dest, 0, destLen)}.
   *
   * @param src the compressed data
   * @param dest the destination buffer to store the decompressed data
   * @param destLen the <b>exact</b> size of the original input
   * @return the number of bytes read to restore the original input
   */
  public final int decompress(byte[] src, byte[] dest, int destLen) {
    return decompress(src, 0, dest, 0, destLen);
  }

  /**
   * Convenience method, equivalent to calling
   * {@link #decompress(byte[], byte[], int) decompress(src, dest, dest.length)}.
   *
   * @param src the compressed data
   * @param dest the destination buffer to store the decompressed data
   * @return the number of bytes read to restore the original input
   */
  public final int decompress(byte[] src, byte[] dest) {
    return decompress(src, dest, dest.length);
  }

  /**
   * Convenience method which returns <code>src[srcOff:?]</code>
   * decompressed.
   * <p><b><span style="color:red">Warning</span></b>: this method has an
   * important overhead due to the fact that it needs to allocate a buffer to
   * decompress into.</p>
   * <p>Here is how this method is implemented:</p>
   * <pre>
   * final byte[] decompressed = new byte[destLen];
   * decompress(src, srcOff, decompressed, 0, destLen);
   * return decompressed;
   * </pre>
   *
   * @param src the compressed data
   * @param srcOff the start offset in src
   * @param destLen the <b>exact</b> size of the original input
   * @return the decompressed data
   */
  public final byte[] decompress(byte[] src, int srcOff, int destLen) {
    final byte[] decompressed = new byte[destLen];
    decompress(src, srcOff, decompressed, 0, destLen);
    return decompressed;
  }

  /**
   * Convenience method, equivalent to calling
   * {@link #decompress(byte[], int, int) decompress(src, 0, destLen)}.
   *
   * @param src the compressed data
   * @param destLen the <b>exact</b> size of the original input
   * @return the decompressed data
   */
  public final byte[] decompress(byte[] src, int destLen) {
    return decompress(src, 0, destLen);
  }

  /**
   * Decompresses <code>src</code> into <code>dest</code>. <code>dest</code>'s
   * {@link ByteBuffer#remaining()} must be exactly the size of the decompressed
   * data. This method moves the positions of the buffers.
   *
   * @param src the compressed data
   * @param dest the destination buffer to store the decompressed data
   */
  public final void decompress(ByteBuffer src, ByteBuffer dest) {
    final int read = decompress(src, src.position(), dest, dest.position(), dest.remaining());
    dest.position(dest.limit());
    src.position(src.position() + read);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
