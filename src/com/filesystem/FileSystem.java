package com.filesystem;

import java.io.*;
import java.util.*;

public class FileSystem {

    private static final int MAX_FILE_SIZE = LDisk.BLOCK_SIZE*3;
    private static final int DESCRIPTORS_PER_BLOCK = 4;
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
    private final int firstDataBlock;
    private final int L;

    //L = number of blocks of disk space
    //B = size of a block in bytes
    //k = number of file descriptors
    //TODO----Theoretically Complete
    public FileSystem(int L, int k, int maxOpenFiles) {

        this.disk = new LDisk(L);
        this.openFileTable = new OpenFile[maxOpenFiles];
        this.L = L;

        disk.write_block(0, new byte[LDisk.BLOCK_SIZE]); //Zero out the bitmap because no blocks are taken
        OpenFile directory = new OpenFile();
        directory.currentPosition = -1;
        directory.fileDescriptorIndex = 0; //The first file descriptor is index 0
        directory.fileLength = 0;

        this.openFileTable[0] = directory;

        int numOfReservedDescriptorBlocks = k / DESCRIPTORS_PER_BLOCK;
        firstDataBlock = numOfReservedDescriptorBlocks + 1;

        //Create a descriptor so we can set its first data block at k+1
        FileDescriptor descriptor = new FileDescriptor();
        descriptor.blockIndices = new int[]{firstDataBlock, 0, 0};
        descriptor.fileLength = 0;

        //Write the directory descriptor at its reserved space (first part of the second block)
        byte[] block = new byte[LDisk.BLOCK_SIZE];
        descriptor.populateBytes(block,0);
        disk.write_block(1, block);

        setBlockUsed(0); //The first block is used for this bitmap
        setBlockUsed(1); //The second block has the the descriptor for the directory in it
        setBlockUsed(firstDataBlock); //The first data block is for the directory data
    }

    //Create a file
    //TODO---Theoretically Complete
    public void create(String fileName) {

        if (fileName.length() > 4) {
            throw new RuntimeException("Name is longer than 4 characters");
        }

        int freeDescriptorIndex = -1;
        //Were going to iterate through all the descriptor blocks and look for a free slot
        outer:
        for(int i = 1; i < firstDataBlock; i++) {
            byte[] block = new byte[LDisk.BLOCK_SIZE];
            disk.read_block(i,block);

            //Iterate through all descriptors in this block
            for(int j = 0; j < DESCRIPTORS_PER_BLOCK; j++) {
                if(FileDescriptor.isFreeDescriptor(block,(j*16))) {

                    freeDescriptorIndex = ((i-1)*DESCRIPTORS_PER_BLOCK)+j;
                    break outer;
                }
            }
        }

        if (freeDescriptorIndex != -1) {
            lseek(0, 0);

            byte[] slot = new byte[8];

            OpenFile directoryFile = openFileTable[0];
            int position = 0;

            //Make sure another file with the same name doesn't already exist
            outer:
            while (true) {
                position += 8;
                //If the next read would put us past the end of the file then there is no conflicting file
                if(position > directoryFile.fileLength)
                    break;

                int bytesRead = read(0, slot, 8);

                if (bytesRead != 8) {
                    break;
                }

                DirectorySlot s = new DirectorySlot(slot);
                if (s.descriptorIndex == 0) //If the descriptor index points to the bitmap it is an empty slot (ignore it)
                    continue;

                char[] name = fileName.toCharArray();
                for (int i = 0; i < name.length; i++) {
                    //Make sure that the input characters from the string
                    if (name[i] != s.name[i])
                        continue outer;
                }

                //If we get here then we found the slot for the file
                throw new RuntimeException("A file with that name already exists");
            }

            position = 0;
            lseek(0,0);

            //Go through all the slots in the directory and look for an open one
            while (true) {
                position += 8;

                //Doing another read would put us past the end of the file
                if(position > directoryFile.fileLength)
                    break;

                int bytesRead = read(0, slot, 8);

                if (bytesRead != 8) {
                    throw new RuntimeException("No open directory slot");
                }

                DirectorySlot directorySlot = new DirectorySlot(slot);

                //If the descriptor index is pointing at the bitmap then its free
                if (directorySlot.descriptorIndex == 0) {
                    break;
                }
            }

            position -= 8;

            DirectorySlot newSlot = new DirectorySlot();
            newSlot.name = fileName.toCharArray();
            newSlot.descriptorIndex = freeDescriptorIndex;

            FileDescriptor descriptor = new FileDescriptor();
            descriptor.fileLength = 0;
            descriptor.blockIndices = new int[3];
            descriptor.blockIndices[0] = -1;

            writeDescriptor(freeDescriptorIndex, descriptor);
            //Make sure we mark this block as being used because it has at least one descriptor in it
            setBlockUsed(getDescriptorBlockNumber(freeDescriptorIndex));

            lseek(0,position);
            write(0,newSlot.getBytes(),8);

        } else
            throw new RuntimeException("No free file descriptor");
    }

