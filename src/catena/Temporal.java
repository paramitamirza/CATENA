package catena;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import catena.parser.entities.EntityEnum;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.parser.entities.Event;
import catena.parser.entities.Links;
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
	
	public void trainModels(String tmlDirpath) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier("te3", "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier("te3", "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier("te3", "liblinear");	
		
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
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, isTTFeature()));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, isETFeature()));
			}
		}
		
		edCls.train(edFvList, getEDModelPath());
		etCls.train(etFvList, getETModelPath());
		eeCls.train(eeFvList, getEEModelPath());
	}
	
	public Links extractRelations(String tmlDirpath, Map<String, String> relTypeMapping) throws Exception {
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
				Links links = extractRelation(tmlFile, relTypeMapping);
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
	
	public Links extractRelation(File tmlFile, Map<String, String> relTypeMapping) throws Exception {
		Links links = new Links();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
//		List<String> columns = tmlToCol.convert(tmlFile, false);
//		Doc doc = colParser.parseLines(columns);
		
//		tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
		Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		
		TimeMLParser.parseTimeML(tmlFile, doc);
		CandidateLinks.setCandidateTlinks(doc);
					
		// Applying temporal rules...
		if (isRuleSieve()) {
			links.getTT().addAll(TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, this.isGoldCandidate()));
			links.getED().addAll(EventTimexTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate()));
			links.getET().addAll(EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate()));
			links.getEE().addAll(EventEventTemporalRule.getEventEventTlinksPerFile(doc, this.isGoldCandidate()));
		}
		
		//Applying temporal reasoner...
		if (isReasoner()) {
			
		}
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventDctTemporalClassifier edCls = new EventDctTemporalClassifier("te3", "liblinear");
			EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier("te3", "liblinear");
			EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier("te3", "liblinear");	
			
			//Init the feature vectors...			
			List<PairFeatureVector> edFv = EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
					false, isGoldCandidate());
			List<PairFeatureVector> etFv = EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
					false, isGoldCandidate(), isTTFeature());
			List<PairFeatureVector> eeFv = EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
					false, isGoldCandidate(), isETFeature());
			
			List<String> edClsLabels = edCls.predict(edFv, getEDModelPath());
			List<String> etClsLabels = etCls.predict(etFv, getETModelPath());
			List<String> eeClsLabels = eeCls.predict(eeFv, getEEModelPath());
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < edFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					EventTimexTemporalRule edRule = new EventTimexTemporalRule((Event) etfv.getE1(), (Timex) etfv.getE2(), 
							doc, etfv.getMateDependencyPath());
					if (edRule.getRelType().equals("O")) {
						links.getED().add(etfv.getE1().getID() 
								+ "\t" + etfv.getE2().getID()
								+ "\t" + etfv.getLabel()
								+ "\t" + edClsLabels.get(i));
					}							
				}
				for (int i = 0; i < etFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					EventTimexTemporalRule etRule = new EventTimexTemporalRule((Event) etfv.getE1(), (Timex) etfv.getE2(), 
							doc, etfv.getMateDependencyPath());
					if (etRule.getRelType().equals("O")) {
						links.getET().add(etfv.getE1().getID() 
								+ "\t" + etfv.getE2().getID()
								+ "\t" + etfv.getLabel()
								+ "\t" + etClsLabels.get(i));
					}							
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					EventEventTemporalRule eeRule = new EventEventTemporalRule((Event) eefv.getE1(), (Event) eefv.getE2(), 
							doc, eefv.getMateDependencyPath());
					if (eeRule.getRelType().equals("O")) {
						links.getEE().add(eefv.getE1().getID() 
								+ "\t" + eefv.getE2().getID()
								+ "\t" + eefv.getLabel()
								+ "\t" + eeClsLabels.get(i));
					}				
				}
				
			} else {
				for (int i = 0; i < edFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					links.getED().add(etfv.getE1().getID() 
							+ "\t" + etfv.getE2().getID()
							+ "\t" + etfv.getLabel()
							+ "\t" + edClsLabels.get(i));
				}
				for (int i = 0; i < etFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					links.getET().add(etfv.getE1().getID() 
							+ "\t" + etfv.getE2().getID()
							+ "\t" + etfv.getLabel()
							+ "\t" + etClsLabels.get(i));
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					links.getEE().add(eefv.getE1().getID() 
							+ "\t" + eefv.getE2().getID()
							+ "\t" + eefv.getLabel()
							+ "\t" + eeClsLabels.get(i));
				}
			}
		}
		
		// Map relation types...
		for (int i = 0; i < links.getTT().size(); i ++) {
			for (String key : relTypeMapping.keySet()) {
				links.getTT().set(i, links.getTT().get(i).replace(key, relTypeMapping.get(key)));
			}
		}
		for (int i = 0; i < links.getED().size(); i ++) {
			for (String key : relTypeMapping.keySet()) {
				links.getED().set(i, links.getED().get(i).replace(key, relTypeMapping.get(key)));
			}
		}
		for (int i = 0; i < links.getET().size(); i ++) {
			for (String key : relTypeMapping.keySet()) {
				links.getET().set(i, links.getET().get(i).replace(key, relTypeMapping.get(key)));
			}
		}
		for (int i = 0; i < links.getEE().size(); i ++) {
			for (String key : relTypeMapping.keySet()) {
				links.getEE().set(i, links.getEE().get(i).replace(key, relTypeMapping.get(key)));
			}
		}
		
		return links;
	}
	
	public static void main(String[] args) throws Exception {
		
		String task = "te3-c";
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Map<String, String> relTypeMapping;
		Links tlinks;
		
		switch(task) {
		
			case "te3-c-rel" :
				// TempEval3 task C (relation only)
				String[] te3CRelLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
						"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
				
				temp = new Temporal(true, 
						"./models/te3-event-dct.model",
						"./models/te3-event-timex.model",
						"./models/te3-event-event.model",
						true, true, false);
				
		//		// TRAIN
		//		temp.trainModels("./data/TempEval3-train_TML/");
				
				// PREDICT
				relTypeMapping = new HashMap<String, String>();
				relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
				tlinks = temp.extractRelations("./data/TempEval3-eval_TML/", relTypeMapping);
				
				// EVALUATE
				ptt = new PairEvaluator(tlinks.getTT());
				ptt.evaluatePerLabel(te3CRelLabel);
				ped = new PairEvaluator(tlinks.getED());
				ped.evaluatePerLabel(te3CRelLabel);
				pet = new PairEvaluator(tlinks.getET());
				pet.evaluatePerLabel(te3CRelLabel);
				pee = new PairEvaluator(tlinks.getEE());
				pee.evaluatePerLabel(te3CRelLabel);
				
				break;
			
			case "te3-c" :
				// TempEval3 task C
				String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
						"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
				
				temp = new Temporal(false, 
						"./models/te3-event-dct.model",
						"./models/te3-event-timex.model",
						"./models/te3-event-event.model",
						true, true, false);
				
		//		// TRAIN
		//		temp.trainModels("./data/TempEval3-train_TML/");
				
				// PREDICT
				relTypeMapping = new HashMap<String, String>();
				relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
				tlinks = temp.extractRelations("./data/TempEval3-eval_TML/", relTypeMapping);
				
				// EVALUATE
				ptt = new PairEvaluator(tlinks.getTT());
				ptt.evaluatePerLabel(te3CLabel);
				ped = new PairEvaluator(tlinks.getED());
				ped.evaluatePerLabel(te3CLabel);
				pet = new PairEvaluator(tlinks.getET());
				pet.evaluatePerLabel(te3CLabel);
				pee = new PairEvaluator(tlinks.getEE());
				pee.evaluatePerLabel(te3CLabel);
				
				break;
		
		}
		
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
