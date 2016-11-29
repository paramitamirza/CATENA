package catena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.model.classifier.EventDctTemporalClassifier;
import catena.model.classifier.EventEventTemporalClassifier;
import catena.model.classifier.EventTimexTemporalClassifier;
import catena.model.rule.EventEventTemporalRule;
import catena.model.rule.EventTimexTemporalRule;
import catena.model.rule.TimexTimexTemporalRule;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Event;
import catena.parser.entities.Links;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.TimeMLDoc;
import catena.parser.entities.Timex;

public class Temporal {
	
	private boolean goldCandidate;
	private boolean ruleSieve;
	private boolean classifierSieve;
	private boolean reasoner;
	
	private boolean ttFeature;
	private boolean etFeature;
	
	private String edModelPath;
	private String etModelPath;
	private String eeModelPath;
	
	public Temporal() {
		
	}
	
	public static void main(String[] args) throws Exception {
		
		String task = "tbdense";
		
		switch(task) {
		
			case "te3-c-rel" :
				TempEval3TaskCRelOnly();
				break;
			
			case "te3-c" :
				TempEval3TaskC();
				break;
				
			case "tbdense" :
				TimeBankDense();
				break;
		
		
		}
		
	}
	
	public static void TimeBankDense() throws Exception {
		String[] devDocs = { 
				"APW19980227.0487.tml", 
				"CNN19980223.1130.0960.tml", 
				"NYT19980212.0019.tml",  
				"PRI19980216.2000.0170.tml", 
				"ed980111.1130.0089.tml" 
			};
			
		String[] testDocs = { 
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
		
		String[] trainDocs = {
			"APW19980219.0476.tml",
			"ea980120.1830.0071.tml",
			"PRI19980205.2000.1998.tml",
			"ABC19980108.1830.0711.tml",
			"AP900815-0044.tml",
			"CNN19980227.2130.0067.tml",
			"NYT19980206.0460.tml",
			"APW19980213.1310.tml",
			"AP900816-0139.tml",
			"APW19980227.0476.tml",
			"PRI19980205.2000.1890.tml",
			"CNN19980222.1130.0084.tml",
			"APW19980227.0468.tml",
			"PRI19980213.2000.0313.tml",
			"ABC19980120.1830.0957.tml",
			"ABC19980304.1830.1636.tml",
			"APW19980213.1320.tml",
			"PRI19980121.2000.2591.tml",
			"ABC19980114.1830.0611.tml",
			"APW19980213.1380.tml",
			"ea980120.1830.0456.tml",
			"NYT19980206.0466.tml"
		};
		
		Map<String, Map<String, String>> tlinkPerFile = getTimeBankDenseTlinks("./data/TimebankDense.T3.txt");
		
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Map<String, String> relTypeMapping;
		Links tlinks;
		
		// TimeBank-Dense
		String[] tbDenseLabel = {"BEFORE", "AFTER", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "VAGUE"};
		String taskName = "tbdense";
		
		temp = new Temporal(true, 
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				false, true, false,
				false, false);
		
		// TRAIN
		temp.trainModels(taskName, "./data/TempEval3-train_TML/", trainDocs, tlinkPerFile, tbDenseLabel);
		
		// PREDICT
		relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		relTypeMapping.put("BEGINS", "BEFORE");
		relTypeMapping.put("BEGUN_BY", "AFTER");
		relTypeMapping.put("ENDS", "AFTER");
		relTypeMapping.put("ENDED_BY", "BEFORE");
		relTypeMapping.put("DURING", "SIMULTANEOUS");
		relTypeMapping.put("DURING_INV", "SIMULTANEOUS");
		tlinks = temp.extractRelations("./data/TempEval3-train_TML/", testDocs, tlinkPerFile, tbDenseLabel, relTypeMapping);
		
		// EVALUATE
		ptt = new PairEvaluator(tlinks.getTT());
		ptt.evaluatePerLabel(tbDenseLabel);
		ped = new PairEvaluator(tlinks.getED());
		ped.evaluatePerLabel(tbDenseLabel);
		pet = new PairEvaluator(tlinks.getET());
		pet.evaluatePerLabel(tbDenseLabel);
		pee = new PairEvaluator(tlinks.getEE());
		pee.evaluatePerLabel(tbDenseLabel);
	}
	
	public static void TempEval3TaskC() throws Exception {
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Links tlinks;
		
		// TempEval3 task C
		String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String taskName = "te3";
		
		temp = new Temporal(false, 
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, true);
		
		// TRAIN
		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
		relTypeMappingTrain.put("IBEFORE", "BEFORE");
		relTypeMappingTrain.put("IAFTER", "AFTER");
		temp.trainModels(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMappingTrain);
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		tlinks = temp.extractRelations("./data/TempEval3-eval_TML/", te3CLabelCollapsed, relTypeMapping);
		
		// EVALUATE
		ptt = new PairEvaluator(tlinks.getTT());
		ptt.evaluatePerLabel(te3CLabel);
		ped = new PairEvaluator(tlinks.getED());
		ped.evaluatePerLabel(te3CLabel);
		pet = new PairEvaluator(tlinks.getET());
		pet.evaluatePerLabel(te3CLabel);
		pee = new PairEvaluator(tlinks.getEE());
		pee.evaluatePerLabel(te3CLabel);
	}
	
	public static void TempEval3TaskCRelOnly() throws Exception {
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Map<String, String> relTypeMapping;
		Links tlinks;
		
		// TempEval3 task C (relation only)
		String[] te3CRelLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String taskName = "te3";
		
		temp = new Temporal(true, 
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, true);
		
		// TRAIN
		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
		relTypeMappingTrain.put("IBEFORE", "BEFORE");
		relTypeMappingTrain.put("IAFTER", "AFTER");
		temp.trainModels(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMappingTrain);
		
		// PREDICT
		relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		tlinks = temp.extractRelations("./data/TempEval3-eval_TML/", te3CLabelCollapsed, relTypeMapping);
		
		// EVALUATE
		ptt = new PairEvaluator(tlinks.getTT());
		ptt.evaluatePerLabel(te3CRelLabel);
		ped = new PairEvaluator(tlinks.getED());
		ped.evaluatePerLabel(te3CRelLabel);
		pet = new PairEvaluator(tlinks.getET());
		pet.evaluatePerLabel(te3CRelLabel);
		pee = new PairEvaluator(tlinks.getEE());
		pee.evaluatePerLabel(te3CRelLabel);
	}
	
	public Temporal(String edModelPath, String etModelPath, String eeModelPath) {
		
		this.setEDModelPath(edModelPath);
		this.setETModelPath(etModelPath);
		this.setEEModelPath(eeModelPath);
		
		this.setGoldCandidate(true);
		this.setRuleSieve(true);
		this.setClassifierSieve(true);
		this.setReasoner(true);
		
		this.setTTFeature(true);
		this.setETFeature(true);		
	}
	
	public Temporal(boolean goldCandidate,
			String edModelPath, String etModelPath, String eeModelPath) {
		
		this.setEDModelPath(edModelPath);
		this.setETModelPath(etModelPath);
		this.setEEModelPath(eeModelPath);
		
		this.setGoldCandidate(goldCandidate);
		this.setRuleSieve(true);
		this.setClassifierSieve(true);
		this.setReasoner(true);
		
		this.setTTFeature(true);
		this.setETFeature(true);
	}
	
	public Temporal(boolean goldCandidate,
			String edModelPath, String etModelPath, String eeModelPath,
			boolean ruleSieve, boolean classifierSieve, boolean reasoner) {
		
		this.setEDModelPath(edModelPath);
		this.setETModelPath(etModelPath);
		this.setEEModelPath(eeModelPath);
		
		this.setGoldCandidate(goldCandidate);
		this.setRuleSieve(ruleSieve);
		this.setClassifierSieve(classifierSieve);
		this.setReasoner(reasoner);
		
		this.setTTFeature(true);
		this.setETFeature(true);
	}
	
	public Temporal(boolean goldCandidate,
			String edModelPath, String etModelPath, String eeModelPath,
			boolean ruleSieve, boolean classifierSieve, boolean reasoner,
			boolean ttFeature, boolean etFeature) {
		
		this.setEDModelPath(edModelPath);
		this.setETModelPath(etModelPath);
		this.setEEModelPath(eeModelPath);
		
		this.setGoldCandidate(goldCandidate);
		this.setRuleSieve(ruleSieve);
		this.setClassifierSieve(classifierSieve);
		this.setReasoner(reasoner);
		
		this.setTTFeature(ttFeature);
		this.setETFeature(etFeature);
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels,
			Map<String, String> relTypeMapping) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, ParserConfig.mateToolsDirpath);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");	
		
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
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) etlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels), relTypeMapping));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), relTypeMapping, ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), relTypeMapping, etlinks));
			}
		}
		
		edCls.train(edFvList, getEDModelPath());
		etCls.train(etFvList, getETModelPath());
		eeCls.train(eeFvList, getEEModelPath());
	}
	
	public Links extractRelations(String tmlDirpath, String[] labels,
			Map<String, String> relTypeMapping) throws Exception {
		Links results = new Links();
		List<String> tt = new ArrayList<String>();
		List<String> ed = new ArrayList<String>();
		List<String> et = new ArrayList<String>();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				Links links = extractRelations(tmlFile, labels, relTypeMapping);
				tt.addAll(links.getTT());
				ed.addAll(links.getED());
				et.addAll(links.getET());
				ee.addAll(links.getEE());
			}
		}
		
		results.setTT(tt);
		results.setED(ed);
		results.setET(et);
		results.setEE(ee);
		
		return results;
	}
	
	public Links extractRelations(File tmlFile, String[] labels, 
			Map<String, String> relTypeMapping) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, ParserConfig.mateToolsDirpath);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
