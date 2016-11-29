package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.CandidateLinks;
import catena.model.classifier.PairClassifier;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;
import catena.ParserConfig;
import catena.evaluator.PairEvaluator;

public class TestEventEventTemporalClassifierTempEval3 {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	
	public TestEventEventTemporalClassifierTempEval3() {
		
	}
	
	public List<PairFeatureVector> getEventEventTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate, boolean etFeature) throws Exception {
		return getEventEventTlinks(tmlDirpath, 
				tmlToCol, colParser, 
				etRelCls, train, goldCandidate, 
				new HashMap<String, String>(),
				etFeature);
	}
	
	public List<PairFeatureVector> getEventEventTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate, 
			Map<String, String> relTypeMapping, 
			boolean etFeature) throws Exception {
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
				
				Map<String, String> etlinks = null;		
				if (etFeature) etlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				List<String> labelList = Arrays.asList(labelCollapsed);
				fvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, etRelCls, 
						train, goldCandidate, labelList, relTypeMapping, etlinks));
				
			}
		}
		
		return fvList;
	}

	public static void main(String [] args) throws Exception {
		
		TestEventEventTemporalClassifierTempEval3 test = new TestEventEventTemporalClassifierTempEval3();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier("te3", "liblinear");
		
		boolean goldCandidate = true;
		boolean etFeature = true;
		
		// TRAIN
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		List<PairFeatureVector> trainFvList = test.getEventEventTlinks(trainTmlDirpath, tmlToCol, colParser,
				eeCls, true, goldCandidate, etFeature);
		eeCls.train(trainFvList, "./models/test/te3-ee.model");
		
		// PREDICT
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		List<PairFeatureVector> evalFvList = test.getEventEventTlinks(evalTmlDirpath, tmlToCol, colParser,
				eeCls, false, goldCandidate, etFeature);
		List<String> eeClsTest = eeCls.predict(evalFvList, "./models/test/te3-ee.model", test.labelCollapsed);
		List<String> eeTestList = new ArrayList<String>();
		for (int i = 0; i < evalFvList.size(); i ++) {
			EventEventFeatureVector eefv = new EventEventFeatureVector(evalFvList.get(i));
			String label = eeClsTest.get(i);
	
			if (label.equals("IDENTITY")) label = "SIMULTANEOUS";	//TempEval-3 evaluation hack
			eeTestList.add(eefv.getE1().getID() 
					+ "\t" + eefv.getE2().getID()
					+ "\t" + eefv.getLabel()
					+ "\t" + label);
		}
		
		// EVALUATE
		PairEvaluator pee = new PairEvaluator(eeTestList);
		pee.evaluatePerLabel(test.label);
	}
}
