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
import catena.evaluator.PairEvaluator;
import catena.evaluator.TempEval3;
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

public class TestEventEventRelationStackLearningClassifier {

	TemporalSignalList tsignalList;
	CausalSignalList csignalList;

	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelGroup1 = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	
	public TestEventEventRelationStackLearningClassifier() {
		
	}
	
	public List<PairFeatureVector> getEventEventTlinks(String embeddingFilePath, String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		BufferedReader bremb = new BufferedReader(new FileReader(embeddingFilePath));
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((line = bremb.readLine()) != null) {
			lbl = brlbl.readLine();
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
	    	
	    	fvList.add(fv);
		}
		
		bremb.close();
		brlbl.close();
		
		return fvList;
	}
	
	public double discretizeValue(String val) {
//		double norm = 0.0;
//		if (Double.parseDouble(val) < 0.5) {
//			norm = Double.parseDouble(val)/0.5;
//		} else {
//			norm = 1.0;
//		}
//		return norm;
		double norm = Double.parseDouble(val);
		if (norm < 0.1) {
			return 0.0;
		} else if (norm < 0.2) {
			return 0.1;
		} else if (norm < 0.3) {
			return 0.2;
		} else if (norm < 0.4) {
			return 0.3;
		} else if (norm < 0.5) {
			return 0.4;
		} else if (norm < 0.6) {
			return 0.5;
		} else if (norm < 0.7) {
			return 0.6;
		} else if (norm < 0.8) {
			return 0.7;
		} else {
			return 1.0;
		}
	}
	
	public String discretizeProbs(String probs) {
		String res = "";
		for (String p : probs.split(",")) {
			res += discretizeValue(p) + ",";
		}
		return res.substring(0, res.length()-1);
	}

	public static void main(String [] args) throws Exception {
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, /*Field.coref_event,*/ Field.tlink};
		
		TestEventEventRelationStackLearningClassifier test = new TestEventEventRelationStackLearningClassifier();
		
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		String trainTxpDirpath = "./data/TempEval3-train_TXP2/";
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		
		String evalTxpDirpath = "./data/TempEval3-eval_TXP/";
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";

		String exp = "te3";
//		String exp = "tbdense";

		EventEventRelationClassifier eeCls = new EventEventRelationClassifier(exp+"-stack", "logit");

		String deduced = "";
//		String deduced = "-deduced";
		
		String group = "0";
//		String group = "7";
		
		boolean binary = true;

		String[] arrLabel = new String[0];
		if (exp.equals("te3")) {
			arrLabel = test.label;
		}
		else if (exp.equals("tbdense")) {
			arrLabel = test.labelDense;
		}
		
