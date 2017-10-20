package com.watchmycourses;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Driver {

    FileSystem fileSystem = null;

    void create(String name) {
        if(fileSystem == null)
            output("error");
        else {
            fileSystem.create(name);
            output(name+" created");
        }
    }

    void destroy(String name) {
        if(fileSystem == null)
            output("error");
        else {
            fileSystem.destroy(name);
            output(name+" destroyed");
        }
    }

    void open(String name) {
        if(fileSystem == null)S
            output("error");
        else {
            int index = fileSystem.open(name);
            output(name+" opened "+index);
        }
    }

    void close(int index) {
        if(fileSystem == null)
            output("error");
        else {
            fileSystem.close(index);
            output(index+" closed");
        }
    }

    void read(int index, int count) {
        if(fileSystem == null)
            output("error");
        else {
            byte[] mem = new byte[count];
            fileSystem.read(index,mem,count);
        }
    }

    void write(int index, char letter, int count) {

    }

    void seek(int index, int position) {
        if(fileSystem == null)
            output("error");
        else {
            fileSystem.lseek(index, position);
            output("position is "+position);
        }
    }

    void directory() {

        if(fileSystem == null)
            output("error");
        else {
            List<String> files = fileSystem.directory();
            StringBuilder builder = new StringBuilder();
            for(String f : files) {
                builder.append(f).append(" ");
            }
            if(files.size() > 0)
                builder.setLength(builder.length()-1);
            output(builder.toString());
        }
    }

    void init(String fileName) {

        fileSystem = new FileSystem(64,24,3);
        File file = new File(fileName);
        if(!file.exists()) {
            output("disk initialized");
        }
        else {
            fileSystem.init(file);
            output("disk restored");
        }
    }

    void save(String fileName) {

        File file = new File(fileName);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(fileSystem == null)
            output("error");
        else {
            fileSystem.save(file);
            output("disk saved");
        }
    }

    void output(String text) {
        System.out.println(text);
    }
}
