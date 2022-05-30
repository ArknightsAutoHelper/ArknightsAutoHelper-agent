package net.jpountz.xxhash;

import java.nio.ByteBuffer;

/*
 * Copyright 2020 Linnaea Von Lavia and the lz4-java contributors.
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
 * A 64-bits hash.
 * <p>
 * Instances of this class are thread-safe.
 */
public abstract class XXHash64 {

  /**
   * Computes the 64-bits hash of <code>buf[off:off+len]</code> using seed
   * <code>seed</code>.
   *
   * @param buf the input data
   * @param off the start offset in buf
   * @param len the number of bytes to hash
   * @param seed the seed to use
   * @return the hash value
   */
  public abstract long hash(byte[] buf, int off, int len, long seed);

  /**
   * Computes the hash of the given slice of the {@link ByteBuffer}.
   * {@link ByteBuffer#position() position} and {@link ByteBuffer#limit() limit}
   * are not modified. 
   *
   * @param buf the input data
   * @param off the start offset in buf
   * @param len the number of bytes to hash
   * @param seed the seed to use
   * @return the hash value
   */
  public abstract long hash(ByteBuffer buf, int off, int len, long seed);

  /**
   * Computes the hash of the given {@link ByteBuffer}. The
   * {@link ByteBuffer#position() position} is moved in order to reflect bytes
   * which have been read.
   *
   * @param buf the input data
   * @param seed the seed to use
   * @return the hash value
   */
  public final long hash(ByteBuffer buf, long seed) {
    final long hash = hash(buf, buf.position(), buf.remaining(), seed);
    buf.position(buf.limit());
    return hash;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
