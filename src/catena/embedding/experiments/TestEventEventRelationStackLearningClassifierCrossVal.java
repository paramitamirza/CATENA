package catena.embedding.experiments;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import evaluator.PairEvaluator;
import libsvm.svm_model;
import model.classifier.EventEventCausalClassifier;
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
import model.feature.Marker;
import model.rule.EventEventRelationRule;
import model.rule.EventTimexRelationRule;
import model.rule.TimexTimexRelationRule;
import parser.TXPParser;
import parser.TimeMLParser;
import parser.TXPParser.Field;
import parser.entities.Doc;
import parser.entities.Entity;
import parser.entities.EntityEnum;
import parser.entities.Event;
import parser.entities.Sentence;
import parser.entities.TemporalRelation;
import parser.entities.TimeMLDoc;
import parser.entities.Timex;
import server.RemoteServer;
import parser.entities.CausalRelation;

public class TestEventEventRelationStackLearningClassifierCrossVal {
	
	private ArrayList<String> features;
	private String name;
	private String TXPPath;
	private String CATPath;
	private String systemCATPath;
	TemporalSignalList tsignalList;
	CausalSignalList csignalList;
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private List<String> labelList = Arrays.asList(label);
	
	private String[] labelGroup1 = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private List<String> labelListGroup1 = Arrays.asList(labelGroup1);
	
	private String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	private List<String> labelListDense = Arrays.asList(labelDense);
	
	private String[] ruleTlinks = {"BEFORE", "AFTER", "SIMULTANEOUS", "INCLUDES", "IS_INCLUDED"};
	private List<String> ruleTlinkTypes = Arrays.asList(ruleTlinks);
	
	public TestEventEventRelationStackLearningClassifierCrossVal() throws IOException {
		
		//temporal & causal signal list files
		tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		csignalList = new CausalSignalList(EntityEnum.Language.EN);
		
		features = new ArrayList<String>();
	}
	
