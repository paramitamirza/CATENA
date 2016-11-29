package catena.model.rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import catena.ParserConfig;
import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;

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
				CandidateLinks.setCandidateTlinks(doc);
							
				// Applying rules...	
				List<String> etPerFile = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, goldCandidate);
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
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
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