    //Destroy a file
    //TODO---Theoretically Complete
    public void destroy(String fileName) {

        //Seek to the start of the directory
        lseek(0, 0);

        DirectorySlot slot = null;
        byte[] fileSlot = new byte[8];

        OpenFile directoryFile = openFileTable[0];
        int position = 0;

        outer:
        while (true) {
            position += 8;

            //If the next read would put us past the end of the file then there is no file with that name and we return
            if(position > directoryFile.fileLength)
                throw new RuntimeException("Could not find a file with the given name");


            int bytesRead = read(0, fileSlot, 8);

            if (bytesRead != 8)
                throw new RuntimeException("Could not find a file with the given name");


            slot = new DirectorySlot(fileSlot);
            if (slot.descriptorIndex == 0) //If the descriptor index points to the bitmap it is an empty slot (ignore it)
                continue;

            char[] name = fileName.toCharArray();
            for (int i = 0; i < name.length; i++) {
                //Make sure that the input characters from the string
                if (name[i] != slot.name[i])
                    continue outer;
            }

            //If we get here then we found the slot for the file
            break;
        }

        //The position of the file slot in the directory
        position -= 8;

        //If the file is already open then we close it first
        for(int i = 1; i < openFileTable.length; i++) {
            if(openFileTable[i] != null && openFileTable[i].fileDescriptorIndex == slot.descriptorIndex) {
                close(i);
            }
        }

        //Read in the file descriptor
        byte[] block = new byte[LDisk.BLOCK_SIZE];
        int blockNumber = getDescriptorBlockNumber(slot.descriptorIndex);
        int positionInBlock = getDescriptorPositionInBlock(slot.descriptorIndex);

        disk.read_block(blockNumber,block);
        FileDescriptor descriptor = new FileDescriptor(block,getDescriptorBytePositionInBlock(slot.descriptorIndex));

        //If any of the data blocks in this file are used then mark them as free
        for(int index : descriptor.blockIndices) {
            if(index > 0) {
                setBlockFree(index);
            }
        }

        boolean blockEmpty = false;
        for(int i = 0; i < DESCRIPTORS_PER_BLOCK; i++) {

            //Ignore the descriptor for the file we are destroying
            if(i != positionInBlock) {
                //If the descriptor isnt free then this block isnt empty
                if(!FileDescriptor.isFreeDescriptor(block,i*16))
                {
                    blockEmpty = false;
                    break;
                }

                //If the descriptor is free then mark this block as being free (for now)
                blockEmpty = true;
            }
        }
        //Free the file descriptor block (if there are no more descriptors in it)
        if(blockEmpty)
            setBlockFree(blockNumber);

        //Overwrite the file slot in the directory
        Arrays.fill(fileSlot, (byte)0);
        lseek(0, position);
        write(0, fileSlot, 8);
    }

