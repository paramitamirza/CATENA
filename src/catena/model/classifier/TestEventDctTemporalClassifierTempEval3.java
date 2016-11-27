package catena.model.classifier;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import catena.model.classifier.PairClassifier.VectorClassifier;
import catena.model.CandidateLinks;
import catena.model.classifier.PairClassifier;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.rule.EventEventTemporalRule;
import catena.model.rule.EventTimexTemporalRule;
import catena.model.rule.TestEventTimexRelationRuleTempEval3;
import catena.model.rule.TestTimexTimexRelationRuleTempEval3;
import catena.model.rule.TimexTimexTemporalRule;
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
import catena.evaluator.PairEvaluator;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class TestEventDctTemporalClassifierTempEval3 {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	
	public TestEventDctTemporalClassifierTempEval3() {
		
	}
	
	public List<PairFeatureVector> getEventDctTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate) throws Exception {
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
				fvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, etRelCls, 
						train, goldCandidate));
			}
		}
		return fvList;
	}

	public static void main(String [] args) throws Exception {
		
		TestEventDctTemporalClassifierTempEval3 test = new TestEventDctTemporalClassifierTempEval3();
		
		// Init the parsers...
				TimeMLToColumns tmlToCol = new TimeMLToColumns();
				ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier("te3", "liblinear");
		
		boolean goldCandidate = true;
		
		// TRAIN
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		List<PairFeatureVector> trainFvList = test.getEventDctTlinks(trainTmlDirpath, tmlToCol, colParser,
				edCls, true, goldCandidate);
		edCls.train(trainFvList, "./models/test/te3-ed.model");
		
		// PREDICT
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		List<PairFeatureVector> evalFvList = test.getEventDctTlinks(evalTmlDirpath, tmlToCol, colParser,
				edCls, false, goldCandidate);
		List<String> edClsTest = edCls.predict(evalFvList, "./models/test/te3-ed.model");
		List<String> edTestList = new ArrayList<String>();
		for (int i = 0; i < evalFvList.size(); i ++) {
			EventTimexFeatureVector edfv = new EventTimexFeatureVector(evalFvList.get(i));
			String label = edClsTest.get(i);
	
			edTestList.add(edfv.getE1().getID() 
					+ "\t" + edfv.getE2().getID()
					+ "\t" + edfv.getLabel()
					+ "\t" + label);
		}
		
		// EVALUATE
		PairEvaluator ped = new PairEvaluator(edTestList);
		ped.evaluatePerLabel(test.label);
		
	}
}
