package catena.embedding.experiments;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import evaluator.PairEvaluator;
import evaluator.TempEval3;
import model.classifier.EventDctRelationClassifier;
import model.classifier.EventEventRelationClassifier;
import model.classifier.EventTimexRelationClassifier;
import model.classifier.PairClassifier;
import model.classifier.PairClassifier.VectorClassifier;
import model.classifier.TestEventTimexRelationClassifierTempEval3;
import model.feature.CausalSignalList;
import model.feature.EventEventFeatureVector;
import model.feature.EventTimexFeatureVector;
import model.feature.PairFeatureVector;
import model.feature.TemporalSignalList;
import model.feature.FeatureEnum.FeatureName;
import model.feature.FeatureEnum.PairType;
import model.rule.EventEventRelationRule;
import model.rule.EventTimexRelationRule;
import model.rule.TimexTimexRelationRule;
import parser.TXPParser;
import parser.TXPParser.Field;
import parser.TimeMLParser;
import parser.entities.Doc;
import parser.entities.Entity;
import parser.entities.EntityEnum;
import parser.entities.Event;
import parser.entities.TemporalRelation;
import parser.entities.TimeMLDoc;
import parser.entities.Timex;
import server.RemoteServer;
import libsvm.*;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

class PrintFeatures {
	
	public static final String[] devDocs = { 
		"APW19980227.0487.tml", 
		"CNN19980223.1130.0960.tml", 
		"NYT19980212.0019.tml",  
		"PRI19980216.2000.0170.tml", 
		"ed980111.1130.0089.tml" 
	};
	
	public static final String[] testDocs = { 
		"APW19980227.0489.tml",
		"APW19980227.0494.tml",
		"APW19980308.0201.tml",
		"APW19980418.0210.tml",
		"CNN19980126.1600.1104.tml",
		"CNN19980213.2130.0155.tml",
		"NYT19980402.0453.tml",
		"PRI19980115.2000.0186.tml",
		"PRI19980306.2000.1675.tml" 
	};
	
	public PrintFeatures() {
	}
	
	public List<String> getEventEventTlinksPerFile(TXPParser txpParser, TimeMLParser tmlParser, 
			File txpFile, File tmlFile, PairClassifier eeRelCls,
			Map<String, Map<String, String>> tlinkPerFile, 
			boolean train, boolean goldCandidate) throws Exception {
		List<String> features = new ArrayList<String>();
		
		Doc docTxp = txpParser.parseDocument(txpFile.getPath());
		Doc docTml = tmlParser.parseDocument(tmlFile.getPath());
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate || train) {	//gold annotated pairs
			if (tlinkPerFile == null) candidateTlinks = docTml.getTlinks();
			else candidateTlinks = getGoldTLINKs(tmlFile, tlinkPerFile);	
		}
		else candidateTlinks = docTxp.getTlinks();	//candidate pairs
		
		for (TemporalRelation tlink : candidateTlinks) {	
			
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& docTxp.getEntities().containsKey(tlink.getSourceID())
					&& docTxp.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = docTxp.getEntities().get(tlink.getSourceID());
				Entity e2 = docTxp.getEntities().get(tlink.getTargetID());
				
				PairFeatureVector fv = new PairFeatureVector(docTxp, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					
					//Add features to feature vector
					if (eeRelCls.classifier.equals(VectorClassifier.yamcha)) {
						eefv.addToVector(FeatureName.id);
					}
					
					for (FeatureName f : eeRelCls.featureList) {
						if (eeRelCls.classifier.equals(VectorClassifier.libsvm)
								|| eeRelCls.classifier.equals(VectorClassifier.liblinear)
								|| eeRelCls.classifier.equals(VectorClassifier.logit)
								|| eeRelCls.classifier.equals(VectorClassifier.weka)) {
							eefv.addBinaryFeatureToVector(f);
						} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha)
								|| eeRelCls.classifier.equals(VectorClassifier.none)) {
							eefv.addToVector(f);
						}
					}
					
//					if (eeRelCls.classifier.equals(VectorClassifier.libsvm) || 
//							eeRelCls.classifier.equals(VectorClassifier.liblinear)) {
//						if (train) eefv.addBinaryFeatureToVector(FeatureName.labelCollapsed);
//						else eefv.addBinaryFeatureToVector(FeatureName.label);
//					} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha) ||
//							eeRelCls.classifier.equals(VectorClassifier.weka) ||
//							eeRelCls.classifier.equals(VectorClassifier.none)){
//						if (train) eefv.addToVector(FeatureName.labelCollapsed);
//						else eefv.addToVector(FeatureName.label);
//					}
					
//					eefv.addToVector(FeatureName.sentDistance);
					
