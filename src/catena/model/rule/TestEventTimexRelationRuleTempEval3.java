package catena.model.rule;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.ColumnParser.Field;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class TestEventTimexRelationRuleTempEval3 {
	
	public TestEventTimexRelationRuleTempEval3() {
		
	}
	
	public List<String> getEventTimexTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			boolean goldCandidate) throws Exception {
		
		List<String> et = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// File pre-processing...
				List<String> columns = tmlToCol.convert(tmlFile, true);
				Doc doc = colParser.parseLines(columns);
				TimeMLParser.parseTimeML(tmlFile, doc);
				ColumnParser.setCandidateTlinks(doc);
							
				// Applying rules...	
				List<String> etPerFile = EventTimexRelationRule.getEventTimexTlinksPerFile(doc, goldCandidate);
				et.addAll(etPerFile);
				
				// Evaluate the results...
				PairEvaluator pe = new PairEvaluator(etPerFile);
				pe.printIncorrectAndSentence(doc);
			}
		}		
		return et;
	}

	public static void main(String [] args) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Apply event-timex rules to the TimeML (.tml) files...
		String tmlDirpath = "./data/TempEval3-eval_TML/";
		boolean goldCandidate = true;
		TestEventTimexRelationRuleTempEval3 test = new TestEventTimexRelationRuleTempEval3();
		List<String> etResult = test.getEventTimexTlinks(tmlDirpath, tmlToCol, colParser, goldCandidate);

		// Evaluate the results...
		PairEvaluator pe = new PairEvaluator(etResult);
		pe.evaluatePerLabel(); 
		
	}
}
