package com.watchmycourses;

public class Util {

    public static void pack(int val, int index, byte[] mem) {
        final int MASK = 0xff;
        for (int i = 3; i >= 0; i--) {
            mem[index + i] = (byte) (val & MASK);
            val = val >> 8;
        }
    }

    public static int unpack(int index, byte[] mem) {
        final int MASK = 0xff;
        int v = (int) mem[index] & MASK;
        for (int i = 1; i < 4; i++) {
            v = v << 8;
            v = v | ((int) mem[index + i] & MASK);
        }
        return v;
    }
}
