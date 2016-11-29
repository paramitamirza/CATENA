package catena.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import catena.ParserConfig;
import catena.evaluator.PairEvaluator;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;

public class TestCandidateLinks {

	public static void evaluateTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser) throws Exception {
		List<String> ttList = new ArrayList<String>();
		List<String> etList = new ArrayList<String>();
		List<String> edList = new ArrayList<String>();
		List<String> eeList = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// File pre-processing...
//				List<String> columns = tmlToCol.convert(tmlFile, false);
//				Doc doc = colParser.parseLines(columns);
				
//				tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
				Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
				
				TimeMLParser.parseTimeML(tmlFile, doc);
				CandidateLinks.setCandidateTlinks(doc);
				
				// Get the feature vectors
				ttList.addAll(CandidateLinks.getTimexTimexTlinks(doc));
				etList.addAll(CandidateLinks.getEventTimexTlinks(doc));
				edList.addAll(CandidateLinks.getEventDctTlinks(doc));
				eeList.addAll(CandidateLinks.getEventEventTlinks(doc));
			}
		}
		
		// Evaluate the results...
		PairEvaluator ptt = new PairEvaluator(ttList);
		ptt.evaluatePerLabel(CandidateLinks.label);
		
		PairEvaluator pet = new PairEvaluator(etList);
		pet.evaluatePerLabel(CandidateLinks.label);
		
		PairEvaluator ped = new PairEvaluator(edList);
		ped.evaluatePerLabel(CandidateLinks.label);
		
		PairEvaluator pee = new PairEvaluator(eeList);
		pee.evaluatePerLabel(CandidateLinks.label);
	}
	
	public static void main(String[] args) {
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		String tmlDirpath = "./data/TempEval3-eval_TML/";
		
		try {
			
			evaluateTlinks(tmlDirpath, tmlToCol, colParser);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
