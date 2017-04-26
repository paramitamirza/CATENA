package catena.embedding.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import evaluator.PairEvaluator;
import evaluator.TempEval3;
import model.classifier.EventDctRelationClassifier;
import model.classifier.EventEventRelationClassifier;
import model.classifier.EventTimexRelationClassifier;
import model.classifier.PairClassifier;
import model.classifier.PairClassifier.VectorClassifier;
import model.feature.CausalSignalList;
import model.feature.EventEventFeatureVector;
import model.feature.EventTimexFeatureVector;
import model.feature.PairFeatureVector;
import model.feature.TemporalSignalList;
import model.feature.FeatureEnum.FeatureName;
import model.feature.FeatureEnum.PairType;
import model.rule.EventEventRelationRule;
import model.rule.EventTimexRelationRule;
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

public class TestEventEventRelationStackLearningSuperClassifier {

	TemporalSignalList tsignalList;
	CausalSignalList csignalList;

	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelGroup1 = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};

	public TestEventEventRelationStackLearningSuperClassifier() {
		
	}
	
	public List<PairFeatureVector> getEventEventTlinks(String[] fileList, String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		List<BufferedReader> brList = new ArrayList<BufferedReader>();
		for (String file : fileList) {
			brList.add(new BufferedReader(new FileReader(file)));
		}
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((lbl = brlbl.readLine()) != null) {
			line = "";
			for (BufferedReader br : brList) {
				line += br.readLine().trim() + ",";
			}
			line = line.substring(0, line.length()-1);
			numCols = line.split(",").length;
	    	
	    	PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
	    			tsignalList, csignalList);
	    	int i=0;
	    	for (String s : line.split(",")) {
//	    		fv.getVectors().add(s);
	    		fv.getFeatures()[i] = Double.parseDouble(s);
	    		i++;
	    	}
//	    	fv.getVectors().add(String.valueOf(labelList.indexOf(lbl)+1));
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;
	    	
//	    	if (!lbl.equals("VAGUE"))
	    	fvList.add(fv);
		}
		
		brlbl.close();
		for (BufferedReader br : brList) {
			br.close();
		}
		
		return fvList;
	}
	
	private Integer getIdxMaxProb(double[] probs) {
		int maxIndex = 0;
		double max = probs[0];
		for (int i = 1; i < probs.length; i++) {
		    if (probs[i] > max) {
		        max = probs[i];
		        maxIndex = i;
		    }
		}
		return maxIndex;
	}
	
	public List<String> getMaxProbTlinks(String[] fileList, String labelFilePath) throws Exception {
		List<String> maxProbLabels = new ArrayList<String>();
		
		System.setProperty("line.separator", "\n");
		List<BufferedReader> brList = new ArrayList<BufferedReader>();
		for (String file : fileList) {
			brList.add(new BufferedReader(new FileReader(file)));
		}
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String lbl;
		String[] line;
		while ((lbl = brlbl.readLine()) != null) {
			double[] probs = new double[labelGroup1.length];
			for (BufferedReader br : brList) {
				line = br.readLine().trim().split(",");
				for (int i=0; i<labelGroup1.length; i++) {
					probs[i] += Double.parseDouble(line[i]);
				}
			}
			for (int i=0; i<labelGroup1.length; i++) {
				probs[i] = probs[i]/fileList.length;
			}
			maxProbLabels.add(labelGroup1[getIdxMaxProb(probs)]);
		}
		
		brlbl.close();
		for (BufferedReader br : brList) {
			br.close();
		}
		
		return maxProbLabels;
	}

	public static void main(String [] args) throws Exception {
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, /*Field.coref_event,*/ Field.tlink};
		
		TestEventEventRelationStackLearningSuperClassifier test = new TestEventEventRelationStackLearningSuperClassifier();
		
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		String trainTxpDirpath = "./data/TempEval3-train_TXP2/";
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		
		String evalTxpDirpath = "./data/TempEval3-eval_TXP/";
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";

//		String exp = "te3";
		String exp = "tbdense";
		String pair = "ed";
		boolean stack = false;
		
		EventEventRelationClassifier cls = new EventEventRelationClassifier(exp+"-"+pair+"-super", "logit");
		
		String deduced = "";