    //Returns the file handle as an int
    //TODO---Theoretically Complete
    public int open(String fileName) {

        //Seek to the start of the directory
        lseek(0, 0);

        DirectorySlot slot = null;
        byte[] fileSlot = new byte[8];

        OpenFile directoryFile = openFileTable[0];
        int position = 0;

        outer:
        while (true) {
            position += 8;

            //If the next read would put us past the end of the file then there is no file with that name and we return
            if(position > directoryFile.fileLength)
                throw new RuntimeException("Could not find a file with the given name");

            int bytesRead = read(0, fileSlot, 8);

            if (bytesRead != 8) {
                throw new RuntimeException("Could not find a file with the given name");
            }

            slot = new DirectorySlot(fileSlot);
            if (slot.descriptorIndex == 0) //If the descriptor index points to the bitmap it is an empty slot (ignore it)
                continue;

            char[] name = fileName.toCharArray();
            for (int i = 0; i < name.length; i++) {
                //Make sure that the input characters from the string
                if (name[i] != slot.name[i])
                    continue outer;
            }

            //If we get here then we found the slot for the file
            break;
        }

        for(int i = 0; i < openFileTable.length; i++) {
            if(openFileTable[i] != null) {
                if(openFileTable[i].fileDescriptorIndex == slot.descriptorIndex)
                    throw new RuntimeException("Attempted to open a file that is already open");
            }
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

            file.currentPosition = -1;
            file.fileDescriptorIndex = slot.descriptorIndex;

            byte[] block = new byte[LDisk.BLOCK_SIZE];
            int blockNumber = getDescriptorBlockNumber(slot.descriptorIndex);
            int bytePositionInBlock = getDescriptorBytePositionInBlock(slot.descriptorIndex);

            disk.read_block(blockNumber, block);

            FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

            file.fileLength = descriptor.fileLength;
            file.buffer = new byte[LDisk.BLOCK_SIZE];

            //If the index of the first data block is less than 1 then the block hasnt been allocated yet
            if(descriptor.blockIndices[0] < 1) {
                descriptor.blockIndices[0] = allocateNewDataBlock();

                if(descriptor.blockIndices[0] == -1)
                    throw new RuntimeException("There are no free data blocks to allocate");

                //Write the descriptor to disk to make sure the indices are good
                descriptor.populateBytes(block, bytePositionInBlock);
                disk.write_block(blockNumber, block);

                //Since we just allocated the block it will be empty and we don't have to read from disk
                Arrays.fill(file.buffer, (byte)0);
            }
            else
             disk.read_block(descriptor.blockIndices[0], file.buffer);

            openFileTable[openFileIndex] = file;

            return openFileIndex;
        } else {
            throw new RuntimeException("Opened the max amount of files");
        }
    }

    //Takes the file handle of the file to close
    //TODO---Theoretically Complete
    public void close(int handle) {

        if(handle < 1 || handle >= openFileTable.length)
            throw new IndexOutOfBoundsException("Attempted to close an invalid file handle");

        OpenFile file = openFileTable[handle];
        if(file == null)
            throw new IndexOutOfBoundsException("Attempted to close an invalid file handle");

        byte[] block = new byte[LDisk.BLOCK_SIZE];

        //Read in the file descriptor
        int blockNumber = getDescriptorBlockNumber(file.fileDescriptorIndex);
        int bytePositionInBlock = getDescriptorBytePositionInBlock(file.fileDescriptorIndex);

        disk.read_block(blockNumber, block);
        FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

        //Write the buffer out to the correct block
        int currentBufferBlock = file.currentPosition / LDisk.BLOCK_SIZE;
        disk.write_block(descriptor.blockIndices[currentBufferBlock], file.buffer);

        //Update the descriptor's file length and write it back to disk
        descriptor.fileLength = file.fileLength;

        descriptor.populateBytes(block, bytePositionInBlock);
        disk.write_block(blockNumber, block);

        //Clear entry from the open file table
        openFileTable[handle] = null;
    }

    private void closeDirectory() {
        OpenFile file = openFileTable[0];
        if(file == null)
            throw new IndexOutOfBoundsException("Attempted to close an invalid file handle");

        byte[] block = new byte[LDisk.BLOCK_SIZE];

        //Read in the file descriptor
        int blockNumber = getDescriptorBlockNumber(file.fileDescriptorIndex);
        int bytePositionInBlock = getDescriptorBytePositionInBlock(file.fileDescriptorIndex);

        disk.read_block(blockNumber, block);
        FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

        //Write the buffer out to the correct block
        int currentBufferBlock = file.currentPosition / LDisk.BLOCK_SIZE;
        disk.write_block(descriptor.blockIndices[currentBufferBlock], file.buffer);

        //Update the descriptor's file length and write it back to disk
        descriptor.fileLength = file.fileLength;

        descriptor.populateBytes(block, bytePositionInBlock);
        disk.write_block(blockNumber, block);

        //Clear entry from the open file table
        openFileTable[0] = null;
    }