		List<PairFeatureVector> trainFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-features.csv", 
				"./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> trainFvList0 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp0.csv", 
				"./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> trainFvList2 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp2.csv", 
				"./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> trainFvList3 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp3.csv", 
				"./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		
		List<PairFeatureVector> trainDctFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ed-train"+deduced+"-features.csv", 
				"./data/embedding/"+exp+"-ed-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> trainDctFvList = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ed-train"+deduced+"-embedding-word2vec-300.csv", 
				"./data/embedding/"+exp+"-ed-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		
		List<PairFeatureVector> trainEtFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-et-train"+deduced+"-features.csv", 
				"./data/embedding/"+exp+"-et-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> trainEtFvList = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-et-train"+deduced+"-embedding-word2vec-300.csv", 
				"./data/embedding/"+exp+"-et-train"+deduced+"-labels-str.gr"+group+".csv",
				arrLabel);
		
		List<PairFeatureVector> evalFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-eval-features.csv", 
				"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> evalFvList0 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp0.csv", 
				"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> evalFvList2 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp2.csv", 
				"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> evalFvList3 = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp3.csv", 
				"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
				arrLabel);
		
		List<PairFeatureVector> evalDctFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ed-eval-features.csv", 
				"./data/embedding/"+exp+"-ed-eval-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> evalDctFvList = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-ed-eval-embedding-word2vec-300.csv", 
				"./data/embedding/"+exp+"-ed-eval-labels-str.gr"+group+".csv",
				arrLabel);
		
		List<PairFeatureVector> evalEtFvListC = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-et-eval-features.csv", 
				"./data/embedding/"+exp+"-et-eval-labels-str.gr"+group+".csv",
				arrLabel);
		List<PairFeatureVector> evalEtFvList = 
				test.getEventEventTlinks("./data/embedding/"+exp+"-et-eval-embedding-word2vec-300.csv", 
				"./data/embedding/"+exp+"-et-eval-labels-str.gr"+group+".csv",
				arrLabel);
		
		eeCls.train2(trainFvListC, "models/" + eeCls.getName() + deduced + "-conv.model");
		eeCls.train2(trainFvList0, "models/" + eeCls.getName() + deduced + "-exp0.model");
		eeCls.train2(trainFvList2, "models/" + eeCls.getName() + deduced + "-exp2.model");
		eeCls.train2(trainFvList3, "models/" + eeCls.getName() + deduced + "-exp3.model");
		
		eeCls.train2(trainDctFvListC, "models/" + eeCls.getName() + deduced + "-dct-conv.model");
		eeCls.train2(trainDctFvList, "models/" + eeCls.getName() + deduced + "-dct.model");
		
		eeCls.train2(trainEtFvListC, "models/" + eeCls.getName() + deduced + "-et-conv.model");
		eeCls.train2(trainEtFvList, "models/" + eeCls.getName() + deduced + "-et.model");
		
		List<String> eeClsTestC = eeCls.predictProbs2(evalFvListC, "models/" + eeCls.getName() + deduced + "-conv.model", arrLabel);
		List<String> eeClsTest0 = eeCls.predictProbs2(evalFvList0, "models/" + eeCls.getName() + deduced + "-exp0.model", arrLabel);
		List<String> eeClsTest2 = eeCls.predictProbs2(evalFvList2, "models/" + eeCls.getName() + deduced + "-exp2.model", arrLabel);
		List<String> eeClsTest3 = eeCls.predictProbs2(evalFvList3, "models/" + eeCls.getName() + deduced + "-exp3.model", arrLabel);
		
		List<String> eeClsDctTestC = eeCls.predictProbs2(evalDctFvListC, "models/" + eeCls.getName() + deduced + "-dct-conv.model", arrLabel);
		List<String> eeClsDctTest = eeCls.predictProbs2(evalDctFvList, "models/" + eeCls.getName() + deduced + "-dct.model", arrLabel);
		
		List<String> eeClsEtTestC = eeCls.predictProbs2(evalEtFvListC, "models/" + eeCls.getName() + deduced + "-et-conv.model", arrLabel);
		List<String> eeClsEtTest = eeCls.predictProbs2(evalEtFvList, "models/" + eeCls.getName() + deduced + "-et.model", arrLabel);
		
		List<String> eeTestListC = new ArrayList<String>();
		List<String> eeTestList0 = new ArrayList<String>();
		List<String> eeTestList2 = new ArrayList<String>();
		List<String> eeTestList3 = new ArrayList<String>();
		
		List<String> eeDctTestListC = new ArrayList<String>();
		List<String> eeDctTestList = new ArrayList<String>();
		
		List<String> eeEtTestListC = new ArrayList<String>();
		List<String> eeEtTestList = new ArrayList<String>();
		
		BufferedWriter bwProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval-probs.conv.csv"));
		BufferedWriter bwProbs0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval-probs.exp0.csv"));
		BufferedWriter bwProbs2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval-probs.exp2.csv"));
		BufferedWriter bwProbs3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval-probs.exp3.csv"));
		BufferedWriter bwLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval-probs-labels.csv"));
		
		BufferedWriter bwDctProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-eval-probs.conv.csv"));
		BufferedWriter bwDctProbs = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-eval-probs.csv"));
		BufferedWriter bwDctLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-eval-probs-labels.csv"));
		
		BufferedWriter bwEtProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-eval-probs.conv.csv"));
		BufferedWriter bwEtProbs = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-eval-probs.csv"));
		BufferedWriter bwEtLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-eval-probs-labels.csv"));
		
		BufferedWriter bwSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval.conv.txt"));
		BufferedWriter bwSig0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval.exp0.txt"));
		BufferedWriter bwSig2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval.exp2.txt"));
		BufferedWriter bwSig3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-eval.exp3.txt"));
		
		BufferedWriter bwDctSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-eval.conv.txt"));
		BufferedWriter bwDctSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-eval.txt"));
		
		BufferedWriter bwEtSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-eval.conv.txt"));
		BufferedWriter bwEtSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-eval.txt"));
		
		for (PairFeatureVector fv : evalFvListC) {
			bwLabels.write(fv.getLabel() + "\n");
		}
		for (int i=0; i<eeClsTestC.size(); i++) {
			eeTestListC.add("-" 
					+ "\t" + "-"
					+ "\t" + evalFvListC.get(i).getLabel()
					+ "\t" + eeClsTestC.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsTestC.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwProbsC.write(task.discretizeProbs(eeClsTestC.get(i).split("#")[1]) + "\n");
				bwProbsC.write(eeClsTestC.get(i).split("#")[1] + "\n");
			}
			
			if (evalFvListC.get(i).getLabel().equals(eeClsTestC.get(i).split("#")[0])) bwSigC.write("1 1 1\n");
			else bwSigC.write("0 1 1\n");
		}
		for (int i=0; i<eeClsTest0.size(); i++) {
			eeTestList0.add("-" 
					+ "\t" + "-"
					+ "\t" + evalFvList0.get(i).getLabel()
					+ "\t" + eeClsTest0.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsTest0.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwProbs0.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwProbs0.write(task.discretizeProbs(eeClsTest0.get(i).split("#")[1]) + "\n");
				bwProbs0.write(eeClsTest0.get(i).split("#")[1] + "\n");
			}
			
			if (evalFvList0.get(i).getLabel().equals(eeClsTest0.get(i).split("#")[0])) bwSig0.write("1 1 1\n");
			else bwSig0.write("0 1 1\n");
		}
		for (int i=0; i<eeClsTest2.size(); i++) {
			eeTestList2.add("-" 
					+ "\t" + "-"
					+ "\t" + evalFvList2.get(i).getLabel()
					+ "\t" + eeClsTest2.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsTest2.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwProbs2.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwProbs2.write(task.discretizeProbs(eeClsTest2.get(i).split("#")[1]) + "\n");
				bwProbs2.write(eeClsTest2.get(i).split("#")[1] + "\n");
			}
			
			if (evalFvList2.get(i).getLabel().equals(eeClsTest2.get(i).split("#")[0])) bwSig2.write("1 1 1\n");
			else bwSig2.write("0 1 1\n");
		}
		for (int i=0; i<eeClsTest3.size(); i++) {
			eeTestList3.add("-" 
					+ "\t" + "-"
					+ "\t" + evalFvList3.get(i).getLabel()
					+ "\t" + eeClsTest3.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsTest3.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwProbs3.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwProbs3.write(task.discretizeProbs(eeClsTest3.get(i).split("#")[1]) + "\n");
				bwProbs3.write(eeClsTest3.get(i).split("#")[1] + "\n");
			}
			
			if (evalFvList3.get(i).getLabel().equals(eeClsTest3.get(i).split("#")[0])) bwSig3.write("1 1 1\n");
			else bwSig3.write("0 1 1\n");
		}
		
		for (PairFeatureVector fv : evalDctFvList) {
			bwDctLabels.write(fv.getLabel() + "\n");
		}
		for (int i=0; i<eeClsDctTestC.size(); i++) {
			eeDctTestListC.add("-" 
					+ "\t" + "-"
					+ "\t" + evalDctFvListC.get(i).getLabel()
					+ "\t" + eeClsDctTestC.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsDctTestC.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwDctProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwDctProbsC.write(task.discretizeProbs(eeClsDctTestC.get(i).split("#")[1]) + "\n");
				bwDctProbsC.write(eeClsDctTestC.get(i).split("#")[1] + "\n");
			}
			
			if (evalDctFvListC.get(i).getLabel().equals(eeClsDctTestC.get(i).split("#")[0])) bwDctSigC.write("1 1 1\n");
			else bwDctSigC.write("0 1 1\n");
		}
		
		for (int i=0; i<eeClsDctTest.size(); i++) {
			eeDctTestList.add("-" 
					+ "\t" + "-"
					+ "\t" + evalDctFvList.get(i).getLabel()
					+ "\t" + eeClsDctTest.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsDctTest.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwDctProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwDctProbs.write(task.discretizeProbs(eeClsDctTest.get(i).split("#")[1]) + "\n");
				bwDctProbs.write(eeClsDctTest.get(i).split("#")[1] + "\n");
			}
			
			if (evalDctFvList.get(i).getLabel().equals(eeClsDctTest.get(i).split("#")[0])) bwDctSig.write("1 1 1\n");
			else bwDctSig.write("0 1 1\n");
		}
		
		for (PairFeatureVector fv : evalEtFvList) {
			bwEtLabels.write(fv.getLabel() + "\n");
		}
		for (int i=0; i<eeClsEtTestC.size(); i++) {
			eeEtTestListC.add("-" 
					+ "\t" + "-"
					+ "\t" + evalEtFvListC.get(i).getLabel()
					+ "\t" + eeClsEtTestC.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsEtTestC.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwEtProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwEtProbsC.write(task.discretizeProbs(eeClsEtTestC.get(i).split("#")[1]) + "\n");
				bwEtProbsC.write(eeClsEtTestC.get(i).split("#")[1] + "\n");
			}
			
			if (evalEtFvListC.get(i).getLabel().equals(eeClsEtTestC.get(i).split("#")[0])) bwEtSigC.write("1 1 1\n");
			else bwEtSigC.write("0 1 1\n");
		}
		for (int i=0; i<eeClsEtTest.size(); i++) {
			eeEtTestList.add("-" 
					+ "\t" + "-"
					+ "\t" + evalEtFvList.get(i).getLabel()
					+ "\t" + eeClsEtTest.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : arrLabel) {
					if (l.equals(eeClsEtTest.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwEtProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwEtProbs.write(task.discretizeProbs(eeClsEtTest.get(i).split("#")[1]) + "\n");
				bwEtProbs.write(eeClsEtTest.get(i).split("#")[1] + "\n");
			}
			
			if (evalEtFvList.get(i).getLabel().equals(eeClsEtTest.get(i).split("#")[0])) bwEtSig.write("1 1 1\n");
			else bwEtSig.write("0 1 1\n");
		}
		
		bwLabels.close();
		bwProbsC.close();
		bwProbs0.close();
		bwProbs2.close();
		bwProbs3.close();
		
		bwDctLabels.close();
		bwDctProbsC.close();
		bwDctProbs.close();
		
		bwEtLabels.close();
		bwEtProbsC.close();
		bwEtProbs.close();
		
		bwSigC.close();
		bwSig0.close();
		bwSig2.close();
		bwSig3.close();
		
		bwDctSigC.close();
		bwDctSig.close();
		
		bwEtSigC.close();
		bwEtSig.close();
		
		//Evaluate
		PairEvaluator peeC = new PairEvaluator(eeTestListC);
		peeC.evaluatePerLabel(arrLabel);
		PairEvaluator pee0 = new PairEvaluator(eeTestList0);
		pee0.evaluatePerLabel(arrLabel);
		PairEvaluator pee2 = new PairEvaluator(eeTestList2);
		pee2.evaluatePerLabel(arrLabel);
		PairEvaluator pee3 = new PairEvaluator(eeTestList3);
		pee3.evaluatePerLabel(arrLabel);
		
		PairEvaluator pedC = new PairEvaluator(eeDctTestListC);
		pedC.evaluatePerLabel(arrLabel);
		PairEvaluator ped = new PairEvaluator(eeDctTestList);
		ped.evaluatePerLabel(arrLabel);
		
		PairEvaluator petC = new PairEvaluator(eeEtTestListC);
		petC.evaluatePerLabel(arrLabel);
		PairEvaluator pet = new PairEvaluator(eeEtTestList);
		pet.evaluatePerLabel(arrLabel);
	}
}
