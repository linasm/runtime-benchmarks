package com.openkappa.runtime.inlining;

import java.util.Arrays;

public class Hasher2 implements Hasher {

  private int hash;

  @Override
  public void hash(byte[] data) {
    hash = 31 * hash + Arrays.hashCode(data);
  }

  @Override
  public void hash(int data) {
    hash = 31 * hash + data;
  }

  @Override
  public int getHash() {
    return hash;
  }
}
