cmake_minimum_required(VERSION 3.18.1)

project("lz4_java_jni")
add_library(lz4-java SHARED net_jpountz_lz4_LZ4JNI.c net_jpountz_xxhash_XXHashJNI.c lz4.c lz4hc.c xxhash.c)
