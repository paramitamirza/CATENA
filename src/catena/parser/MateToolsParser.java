package catena.parser;

import java.io.File;
import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import catena.ParserConfig;
import catena.parser.entities.EntityEnum;

public class MateToolsParser {

	private String mateLemmatizerModel;
	private String mateTaggerModel;
	private String mateParserModel;
	
	private EntityEnum.Language language;
	
	public MateToolsParser() {
		
	}
	
	public MateToolsParser(String mateLemmatizerModelPath, String mateTaggerModelPath, String mateParserModelPath) {
		this.setMateLemmatizerModel(mateLemmatizerModelPath);
		this.setMateTaggerModel(mateTaggerModelPath);
		this.setMateParserModel(mateParserModelPath);
		this.setLanguage(EntityEnum.Language.EN);
	}
	
	public MateToolsParser(String mateLemmatizerModelPath, String mateTaggerModelPath, String mateParserModelPath, 
			EntityEnum.Language lang) {
		this.setMateLemmatizerModel(mateLemmatizerModelPath);
		this.setMateTaggerModel(mateTaggerModelPath);
		this.setMateParserModel(mateParserModelPath);
		this.setLanguage(lang);
	}

	public EntityEnum.Language getLanguage() {
		return language;
	}

	public void setLanguage(EntityEnum.Language language) {
		this.language = language;
	}
	
	public void run(File inputFile, File outputFile) throws Exception {
		
		PrintStream originalOutStream = System.out;
		PrintStream originalErrStream = System.err;
		PrintStream dummyStream    = new PrintStream(new OutputStream(){
		    public void write(int b) {
		        //NO-OP
		    }
		});
		System.setOut(dummyStream);
		System.setErr(dummyStream);
		
		String[] lemmatizerArgs = {"-model", this.getMateLemmatizerModel(),
				"-test", inputFile.getPath(),
				"-out", "./data/temp"};
		is2.lemmatizer.Lemmatizer.main(lemmatizerArgs);
		
		String[] taggerArgs = {"-model", this.getMateTaggerModel(),
				"-test", "./data/temp",
				"-out", "./data/temp2"};
		is2.tag.Tagger.main(taggerArgs);
		
		String[] parserArgs = {"-model", this.getMateParserModel(),
				"-test", "./data/temp2",
				"-out", outputFile.getPath()};
		is2.parser.Parser.main(parserArgs);
		
		Files.delete(new File("./data/temp").toPath());
		Files.delete(new File("./data/temp2").toPath());
		
		System.setOut(originalOutStream);
		System.setErr(originalErrStream);
	}
	
	public List<String> run(File inputFile) throws Exception {
		List<String> result = new ArrayList<String>();
		
		run (inputFile, new File(inputFile.getPath() + ".dep"));
		
		Scanner fileScanner = new Scanner(new File(inputFile.getPath() + ".dep"));
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    result.add(next);
		}
		fileScanner.close();
		
		Files.delete(new File(inputFile + ".dep").toPath());
		
		return result;
	}
	
	public static void main(String[] args) {
		
		try {
			
			MateToolsParser mateTools = new MateToolsParser(ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
			List<String> mateToolsColumns = mateTools.run(new File("./data/example_CoNLL/wsj_1014.conll"));
			for (String s : mateToolsColumns) System.out.println(s);
			
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public String getMateLemmatizerModel() {
		return mateLemmatizerModel;
	}

	public void setMateLemmatizerModel(String mateLemmatizerModel) {
		this.mateLemmatizerModel = mateLemmatizerModel;
	}

	public String getMateTaggerModel() {
		return mateTaggerModel;
	}

	public void setMateTaggerModel(String mateTaggerModel) {
		this.mateTaggerModel = mateTaggerModel;
	}

	public String getMateParserModel() {
		return mateParserModel;
	}

	public void setMateParserModel(String mateParserModel) {
		this.mateParserModel = mateParserModel;
	}
}
