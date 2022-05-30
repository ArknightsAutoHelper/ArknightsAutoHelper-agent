// Auto-generated: DO NOT EDIT

package net.jpountz.xxhash;

import static net.jpountz.xxhash.XXHashConstants.*;
import static java.lang.Long.rotateLeft;

import java.nio.ByteBuffer;

import net.jpountz.util.UnsafeUtils;
import net.jpountz.util.ByteBufferUtils;

/**
 * {@link XXHash64} implementation.
 */
final class XXHash64JavaUnsafe extends XXHash64 {

  public static final XXHash64 INSTANCE = new XXHash64JavaUnsafe();


  @Override
  public long hash(byte[] buf, int off, int len, long seed) {

    UnsafeUtils.checkRange(buf, off, len);

    final int end = off + len;
    long h64;

    if (len >= 32) {
      final int limit = end - 32;
      long v1 = seed + PRIME64_1 + PRIME64_2;
      long v2 = seed + PRIME64_2;
      long v3 = seed + 0;
      long v4 = seed - PRIME64_1;
      do {
        v1 += UnsafeUtils.readLongLE(buf, off) * PRIME64_2;
        v1 = rotateLeft(v1, 31);
        v1 *= PRIME64_1;
        off += 8;

        v2 += UnsafeUtils.readLongLE(buf, off) * PRIME64_2;
        v2 = rotateLeft(v2, 31);
        v2 *= PRIME64_1;
        off += 8;

        v3 += UnsafeUtils.readLongLE(buf, off) * PRIME64_2;
        v3 = rotateLeft(v3, 31);
        v3 *= PRIME64_1;
        off += 8;

        v4 += UnsafeUtils.readLongLE(buf, off) * PRIME64_2;
        v4 = rotateLeft(v4, 31);
        v4 *= PRIME64_1;
        off += 8;
      } while (off <= limit);

      h64 = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18);

      v1 *= PRIME64_2; v1 = rotateLeft(v1, 31); v1 *= PRIME64_1; h64 ^= v1;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v2 *= PRIME64_2; v2 = rotateLeft(v2, 31); v2 *= PRIME64_1; h64 ^= v2;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v3 *= PRIME64_2; v3 = rotateLeft(v3, 31); v3 *= PRIME64_1; h64 ^= v3;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v4 *= PRIME64_2; v4 = rotateLeft(v4, 31); v4 *= PRIME64_1; h64 ^= v4;
      h64 = h64 * PRIME64_1 + PRIME64_4;
    } else {
      h64 = seed + PRIME64_5;
    }

    h64 += len;

    while (off <= end - 8) {
      long k1 = UnsafeUtils.readLongLE(buf, off);
      k1 *= PRIME64_2; k1 = rotateLeft(k1, 31); k1 *= PRIME64_1; h64 ^= k1;
      h64 = rotateLeft(h64, 27) * PRIME64_1 + PRIME64_4;
      off += 8;
    }

    if (off <= end - 4) {
      h64 ^= (UnsafeUtils.readIntLE(buf, off) & 0xFFFFFFFFL) * PRIME64_1;
      h64 = rotateLeft(h64, 23) * PRIME64_2 + PRIME64_3;
      off += 4;
    }

    while (off < end) {
      h64 ^= (UnsafeUtils.readByte(buf, off) & 0xFF) * PRIME64_5;
      h64 = rotateLeft(h64, 11) * PRIME64_1;
      ++off;
    }

    h64 ^= h64 >>> 33;
    h64 *= PRIME64_2;
    h64 ^= h64 >>> 29;
    h64 *= PRIME64_3;
    h64 ^= h64 >>> 32;

    return h64;
  }


  @Override
  public long hash(ByteBuffer buf, int off, int len, long seed) {

    if (buf.hasArray()) {
      return hash(buf.array(), off + buf.arrayOffset(), len, seed);
    }
    ByteBufferUtils.checkRange(buf, off, len);
    buf = ByteBufferUtils.inLittleEndianOrder(buf);

    final int end = off + len;
    long h64;

    if (len >= 32) {
      final int limit = end - 32;
      long v1 = seed + PRIME64_1 + PRIME64_2;
      long v2 = seed + PRIME64_2;
      long v3 = seed + 0;
      long v4 = seed - PRIME64_1;
      do {
        v1 += ByteBufferUtils.readLongLE(buf, off) * PRIME64_2;
        v1 = rotateLeft(v1, 31);
        v1 *= PRIME64_1;
        off += 8;

        v2 += ByteBufferUtils.readLongLE(buf, off) * PRIME64_2;
        v2 = rotateLeft(v2, 31);
        v2 *= PRIME64_1;
        off += 8;

        v3 += ByteBufferUtils.readLongLE(buf, off) * PRIME64_2;
        v3 = rotateLeft(v3, 31);
        v3 *= PRIME64_1;
        off += 8;

        v4 += ByteBufferUtils.readLongLE(buf, off) * PRIME64_2;
        v4 = rotateLeft(v4, 31);
        v4 *= PRIME64_1;
        off += 8;
      } while (off <= limit);

      h64 = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18);

      v1 *= PRIME64_2; v1 = rotateLeft(v1, 31); v1 *= PRIME64_1; h64 ^= v1;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v2 *= PRIME64_2; v2 = rotateLeft(v2, 31); v2 *= PRIME64_1; h64 ^= v2;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v3 *= PRIME64_2; v3 = rotateLeft(v3, 31); v3 *= PRIME64_1; h64 ^= v3;
      h64 = h64 * PRIME64_1 + PRIME64_4;

      v4 *= PRIME64_2; v4 = rotateLeft(v4, 31); v4 *= PRIME64_1; h64 ^= v4;
      h64 = h64 * PRIME64_1 + PRIME64_4;
    } else {
      h64 = seed + PRIME64_5;
    }

    h64 += len;

    while (off <= end - 8) {
      long k1 = ByteBufferUtils.readLongLE(buf, off);
      k1 *= PRIME64_2; k1 = rotateLeft(k1, 31); k1 *= PRIME64_1; h64 ^= k1;
      h64 = rotateLeft(h64, 27) * PRIME64_1 + PRIME64_4;
      off += 8;
    }

    if (off <= end - 4) {
      h64 ^= (ByteBufferUtils.readIntLE(buf, off) & 0xFFFFFFFFL) * PRIME64_1;
      h64 = rotateLeft(h64, 23) * PRIME64_2 + PRIME64_3;
      off += 4;
    }

    while (off < end) {
      h64 ^= (ByteBufferUtils.readByte(buf, off) & 0xFF) * PRIME64_5;
      h64 = rotateLeft(h64, 11) * PRIME64_1;
      ++off;
    }

    h64 ^= h64 >>> 33;
    h64 *= PRIME64_2;
    h64 ^= h64 >>> 29;
    h64 *= PRIME64_3;
    h64 ^= h64 >>> 32;

    return h64;
  }


}

