package catena.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import catena.parser.entities.EntityEnum;

public class MateToolsParser {

	private String mateToolsPath;
	private EntityEnum.Language language;
	
	public MateToolsParser() {
		
	}
	
	public MateToolsParser(String mateToolsPath) {
		this.setMateToolsPath(mateToolsPath);
		this.setLanguage(EntityEnum.Language.EN);
	}
	
	public MateToolsParser(String mateToolsPath, EntityEnum.Language lang) {
		this.setMateToolsPath(mateToolsPath);
		this.setLanguage(lang);
	}

	public String getMateToolsPath() {
		return mateToolsPath;
	}

	public void setMateToolsPath(String mateToolsPath) {
		this.mateToolsPath = mateToolsPath;
	}

	public EntityEnum.Language getLanguage() {
		return language;
	}

	public void setLanguage(EntityEnum.Language language) {
		this.language = language;
	}
	
	public void run(File inputFile, File outputFile) throws Exception {
		
		String[] lemmatizerArgs = {"-model", this.getMateToolsPath() + "models/lemmatizer-eng-4M-v36.mdl",
				"-test", inputFile.getPath(),
				"-out", "./data/temp"};
		is2.lemmatizer.Lemmatizer.main(lemmatizerArgs);
		
		String[] taggerArgs = {"-model", this.getMateToolsPath() + "models/tagger-eng-4M-v36.mdl",
				"-test", "./data/temp",
				"-out", "./data/temp2"};
		is2.tag.Tagger.main(taggerArgs);
		
		String[] parserArgs = {"-model", this.getMateToolsPath() + "models/parser-eng-12M-v36.mdl",
				"-test", "./data/temp2",
				"-out", outputFile.getPath()};
		is2.parser.Parser.main(parserArgs);
		
		Files.delete(new File("./data/temp").toPath());
		Files.delete(new File("./data/temp2").toPath());
	}
	
	public List<String> run(File inputFile) throws Exception {
		List<String> result = new ArrayList<String>();
		
		run (inputFile, new File(inputFile.getPath() + ".dep"));
		
		Scanner fileScanner = new Scanner(new File(inputFile.getPath() + ".dep"));
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    result.add(next);
		}
		
		Files.delete(new File(inputFile + ".dep").toPath());
		
		return result;
	}
	
	public static void main(String[] args) {
		
		try {
			
			MateToolsParser mateTools = new MateToolsParser("./tools/MateTools/");
			List<String> mateToolsColumns = mateTools.run(new File("./data/example_CoNLL/wsj_1014.conll"));
			for (String s : mateToolsColumns) System.out.println(s);
			
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