	public List<Map<Integer,Integer>> getEventEventLabels(String labelFilePath, int numFold) throws IOException {
		List<Map<Integer,Integer>> idxList = new ArrayList<Map<Integer,Integer>>();
		for(int i=0; i<numFold; i++) idxList.add(new HashMap<Integer, Integer>());
		
		List<Integer> idxListBefore = new ArrayList<Integer>();
		List<Integer> idxListAfter = new ArrayList<Integer>();
		List<Integer> idxListIdentity = new ArrayList<Integer>();
		List<Integer> idxListSimultaneous = new ArrayList<Integer>();
		List<Integer> idxListIncludes = new ArrayList<Integer>();
		List<Integer> idxListIsIncluded = new ArrayList<Integer>();
		List<Integer> idxListBegins = new ArrayList<Integer>();
		List<Integer> idxListBegunBy = new ArrayList<Integer>();
		List<Integer> idxListEnds = new ArrayList<Integer>();
		List<Integer> idxListEndedBy = new ArrayList<Integer>();
		
		System.setProperty("line.separator", "\n");
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String lbl; Integer idx = 0;
		while ((lbl = brlbl.readLine()) != null) {
			switch(lbl) {
	    		case "BEFORE": idxListBefore.add(idx); break;
	    		case "AFTER": idxListAfter.add(idx); break;
	    		case "IDENTITY": idxListIdentity.add(idx); break;
	    		case "SIMULTANEOUS": idxListSimultaneous.add(idx); break;
	    		case "INCLUDES": idxListIncludes.add(idx); break;
	    		case "IS_INCLUDED": idxListIsIncluded.add(idx); break;
	    		case "BEGINS": idxListBegins.add(idx); break;
	    		case "BEGUN_BY": idxListBegunBy.add(idx); break;
	    		case "ENDS": idxListEnds.add(idx); break;
	    		case "ENDED_BY": idxListEndedBy.add(idx); break;
	    	}
			idx ++;
		}
		brlbl.close();
		
		Collections.shuffle(idxListBefore);
		Collections.shuffle(idxListAfter);
		Collections.shuffle(idxListIdentity);
		Collections.shuffle(idxListSimultaneous);
		Collections.shuffle(idxListIncludes);
		Collections.shuffle(idxListIsIncluded);
		Collections.shuffle(idxListBegins);
		Collections.shuffle(idxListBegunBy);
		Collections.shuffle(idxListEnds);
		Collections.shuffle(idxListEndedBy);
		
		int numBeforePerFold = (int)Math.floor(idxListBefore.size()/((double)numFold));
		int numAfterPerFold = (int)Math.floor(idxListAfter.size()/((double)numFold));
		int numIdentityPerFold = (int)Math.floor(idxListIdentity.size()/((double)numFold));
		int numSimultaneousPerFold = (int)Math.floor(idxListSimultaneous.size()/((double)numFold));
		int numIncludesPerFold = (int)Math.floor(idxListIncludes.size()/((double)numFold));
		int numIsIncludedPerFold = (int)Math.floor(idxListIsIncluded.size()/((double)numFold));
		int numBeginsPerFold = (int)Math.floor(idxListBegins.size()/((double)numFold));
		int numBegunByPerFold = (int)Math.floor(idxListBegunBy.size()/((double)numFold));
		int numEndsPerFold = (int)Math.floor(idxListEnds.size()/((double)numFold));
		int numEndedByPerFold = (int)Math.floor(idxListEndedBy.size()/((double)numFold));
		
		int idxBefore = 0, idxAfter = 0, idxIdentity = 0, idxSimultaneous = 0, idxIncludes = 0,
				idxIsIncluded = 0, idxBegins = 0, idxBegunBy = 0, idxEnds = 0, idxEndedBy = 0;
		for (int i=0; i<numFold; i++) {
			for (int j=0; j<numBeforePerFold; j++) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelList.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			for (int j=0; j<numAfterPerFold; j++) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelList.indexOf("AFTER")+1);
				idxAfter ++;
			}
			for (int j=0; j<numIdentityPerFold; j++) {
				idxList.get(i).put(idxListIdentity.get(idxIdentity), labelList.indexOf("IDENTITY")+1);
				idxIdentity ++;
			}
			for (int j=0; j<numSimultaneousPerFold; j++) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelList.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			for (int j=0; j<numIncludesPerFold; j++) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelList.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			for (int j=0; j<numIsIncludedPerFold; j++) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelList.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			for (int j=0; j<numBeginsPerFold; j++) {
				idxList.get(i).put(idxListBegins.get(idxBegins), labelList.indexOf("BEGINS")+1);
				idxBegins ++;
			}
			for (int j=0; j<numBegunByPerFold; j++) {
				idxList.get(i).put(idxListBegunBy.get(idxBegunBy), labelList.indexOf("BEGUN_BY")+1);
				idxBegunBy ++;
			}
			for (int j=0; j<numEndsPerFold; j++) {
				idxList.get(i).put(idxListEnds.get(idxEnds), labelList.indexOf("ENDS")+1);
				idxEnds ++;
			}
			for (int j=0; j<numEndedByPerFold; j++) {
				idxList.get(i).put(idxListEndedBy.get(idxEndedBy), labelList.indexOf("ENDED_BY")+1);
				idxEndedBy ++;
			}
		}
		for (int i=0; i<numFold; i++) {
			if (idxBefore < idxListBefore.size()) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelList.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			if (idxAfter < idxListAfter.size()) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelList.indexOf("AFTER")+1);
				idxAfter ++;
			}
			if (idxIdentity < idxListIdentity.size()) {
				idxList.get(i).put(idxListIdentity.get(idxIdentity), labelList.indexOf("IDENTITY")+1);
				idxIdentity ++;
			}
			if (idxSimultaneous < idxListSimultaneous.size()) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelList.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			if (idxIncludes < idxListIncludes.size()) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelList.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			if (idxIsIncluded < idxListIsIncluded.size()) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelList.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			if (idxBegins < idxListBegins.size()) {
				idxList.get(i).put(idxListBegins.get(idxBegins), labelList.indexOf("BEGINS")+1);
				idxBegins ++;
			}
			if (idxBegunBy < idxListBegunBy.size()) {
				idxList.get(i).put(idxListBegunBy.get(idxBegunBy), labelList.indexOf("BEGUN_BY")+1);
				idxBegunBy ++;
			}
			if (idxEnds < idxListEnds.size()) {
				idxList.get(i).put(idxListEnds.get(idxEnds), labelList.indexOf("ENDS")+1);
				idxEnds ++;
			}
			if (idxEndedBy < idxListEndedBy.size()) {
				idxList.get(i).put(idxListEndedBy.get(idxEndedBy), labelList.indexOf("ENDED_BY")+1);
				idxEndedBy ++;
			}
		}
		
		return idxList;
	}
	
	public List<Map<Integer,Integer>> getEventEventDenseLabels(String labelFilePath, int numFold) throws IOException {
		List<Map<Integer,Integer>> idxList = new ArrayList<Map<Integer,Integer>>();
		for(int i=0; i<numFold; i++) idxList.add(new HashMap<Integer, Integer>());
		
		List<Integer> idxListBefore = new ArrayList<Integer>();
		List<Integer> idxListAfter = new ArrayList<Integer>();
		List<Integer> idxListSimultaneous = new ArrayList<Integer>();
		List<Integer> idxListIncludes = new ArrayList<Integer>();
		List<Integer> idxListIsIncluded = new ArrayList<Integer>();
		List<Integer> idxListVague = new ArrayList<Integer>();
		
		System.setProperty("line.separator", "\n");
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String lbl; Integer idx = 0;
		while ((lbl = brlbl.readLine()) != null) {
			switch(lbl) {
	    		case "BEFORE": idxListBefore.add(idx); break;
	    		case "AFTER": idxListAfter.add(idx); break;
	    		case "SIMULTANEOUS": idxListSimultaneous.add(idx); break;
	    		case "INCLUDES": idxListIncludes.add(idx); break;
	    		case "IS_INCLUDED": idxListIsIncluded.add(idx); break;
	    		case "VAGUE": idxListVague.add(idx); break;
	    	}
			idx ++;
		}
		brlbl.close();
		
		Collections.shuffle(idxListBefore);
		Collections.shuffle(idxListAfter);
		Collections.shuffle(idxListSimultaneous);
		Collections.shuffle(idxListIncludes);
		Collections.shuffle(idxListIsIncluded);
		Collections.shuffle(idxListVague);
		
		int numBeforePerFold = (int)Math.floor(idxListBefore.size()/((double)numFold));
		int numAfterPerFold = (int)Math.floor(idxListAfter.size()/((double)numFold));
		int numSimultaneousPerFold = (int)Math.floor(idxListSimultaneous.size()/((double)numFold));
		int numIncludesPerFold = (int)Math.floor(idxListIncludes.size()/((double)numFold));
		int numIsIncludedPerFold = (int)Math.floor(idxListIsIncluded.size()/((double)numFold));
		int numVaguePerFold = (int)Math.floor(idxListVague.size()/((double)numFold));
		
		int idxBefore = 0, idxAfter = 0, idxSimultaneous = 0, idxIncludes = 0, idxIsIncluded = 0, idxVague = 0;
		for (int i=0; i<numFold; i++) {
			for (int j=0; j<numBeforePerFold; j++) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelListDense.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			for (int j=0; j<numAfterPerFold; j++) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelListDense.indexOf("AFTER")+1);
				idxAfter ++;
			}
			for (int j=0; j<numSimultaneousPerFold; j++) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelListDense.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			for (int j=0; j<numIncludesPerFold; j++) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelListDense.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			for (int j=0; j<numIsIncludedPerFold; j++) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelListDense.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			for (int j=0; j<numVaguePerFold; j++) {
				idxList.get(i).put(idxListVague.get(idxVague), labelListDense.indexOf("VAGUE")+1);
				idxVague ++;
			}
		}
		for (int i=0; i<numFold; i++) {
			if (idxBefore < idxListBefore.size()) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelListDense.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			if (idxAfter < idxListAfter.size()) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelListDense.indexOf("AFTER")+1);
				idxAfter ++;
			}
			if (idxSimultaneous < idxListSimultaneous.size()) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelListDense.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			if (idxIncludes < idxListIncludes.size()) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelListDense.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			if (idxIsIncluded < idxListIsIncluded.size()) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelListDense.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			if (idxVague < idxListVague.size()) {
				idxList.get(i).put(idxListVague.get(idxVague), labelListDense.indexOf("VAGUE")+1);
				idxVague ++;
			}
		}
		
		return idxList;
	}
	
	public List<List<PairFeatureVector>> getEventEventTlinks(String embeddingFilePath, List<Map<Integer,Integer>> idxList,
			String[] arrLabel) throws Exception {
		
		List<List<PairFeatureVector>> fvList = new ArrayList<List<PairFeatureVector>>();
		for(int i=0; i<idxList.size(); i++) fvList.add(new ArrayList<PairFeatureVector>());
		
		List<String> fvListAll = new ArrayList<String>();
		System.setProperty("line.separator", "\n");
		BufferedReader bremb = new BufferedReader(new FileReader(embeddingFilePath));
		String line;
		while ((line = bremb.readLine()) != null) {
			fvListAll.add(line.trim());
		}
		bremb.close();
		
		int numCols; String lbl;
		for (int i=0; i<idxList.size(); i++) {
			for (Integer idx : idxList.get(i).keySet()) {
				numCols = fvListAll.get(idx).split(",").length;
				lbl = arrLabel[idxList.get(i).get(idx)-1];
				PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
		    			tsignalList, csignalList);
				int col=0;
				for (String s : fvListAll.get(idx).split(",")) {
		    		fv.getFeatures()[col] = Double.parseDouble(s);
		    		col++;
		    	}		    	
		    	fv.getFeatures()[col] = (double) idxList.get(i).get(idx);
		    	fvList.get(i).add(fv);
			}
		}
		
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
//		PrintStream out = new PrintStream(new FileOutputStream("stack_learning_output.txt"));
//		System.setOut(out);
		PrintStream log = new PrintStream(new FileOutputStream("stack_learning_log.txt"));
		System.setErr(log);
		
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, /*Field.coref_event,*/ Field.tlink};
		
		TestEventEventRelationStackLearningClassifierCrossVal task = new TestEventEventRelationStackLearningClassifierCrossVal();
		
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		String trainTxpDirpath = "./data/TempEval3-train_TXP2/";
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
		
		String evalTxpDirpath = "./data/TempEval3-eval_TXP/";
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";

