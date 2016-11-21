package catena.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import catena.parser.entities.EntityEnum;

import is2.lemmatizer.*;
import is2.tag.*;
import is2.parser.*;

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
	
	public void run(String inputFile) throws Exception {
		run (inputFile, inputFile + ".dep");
	}
	
	public void run(String inputFile, String outputFile) throws Exception {
		
		String[] lemmatizerArgs = {"-model", this.getMateToolsPath() + "models/lemmatizer-eng-4M-v36.mdl",
				"-test", inputFile,
				"-out", "./data/temp"};
		Lemmatizer.main(lemmatizerArgs);
		
		String[] taggerArgs = {"-model", this.getMateToolsPath() + "models/tagger-eng-4M-v36.mdl",
				"-test", "./data/temp",
				"-out", "./data/temp2"};
		Tagger.main(taggerArgs);
		
		String[] parserArgs = {"-model", this.getMateToolsPath() + "models/parser-eng-12M-v36.mdl",
				"-test", "./data/temp2",
				"-out", outputFile};
		Parser.main(parserArgs);
		
		Files.delete(new File("./data/temp").toPath());
		Files.delete(new File("./data/temp2").toPath());
	}
	
	public static void main(String[] args) {
		
		try {
			
			MateToolsParser mateTools = new MateToolsParser("./tools/MateTools/");
			mateTools.run("./data/example_CoNLL/wsj_1014.conll");
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
