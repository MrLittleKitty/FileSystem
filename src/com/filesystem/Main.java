package com.filesystem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] a) {

    	Scanner scanner = new Scanner(System.in);
		PrintStream out = System.out;

		out.println("Enter path to input file:");
		String inputPath = scanner.nextLine();

		out.println("Enter path to output file:");
		String outputPath = scanner.nextLine();

		List<String> output = new ArrayList<>();
		Driver driver = new Driver();
		driver.output = output;

		File inputFile = new File(inputPath);
		try(FileReader fileReader = new FileReader(inputFile)) {
			try(BufferedReader reader = new BufferedReader(fileReader)) {

				String line = null;

				while((line = reader.readLine()) != null) {
					if(line.trim().isEmpty()) {
						output.add("");
					}
					else {
						String[] args = line.split(" ");

						switch(args[0].toLowerCase()) {

							case "cr":
								driver.create(args[1]);
								break;
							case "de":
								driver.destroy(args[1]);
								break;
							case "op":
								driver.open(args[1]);
								break;
							case "cl":
								driver.close(Integer.parseInt(args[1]));
								break;
							case "rd":
								driver.read(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
								break;
							case "wr":
								driver.write(Integer.parseInt(args[1]),args[2].charAt(0),Integer.parseInt(args[3]));
								break;
							case "sk":
								driver.seek(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
								break;
							case "dr":
								driver.directory();
								break;
							case "in":
								if(args.length > 1)
									driver.init(args[1]);
								else
									driver.init();
								break;
							case "sv":
								driver.save(args[1]);
								break;
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for(String line : output)
			System.out.println(line);

		File outputFile = new File(outputPath);
		try(FileWriter fileWriter = new FileWriter(outputFile)) {
			try(BufferedWriter writer = new BufferedWriter(fileWriter)) {
				for(String line : output) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
