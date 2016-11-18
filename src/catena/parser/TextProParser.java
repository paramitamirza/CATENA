package catena.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.JAXBException;

public class TextProParser {
	
	private String textProPath;
	
	public TextProParser() {
		
	}
	
	public TextProParser(String textProPath) {
		setTextProPath(textProPath);
	}

	public String getTextProPath() {
		return textProPath;
	}

	public void setTextProPath(String textProPath) {
		this.textProPath = textProPath;
	}
	
	public void run(String language, String[] annotations, String inputFilePath, String outputFilePath) throws IOException, InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, JAXBException {
		List<String> annotationList = Arrays.asList(annotations);
		
		Files.copy(new File(inputFilePath).toPath(), new File(getTextProPath() + "temp").toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "textpro.sh", "-v", 
				"-l", language, 
				"-c", String.join("+", annotationList), 
//				"-n", outputFilePath,
				"-y",
				"temp");
		Map<String, String> env = pb.environment();
		pb.directory(new File(getTextProPath()));
		Process p = pb.start();
		p.waitFor();
		
		//Copy the output file as it is
//		Files.copy(new File(getTextProPath() + "temp.txp").toPath(), new File(outputFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);

		//Copy the output file without the first four lines
		Scanner fileScanner = new Scanner(new File(getTextProPath() + "temp.txp"));
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		fileScanner.nextLine();
		FileWriter fileStream = new FileWriter(new File(outputFilePath));
		BufferedWriter out = new BufferedWriter(fileStream);
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    out.write(next + "\n");
		}
		out.close();
	}
	
	public static void main(String[] args) {
		
		try {
			TextProParser textpro = new TextProParser("./tools/TextPro2.0/");
			String[] annotations = {"token", "pos", "chunk"};
			textpro.run("eng", annotations, "./data/sample.txt", "./data/sample.txt.txp");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
