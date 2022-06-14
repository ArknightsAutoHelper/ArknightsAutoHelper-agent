package net.jpountz.util;

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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

/** FOR INTERNAL USE ONLY */
public enum Native {
  ;

  private static boolean loaded = false;

  public static synchronized boolean isLoaded() {
    return loaded;
  }

  public static synchronized void load() {
    if (loaded) {
      return;
    }
    String arch = System.getProperty("os.arch");
    String libname = String.format("lib/%s/liblz4-java.so", arch);

    ClassLoader loader = Objects.requireNonNull(Native.class.getClassLoader());
    // resolve apk/jar file path
    URL url = loader.getResource(libname);
    if (url == null) {
      throw new RuntimeException("Could not find native library: " + libname);
    }
    String file = url.getFile();
    if (file.startsWith("file:")) {
      file = file.substring(5);
    }
    try {
      System.load(file);
    } catch (UnsatisfiedLinkError e) {
      String filename = String.format("/data/local/tmp/liblz4-java-%s-%s.so", arch, UUID.randomUUID().toString().substring(0, 8));
      File tempLibFile = new File(filename);
      try {
        if (!tempLibFile.createNewFile()) {
          throw new IOException("Could not create temporary file: " + filename);
        }
      } catch (IOException e1) {
        System.err.println("Failed to create temp lib file: " + e1.getMessage());
        throw new RuntimeException(e1);
      }
      try (FileOutputStream fos = new FileOutputStream(tempLibFile);
           InputStream is = loader.getResourceAsStream(libname)) {
          byte[] buffer = new byte[4096];
          int len;
          while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
          }
      } catch (IOException e1) {
        System.err.println("Failed to write temp lib file: " + e1.getMessage());
      }
      try {
        System.load(tempLibFile.getAbsolutePath());
      } catch (UnsatisfiedLinkError e1) {
        System.err.println("Failed to load native library: " + e1.getMessage());
        throw e1;
      }
      // unlink the file from filesystem
      tempLibFile.delete();
    }
    loaded = true;
  }
}
