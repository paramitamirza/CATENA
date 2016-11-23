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
import catena.model.classifier.PairClassifier;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.rule.EventEventRelationRule;
import catena.model.rule.EventTimexRelationRule;
import catena.model.rule.TestEventTimexRelationRuleTempEval3;
import catena.model.rule.TestTimexTimexRelationRuleTempEval3;
import catena.model.rule.TimexTimexRelationRule;
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

public class TestEventTimexRelationClassifierTempEval3 {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	
	public TestEventTimexRelationClassifierTempEval3() {
		
	}
	
	public List<PairFeatureVector> getEventTimexTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate, boolean ttFeature) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			System.out.println("Processing " + tmlFile.getPath());
			
			// File pre-processing...
			List<String> columns = tmlToCol.convert(tmlFile, true);
			Doc doc = colParser.parseLines(columns);
			TimeMLParser.parseTimeML(tmlFile, doc);
			ColumnParser.setCandidateTlinks(doc);
			
			// Get the feature vectors
			fvList.addAll(EventTimexRelationClassifier.getEventTimexTlinksPerFile(doc, etRelCls, 
					train, goldCandidate, ttFeature));
		}
		return fvList;
	}

	public static void main(String [] args) throws Exception {
		
		TestEventTimexRelationClassifierTempEval3 test = new TestEventTimexRelationClassifierTempEval3();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventTimexRelationClassifier etCls = new EventTimexRelationClassifier("te3", "liblinear");
		
		// TRAIN
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		List<PairFeatureVector> trainFvList = test.getEventTimexTlinks(trainTmlDirpath, tmlToCol, colParser,
				etCls, true, true, false);
		etCls.train(trainFvList, "./models/test/te3-et.model");
		
		// EVALUATE
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		List<PairFeatureVector> evalFvList = test.getEventTimexTlinks(trainTmlDirpath, tmlToCol, colParser,
				etCls, false, true, false);
		etCls.evaluate(evalFvList, "./models/test/te3-et.model");
		
		// PREDICT
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
		
//		List<String> etResult = new ArrayList<String>();
//		for (String tlink : etTestList) {
//			if (precisionOnly) {
//				if (!tlink.endsWith("NONE")) etResult.add(tlink);
//			} else etResult.add(tlink);
//		}
		PairEvaluator pet = new PairEvaluator(etTestList);
		pet.evaluatePerLabel(test.label);
		
	}
}
