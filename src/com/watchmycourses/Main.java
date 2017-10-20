package com.watchmycourses;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

	    FileSystem fileSystem = new FileSystem(64,24,3);

		File file = new File("src/testFile.txt");

		fileSystem.init(file);
//	    fileSystem.create("abc");
//	    fileSystem.create("ac");
//	    fileSystem.create("ab");
//	    fileSystem.create("aa");
//	    List<String> list = fileSystem.directory();
//
//	    int handle = fileSystem.open("abc");
//
//	    byte[] data = new byte[8];
//
//		Arrays.fill(data,(byte)9); //Binary 1001
//		fileSystem.write(handle,data,8);
//
//		Arrays.fill(data,(byte)8); //Binary 1001
//		fileSystem.write(handle,data,8);
//
//		byte[] newData = new byte[8];
//
//		fileSystem.lseek(handle, 8); //Read in the 8's
//		fileSystem.read(handle,newData,8);
//
//		fileSystem.lseek(handle, 0); //Read in the 9's
//		fileSystem.read(handle,newData,8);
//
//		File file = new File("src/testFile.txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		fileSystem.save(file);
	}
}
