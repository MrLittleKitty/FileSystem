package com.filesystem;

public class LDisk {

    public static final int BLOCK_SIZE = 64;

    //L is number of logical blocks
    private final byte[] disk;

    public LDisk(int L) {

        disk = new byte[L*BLOCK_SIZE];
    }

    public void read_block(int i, byte[] p) {

        int index = i * BLOCK_SIZE;

        for(int j = 0; j < p.length && j < BLOCK_SIZE; j++) {

            p[j] = disk[index + j];
        }
    }

    public void write_block(int i, byte[] p) {

        int index = i * BLOCK_SIZE;

        for(int j = 0; j < p.length && j < BLOCK_SIZE; j++) {

            disk[index + j] = p[j];
        }
    }
}