//		String exp = "te3";
		String exp = "tbdense";

		//Init classifiers
		EventEventRelationClassifier eeCls = new EventEventRelationClassifier(exp+"-stack", "logit");
		
		int numFold = 10;
		
		String deduced = "";
//		String deduced = "-deduced";
		
		String group = "0";
//		String group = "7";
//		String group2 = "8";
		
		boolean binary = true;
		
		List<Map<Integer, Integer>> idxListList = new ArrayList<Map<Integer, Integer>>();
		List<Map<Integer, Integer>> idxListList2 = new ArrayList<Map<Integer, Integer>>();
		
		List<Map<Integer, Integer>> idxDctListList = new ArrayList<Map<Integer, Integer>>();
		List<Map<Integer, Integer>> idxEtListList = new ArrayList<Map<Integer, Integer>>();
		
		String[] arrLabel = new String[0];
		if (exp.equals("te3")) {
			arrLabel = task.label;
			idxListList = task.getEventEventLabels("./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			idxListList2 = task.getEventEventLabels("./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			
			idxDctListList = task.getEventEventLabels("./data/embedding/"+exp+"-ed-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			idxEtListList = task.getEventEventLabels("./data/embedding/"+exp+"-et-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
		}
		else if (exp.equals("tbdense")) {
			arrLabel = task.labelDense;
			idxListList = task.getEventEventDenseLabels("./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			idxListList2 = task.getEventEventDenseLabels("./data/embedding/"+exp+"-ee-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			
			idxDctListList = task.getEventEventDenseLabels("./data/embedding/"+exp+"-ed-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
			idxEtListList = task.getEventEventDenseLabels("./data/embedding/"+exp+"-et-train"+deduced+"-labels-str.gr"+group+".csv", 
					numFold);
		}
		
		List<List<PairFeatureVector>> fvListListC = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-features.csv", 
						idxListList, arrLabel);
		List<List<PairFeatureVector>> fvListList0 = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp0.csv", 
						idxListList, arrLabel);
		List<List<PairFeatureVector>> fvListList2 = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp2.csv", 
						idxListList2, arrLabel);
		List<List<PairFeatureVector>> fvListList3 = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ee-train"+deduced+"-embedding-word2vec-300.exp3.csv", 
						idxListList2, arrLabel);
		
		List<List<PairFeatureVector>> fvDctListListC = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ed-train"+deduced+"-features.csv", 
						idxDctListList, arrLabel);
		List<List<PairFeatureVector>> fvDctListList = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-ed-train"+deduced+"-embedding-word2vec-300.csv", 
						idxDctListList, arrLabel);
		
		List<List<PairFeatureVector>> fvEtListListC = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-et-train"+deduced+"-features.csv", 
						idxEtListList, arrLabel);
		List<List<PairFeatureVector>> fvEtListList = 
				task.getEventEventTlinks("./data/embedding/"+exp+"-et-train"+deduced+"-embedding-word2vec-300.csv", 
						idxEtListList, arrLabel);
		
		BufferedWriter bwProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-probs.conv.csv"));
		BufferedWriter bwProbs0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp0.csv"));
		BufferedWriter bwProbs2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp2.csv"));
		BufferedWriter bwProbs3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-probs.exp3.csv"));
		BufferedWriter bwLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-probs-labels.csv"));
		BufferedWriter bwConv = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+"-conv-features.csv"));
		
		BufferedWriter bwDctProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+"-probs.conv.csv"));
		BufferedWriter bwDctProbs = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+"-probs.csv"));
		BufferedWriter bwDctLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+"-probs-labels.csv"));
		BufferedWriter bwDctConv = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+"-conv-features.csv"));
		
		BufferedWriter bwEtProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+"-probs.conv.csv"));
		BufferedWriter bwEtProbs = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+"-probs.csv"));
		BufferedWriter bwEtLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+"-probs-labels.csv"));
		BufferedWriter bwEtConv = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+"-conv-features.csv"));
		
		BufferedWriter bwSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+".conv.txt"));
		BufferedWriter bwSig0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+".exp0.txt"));
		BufferedWriter bwSig2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+".exp2.txt"));
		BufferedWriter bwSig3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train"+deduced+".exp3.txt"));
		
		BufferedWriter bwDctSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+".conv.txt"));
		BufferedWriter bwDctSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train"+deduced+".txt"));
		
		BufferedWriter bwEtSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+".conv.txt"));
		BufferedWriter bwEtSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-et-train"+deduced+".txt"));
		
		System.out.println(fvListList0.size() + "-" + fvListList3.size());
		
		for (int fold=0; fold<numFold; fold++) {
			
			System.err.println("Fold " + (fold+1) + "...");
			
			List<PairFeatureVector> evalFvListC = fvListListC.get(fold);
			List<PairFeatureVector> evalFvList0 = fvListList0.get(fold);
			List<PairFeatureVector> evalFvList2 = fvListList2.get(fold);
			List<PairFeatureVector> evalFvList3 = fvListList3.get(fold);
			
			List<PairFeatureVector> evalDctFvListC = fvDctListListC.get(fold);
			List<PairFeatureVector> evalDctFvList = fvDctListList.get(fold);
			
			List<PairFeatureVector> evalEtFvListC = fvEtListListC.get(fold);
			List<PairFeatureVector> evalEtFvList = fvEtListList.get(fold);
			
			List<PairFeatureVector> trainFvListC = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList0 = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList2 = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList3 = new ArrayList<PairFeatureVector>();
			
			List<PairFeatureVector> trainDctFvListC = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainDctFvList = new ArrayList<PairFeatureVector>();
			
			List<PairFeatureVector> trainEtFvListC = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainEtFvList = new ArrayList<PairFeatureVector>();
			
			for (int n=0; n<numFold; n++) {
				if (n != fold) {
					trainFvListC.addAll(fvListListC.get(n));
					trainFvList0.addAll(fvListList0.get(n));
					trainFvList2.addAll(fvListList2.get(n));
					trainFvList3.addAll(fvListList3.get(n));
					
					trainDctFvListC.addAll(fvDctListListC.get(n));
					trainDctFvList.addAll(fvDctListList.get(n));
					
					trainEtFvListC.addAll(fvEtListListC.get(n));
					trainEtFvList.addAll(fvEtListList.get(n));
				}
			}
			
			if (eeCls.classifier.equals(VectorClassifier.logit)) {
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
				
				List<String> edClsTestC = eeCls.predictProbs2(evalDctFvListC, "models/" + eeCls.getName() + deduced + "-dct-conv.model", arrLabel);
				List<String> edClsTest = eeCls.predictProbs2(evalDctFvList, "models/" + eeCls.getName() + deduced + "-dct.model", arrLabel);
				
				List<String> etClsTestC = eeCls.predictProbs2(evalEtFvListC, "models/" + eeCls.getName() + deduced + "-et-conv.model", arrLabel);
				List<String> etClsTest = eeCls.predictProbs2(evalEtFvList, "models/" + eeCls.getName() + deduced + "-et.model", arrLabel);
				
				List<String> eeTestListC = new ArrayList<String>();
				List<String> eeTestList0 = new ArrayList<String>();
				List<String> eeTestList2 = new ArrayList<String>();
				List<String> eeTestList3 = new ArrayList<String>();
				
				List<String> edTestListC = new ArrayList<String>();
				List<String> edTestList = new ArrayList<String>();
				
				List<String> etTestListC = new ArrayList<String>();
				List<String> etTestList = new ArrayList<String>();
				
				for (PairFeatureVector fv : evalFvListC) {
					bwLabels.write(fv.getLabel() + "\n");
					bwConv.write(fv.toCSVString() + "\n");
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
//						bwProbsC.write(task.discretizeProbs(eeClsTestC.get(i).split("#")[1]) + "\n");
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
//						bwProbs0.write(task.discretizeProbs(eeClsTest0.get(i).split("#")[1]) + "\n");
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
//						bwProbs2.write(task.discretizeProbs(eeClsTest2.get(i).split("#")[1]) + "\n");
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
//						bwProbs3.write(task.discretizeProbs(eeClsTest3.get(i).split("#")[1]) + "\n");
						bwProbs3.write(eeClsTest3.get(i).split("#")[1] + "\n");
					}
					
					if (evalFvList3.get(i).getLabel().equals(eeClsTest3.get(i).split("#")[0])) bwSig3.write("1 1 1\n");
					else bwSig3.write("0 1 1\n");
				}
				
				for (PairFeatureVector fv : evalDctFvListC) {
					bwDctLabels.write(fv.getLabel() + "\n");
					bwDctConv.write(fv.toCSVString() + "\n");
				}
				for (int i=0; i<edClsTestC.size(); i++) {
					edTestListC.add("-" 
							+ "\t" + "-"
							+ "\t" + evalDctFvListC.get(i).getLabel()
							+ "\t" + edClsTestC.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(edClsTestC.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwDctProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwDctProbsC.write(task.discretizeProbs(edClsTestC.get(i).split("#")[1]) + "\n");
						bwDctProbsC.write(edClsTestC.get(i).split("#")[1] + "\n");
					}
					
					if (evalDctFvListC.get(i).getLabel().equals(edClsTestC.get(i).split("#")[0])) bwDctSigC.write("1 1 1\n");
					else bwDctSigC.write("0 1 1\n");
				}
				for (int i=0; i<edClsTest.size(); i++) {
					edTestList.add("-" 
							+ "\t" + "-"
							+ "\t" + evalDctFvList.get(i).getLabel()
							+ "\t" + edClsTest.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(edClsTest.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwDctProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwDctProbs.write(task.discretizeProbs(edClsTest.get(i).split("#")[1]) + "\n");
						bwDctProbs.write(edClsTest.get(i).split("#")[1] + "\n");
					}
					
					if (evalDctFvList.get(i).getLabel().equals(edClsTest.get(i).split("#")[0])) bwDctSig.write("1 1 1\n");
					else bwDctSig.write("0 1 1\n");
				}
				
				for (PairFeatureVector fv : evalEtFvListC) {
					bwEtLabels.write(fv.getLabel() + "\n");
					bwEtConv.write(fv.toCSVString() + "\n");
				}
				for (int i=0; i<etClsTestC.size(); i++) {
					etTestListC.add("-" 
							+ "\t" + "-"
							+ "\t" + evalEtFvListC.get(i).getLabel()
							+ "\t" + etClsTestC.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(etClsTestC.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwEtProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwEtProbsC.write(task.discretizeProbs(etClsTestC.get(i).split("#")[1]) + "\n");
						bwEtProbsC.write(etClsTestC.get(i).split("#")[1] + "\n");
					}
					
					if (evalEtFvListC.get(i).getLabel().equals(etClsTestC.get(i).split("#")[0])) bwEtSigC.write("1 1 1\n");
					else bwEtSigC.write("0 1 1\n");
				}
				for (int i=0; i<etClsTest.size(); i++) {
					etTestList.add("-" 
							+ "\t" + "-"
							+ "\t" + evalEtFvList.get(i).getLabel()
							+ "\t" + etClsTest.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(etClsTest.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwEtProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwEtProbs.write(task.discretizeProbs(etClsTest.get(i).split("#")[1]) + "\n");
						bwEtProbs.write(etClsTest.get(i).split("#")[1] + "\n");
					}
					
					if (evalEtFvList.get(i).getLabel().equals(etClsTest.get(i).split("#")[0])) bwEtSig.write("1 1 1\n");
					else bwEtSig.write("0 1 1\n");
				}
				
				//Evaluate
				PairEvaluator peeC = new PairEvaluator(eeTestListC);
				peeC.evaluatePerLabel(arrLabel);
				PairEvaluator pee0 = new PairEvaluator(eeTestList0);
				pee0.evaluatePerLabel(arrLabel);
				PairEvaluator pee2 = new PairEvaluator(eeTestList2);
				pee2.evaluatePerLabel(arrLabel);
				PairEvaluator pee3 = new PairEvaluator(eeTestList3);
				pee3.evaluatePerLabel(arrLabel);
				
				PairEvaluator pedC = new PairEvaluator(edTestListC);
				pedC.evaluatePerLabel(arrLabel);
				PairEvaluator ped = new PairEvaluator(edTestList);
				ped.evaluatePerLabel(arrLabel);
				
				PairEvaluator petC = new PairEvaluator(etTestListC);
				petC.evaluatePerLabel(arrLabel);
				PairEvaluator pet = new PairEvaluator(etTestList);
				pet.evaluatePerLabel(arrLabel);
				
			}
		}
		
		bwLabels.close();
		bwConv.close();
		bwProbsC.close();
		bwProbs0.close();
		bwProbs2.close();
		bwProbs3.close();
		
		bwDctLabels.close();
		bwDctConv.close();
		bwDctProbsC.close();
		bwDctProbs.close();
		
		bwEtLabels.close();
		bwEtConv.close();
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
	}
}
