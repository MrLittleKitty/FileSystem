package com.filesystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CommandTest
{
    private FileSystem fs = new FileSystem(64,24,4);
    private static int passed = 0;
    private static int failed = 0;


    void runAllTests() {
        // create
        try {
            Create_23Files_Pass();
            Create_ExistingFile_Error();
            Create_ExceedFileDescriptors_Error();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with create");
            System.out.println();
        }

        // destroy
        try {
            Destroy_1File_Pass();
            Destroy_AllFiles_Pass();
            Destroy_MissingFile_Error();
            Destroy_OpenFile_Pass();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with destroy");
            System.out.println();
        }

        // open
        try {
            Open_3Files_Pass();
            Open_4Files_Error();
            Open_MissingFile_Error();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with open");
            System.out.println();
        }

        // close
        try {
            Close_File_Pass();
            Close_NotOpenFile_Error();
            Close_OutOfBoundsIndex_Error();
            Close_Directory_Error();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with close");
            System.out.println();
        }

        // read
        try {
            Read_NotOpenFile_Error();
            Read_PastEndOfFile_Pass();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with Read");
            System.out.println();
        }

        // write
        try {
            Write_All3Blocks_Pass();
            Write_NotOpenFile_Error();
            Write_PastAll3Blocks_Error();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with write");
            System.out.println();
        }

        // seek
        try {
            Seek_Negative_Error();
            Seek_PastEndOfFile_Error();
            Seek_ToEndOfFile_Pass();

            System.out.println();
        } catch (Exception e) {
            System.out.println("Something wrong with seek");
            System.out.println();
        }

        System.out.println("Passed " + passed + " out of " + (passed + failed) + " total tests");
    }

    void cleanup() {
        fs = new FileSystem(64,24,4);
    }

    void Create_23Files_Pass() {
        System.out.print("Running Create_23Files_Pass... ");
        List<String> files = new ArrayList<>();
        String name = "fo";

        for (int i = 1; i < 24; i++) {
            files.add(name + i);
        }

        boolean result;
        for (String file : files) {
            try {
                fs.create(file);
                result = true;
            }
            catch(Throwable t) {
                t.printStackTrace();
                result = false;
            }

            if (!result) {
                System.out.println("Failed test");
                failed++;
                return;
            }
        }
        System.out.println("Passed test");
        passed++;
        cleanup();
    }

    void Create_ExistingFile_Error() {
        System.out.print("Running Create_ExistingFile_Error... ");
        String file = "foo";

        fs.create(file);
        boolean result;
        try {
            fs.create(file);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Create_ExceedFileDescriptors_Error() {
        System.out.print("Running Create_ExceedFileDescriptors_Error... ");
        List<String> files = new ArrayList<>();
        String name = "fo";

        for (int i = 1; i < 24; i++) {
            files.add(name + i);
        }
        for (String file : files) {
            fs.create(file);
        }
        boolean result;
        try {
            fs.create("fo24");
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Destroy_MissingFile_Error() {
        System.out.print("Running Destroy_MissingFile_Error... ");
        boolean result;
        try {
            fs.destroy("foo");
            result = true;
        }
        catch (Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Destroy_1File_Pass() {
        System.out.print("Running Destroy_1File_Pass... ");
        String file = "foo";
        fs.create(file);
        boolean result;
        try {
            fs.destroy(file);
            result = true;
        }
        catch(Throwable t) {
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    void Destroy_AllFiles_Pass() {
        System.out.print("Running Destroy_AllFiles_Pass... ");
        List<String> files = new ArrayList<>();
        String foo = "fo";
        for (int i = 1; i < 24; i++)
            files.add(foo + i);
        for (String file : files)
            fs.create(file);
        for (String file : files)
        {
            boolean result;
            try{
                fs.destroy(file);
                result = true;
            }
            catch(Throwable t) {
                t.printStackTrace();
                result = false;
            }
            if (!result) {
                System.out.println("Failed test");
                failed++;
                return;
            }
        }

        System.out.println("Passed test");
        passed++;

        cleanup();
    }

    void Destroy_OpenFile_Pass() {
        System.out.print("Running Destroy_OpenFile_Pass... ");
        fs.create("foo");
        fs.open("foo");
        boolean result;
        try {
            fs.destroy("foo");
            result = true;
        }
        catch(Throwable t) {
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    void Open_MissingFile_Error() {
        System.out.print("Running Open_MissingFile_Error... ");
        boolean result;
        try {
            int handle = fs.open("bar");
            result = true;
            if(handle == -1)
                result = false;
        }
        catch (Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Open_3Files_Pass() {
        System.out.print("Running Open_3Files_Pass... ");
        String[] files = {"foo", "foo1" , "foo2"};
        for (String file : files)
            fs.create(file);
        for (String file : files)
        {
            boolean result;
            try{
                int handle = fs.open(file);
                result = true;
                if(handle == -1)
                    result = false;
            }
            catch (Throwable t) {
                t.printStackTrace();
                result = false;
            }
            if (!result)
            {
                System.out.println("Failed test");
                failed++;
                return;
            }
        }

        System.out.println("Passed test");
        passed++;

        cleanup();
    }

    void Open_4Files_Error() {
        System.out.print("Running Open_4Files_Error... ");
        String[] files = {"foo", "foo1", "foo2", "foo3"};
        for (String file : files)
            fs.create(file);
        for (int i = 0; i < files.length; i++)
        {
            boolean result;
            try{
                fs.open(files[i]);
                result = true;
            }
            catch (Throwable t) {
                result = false;
            }

            if  (i == 3)  // attempting to open a 4th file
            {
                updateShouldFail(result);
            }
        }

        cleanup();
    }

    void Close_NotOpenFile_Error() {
        System.out.print("Running Close_NotOpenFile_Error... ");
        boolean result;
        try {
            fs.close(1);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Close_OutOfBoundsIndex_Error() {
        System.out.print("Running Close_OutOfBoundsIndex_Error... ");
        boolean result;
        try {
            fs.close(-1);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Close_File_Pass() {
        System.out.print("Running Close_File_Pass... ");
        fs.create("foo");
        int index = fs.open("foo");
        boolean result;
        try {
            fs.close(index);
            result = true;
        }
        catch(Throwable t) {
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    void Close_Directory_Error() {
        System.out.print("Running Close_Directory_Error... ");
        boolean result;
        try {
            fs.close(0);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Read_NotOpenFile_Error() {
        System.out.print("Running Read_NotOpenFile_Error... ");
        boolean result;
        try {
            fs.read(1, new byte[4], 4);
            result = true;
        }
        catch (Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Read_PastEndOfFile_Pass() {
        System.out.print("Running Read_PastEndOfFile_Pass... ");
        fs.create("foo");
        int index = fs.open("foo");
        boolean result;
        try {
            byte[] b = new byte[60];
            fs.read(index, b, 60);
            result = true;
        }
        catch(Throwable t){
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    void Write_NotOpenFile_Error() {
        System.out.print("Running Write_NotOpenFile_Error... ");
        boolean result;
        try {
            byte[] b = new byte[10];
            Arrays.fill(b,(byte)'x');
            fs.write(1, b, 10);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Write_All3Blocks_Pass() {
        System.out.print("Running Write_All3Blocks_Pass... ");
        fs.create("foo");
        int index = fs.open("foo");
        boolean result;
        try{
            byte[] b = new byte[192];
            Arrays.fill(b,(byte)'x');
            fs.write(index, b, 192);
            result = true;
        }
        catch(Throwable t){
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    void Write_PastAll3Blocks_Error() {
        System.out.print("Running Write_PastAll3Blocks_Error... ");
        fs.create("foo");
        int index = fs.open("foo");
        boolean result;
        try{
            byte[] b = new byte[193];
            Arrays.fill(b,(byte)'x');
            fs.write(index, b, 193);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Seek_Negative_Error() {
        System.out.print("Running Seek_Negative_Error... ");
        boolean result;
        try{
            fs.lseek(0, -1);
            result = true;
        }
        catch(Throwable t) {
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Seek_PastEndOfFile_Error() {
        System.out.print("Running Seek_PastEndOfFile_Error... ");
        boolean result;
        try{
            fs.lseek(0, 193);
            result = true;
        }
        catch(Throwable t){
            result = false;
        }
        updateShouldFail(result);

        cleanup();
    }

    void Seek_ToEndOfFile_Pass() {
        System.out.print("Running Seek_ToEndOfFile_Pass... ");
        fs.create("foo");
        int index = fs.open("foo");
        byte[] b = new byte[10];
        Arrays.fill(b,(byte)'x');
        fs.write(index, b, 10);
        boolean result;
        try{
            fs.lseek(index, 10);
            result = true;
        }
        catch(Throwable t){
            t.printStackTrace();
            result = false;
        }
        updateShouldPass(result);

        cleanup();
    }

    private void updateShouldFail(boolean result)
    {
        if (!result) {
            System.out.println("Passed test");
            passed++;
        }
        else {
            System.out.println("Failed test");
            failed++;
        }
    }

    private void updateShouldPass(boolean result)
    {
        if (!result) {
            System.out.println("Failed test");
            failed++;
        }
        else {
            System.out.println("Passed test");
            passed++;
        }
    }

    public static void main (String[] args)
    {
        CommandTest test = new CommandTest();
        test.runAllTests();
    }
}

