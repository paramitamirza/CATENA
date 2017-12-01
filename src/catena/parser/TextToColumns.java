package catena.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import catena.ParserConfig;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class TextToColumns {
	
	private static void writeToConllFile(String title, List<String> sentences) throws IOException {
		StringBuilder conllString = new StringBuilder();
		
		for (String s : sentences) {
			Sentence sent = new Sentence(s);
			conllString.append(MateToolsParser.toConllString(sent.words()));
		}
        
        BufferedWriter bw = new BufferedWriter(new FileWriter("./data/" + title + ".conll"));
        bw.write(conllString.toString());
        bw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		String[] sentences = {
				"Plants reflect green light and absorb other wavelengths.", 
				"The action of gravitational force on regions of different densities causes them to rise or fall-and such circulation, influenced by the rotation of the earth, produces winds and ocean currents.", 
				"First, somewhat obviously, roots firmly anchor the plant to a fixed spot.", 
				"The pieces of the plant and animal puzzle depend on each other to form a complete picture.", 
				"Tillandsias are tropical plants that usually live for several years and will bloom and produce flowers only one time during their lifetime."
		};
		
		String title = "science-sentences";
		
		writeToConllFile(title, Arrays.asList(sentences));
		MateToolsParser mateTools = new MateToolsParser(ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel, ParserConfig.mateSrlModel);
		mateTools.runFullPipeline(new File("./data/" + title + ".conll"), new File("./data/" + title + ".srl"));
		
	}

}