    //Copies the "count" number of bytes from the given file into the mem_area
    //TODO---Theoretically Complete
    public int read(int handle, byte[] mem_area, int count) {

        if(handle < 0 || handle >= openFileTable.length)
            throw new IndexOutOfBoundsException("Attempted to read from an invalid file handle");

        OpenFile file = openFileTable[handle];
        if(file == null)
            throw new IndexOutOfBoundsException("Attempted to read from an invalid file handle");

        int bytesRead = 0;
        int currentPosition = file.currentPosition;
        int newPosition = file.currentPosition;
        int currentBlock = currentPosition / LDisk.BLOCK_SIZE;

        byte[] block = new byte[LDisk.BLOCK_SIZE];

        int blockNumber = getDescriptorBlockNumber(file.fileDescriptorIndex);
        int bytePositionInBlock = getDescriptorBytePositionInBlock(file.fileDescriptorIndex);

        disk.read_block(blockNumber,block);
        FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

        //This is the loop that is going to read each individual byte
        //position + index needs to be less than file length so we don't read past the end of the file
        for(int index = 1; index <= count && currentPosition + index < MAX_FILE_SIZE; index++) {

            newPosition = currentPosition + index;
            int newBlock = newPosition / LDisk.BLOCK_SIZE;

            //If the next byte to read is not in the same block, then we write the old block and load the new one
            if(newBlock != currentBlock) {

                //Write the buffer to disk
                disk.write_block(descriptor.blockIndices[currentBlock], file.buffer);

                //If the index of the data block is 0 then the block hasnt been allocated yet
                if(descriptor.blockIndices[newBlock] < 1) {
                    descriptor.blockIndices[newBlock] = allocateNewDataBlock();

                    if(descriptor.blockIndices[newBlock] == -1) {
                        throw new RuntimeException("Could not allocate any more data blocks");
                    }

                    //Write the descriptor to disk to make sure the indices are good
                    descriptor.populateBytes(block, bytePositionInBlock);
                    disk.write_block(blockNumber, block);

                    //Since we just allocated the block it will be empty and we don't have to read from disk
                    Arrays.fill(file.buffer, (byte)0);
                }
                else
                    disk.read_block(descriptor.blockIndices[newBlock], file.buffer);

                currentBlock = newBlock;
            }

            int indexPosition = newPosition % LDisk.BLOCK_SIZE;
            mem_area[index-1] = file.buffer[indexPosition];
            bytesRead++;
            newPosition++;
        }

        if(bytesRead >0)
            file.currentPosition = newPosition-1;
        else
            file.currentPosition = newPosition;

        return bytesRead;
    }

    //Writes the "count" number of bytes from the mem_area into the given file
    //TODO---Theoretically Complete
    public int write(int handle, byte[] mem_area, int count) {

        if(handle < 0 || handle >= openFileTable.length)
            throw new IndexOutOfBoundsException("Attempted to write to an invalid file handle");

        OpenFile file = openFileTable[handle];
        if(file == null)
            throw new IndexOutOfBoundsException("Attempted to write to an invalid file handle");

        int bytesWritten = 0;
        int currentPosition = file.currentPosition;
        int newPosition = file.currentPosition;
        int currentBlock = currentPosition / LDisk.BLOCK_SIZE;

        byte[] block = new byte[LDisk.BLOCK_SIZE];
        int blockNumber = getDescriptorBlockNumber(file.fileDescriptorIndex);
        int bytePositionInBlock = getDescriptorBytePositionInBlock(file.fileDescriptorIndex);

        //This is the loop that is going to write each individual byte
        //position + index needs to be less than the max file length so we don't write past the 3rd block
        for(int index = 1; index <= count && currentPosition + index < MAX_FILE_SIZE; index++) {

            newPosition = currentPosition + index;
            int newBlock = newPosition / LDisk.BLOCK_SIZE;

            //If the next byte to write is in a different block then we write the current block and get a new one
            if(newBlock != currentBlock) {

                //Read in the file descriptor
                disk.read_block(blockNumber,block);
                FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

                //Write the buffer to disk
                disk.write_block(descriptor.blockIndices[currentBlock], file.buffer);

                //If the index of the data block is 0 then the block hasnt been allocated yet
                if(descriptor.blockIndices[newBlock] < 1) {
                    descriptor.blockIndices[newBlock] = allocateNewDataBlock();

                    if(descriptor.blockIndices[newBlock] == -1) {
                        throw new RuntimeException("Could not allocate any more data blocks");
                    }

                    //Write the descriptor to disk to make sure the indices are good
                    descriptor.populateBytes(block, bytePositionInBlock);
                    disk.write_block(blockNumber, block);

                    //Since we just allocated the block it will be empty and we don't have to read from disk
                    Arrays.fill(file.buffer, (byte)0);
                }
                else
                    disk.read_block(descriptor.blockIndices[newBlock], file.buffer);

                currentBlock = newBlock;
            }

            int indexPosition = newPosition % LDisk.BLOCK_SIZE;
            file.buffer[indexPosition] = mem_area[index-1];
            bytesWritten++;
            newPosition++;
        }

        file.currentPosition = newPosition-1;
        if(newPosition > file.fileLength)
            file.fileLength = newPosition;

        return bytesWritten;
    }

