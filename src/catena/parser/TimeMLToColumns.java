package catena.parser;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import catena.parser.entities.EntityEnum;
import catena.parser.entities.TimeMLDoc;

public class TimeMLToColumns {
	
	public TimeMLToColumns() {
		
	}
	
	public static void main(String[] args) {
		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		try {
			//Get <TEXT> content from TimeML document
			TimeMLDoc tmlDoc = new TimeMLDoc("./data/example_TML/wsj_1014.tml");
			String tmlText = tmlParser.getTextOnly(tmlDoc);
			System.out.println(tmlText);
			
			//Run TextPro parser on the content string
			TextProParser textpro = new TextProParser("./tools/TextPro2.0/");
			String[] annotations = {"token"};
			String result = textpro.run(annotations, tmlText);
			System.out.println(result);
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