//		List<String> columns = tmlToCol.convert(tmlFile, false);
//		Doc doc = colParser.parseLines(columns);
		
//		tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
		Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		
		TimeMLParser.parseTimeML(tmlFile, doc);
		CandidateLinks.setCandidateTlinks(doc);
		
		TimeMLDoc tmlDoc = new TimeMLDoc(tmlFile);
		tmlDoc.removeLinks();
		String tmlString = tmlDoc.toString();
					
		// Applying temporal rules...
		if (isRuleSieve()) {
			List<String> ttRule = TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, this.isGoldCandidate());
			List<String> edRule = EventTimexTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate());
			List<String> etRule = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate());
			List<String> eeRule = EventEventTemporalRule.getEventEventTlinksPerFile(doc, this.isGoldCandidate());
			tmlString = TimeMLDoc.timeMLFileToString(doc, tmlFile,
					ttRule, edRule, etRule, eeRule);
		}
		
		//Applying temporal reasoner...
		if (isReasoner()) {			
			Reasoner r = new Reasoner();
			tmlString = r.deduceTlinksPerFile(tmlString);
		}
		
		Doc docSieved = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);		
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventDctTemporalClassifier edCls = new EventDctTemporalClassifier("te3", "liblinear");
			EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier("te3", "liblinear");
			EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier("te3", "liblinear");	
			
			//Init the feature vectors...	
			Map<String, String> ttlinks = null, etlinks = null;
			if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
			if (isETFeature()) etlinks = docSieved.getTlinkTypes();
			
			List<PairFeatureVector> edFv = EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
					false, isGoldCandidate(), Arrays.asList(labels));
			List<PairFeatureVector> etFv = EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
					false, isGoldCandidate(), Arrays.asList(labels), ttlinks);
			List<PairFeatureVector> eeFv = EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
					false, isGoldCandidate(), Arrays.asList(labels), etlinks);
			
			List<String> edClsLabels = edCls.predict(edFv, getEDModelPath(), labels);
			List<String> etClsLabels = etCls.predict(etFv, getETModelPath(), labels);
			List<String> eeClsLabels = eeCls.predict(eeFv, getEEModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < edFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < etFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
					}				
				}
				
			} else {
				for (int i = 0; i < edFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
				}
				for (int i = 0; i < etFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
				}
			}
		}
		
		// Temporal links to string
		Links links = relationToString(doc, docSieved, relTypeMapping);
		
		return links;
	}
	
	public Links relationToString(Doc gold, Doc system, Map<String, String> relTypeMapping) {
		Links links = new Links();
		Set<String> extracted = new HashSet<String>();
		
		for (TemporalRelation tlink : system.getTlinks()) {
			
			Entity e1 = system.getEntities().get(tlink.getSourceID());
			Entity e2 = system.getEntities().get(tlink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(system, e1, e2, tlink.getRelType(), null, null);
			
			for (String key : relTypeMapping.keySet()) {
				fv.setLabel(fv.getLabel().replace(key, relTypeMapping.get(key)));
			}
			
			String label = "NONE";
			if (gold.getTlinkTypes().containsKey(e1.getID() + "," + e2.getID())) {
				label = gold.getTlinkTypes().get(e1.getID() + "," + e2.getID());
			} 
			
			if (!label.equals("NONE") && !extracted.contains(e1.getID()+","+e2.getID())) {
				if (fv.getPairType().equals(PairType.timex_timex)) {
					links.getTT().add(e1.getID()
							+ "\t" + e2.getID()
							+ "\t" + label
							+ "\t" + fv.getLabel());
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
					
				} else if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					if (((Timex) etfv.getE2()).isDct()) {
						links.getED().add(e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + label
								+ "\t" + fv.getLabel());
					} else {
						links.getET().add(e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + label
								+ "\t" + fv.getLabel());
					}
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
					
				} else if (fv.getPairType().equals(PairType.event_event)) {
					links.getEE().add(e1.getID()
							+ "\t" + e2.getID()
							+ "\t" + label
							+ "\t" + fv.getLabel());
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
					
				}
			}
		}
		
		return links;
	}
		
	public void trainModels(String taskName, String tmlDirpath, String[] tmlFileNames, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, ParserConfig.mateToolsDirpath);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");
		
		if (taskName.equals("tbdense")) {	//TimeBank-Dense --- Logistic Regression!
			edCls = new EventDctTemporalClassifier(taskName, "liblinear");
			etCls = new EventTimexTemporalClassifier(taskName, "none");
			eeCls = new EventEventTemporalClassifier(taskName, "logit");
		} 
		
		List<String> tmlFileList = Arrays.asList(tmlFileNames);
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml") && tmlFileList.contains(tmlFile.getName())) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// File pre-processing...
//				List<String> columns = tmlToCol.convert(tmlFile, false);
//				Doc doc = colParser.parseLines(columns);
				
