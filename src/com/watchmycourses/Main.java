package com.watchmycourses;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {

	    FileSystem fileSystem = new FileSystem(64,24,3);

	    fileSystem.create("abc");

	    int handle = fileSystem.open("abc");

	    byte[] data = new byte[8];
		Arrays.fill(data,(byte)9); //Binary 1001

		fileSystem.write(handle,data,8);

		byte[] newData = new byte[8];

		fileSystem.lseek(handle, 0);

		fileSystem.read(handle,newData,8);

	    return;
    }
}
