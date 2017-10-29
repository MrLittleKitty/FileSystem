package com.filesystem;

public class FileDescriptor
{
    public int fileLength;
    public int[] blockIndices;

    public FileDescriptor() {

    }

    public FileDescriptor(byte[] block, int index) {

        fileLength = Util.unpack(index,block);
        blockIndices = new int[3];

        for(int i = 0; i < blockIndices.length; i++) {
            blockIndices[i] = Util.unpack(index+4+(i*4),block);
        }
    }

    public void populateBytes(byte[] block, int index) {
        Util.pack(fileLength,index,block);
        for(int i = 0; i < blockIndices.length; i++) {
            Util.pack(blockIndices[i], index+4+(i*4),block);
        }
    }

    public static boolean isFreeDescriptor(byte[] block, int index) {
        //Index + 4 is the first data block index. If that is 0 (pointing to bitmap) then the descriptor is not in use
        int unPacked = Util.unpack(index+4, block);
        return unPacked == 0;
    }
}
