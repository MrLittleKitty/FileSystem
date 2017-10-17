package com.watchmycourses;

public class OpenFile {

    public OpenFile() {
        this.buffer = new byte[LDisk.BLOCK_SIZE];
    }

    byte[] buffer;
    int currentPosition;
    int fileDescriptorIndex;
    int fileLength;
}
