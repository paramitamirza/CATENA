package catena.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import catena.parser.entities.EntityEnum;

public class TextProParser {
	
	private String textProPath;
	private String language;
	
	public TextProParser() {
		
	}
	
	public TextProParser(String textProPath) {
		this.setTextProPath(textProPath);
		this.setLanguage("eng");
	}
	
	public TextProParser(String textProPath, EntityEnum.Language lang) {
		setTextProPath(textProPath);
		switch(lang) {
			case EN: this.setLanguage("eng"); break;
			case IT: this.setLanguage("it"); break;
			default: this.setLanguage("eng"); break;
		}		
	}

	public String getTextProPath() {
		return textProPath;
	}

	public void setTextProPath(String textProPath) {
		this.textProPath = textProPath;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	public void run(String filepath, String[] annotations, boolean tokenized) throws IOException, InterruptedException {
		List<String> annotationList = Arrays.asList(annotations);
		ProcessBuilder pb;
		if (tokenized) {
			pb = new ProcessBuilder("/bin/sh",
					"textpro.sh", "-v", 
					"-l", getLanguage(), 
					"-c", String.join("+", annotationList), 
	//				"-n", outputFilePath,
					"-d", "tokenization+sentence",
					"-y",
					filepath);
		} else {
			pb = new ProcessBuilder("/bin/sh",
					"textpro.sh", "-v", 
					"-l", getLanguage(), 
					"-c", String.join("+", annotationList), 
	//				"-n", outputFilePath,
					"-y",
					filepath);
		}
		Map<String, String> env = pb.environment();
		pb.directory(new File(getTextProPath()));
		Process p = pb.start();
		p.waitFor();
	}
	
	public String run(String[] annotations, String inputText, boolean tokenized) throws IOException, InterruptedException {
		FileWriter fileStream = new FileWriter(new File(getTextProPath() + "temp"));
		BufferedWriter out = new BufferedWriter(fileStream);
		out.write(inputText);
		out.close();
		
		run("temp", annotations, tokenized);
		
		StringBuilder sb = new StringBuilder();
		Scanner fileScanner = new Scanner(new File(getTextProPath() + "temp.txp"));
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    sb.append(next);
	        sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
	
	public void run(String[] annotations, File inputFile, File outputFile, boolean tokenized) throws IOException, InterruptedException {
		
		String inputName = inputFile.getName();
		
		Files.copy(inputFile.toPath(), new File(getTextProPath() + "temp_" + inputName).toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		run("temp_" + inputName, annotations, tokenized);   		
		//Copy the output file as it is
//		Files.copy(new File(getTextProPath() + "temp.txp").toPath(), new File(outputFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);

		//Copy the output file without the first four lines
		Scanner fileScanner = new Scanner(new File(getTextProPath() + "temp_" + inputName + ".txp"));
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		FileWriter fileStream = new FileWriter(outputFile);
		BufferedWriter out = new BufferedWriter(fileStream);
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    out.write(next + "\n");
		}
		out.close();
	}
	
	public List<String> run(String[] annotations, File inputFile, boolean tokenized) throws IOException, InterruptedException {
		List<String> result = new ArrayList<String>();
		
		String inputName = inputFile.getName();
		
		Files.copy(inputFile.toPath(), new File(getTextProPath() + "temp_" + inputName).toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		run("temp_" + inputName, annotations, tokenized);
		
		Scanner fileScanner = new Scanner(new File(getTextProPath() + "temp_" + inputName + ".txp"));
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    result.add(next);
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		
		try {
			TextProParser textpro = new TextProParser("./tools/TextPro2.0/");
			String[] annotations = {"token", "pos", "chunk"};
			
			textpro.run(annotations, new File("./data/example_TXP/sample.txt"), new File("./data/example_TXP/sample.txt.txp"), false);
			textpro.run(annotations, new File("./data/example_TXP/sample_tokenized.txt"), new File("./data/example_TXP/sample_tokenized.txt.txp"), true);
			
			String result = textpro.run(annotations, "Cat is angry.", false);
			System.out.println(result);
			
		} catch (IOException | InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

}