//		String deduced = "-deduced";

		String group = "0";
		String group2 = "0";
//		String group = "7";
//		String group2 = "8";

		String[] arrLabel = new String[0];
		if (exp.equals("te3")) {
			arrLabel = test.label;
		}
		else if (exp.equals("tbdense")) {
			arrLabel = test.labelDense;
		}
		
		List<PairFeatureVector> trainFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> evalFvList = new ArrayList<PairFeatureVector>();
		
		if (stack) {
			switch(pair) {
				case "ee":
					String[] trainFileList = {
							"./data/embedding/"+exp+"-ee-train"+deduced+"-conv-features.csv",
//							"./data/embedding/"+exp+"-ee-train"+deduced+"-probs.conv.csv", 
			                "./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp0.csv", 
//			                "./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp2.csv", 
//			                "./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp3.csv"
							};
					trainFvList = 
							test.getEventEventTlinks(trainFileList, 
							"./data/embedding/"+exp+"-ee-train"+deduced+"-probs-labels.csv",
							arrLabel);
					
					String[] evalFileList = {
							"./data/embedding/"+exp+"-ee-eval-features.csv", 
//							"./data/embedding/"+exp+"-ee-eval-probs.conv.csv", 
							"./data/embedding/"+exp+"-ee-eval-probs.exp0.csv", 
//			                "./data/embedding/"+exp+"-ee-eval-probs.exp2.csv", 
//			                "./data/embedding/"+exp+"-ee-eval-probs.exp3.csv"
							};
					evalFvList = 
							test.getEventEventTlinks(evalFileList, 
							"./data/embedding/"+exp+"-ee-eval-probs-labels.csv",
							arrLabel);
					break;
				
				case "ed":
					String[] edTrainFileList = {
							"./data/embedding/"+exp+"-ed-train"+deduced+"-conv-features.csv",
			                "./data/embedding/"+exp+"-ed-train"+deduced+"-probs.csv"
							};
					trainFvList = 
							test.getEventEventTlinks(edTrainFileList, 
							"./data/embedding/"+exp+"-ed-train"+deduced+"-probs-labels.csv",
							arrLabel);
					
					String[] edEvalFileList = {
							"./data/embedding/"+exp+"-ed-eval-features.csv",
							"./data/embedding/"+exp+"-ed-eval-probs.csv"
							};
					evalFvList = 
							test.getEventEventTlinks(edEvalFileList, 
							"./data/embedding/"+exp+"-ed-eval-probs-labels.csv",
							arrLabel);
					break;
					
				case "et":
					String[] etTrainFileList = {
							"./data/embedding/"+exp+"-et-train"+deduced+"-conv-features.csv",
			                "./data/embedding/"+exp+"-et-train"+deduced+"-probs.csv"
							};
					trainFvList = 
							test.getEventEventTlinks(etTrainFileList, 
							"./data/embedding/"+exp+"-et-train"+deduced+"-probs-labels.csv",
							arrLabel);
					
					String[] etEvalFileList = {
							"./data/embedding/"+exp+"-et-eval-features.csv",
							"./data/embedding/"+exp+"-et-eval-probs.csv"
							};
					evalFvList = 
							test.getEventEventTlinks(etEvalFileList, 
							"./data/embedding/"+exp+"-et-eval-probs-labels.csv",
							arrLabel);
					break;
			}
			
		} else {
			switch(pair) {
				case "ee":
					String[] trainFileList = {
							"./data/embedding/"+exp+"-ee-train"+deduced+"-features.csv",
			                "./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp0.csv",
//			              "./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp2.csv",
//			              "./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp3.csv",
							};
					trainFvList = 
							test.getEventEventTlinks(trainFileList, 
							"./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] evalFileList = {
							"./data/embedding/"+exp+"-ee-eval-features.csv",
			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp0.csv",
//			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp2.csv",
//			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp3.csv",
							};
					evalFvList = 
							test.getEventEventTlinks(evalFileList, 
									"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
					
				case "ed":
					String[] edTrainFileList = {
							"./data/embedding/"+exp+"-ed-train"+deduced+"-features.csv",
			                "./data/embedding/"+exp+"-ed-train"+deduced+"-embedding-word2vec-300.csv"
							};
					trainFvList = 
							test.getEventEventTlinks(edTrainFileList, 
							"./data/embedding/"+exp+"-ed-train"+deduced+"-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] edEvalFileList = {
							"./data/embedding/"+exp+"-ed-eval-features.csv",
			                "./data/embedding/"+exp+"-ed-eval-embedding-word2vec-300.csv"
							};
					evalFvList = 
							test.getEventEventTlinks(edEvalFileList, 
									"./data/embedding/"+exp+"-ed-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
					
				case "et":
					String[] etTrainFileList = {
							"./data/embedding/"+exp+"-et-train"+deduced+"-features.csv",
			                "./data/embedding/"+exp+"-et-train"+deduced+"-embedding-word2vec-300.csv"
							};
					trainFvList = 
							test.getEventEventTlinks(etTrainFileList, 
							"./data/embedding/"+exp+"-et-train"+deduced+"-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] etEvalFileList = {
							"./data/embedding/"+exp+"-et-eval-features.csv",
			                "./data/embedding/"+exp+"-et-eval-embedding-word2vec-300.csv"
							};
					evalFvList = 
							test.getEventEventTlinks(etEvalFileList, 
									"./data/embedding/"+exp+"-et-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
			}
		}
		
		cls.train2(trainFvList, "models/" + cls.getName() + deduced + ".model");
		
		List<String> eeClsTest = cls.predict2(evalFvList, "models/" + cls.getName() + deduced + ".model", arrLabel);
//		List<String> eeClsTest = 
//				test.getMaxProbTlinks(evalFileList, 
//						"./data/embedding/"+exp+"-ee-eval-probs-labels.csv");
		
		//Evaluate
		
		List<Integer> sentDistance = new ArrayList<Integer>();
		BufferedReader brSent = new BufferedReader(new FileReader("./data/embedding/"+exp+"-"+pair+"-eval-features-sent.csv"));
		String distance;
		while ((distance = brSent.readLine()) != null) {
			sentDistance.add(Integer.parseInt(distance));
		}
		brSent.close();
		
		String combine = "concat";
		if (stack) combine = "stack";	
		
		BufferedWriter bwSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-"+combine+".txt"));
		BufferedWriter bwSigSame = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-same-"+combine+".txt"));
		BufferedWriter bwSigDiff = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-diff-"+combine+".txt"));
		
		for (int i=0; i<eeClsTest.size(); i++) {
			if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSig.write("1 1 1\n");
			else bwSig.write("0 1 1\n");
		}
		bwSig.close();
		
		//Same sentence
		List<String> eeTestListSame = new ArrayList<String>();
		for (int i=0; i<eeClsTest.size(); i++) {
			if (sentDistance.get(i) == 0) {
				eeTestListSame.add("-" 
						+ "\t" + "-"
						+ "\t" + evalFvList.get(i).getLabel()
						+ "\t" + eeClsTest.get(i));
				if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSigSame.write("1 1 1\n");
				else bwSigSame.write("0 1 1\n");
			}
		}
		PairEvaluator pees = new PairEvaluator(eeTestListSame);
		pees.evaluatePerLabel(arrLabel);
		bwSigSame.close();
		
		//Different sentence
		List<String> eeTestListDiff = new ArrayList<String>();
		for (int i=0; i<eeClsTest.size(); i++) {
			if (sentDistance.get(i) != 0) {
				eeTestListDiff.add("-" 
						+ "\t" + "-"
						+ "\t" + evalFvList.get(i).getLabel()
						+ "\t" + eeClsTest.get(i));
				if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSigDiff.write("1 1 1\n");
				else bwSigDiff.write("0 1 1\n");
			}
		}
		PairEvaluator peed = new PairEvaluator(eeTestListDiff);
		peed.evaluatePerLabel(arrLabel);
		bwSigDiff.close();
	}
}
