package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import catena.model.CandidateLinks;
import catena.model.classifier.PairClassifier;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;
import catena.evaluator.PairEvaluator;

public class TestEventTimexTemporalClassifierTempEval3 {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	
	public TestEventTimexTemporalClassifierTempEval3() {
		
	}
	
	public List<PairFeatureVector> getEventTimexTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate, boolean ttFeature) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
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
				if (!train) CandidateLinks.setCandidateTlinks(doc);
				
				// Get the feature vectors
				fvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etRelCls, 
						train, goldCandidate, ttFeature));
			}
		}
		return fvList;
	}

	public static void main(String [] args) throws Exception {
		
		TestEventTimexTemporalClassifierTempEval3 test = new TestEventTimexTemporalClassifierTempEval3();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier("te3", "liblinear");
		
		boolean goldCandidate = false;
		boolean ttFeature = true;
		
		// TRAIN
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		List<PairFeatureVector> trainFvList = test.getEventTimexTlinks(trainTmlDirpath, tmlToCol, colParser,
				etCls, true, goldCandidate, ttFeature);
		etCls.train(trainFvList, "./models/test/te3-et.model");
		
		// PREDICT
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		List<PairFeatureVector> evalFvList = test.getEventTimexTlinks(evalTmlDirpath, tmlToCol, colParser,
				etCls, false, goldCandidate, ttFeature);
		List<String> etClsTest = etCls.predict(evalFvList, "./models/test/te3-et.model");
		List<String> etTestList = new ArrayList<String>();
		for (int i = 0; i < evalFvList.size(); i ++) {
			EventTimexFeatureVector etfv = new EventTimexFeatureVector(evalFvList.get(i));
			String label = etClsTest.get(i);
	
			etTestList.add(etfv.getE1().getID() 
					+ "\t" + etfv.getE2().getID()
					+ "\t" + etfv.getLabel()
					+ "\t" + label);
		}
		
		// EVALUATE
		PairEvaluator pet = new PairEvaluator(etTestList);
		pet.evaluatePerLabel(test.label);
		
	}
}