					features.add(eefv.printCSVVectors());
				}
			}
		}
		return features;
	}
	
	public List<String> getEventDctTlinksPerFile(TXPParser txpParser, TimeMLParser tmlParser, 
			File txpFile, File tmlFile, PairClassifier eeRelCls,
			Map<String, Map<String, String>> tlinkPerFile, 
			boolean train, boolean goldCandidate) throws Exception {
		List<String> features = new ArrayList<String>();
		
		Doc docTxp = txpParser.parseDocument(txpFile.getPath());
		Doc docTml = tmlParser.parseDocument(tmlFile.getPath());
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate || train) {	//gold annotated pairs
			if (tlinkPerFile == null) candidateTlinks = docTml.getTlinks();
			else candidateTlinks = getGoldTLINKs(tmlFile, tlinkPerFile);	
		}
		else candidateTlinks = docTxp.getTlinks();	//candidate pairs
		
		for (TemporalRelation tlink : candidateTlinks) {	
			
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& docTxp.getEntities().containsKey(tlink.getSourceID())
					&& docTxp.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = docTxp.getEntities().get(tlink.getSourceID());
				Entity e2 = docTxp.getEntities().get(tlink.getTargetID());
				
				PairFeatureVector fv = new PairFeatureVector(docTxp, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (((Timex) etfv.getE2()).isDct()) {
					
						//Add features to feature vector
						if (eeRelCls.classifier.equals(VectorClassifier.yamcha)) {
							etfv.addToVector(FeatureName.id);
						}
						
						for (FeatureName f : eeRelCls.featureList) {
							if (eeRelCls.classifier.equals(VectorClassifier.libsvm)
									|| eeRelCls.classifier.equals(VectorClassifier.liblinear)
									|| eeRelCls.classifier.equals(VectorClassifier.logit)
									|| eeRelCls.classifier.equals(VectorClassifier.weka)) {
								etfv.addBinaryFeatureToVector(f);
							} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha)
									|| eeRelCls.classifier.equals(VectorClassifier.none)) {
								etfv.addToVector(f);
							}
						}
						
	//					if (eeRelCls.classifier.equals(VectorClassifier.libsvm) || 
	//							eeRelCls.classifier.equals(VectorClassifier.liblinear)) {
	//						if (train) eefv.addBinaryFeatureToVector(FeatureName.labelCollapsed);
	//						else eefv.addBinaryFeatureToVector(FeatureName.label);
	//					} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha) ||
	//							eeRelCls.classifier.equals(VectorClassifier.weka) ||
	//							eeRelCls.classifier.equals(VectorClassifier.none)){
	//						if (train) eefv.addToVector(FeatureName.labelCollapsed);
	//						else eefv.addToVector(FeatureName.label);
	//					}
						
//						etfv.addToVector(FeatureName.sentDistance);
						
						features.add(etfv.printCSVVectors());
					}
				}
			}
		}
		return features;
	}
	
	public List<String> getEventTimexTlinksPerFile(TXPParser txpParser, TimeMLParser tmlParser, 
			File txpFile, File tmlFile, EventTimexRelationClassifier eeRelCls,
			Map<String, Map<String, String>> tlinkPerFile, 
			boolean train, boolean goldCandidate) throws Exception {
		List<String> features = new ArrayList<String>();
		
		Doc docTxp = txpParser.parseDocument(txpFile.getPath());
		Doc docTml = tmlParser.parseDocument(tmlFile.getPath());
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate || train) {	//gold annotated pairs
			if (tlinkPerFile == null) candidateTlinks = docTml.getTlinks();
			else candidateTlinks = getGoldTLINKs(tmlFile, tlinkPerFile);	
		}
		else candidateTlinks = docTxp.getTlinks();	//candidate pairs
		
		for (TemporalRelation tlink : candidateTlinks) {	
			
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& docTxp.getEntities().containsKey(tlink.getSourceID())
					&& docTxp.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = docTxp.getEntities().get(tlink.getSourceID());
				Entity e2 = docTxp.getEntities().get(tlink.getTargetID());
				
				PairFeatureVector fv = new PairFeatureVector(docTxp, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (!((Timex) etfv.getE2()).isDct()) {
					
						//Add features to feature vector
						if (eeRelCls.classifier.equals(VectorClassifier.yamcha)) {
							etfv.addToVector(FeatureName.id);
						}
						
						for (FeatureName f : eeRelCls.featureList) {
							if (eeRelCls.classifier.equals(VectorClassifier.libsvm)
									|| eeRelCls.classifier.equals(VectorClassifier.liblinear)
									|| eeRelCls.classifier.equals(VectorClassifier.logit)
									|| eeRelCls.classifier.equals(VectorClassifier.weka)) {
								etfv.addBinaryFeatureToVector(f);
							} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha)
									|| eeRelCls.classifier.equals(VectorClassifier.none)) {
								etfv.addToVector(f);
							}
						}
						
	//					if (eeRelCls.classifier.equals(VectorClassifier.libsvm) || 
	//							eeRelCls.classifier.equals(VectorClassifier.liblinear)) {
	//						if (train) eefv.addBinaryFeatureToVector(FeatureName.labelCollapsed);
	//						else eefv.addBinaryFeatureToVector(FeatureName.label);
	//					} else if (eeRelCls.classifier.equals(VectorClassifier.yamcha) ||
	//							eeRelCls.classifier.equals(VectorClassifier.weka) ||
	//							eeRelCls.classifier.equals(VectorClassifier.none)){
	//						if (train) eefv.addToVector(FeatureName.labelCollapsed);
	//						else eefv.addToVector(FeatureName.label);
	//					}
						
//						etfv.addToVector(FeatureName.sentDistance);
						
						features.add(etfv.printCSVVectors());
					}
				}
			}
		}
		return features;
	}
	
	public List<String> getEventEventTlinks(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, PairClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		List<String> features = new ArrayList<String>();
		for (File txpFile : txpFiles) {	//assuming that there is no sub-directory
			File tmlFile = new File(dirTmlPath, txpFile.getName().replace(".txp", ""));
			features.addAll(getEventEventTlinksPerFile(txpParser, tmlParser, 
					txpFile, tmlFile, eeRelCls, null, train, goldCandidate));
		}
		return features;
	}
	
	public List<String> getEventDctTlinks(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, PairClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		List<String> features = new ArrayList<String>();
		for (File txpFile : txpFiles) {	//assuming that there is no sub-directory
			File tmlFile = new File(dirTmlPath, txpFile.getName().replace(".txp", ""));
			features.addAll(getEventDctTlinksPerFile(txpParser, tmlParser, 
					txpFile, tmlFile, eeRelCls, null, train, goldCandidate));
		}
		return features;
	}
	
	public List<String> getEventTimexTlinks(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, EventTimexRelationClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		List<String> features = new ArrayList<String>();
		for (File txpFile : txpFiles) {	//assuming that there is no sub-directory
			File tmlFile = new File(dirTmlPath, txpFile.getName().replace(".txp", ""));
			features.addAll(getEventTimexTlinksPerFile(txpParser, tmlParser, 
					txpFile, tmlFile, eeRelCls, null, train, goldCandidate));
		}
		return features;
	}
	
	public String getRelTypeTimeBankDense(String type) {
		switch(type) {
			case "s": return "SIMULTANEOUS";
			case "b": return "BEFORE";
			case "a": return "AFTER";
			case "i": return "INCLUDES";
			case "ii": return "IS_INCLUDED";
			default: return "VAGUE";
		}
	}
	
	public String getInverseRelTypeTimeBankDense(String type) {
		switch(type) {
			case "BEFORE": return "AFTER";
			case "AFTER": return "BEFORE";
			case "INCLUDES": return "IS_INCLUDED";
			case "IS_INCLUDED": return "INCLUDES";
			default: return type;
		}
	}
	
	public Map<String, Map<String, String>> getTLINKs(String tlinkPath, boolean inverse) throws Exception {
		Map<String, Map<String, String>> tlinkPerFile = new HashMap<String, Map<String, String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(tlinkPath)));
		String line;
		String filename, e1, e2, tlink;
	    while ((line = br.readLine()) != null) {
	    	String[] cols = line.split("\t");
	    	filename = cols[0] + ".tml";
	    	e1 = cols[1]; e2 = cols[2];
	    	if (e1.startsWith("t")) e1 = e1.replace("t", "tmx");
	    	if (e2.startsWith("t")) e2 = e2.replace("t", "tmx");
	    	tlink = this.getRelTypeTimeBankDense(cols[3]);
	    	
	    	if (!tlinkPerFile.containsKey(filename)) {
	    		tlinkPerFile.put(filename, new HashMap<String, String>());
	    	}
    		tlinkPerFile.get(filename).put(e1+"\t"+e2, tlink);
    		if (inverse) tlinkPerFile.get(filename).put(e2+"\t"+e1, getInverseRelTypeTimeBankDense(tlink));
	    }
	    br.close();
		
		return tlinkPerFile;
	}
	
	public List<TemporalRelation> getGoldTLINKs(File tmlFile,
			Map<String, Map<String, String>> tlinkPerFile) {
		List<TemporalRelation> tlinks = new ArrayList<TemporalRelation>();
		
		for (String pair : tlinkPerFile.get(tmlFile.getName()).keySet()) {	//for every TLINK in TimeBank-Dense file
			String sourceID = pair.split("\t")[0];
			String targetID = pair.split("\t")[1];
			String tlinkType = tlinkPerFile.get(tmlFile.getName()).get(pair);
			
			TemporalRelation tlink = new TemporalRelation(sourceID, targetID);
			tlink.setRelType(tlinkType);
			
			tlinks.add(tlink);
		}
		
		return tlinks;
	}
	
	private static boolean exists(String name, String[] names) {
		for( String nn : names )
			if( name.equals(nn) ) return true;
		return false;
	}
	
	public List<String> getEventEventTlinksTimeBankDense(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, PairClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		Map<String, Map<String, String>> tlinkPerFile = getTLINKs("./data/TimebankDense.T3.txt", false);
		
		List<String> features = new ArrayList<String>();
		for (String filename : tlinkPerFile.keySet()) {	//assuming that there is no sub-directory
			File txpFile = new File(dirTxpPath, filename + ".txp");
			File tmlFile = new File(dirTmlPath, filename);
			if (train) {
				if (!exists(tmlFile.getName(), devDocs) && !exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventEventTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			} else {
				if (exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventEventTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			}
		}
		return features;
	}
	
	public List<String> getEventDctTlinksTimeBankDense(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, PairClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		Map<String, Map<String, String>> tlinkPerFile = getTLINKs("./data/TimebankDense.T3.txt", false);
		
		List<String> features = new ArrayList<String>();
		for (String filename : tlinkPerFile.keySet()) {	//assuming that there is no sub-directory
			File txpFile = new File(dirTxpPath, filename + ".txp");
			File tmlFile = new File(dirTmlPath, filename);
			if (train) {
				if (!exists(tmlFile.getName(), devDocs) && !exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventDctTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			} else {
				if (exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventDctTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			}
		}
		return features;
	}
	
	public List<String> getEventTimexTlinksTimeBankDense(TXPParser txpParser, TimeMLParser tmlParser, 
			String dirTxpPath, String dirTmlPath, EventTimexRelationClassifier eeRelCls,
			boolean train, boolean goldCandidate) throws Exception {
		File[] txpFiles = new File(dirTxpPath).listFiles();		
		if (dirTxpPath == null) return null;
		
		Map<String, Map<String, String>> tlinkPerFile = getTLINKs("./data/TimebankDense.T3.txt", false);
		
		List<String> features = new ArrayList<String>();
		for (String filename : tlinkPerFile.keySet()) {	//assuming that there is no sub-directory
			File txpFile = new File(dirTxpPath, filename + ".txp");
			File tmlFile = new File(dirTmlPath, filename);
			if (train) {
				if (!exists(tmlFile.getName(), devDocs) && !exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventTimexTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			} else {
				if (exists(tmlFile.getName(), testDocs)) {
					features.addAll(getEventTimexTlinksPerFile(txpParser, tmlParser, 
							txpFile, tmlFile, eeRelCls, tlinkPerFile, train, goldCandidate));
				}
			}
		}
		return features;
	}
	
	public void printFeatures(List<String> trainTokenList, List<String> evalTokenList,
			String trainFile, String evalFile) throws FileNotFoundException {
		StringBuilder eeTrainTokens = new StringBuilder();
		StringBuilder eeEvalTokens = new StringBuilder();
		for (String s : trainTokenList) eeTrainTokens.append(s + "\n");
		for (String s : evalTokenList) eeEvalTokens.append(s + "\n");
		
		File eeTrainFile = new File(trainFile);
		PrintWriter eeTrain = new PrintWriter(eeTrainFile.getPath());
		eeTrain.write(eeTrainTokens.toString());
		eeTrain.close();
		
		File eeEvalFile = new File(evalFile);
		PrintWriter eeEval = new PrintWriter(eeEvalFile.getPath());
		eeEval.write(eeEvalTokens.toString());
		eeEval.close();
	}
	
	public static void main(String [] args) throws Exception {
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, Field.coref_event, Field.tlink};
		
		PrintFeatures task = new PrintFeatures();
		
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
		String trainTxpDirpath = "./data/TempEval3-train_TXP2/";
		String trainTmlDirpath = "./data/TempEval3-train_TML/";
//		String trainTmlDirpath = "./data/TempEval3-train_TML_deduced/";
		
		String evalTxpDirpath = "./data/TempEval3-eval_TXP/";
		String evalTmlDirpath = "./data/TempEval3-eval_TML/";
		
		EventEventRelationClassifier eeCls = new EventEventRelationClassifier("te3", "logit");
		EventDctRelationClassifier edCls = new EventDctRelationClassifier("te3", "logit");
		EventTimexRelationClassifier etCls = new EventTimexRelationClassifier("te3", "logit");
		
		List<String> eeTrainTokenList, eeEvalTokenList;
		List<String> edTrainTokenList, edEvalTokenList;
		List<String> etTrainTokenList, etEvalTokenList;
		
		//TempEval-3
		//event-event
		eeTrainTokenList = task.getEventEventTlinks(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, eeCls, true, true);
		eeEvalTokenList = task.getEventEventTlinks(txpParser, tmlParser, 
				evalTxpDirpath, evalTmlDirpath, eeCls, false, true);
		task.printFeatures(eeTrainTokenList, eeEvalTokenList,
				"./data/tokens/te3-ee-train-features.csv", 
				"./data/tokens/te3-ee-eval-features.csv");
		
		//event-dct
		eeTrainTokenList = task.getEventDctTlinks(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, edCls, true, true);
		eeEvalTokenList = task.getEventDctTlinks(txpParser, tmlParser, 
				evalTxpDirpath, evalTmlDirpath, edCls, false, true);
		task.printFeatures(eeTrainTokenList, eeEvalTokenList,
				"./data/tokens/te3-ed-train-features.csv", 
				"./data/tokens/te3-ed-eval-features.csv");
		
		//event-timex
		eeTrainTokenList = task.getEventTimexTlinks(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, etCls, true, true);
		eeEvalTokenList = task.getEventTimexTlinks(txpParser, tmlParser, 
				evalTxpDirpath, evalTmlDirpath, etCls, false, true);
		task.printFeatures(eeTrainTokenList, eeEvalTokenList,
				"./data/tokens/te3-et-train-features.csv", 
				"./data/tokens/te3-et-eval-features.csv");
		
		//TimeBank-Dense
		//event-event
		eeTrainTokenList = task.getEventEventTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, eeCls, true, true);
		eeEvalTokenList = task.getEventEventTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, eeCls, false, true);
		task.printFeatures(eeTrainTokenList, eeEvalTokenList,
				"./data/tokens/tbdense-ee-train-features.csv", 
				"./data/tokens/tbdense-ee-eval-features.csv");
		
		//event-dct
		edTrainTokenList = task.getEventDctTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, edCls, true, true);
		edEvalTokenList = task.getEventDctTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, edCls, false, true);
		task.printFeatures(edTrainTokenList, edEvalTokenList,
				"./data/tokens/tbdense-ed-train-features.csv", 
				"./data/tokens/tbdense-ed-eval-features.csv");
		
		//event-timex
		etTrainTokenList = task.getEventTimexTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, etCls, true, true);
		etEvalTokenList = task.getEventTimexTlinksTimeBankDense(txpParser, tmlParser, 
				trainTxpDirpath, trainTmlDirpath, etCls, false, true);
		task.printFeatures(etTrainTokenList, etEvalTokenList,
				"./data/tokens/tbdense-et-train-features.csv", 
				"./data/tokens/tbdense-et-eval-features.csv");
	}

}