//				tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
				Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
				
				Map<String, String> tlinks = tlinkPerFile.get(tmlFile.getName());
				TimeMLParser.parseTimeML(tmlFile, doc, tlinks);
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) etlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels)));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), etlinks));
			}
		}
		
		for (PairFeatureVector pfv : etFvList) {
			System.out.println(pfv.toString());
		}
		
		edCls.train(edFvList, getEDModelPath());
		etCls.train(etFvList, getETModelPath());
		eeCls.train(eeFvList, getEEModelPath());
	}
	
	public Links extractRelations(String tmlDirpath, String[] tmlFileNames,
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels,
			Map<String, String> relTypeMapping) throws Exception {
		Links results = new Links();
		List<String> tt = new ArrayList<String>();
		List<String> ed = new ArrayList<String>();
		List<String> et = new ArrayList<String>();
		List<String> ee = new ArrayList<String>();
		
		List<String> tmlFileList = Arrays.asList(tmlFileNames);
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml") && tmlFileList.contains(tmlFile.getName())) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				Map<String, String> tlinks = tlinkPerFile.get(tmlFile.getName());
				Links links = extractRelations(tmlFile, tlinks, labels, relTypeMapping);
				tt.addAll(links.getTT());
				ed.addAll(links.getED());
				et.addAll(links.getET());
				ee.addAll(links.getEE());
			}
		}
		
		results.setTT(tt);
		results.setED(ed);
		results.setET(et);
		results.setEE(ee);
		
		return results;
	}	
	
	public Links extractRelations(File tmlFile, Map<String, String> tlinks, 
			String[] labels,
			Map<String, String> relTypeMapping) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, ParserConfig.mateToolsDirpath);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
