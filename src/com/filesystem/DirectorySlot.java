package com.filesystem;

import java.nio.charset.Charset;

public class DirectorySlot {

    public char[] name;
    public int descriptorIndex;

    public DirectorySlot() {
    }

    public DirectorySlot(byte[] memory) {
        name = new char[4];
        for(int i = 0; i < name.length; i++) {
            name[i] = (char)memory[i];
        }
        descriptorIndex = Util.unpack(4,memory);
    }

    public byte[] getBytes() {
        byte[] memory = new byte[8];
        for(int i = 0; i < name.length; i++) {
            memory[i] = (name[i]+"").getBytes(Charset.forName("UTF-8"))[0];
        }
        Util.pack(descriptorIndex,4,memory);
        return memory;
    }

    public String getNiceName() {
        StringBuilder builder = new StringBuilder();
        for(char c : name) {
            if(c != Character.UNASSIGNED)
                builder.append(c);
        }
        return builder.toString();
    }
}