    //Seek to a new position of the specified file
    //TODO---Theoretically Compete
    public void lseek(int handle, int position) {

        if (handle < 0 || handle >= openFileTable.length)
            throw new IndexOutOfBoundsException();

        OpenFile file = openFileTable[handle];
        if (file != null) {

            int currentBlock = file.currentPosition / LDisk.BLOCK_SIZE;
            int newBlock = (position-1) / LDisk.BLOCK_SIZE;

            if(position < 0 || position > file.fileLength) {
                throw new RuntimeException("Attempted to seek to an invalid position");
            }

            if(newBlock > 2)
                newBlock = 2;

            if(currentBlock == newBlock) {
                file.currentPosition = position-1; //The -1 is because the current position is one less than the write position
            }
            else {
                byte[] block = new byte[LDisk.BLOCK_SIZE];

                int blockNumber = getDescriptorBlockNumber(file.fileDescriptorIndex);
                int bytePositionInBlock = getDescriptorBytePositionInBlock(file.fileDescriptorIndex);

                //Read in the descriptor for this file
                disk.read_block(blockNumber, block);

                //Get the index of the correct block from the descriptor
                FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);
                int oldBlockIndex = descriptor.blockIndices[currentBlock];

                //Write the buffer to the disk
                disk.write_block(oldBlockIndex, file.buffer);

                int blockIndex = descriptor.blockIndices[newBlock];

                //This means that there is no allocated space for this data block
                if(blockIndex < 1) {
                    //Allocate a new data block for this file
                    blockIndex = allocateNewDataBlock();
                    if(blockIndex == -1) {
                        throw new RuntimeException("No free data blocks to allocate");
                    }
                    //Makre sure the descriptor has the correct block index
                    descriptor.blockIndices[newBlock] = blockIndex;

                    descriptor.populateBytes(block, bytePositionInBlock);
                    //Write the descriptor to disk so all the file indices are correct
                    disk.write_block(blockNumber, block);

                    //Since we allocated a new block its going to be empty and we don't need to read it in
                    Arrays.fill(block, (byte)0);
                }
                else //Read in the block
                    disk.read_block(blockIndex, block);

                //Set the new position for the open file and set the buffer correctly
                file.currentPosition = position-1;
                file.buffer = block;
            }
        } else
            throw new RuntimeException("Attempted to seek on an invalid file handle");
    }

    //Return a list of files
    //TODO---Theoretically Complete
    public List<String> directory() {

        List<String> files = new ArrayList<>();
        //Seek to the start of the directory
        lseek(0, 0);

        DirectorySlot slot = null;
        byte[] fileSlot = new byte[8];

        OpenFile directoryFile = openFileTable[0];
        int position = 0;

        outer:
        while (true) {
            position += 8;

            //If the next read would put us past the end of the file then there is no file with that name and we return
            if(position > directoryFile.fileLength)
               break;

            read(0, fileSlot, 8);

            slot = new DirectorySlot(fileSlot);
            if (slot.descriptorIndex == 0) //If the descriptor index points to the bitmap it is an empty slot (ignore it)
                continue;

            files.add(slot.getNiceName());
        }

        return files;
    }

    //Restore the disk from the file or else create a new disk if the file doesnt exist
    //TODO---Theoretically Complete
    public void init(File file) {

        byte[] block = new byte[LDisk.BLOCK_SIZE];

        //First we completely rewrite the disk with the contents of the file
        try(FileReader fileStream = new FileReader(file)) {
            try(BufferedReader reader = new BufferedReader(fileStream)) {

                int blockNumber = 0;
                String line = null;
                while((line = reader.readLine()) != null) {

                    String[] bytes = line.split(" ");

                    for (int i = 0; i < LDisk.BLOCK_SIZE; i++) {
                        block[i] = Byte.parseByte(bytes[i]);
                    }

                    disk.write_block(blockNumber, block);
                    blockNumber++;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found");
        } catch (IOException e) {
            throw new RuntimeException("IO Exception");
        }

        //Next we reload the directory open file from the disk
        OpenFile directory = openFileTable[0];

        //Read in the updated file descriptor for the directory

        int blockNumber = getDescriptorBlockNumber(directory.fileDescriptorIndex);
        int bytePositionInBlock = getDescriptorBytePositionInBlock(directory.fileDescriptorIndex);

        disk.read_block(blockNumber,block);
        FileDescriptor descriptor = new FileDescriptor(block, bytePositionInBlock);

        //Update the file length and the current position
        directory.fileLength = descriptor.fileLength;
        directory.currentPosition = -1;

        //If there is an allocated data block for the directory, load it
        if(descriptor.blockIndices[0] != 0) {
            disk.read_block(descriptor.blockIndices[0],directory.buffer);
        }
        else //if no allocated data, zero out the buffer
            Arrays.fill(directory.buffer, (byte)0);
    }

    //Save the disk to the file
    //TODO---Theoretically Complete
    public void save(File file) {

        closeDirectory();
        for(int i = 1; i < openFileTable.length; i++) {
            if(openFileTable[i] != null) {
                close(i);
            }
        }

        //the mitchhronsid aid the owirhdiusr ofnthencrll
        byte[] block = new byte[LDisk.BLOCK_SIZE];
        try(FileWriter stream = new FileWriter(file)) {
            try(BufferedWriter writer = new BufferedWriter(stream)) {
                for(int i = 0; i < L; i++) {
                    disk.read_block(i,block);

                    StringBuilder builder = new StringBuilder();
                    for (byte b : block) {
                        builder.append(b).append(" ");
                    }

                    writer.write(builder.toString());
                    writer.newLine();
                }

                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException");
        }
    }

    private int allocateNewDataBlock() {

        int[] bitmap = getBitMap();
        int freeBlockIndex = -1;

        outer:
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 32; j++) {

                if (j + (32 * i) <= firstDataBlock) //If they arent in the data area then continue looking
                    continue;

                int test = bitmap[i] & MASK[j];
                if (test == 0) {
                    freeBlockIndex = j + (32 * i);
                    break outer;
                }
            }
        }

        //If we found a free data block then mark it as taken
        if (freeBlockIndex != -1) {
            setBlockUsed(freeBlockIndex);
        }

        return freeBlockIndex;
    }

    private void writeDescriptor(int descriptorIndex, FileDescriptor descriptor) {
        byte[] block = new byte[LDisk.BLOCK_SIZE];

        int blockNumber = descriptorIndex / DESCRIPTORS_PER_BLOCK;
        int posInBlock = descriptorIndex % DESCRIPTORS_PER_BLOCK;

        disk.read_block(1+blockNumber, block);
        descriptor.populateBytes(block,16*posInBlock);
        disk.write_block(1+blockNumber, block);
    }

    private int getDescriptorBlockNumber(int descriptorIndex) {
        return 1+(descriptorIndex/DESCRIPTORS_PER_BLOCK);
    }

    private int getDescriptorPositionInBlock(int descriptorIndex) {
        return descriptorIndex % DESCRIPTORS_PER_BLOCK;
    }

    private int getDescriptorBytePositionInBlock(int descriptorIndex) {
        return getDescriptorPositionInBlock(descriptorIndex)*16;
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
