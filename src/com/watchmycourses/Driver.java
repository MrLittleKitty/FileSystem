package com.watchmycourses;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Driver {

    private FileSystem fileSystem = null;
    public List<String> output;

    void create(String name) {
        if(fileSystem == null)
            output("error");
        else {
            try{
                fileSystem.create(name);
                output(name+" created");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void destroy(String name) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                fileSystem.destroy(name);
                output(name + " destroyed");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void open(String name) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                int index = fileSystem.open(name);
                output(name+" opened "+index);
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void close(int index) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                fileSystem.close(index);
                output(index+" closed");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void read(int index, int count) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                byte[] mem = new byte[count];
                int numRead = fileSystem.read(index,mem,count);

                char[] chars = new char[numRead];
                for(int i = 0; i < numRead; i++) {
                    chars[i] = (char)mem[i];
                }
                output(new String(chars));
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void write(int index, char letter, int count) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                byte b = (""+letter).getBytes()[0];

                byte[] bytes = new byte[count];
                for(int i = 0; i < count; i++) {
                    bytes[i] = b;
                }

                fileSystem.write(index, bytes, count);
                output(count+" bytes written");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void seek(int index, int position) {
        if(fileSystem == null)
            output("error");
        else {
            try {
                fileSystem.lseek(index, position);
                output("position is "+position);
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void directory() {

        if(fileSystem == null)
            output("error");
        else {
            try {
                List<String> files = fileSystem.directory();
                StringBuilder builder = new StringBuilder();
                for(String f : files) {
                    builder.append(f).append(" ");
                }
                if(files.size() > 0)
                    builder.setLength(builder.length()-1);
                output(builder.toString());
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void init() {
        fileSystem = new FileSystem(64,24,3);
        output("disk initialized");
    }

    void init(String fileName) {

        fileSystem = new FileSystem(64,24,3);
        File file = new File(fileName);
        if(!file.exists()) {
            output("disk initialized");
        }
        else {
            try {
                fileSystem.init(file);
                output("disk restored");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void save(String fileName) {

        File file = new File(fileName);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                output("error");
            }
        }

        if(fileSystem == null)
            output("error");
        else {
            try {
                fileSystem.save(file);
                output("disk saved");
            }
            catch(Throwable t) {
                output("error");
            }
        }
    }

    void output(String text) {
        output.add(text);
    }
}
