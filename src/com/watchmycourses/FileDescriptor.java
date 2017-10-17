package com.watchmycourses;

public class FileDescriptor
{
    public int fileLength;
    public int[] blockIndices;

    public FileDescriptor() {

    }

    public FileDescriptor(byte[] block) {

        fileLength = Util.unpack(0,block);
        blockIndices = new int[3];

        for(int i = 0; i < blockIndices.length; i++) {
            blockIndices[i] = Util.unpack(4+(i*4),block);
        }
    }

    public byte[] getBytes() {
        byte[] block = new byte[LDisk.BLOCK_SIZE];
        Util.pack(fileLength,0,block);
        for(int i = 0; i < blockIndices.length; i++) {
            Util.pack(blockIndices[i], 4+(i*4),block);
        }
        return block;
    }
}