//		List<String> columns = tmlToCol.convert(tmlFile, false);
//		Doc doc = colParser.parseLines(columns);
		
//		tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
		Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		
		TimeMLParser.parseTimeML(tmlFile, doc, tlinks);
		CandidateLinks.setCandidateTlinks(doc);
		
		TimeMLDoc tmlDoc = new TimeMLDoc(tmlFile);
		tmlDoc.removeLinks();
		String tmlString = tmlDoc.toString();
					
		// Applying temporal rules...
		if (isRuleSieve()) {
			List<String> ttRule = TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, this.isGoldCandidate());
			List<String> edRule = EventTimexTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate());
			List<String> etRule = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate());
			List<String> eeRule = EventEventTemporalRule.getEventEventTlinksPerFile(doc, this.isGoldCandidate());
			tmlString = TimeMLDoc.timeMLFileToString(doc, tmlFile,
					ttRule, edRule, etRule, eeRule);
		}
		
		//Applying temporal reasoner...
		if (isReasoner()) {			
			Reasoner r = new Reasoner();
			tmlString = r.deduceTlinksPerFile(tmlString);
		}
		
		Doc docSieved = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);		
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventDctTemporalClassifier edCls = new EventDctTemporalClassifier("te3", "liblinear");
			EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier("te3", "liblinear");
			EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier("te3", "liblinear");	
			
			//Init the feature vectors...	
			Map<String, String> ttlinks = null, etlinks = null;
			if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
			if (isETFeature()) etlinks = docSieved.getTlinkTypes();
			
			List<PairFeatureVector> edFv = EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
					false, isGoldCandidate(), Arrays.asList(labels));
			List<PairFeatureVector> etFv = EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
					false, isGoldCandidate(), Arrays.asList(labels), ttlinks);
			List<PairFeatureVector> eeFv = EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
					false, isGoldCandidate(), Arrays.asList(labels), etlinks);
			
			List<String> edClsLabels = edCls.predict(edFv, getEDModelPath(), labels);
			List<String> etClsLabels = etCls.predict(etFv, getETModelPath(), labels);
			List<String> eeClsLabels = eeCls.predict(eeFv, getEEModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < edFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < etFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
					}				
				}
				
			} else {
				for (int i = 0; i < edFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
				}
				for (int i = 0; i < etFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
				}
			}
		}
		
		// Temporal links to string
		Links links = relationToString(doc, docSieved, relTypeMapping);
		
		return links;
	}
	
	public static String getRelTypeTimeBankDense(String type) {
		switch(type) {
			case "s": return "SIMULTANEOUS";
			case "b": return "BEFORE";
			case "a": return "AFTER";
			case "i": return "INCLUDES";
			case "ii": return "IS_INCLUDED";
			default: return "VAGUE";
		}
	}
	
	public static Map<String, Map<String, String>> getTimeBankDenseTlinks(String tlinkPath) throws Exception {
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
	    	tlink = getRelTypeTimeBankDense(cols[3]);
	    	
	    	if (!tlinkPerFile.containsKey(filename)) {
	    		tlinkPerFile.put(filename, new HashMap<String, String>());
	    	}
    		tlinkPerFile.get(filename).put(e1+","+e2, tlink);
	    }
	    br.close();
		
		return tlinkPerFile;
	}

	public boolean isGoldCandidate() {
		return goldCandidate;
	}

	public void setGoldCandidate(boolean goldCandidate) {
		this.goldCandidate = goldCandidate;
	}

	public boolean isRuleSieve() {
		return ruleSieve;
	}

	public void setRuleSieve(boolean ruleSieve) {
		this.ruleSieve = ruleSieve;
	}

	public boolean isClassifierSieve() {
		return classifierSieve;
	}

	public void setClassifierSieve(boolean classifierSieve) {
		this.classifierSieve = classifierSieve;
	}

	public boolean isReasoner() {
		return reasoner;
	}

	public void setReasoner(boolean reasoner) {
		this.reasoner = reasoner;
	}
	
	public boolean isTTFeature() {
		return ttFeature;
	}

	public void setTTFeature(boolean ttFeature) {
		this.ttFeature = ttFeature;
	}
	
	public boolean isETFeature() {
		return etFeature;
	}

	public void setETFeature(boolean etFeature) {
		this.etFeature = etFeature;
	}

	public String getEDModelPath() {
		return edModelPath;
	}

	public void setEDModelPath(String edModelPath) {
		this.edModelPath = edModelPath;
	}

	public String getETModelPath() {
		return etModelPath;
	}

	public void setETModelPath(String etModelPath) {
		this.etModelPath = etModelPath;
	}

	public String getEEModelPath() {
		return eeModelPath;
	}

	public void setEEModelPath(String eeModelPath) {
		this.eeModelPath = eeModelPath;
	}
}
