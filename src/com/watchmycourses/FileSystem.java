package com.watchmycourses;

import com.sun.org.apache.bcel.internal.generic.LDIV;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystem {

    private static final int[] MASK = new int[32];
    private static final int[] MASK2 = new int[32];

    static {
        for (int i = 0; i < MASK.length; i++) {
            MASK[i] = 0x80000000 >>> i;
        }

        for (int i = 0; i < MASK.length; i++) {
            MASK2[i] = ~MASK[i];
        }
    }

    private final LDisk disk;
    private final OpenFile[] openFileTable;
    private final int k;

    //L = number of blocks of disk space
    //B = size of a block in bytes
    //k = number of file descriptors
    public FileSystem(int L, int k, int maxOpenFiles) {

        this.disk = new LDisk(L);
        this.openFileTable = new OpenFile[maxOpenFiles];
        this.k = k;

        disk.write_block(0, new byte[LDisk.BLOCK_SIZE]); //Zero out the bitmap because no blocks are taken
        OpenFile directory = new OpenFile();
        directory.currentPosition = 0;
        directory.fileDescriptorIndex = 1;
        directory.fileLength = 0;

        this.openFileTable[0] = directory;

        //Create a descriptor so we can set its first data block at k+1
        FileDescriptor descriptor = new FileDescriptor();
        descriptor.blockIndices = new int[]{k + 1, 0, 0};
        descriptor.fileLength = 0;

        //Write the directory descriptor at its reserved block
        disk.write_block(1, descriptor.getBytes());

        setBlockUsed(0); //The first block is used for this bitmap
        setBlockUsed(1); //The second block is used for the directory file descriptor
        setBlockUsed(k + 1); //The first data block is for the directory data
    }

    //Create a file
    public void create(String fileName) {

        if (fileName.length() > 4) {
            System.out.println("Error. Filename is longer than 4 characters.");
            return;
        }

        int[] bitmap = getBitMap();

        int freeBlockIndex = -1;

        outer:
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 32; j++) {

                if (j + (32 * i) > k) //Don't let them leave the file descriptor area
                    break outer;

                int test = bitmap[i] & MASK[j];
                if (test == 0) {
                    freeBlockIndex = j + (32 * i);
                    break outer;
                }
            }
        }

        if (freeBlockIndex != -1) {
            lseek(0, 0);

            byte[] slot = new byte[8];
            int foundIndex = 0;

            //Go through all the slots in the directory and look for an open one
            while (true) {
                int bytesRead = read(0, slot, 8);

                if (bytesRead != 8) {
                    System.out.println("Error. No free file slot in the directory.");
                    return;
                }

                DirectorySlot directorySlot = new DirectorySlot(slot);

                //If the descriptor index is pointing at the bitmap then its free
                if (directorySlot.descriptorIndex == 0) {
                    break;
                }

                foundIndex += 8;
            }

            DirectorySlot newSlot = new DirectorySlot();
            newSlot.name = fileName.toCharArray();
            newSlot.descriptorIndex = freeBlockIndex;

            FileDescriptor descriptor = new FileDescriptor();
            descriptor.fileLength = 0;
            descriptor.blockIndices = new int[3];

            disk.write_block(freeBlockIndex, descriptor.getBytes());
            setBlockUsed(freeBlockIndex);

            lseek(0,foundIndex);
            write(0,newSlot.getBytes(),8);

        } else
            System.out.println("Error. No free file descriptor for new file.");
    }

    //Destroy a file
    public void destroy(String fileName) {

    }

    //Returns the file handle as an int
    public int open(String fileName) {

        //Seek to the start of the directory
        lseek(0, 0);

        DirectorySlot slot = null;
        byte[] fileSlot = new byte[8];

        outer:
        while (true) {
            int bytesRead = read(0, fileSlot, 8);

            if (bytesRead != 8) {
                System.out.println("Could not find the file with the given name");
                return -1;
            }

            slot = new DirectorySlot(fileSlot);
            if (slot.descriptorIndex == 0) //If the descriptor index points to the bitmap it is an empty slot
                continue;

            char[] name = fileName.toCharArray();
            for (int i = 0; i < name.length; i++) {
                //Make sure that the input characters from the string
                name[i] = (char) (name[i] + "").getBytes(Charset.forName("UTF-8"))[0];
                if (name[i] != slot.name[0])
                    continue outer;
            }

            //If we get here then we found the slot for the file
            break;
        }

        int openFileIndex = -1;
        for (int i = 0; i < openFileTable.length; i++) {

            if (openFileTable[i] == null) {
                openFileIndex = i;
                break;
            }
        }

        if (openFileIndex != -1) {
            OpenFile file = new OpenFile();

            file.currentPosition = 0;
            file.fileDescriptorIndex = slot.descriptorIndex;

            byte[] block = new byte[LDisk.BLOCK_SIZE];
            disk.read_block(slot.descriptorIndex, block);

            FileDescriptor descriptor = new FileDescriptor(block);

            file.fileLength = descriptor.fileLength;
            file.buffer = new byte[LDisk.BLOCK_SIZE];
            disk.read_block(descriptor.blockIndices[0], file.buffer);

            return openFileIndex;
        } else {
            System.out.println("You have opened the maximum amount of files.");
            return -1;
        }
    }

    //Takes the file handle of the file to close
    public void close(int handle) {

    }

    //Copies the "count" number of bytes from the given file into the mem_area
    public int read(int handle, byte[] mem_area, int count) {

//        if(handle < 0 || handle >= openFileTable.length)
//            throw new IndexOutOfBoundsException();
//
//        OpenFile file = openFileTable[handle];
//        if(file != null) {
//
//        }
//        else
//            System.out.println("Attempted to read from an invalid file handle");

        throw new NotImplementedException();
    }

    //Writes the "count" number of bytes from the mem_area into the given file
    public int write(int handle, byte[] mem_area, int count) {

        throw new NotImplementedException();
    }


    //Seek to a new position of the specified file
    public void lseek(int handle, int position) {

        if (handle < 0 || handle >= openFileTable.length)
            throw new IndexOutOfBoundsException();

        OpenFile file = openFileTable[handle];
        if (file != null) {

            if (position == 0 || (position >= 0 && position < file.fileLength)) {

                //Get the block number that we need to load into the buffer
                int blockNum = position / LDisk.BLOCK_SIZE;

                byte[] block = new byte[LDisk.BLOCK_SIZE];

                //Read in the descriptor for this file
                disk.read_block(file.fileDescriptorIndex, block);

                //Get the index of the correct block from the descriptor
                FileDescriptor descriptor = new FileDescriptor(block);
                int blockIndex = descriptor.blockIndices[blockNum];

                //Read in the block
                disk.read_block(blockIndex, block);

                //Set the new position for the open file and set the buffer correctly
                file.currentPosition = position;
                file.buffer = block;
            } else
                System.out.println("Attempted to seek to an invalid position on the file");
        } else
            System.out.println("Attempted to seek on an invalid file handle");
    }

    //Return a list of files
    public List<String> directory() {
        throw new NotImplementedException();
    }

    //Restore the disk from the file or else create a new disk if the file doesnt exist
    public void init(File file) {

    }

    //Save the disk to the file
    public void save(File file) {

    }

    private boolean isFreeBlock(int blockIndex) {
        int[] bitmap = getBitMap();

        int j = blockIndex / 32;
        int i = blockIndex % 32;

        return (bitmap[i] & MASK[j]) == 0;
    }

    private void setBlockFree(int blockIndex) {
        int[] bitmap = getBitMap();

        int j = blockIndex / 32;
        int i = blockIndex % 32;

        bitmap[j] = bitmap[j] & MASK2[i];

        byte[] block = new byte[LDisk.BLOCK_SIZE];
        for (int k = 0; k < bitmap.length; k++) {
            Util.pack(bitmap[k], 4 * k, block);
        }

        disk.write_block(0, block);
    }

    private void setBlockUsed(int blockIndex) {
        int[] bitmap = getBitMap();

        int j = blockIndex / 32;
        int i = blockIndex % 32;

        bitmap[j] = bitmap[j] | MASK[i];

        byte[] block = new byte[LDisk.BLOCK_SIZE];
        for (int k = 0; k < bitmap.length; k++) {
            Util.pack(bitmap[k], 4 * k, block);
        }

        disk.write_block(0, block);
    }

    private int[] getBitMap() {
        byte[] block = new byte[LDisk.BLOCK_SIZE];
        disk.read_block(0, block);
        int[] bitmap = new int[2];
        bitmap[0] = Util.unpack(0, block);
        bitmap[1] = Util.unpack(4, block);

        return bitmap;
    }
}
