package net.jpountz.xxhash;

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
 * A 32-bits hash.
 * <p>
 * Instances of this class are thread-safe.
 */
public abstract class XXHash32 {

  /**
   * Computes the 32-bits hash of <code>buf[off:off+len]</code> using seed
   * <code>seed</code>.
   *
   * @param buf the input data
   * @param off the start offset in buf
   * @param len the number of bytes to hash
   * @param seed the seed to use
   * @return the hash value
   */
  public abstract int hash(byte[] buf, int off, int len, int seed);

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
  public abstract int hash(ByteBuffer buf, int off, int len, int seed);

  /**
   * Computes the hash of the given {@link ByteBuffer}. The
   * {@link ByteBuffer#position() position} is moved in order to reflect bytes
   * which have been read.
   *
   * @param buf the input data
   * @param seed the seed to use
   * @return the hash value
   */
  public final int hash(ByteBuffer buf, int seed) {
    final int hash = hash(buf, buf.position(), buf.remaining(), seed);
    buf.position(buf.limit());
    return hash;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
