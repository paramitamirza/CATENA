package catena.model.rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;

public class TestTimexTimexRelationRuleTempEval3 {
	
	public TestTimexTimexRelationRuleTempEval3() {
		
	}
	
	public List<String> getTimexTimexTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			boolean goldCandidate) throws Exception {
		
		List<String> tt = new ArrayList<String>();
		
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
				List<String> ttPerFile = TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, goldCandidate);
				tt.addAll(ttPerFile);
				
				// Evaluate the results...
				PairEvaluator pe = new PairEvaluator(ttPerFile);
				pe.printIncorrectAndSentence(doc);
			}
		}		
		return tt;
	}

	public static void main(String [] args) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Apply timex-timex rules to the TimeML (.tml) files...
		String tmlDirpath = "./data/TempEval3-eval_TML/";
		boolean goldCandidate = true;
		TestTimexTimexRelationRuleTempEval3 test = new TestTimexTimexRelationRuleTempEval3();
		List<String> ttResult = test.getTimexTimexTlinks(tmlDirpath, tmlToCol, colParser, goldCandidate);

		// Evaluate the results...
		PairEvaluator pe = new PairEvaluator(ttResult);
		pe.evaluatePerLabel(); 
		
	}
}
