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
import catena.parser.ColumnParser;
import catena.parser.TimeMLToColumns;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import evaluator.PairEvaluator;
import evaluator.TempEval3;
import model.feature.CausalSignalList;
import model.feature.EventEventFeatureVector;
import model.feature.EventTimexFeatureVector;
import model.feature.PairFeatureVector;
import model.feature.TemporalSignalList;
import model.feature.FeatureEnum.FeatureName;
import model.feature.FeatureEnum.PairType;
import model.rule.EventEventRelationRule;
import parser.TXPParser;
import parser.TimeMLParser;
import parser.TXPParser.Field;
import parser.entities.Doc;
import parser.entities.Entity;
import parser.entities.EntityEnum;
import parser.entities.Event;
import parser.entities.Sentence;
import parser.entities.TemporalRelation;
import parser.entities.Timex;
import simplifier.SentenceSimplifier;
import task.SortMapByValue;

public class TestEventEventRelationClassifierTempEval3 {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	
	public TestEventEventRelationClassifierTempEval3() {
		
	}
	
	public List<PairFeatureVector> getEventEventTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			PairClassifier etRelCls, boolean train, 
			boolean goldCandidate, boolean etFeature) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		for (File txpFile : txpFiles) {	//assuming that there is no sub-directory
			File tmlFile = new File(dirTmlPath, txpFile.getName().replace(".txp", ""));
			fvList.addAll(getEventEventTlinksPerFile(txpParser, tmlParser, 
					txpFile, tmlFile, etRelCls, train));
		}
		return fvList;
	}

	public static void main(String [] args) throws Exception {
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, /*Field.coref_event,*/ Field.tlink};
		
		TestEventEventRelationClassifierTempEval3 test = new TestEventEventRelationClassifierTempEval3();
		
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		String trainTxpDirpath = "./data/TempEval3-train_TXP2/";
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		
		String evalTxpDirpath = "./data/TempEval3-eval_TXP/";
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		
		EventEventRelationClassifier eeCls = new EventEventRelationClassifier("te3", "liblinear");
		
//		List<PairFeatureVector> trainFvList = test.getEventEventTlinks(txpParser, tmlParser, 
//				trainTxpDirpath, trainTmlDirpath, eeCls, true);
		List<PairFeatureVector> evalFvList = test.getEventEventTlinks(txpParser, tmlParser, 
				evalTxpDirpath, evalTmlDirpath, eeCls, false);
		
//		eeCls.train(trainFvList, "ee-model");   
		eeCls.evaluate(evalFvList, "ee-model");
		
		File[] txpFiles = new File(evalTxpDirpath).listFiles();
		//For each file in the evaluation dataset
		for (File txpFile : txpFiles) {
			if (txpFile.isFile()) {	
				File tmlFile = new File(evalTmlDirpath, txpFile.getName().replace(".txp", ""));
//				System.err.println(tmlFile.getName());
				Doc docTxp = txpParser.parseDocument(txpFile.getPath());
				
				//Predict labels
				List<PairFeatureVector> eeFvList = test.getEventEventTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeCls, false);
				List<String> eeClsTest = eeCls.predict(eeFvList, "ee-model");
				
				for (int i=0; i<eeFvList.size(); i++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFvList.get(i));
					EventEventRelationRule eeRule = new EventEventRelationRule((Event) eefv.getE1(), (Event) eefv.getE2(), 
							docTxp, eefv.getMateDependencyPath());
					
					//Prefer labels from rules than classifier 
					String label = eeClsTest.get(i);
//					if (!eeRule.getRelType().equals("O")) label = eeRule.getRelType();
					
					System.out.println(txpFile.getName()
							+ "\t" + eefv.getE1().getID() 
							+ "\t" + eefv.getE2().getID() 
							+ "\t" + eefv.getLabel()
							+ "\t" + label);
				}
			}
		}
	}
}
