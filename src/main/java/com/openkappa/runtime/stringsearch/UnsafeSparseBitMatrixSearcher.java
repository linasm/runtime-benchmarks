package com.openkappa.runtime.stringsearch;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeSparseBitMatrixSearcher implements Searcher, AutoCloseable {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final long masksOffset;
    private final long positionsOffset;
    private final long success;

    public UnsafeSparseBitMatrixSearcher(byte[] searchString) {
        if (searchString.length > 64) {
            throw new IllegalArgumentException("Too many bytes");
        }
        int cardinality = 0;
        long[] existence = new long[4];
        for (byte key : searchString) {
            int value = key & 0xFF;
            long word = existence[value >>> 6];
            if ((word & (1L << value)) == 0) {
                ++cardinality;
                existence[value >>> 6] |= (1L << value);
            }
        }
        int masksStorage = (cardinality + 1) * Long.BYTES;
        int positionsStorage = 256;
        this.masksOffset = UNSAFE.allocateMemory(masksStorage + positionsStorage);
        UNSAFE.setMemory(masksOffset, masksStorage + positionsStorage, (byte)0);
        this.positionsOffset = masksOffset + masksStorage;
        int index = 0;
        for (byte key : searchString) {
            int position = rank(key, existence);
            UNSAFE.putByte(positionsOffset + (key & 0xFF), (byte)position);
            UNSAFE.putLong(masksOffset + position, UNSAFE.getLong(masksOffset + position) | (1L << index));
            ++index;
        }
        this.success = 1L << (searchString.length - 1);
    }

    public int find(byte[] data) {
        long current = 0L;
        for (int i = 0; i < data.length; ++i) {
            int value = data[i] & 0xFF;
            int position = UNSAFE.getByte(positionsOffset + value) & 0xFF;
            long mask = UNSAFE.getLong(masksOffset + position);
            current = ((current << 1) | 1) & mask;
            if ((current & success) == success) {
                return i - Long.numberOfTrailingZeros(success);
            }
        }
        return -1;
    }

    private static int rank(byte key, long[] existence) {
        int value = (key & 0xFF);
        int wi = value >>> 6;
        int i = 0;
        int position = 0;
        while (i < wi) {
            position += Long.bitCount(existence[i]);
            ++i;
        }
        return position + Long.bitCount(existence[wi] & ((1L << value) - 1));
    }

    @Override
    public void close() {
        UNSAFE.freeMemory(masksOffset);
    }
}
