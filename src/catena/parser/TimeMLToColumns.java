package catena.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import catena.parser.entities.EntityEnum;
import catena.parser.entities.TimeMLDoc;

import edu.stanford.nlp.simple.*;

public class TimeMLToColumns {
	
	private EntityEnum.Language language;
	
	public TimeMLToColumns() {
		
	}
	
	public TimeMLToColumns(EntityEnum.Language lang) {
		this.setLanguage(lang);
	}

	public EntityEnum.Language getLanguage() {
		return language;
	}

	public void setLanguage(EntityEnum.Language language) {
		this.language = language;
	}
	
	public List<String> parse(String timeMLFilePath) throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		List<String> columns = new ArrayList<String>();
		
		//Get <TEXT> content from TimeML document
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		TimeMLDoc tmlDoc = new TimeMLDoc(timeMLFilePath);
		String tmlText = tmlParser.getText(tmlDoc);
//		System.out.println(tmlText);			
		String tmlTextOnly = tmlParser.getTextOnly(tmlDoc);
//		System.out.println(tmlTextOnly);
		
		//Get <MAKEINSTANCE> map (eventID to tense, aspect, and polarity) from TimeML document
		Map<String, String> mapInstances = tmlParser.getEventTenseAspectPolarity(tmlDoc);
		
//		//Run TextPro parser on the content string
//		TextProParser textpro = new TextProParser("./tools/TextPro2.0/");
//		String[] annotations = {"token"};
//		String result = textpro.run(annotations, tmlText);
//		System.out.println(result);
		
		//TODO DCT into columns
		
		//Run Stanford parser on the content string (with tags)
		List<String> tokensWithTags = new ArrayList<String>();
		Document doc = new Document(tmlText);
		for (Sentence sent : doc.sentences()) {
			for (String word : sent.words()) {
				if (word.equals("``")) tokensWithTags.add("\"");
				else if (word.equals("''")) tokensWithTags.add("\"");
				else tokensWithTags.add(word);
			}
			tokensWithTags.add("");
        }
		
		//Run Stanford parser on the content string (text only)
		List<String> tokens = new ArrayList<String>();
		List<String> lemmas = new ArrayList<String>();
		doc = new Document(tmlTextOnly);
		for (Sentence sent : doc.sentences()) {
			for (String word : sent.words()) {
				if (word.equals("``")) tokens.add("\"");
				else if (word.equals("''")) tokens.add("\"");
				else tokens.add(word);
			}
			for (String lemma : sent.lemmas()) {
				if (lemma.equals("``")) lemmas.add("\"");
				else if (lemma.equals("''")) lemmas.add("\"");
				else lemmas.add(lemma);
			}
			tokens.add("");
			lemmas.add("");
        }
		
		
		int i = 0, j = 0, sent = 1, idx = 1;
		String evId = "O", evClass = "O";
		String tmxId = "O", tmxType = "O", tmxValue = "O";
		String tenseAspectPolarity = "O";
		while (i < tokensWithTags.size()) {
			if (tokens.get(j).equals("")) {
				columns.add(tokens.get(j));
				i ++;
				j ++;
				sent ++;
			} else {
				if (tokensWithTags.get(i).equals(tokens.get(j))) {
					columns.add(tokens.get(j) 
							+ "\t" + "t" + idx 
							+ "\t" + sent
							+ "\t" + lemmas.get(j)
							+ "\t" + evId
							+ "\t" + evClass
							+ "\t" + tenseAspectPolarity
							+ "\t" + tmxId
							+ "\t" + tmxType
							+ "\t" + tmxValue);
					i ++;
					j ++;
					idx ++;
				} else {
					if (tokensWithTags.get(i).equals(".")) i ++;
					else if (tokensWithTags.get(i).equals("")) i ++;
					else if (tokensWithTags.get(i).equals("</EVENT>")) {
						evId = "O";
						evClass = "O";
						tenseAspectPolarity = "O";
						i ++;
					}
					else if (tokensWithTags.get(i).equals("</TIMEX3>")) {
						tmxId = "O";
						tmxType = "O";
						tmxValue = "O";
						i ++;
					}
					else if (tokensWithTags.get(i).contains("<EVENT")) {
						Pattern pEvId = Pattern.compile("eid=\"(.*?)\"");
						Matcher mEvId = pEvId.matcher(tokensWithTags.get(i));							
						if (mEvId.find()) {
							evId = mEvId.group(1);
						}
						Pattern pEvClass = Pattern.compile("class=\"(.*?)\"");
						Matcher mEvClass = pEvClass.matcher(tokensWithTags.get(i));							
						if (mEvClass.find()) {
							evClass = mEvClass.group(1);
						}
						tenseAspectPolarity = mapInstances.get(evId);
						i ++;
					} else if (tokensWithTags.get(i).contains("<TIMEX3")) {
						Pattern pTmxId = Pattern.compile("tid=\"(.*?)\"");
						Matcher mTmxId = pTmxId.matcher(tokensWithTags.get(i));							
						if (mTmxId.find()) {
							tmxId = mTmxId.group(1);
						}
						Pattern pTmxType = Pattern.compile("type=\"(.*?)\"");
						Matcher mTmxType = pTmxType.matcher(tokensWithTags.get(i));							
						if (mTmxType.find()) {
							tmxType = mTmxType.group(1);
						}
						Pattern pTmxValue = Pattern.compile("value=\"(.*?)\"");
						Matcher mTmxValue = pTmxValue.matcher(tokensWithTags.get(i));							
						if (mTmxValue.find()) {
							tmxValue = mTmxValue.group(1);
						}
						i ++;
					}
				}
			}
		}
		
		return columns;
	}
	
	public void printToConllFile(List<String> columns, String outputFilePath) throws IOException {
		FileWriter fileStream = new FileWriter(new File(outputFilePath));
		BufferedWriter out = new BufferedWriter(fileStream);	
		String sent = ""; 
		int idx = 1;
		for (String s : columns) {
			if (!s.equals("")) {
				String[] cols = s.split("\t");
				if (!sent.equals(cols[2])) {
					sent = cols[2];
					idx = 1;
				}
				out.write(idx + "\t" + cols[0]);
				for (int i = 0; i < 13; i ++) {
					out.write("\t_");
				}
				out.write("\n");
			} else {
				out.write("\n");
			}
			idx ++;
		}
		out.close();
	}
	
	public static void main(String[] args) {
		
		try {
			
			TimeMLToColumns tmlToCol = new TimeMLToColumns();
			List<String> columns = tmlToCol.parse("./data/example_TML/wsj_1014.tml");
			
			//Print in CoNLL format as the input for the Mate tools
			tmlToCol.printToConllFile(columns, "./data/example_CoNLL/wsj_1014.conll");
			
			//TODO Print tokens in lines as the input for TextPro
			
			//TODO Run TextPro
			
			//TODO Run Mate tools
			
			//TODO Merge the results into columns
			
		} catch (ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError
				| TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
