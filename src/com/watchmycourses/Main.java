package com.watchmycourses;

public class Main {

    public static void main(String[] args) {

	    FileSystem fileSystem = new FileSystem(64,24,3);

	    fileSystem.create("abc");

	    int handle = fileSystem.open("abc");

	    return;
    }
}
