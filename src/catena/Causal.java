package catena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.model.classifier.EventEventCausalClassifier;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.rule.EventEventCausalRule;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.CLINK;
import catena.parser.entities.CausalRelation;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TLINK;
import catena.parser.entities.TimeMLDoc;

public class Causal {
	
	private boolean ruleSieve;
	private boolean classifierSieve;
	
	private boolean tlinkFeature;
	
	private String causalModelPath;
	
	public Causal() {
		
	}

	public static void main(String[] args) throws Exception {
		
		String task = "tbdense";
		
		switch(task) {
		
			case "te3" :
				TempEval3();
				break;
				
			case "tbdense" :
				TimeBankDense();
				break;
		}
	}
	
	public static void TempEval3() throws Exception {
		
		Map<String, Map<String, String>> clinkPerFile = getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
		Causal causal;
		PairEvaluator pee;
		CLINK clinks;
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String[] causalLabelEval = {"CLINK", "NONE"};
		String taskName = "te3";
		
		causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		// TRAIN
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel);
		
		// PREDICT
		clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel);
		
		// EVALUATE
		pee = new PairEvaluator(clinks.getEE());
		pee.evaluatePerLabel(causalLabelEval);
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
		
		Causal causal;
		PairEvaluator pee;
		CLINK clinks;
		
		// TimeBank-Dense
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String[] causalLabelEval = {"CLINK", "NONE"};
		String taskName = "tbdense";
		
		causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		// TRAIN
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", trainDocs, causalLabel);
		
		// PREDICT
		clinks = causal.extractRelations(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel);
		
		// EVALUATE
		pee = new PairEvaluator(clinks.getEE());
		pee.evaluatePerLabel(causalLabelEval);
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels) throws Exception {
		Map<String, String> tlinks = null;
		String[] tlinkLabels = null;
		trainModels(taskName, tmlDirpath, labels, tlinks, tlinkLabels);
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels, 
			Map<String, String> tlinks, String[] tlinkLabels) throws Exception {
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "liblinear");	
		
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
				CandidateLinks.setCandidateClinks(doc);
				
//				if (tlinks != null) tlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						true, Arrays.asList(labels), tlinks, Arrays.asList(tlinkLabels)));
			}
		}

		eeCls.train(eeFvList, getCausalModelPath());
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] tmlFileNames, String[] labels) throws Exception {
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "liblinear");	
		
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
				
				TimeMLParser.parseTimeML(tmlFile, doc);
				CandidateLinks.setCandidateClinks(doc);
				
				// Get the feature vectors
				eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						true, Arrays.asList(labels)));
			}
		}

		eeCls.train(eeFvList, getCausalModelPath());
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			Map<String, Map<String, String>> clinkPerFile,
			String[] labels) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				clinkPerFile,
				labels,
				null, null);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			Map<String, Map<String, String>> clinkPerFile,
			String[] labels,
			Map<String, String> tlinks, String[] tlinkLabels) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				Map<String, String> clinks = new HashMap<String, String>();
				if (clinkPerFile.containsKey(tmlFile.getName())) clinks = clinkPerFile.get(tmlFile.getName());
				CLINK links = extractRelations(taskName, tmlFile, clinks, labels, tlinks, tlinkLabels);
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
	}	
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] tmlFileNames, String[] labels) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				tmlFileNames, labels, 
				null, null);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] tmlFileNames, String[] labels, 
			Map<String, String> tlinks, String[] tlinkLabels) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		List<String> tmlFileList = Arrays.asList(tmlFileNames);
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml") && tmlFileList.contains(tmlFile.getName())) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				CLINK links = extractRelations(taskName, tmlFile, null, labels, tlinks, tlinkLabels);
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
	}	
	
	public CLINK extractRelations(String taskName, File tmlFile, Map<String, String> clinks, 
			String[] labels,
			Map<String, String> tlinks, String[] tlinkLabels) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
//		List<String> columns = tmlToCol.convert(tmlFile, false);
//		Doc doc = colParser.parseLines(columns);
		
//		tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
		Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		
		TimeMLParser.parseTimeML(tmlFile, doc, null, clinks);
		CandidateLinks.setCandidateClinks(doc);
		
		TimeMLDoc tmlDoc = new TimeMLDoc(tmlFile);
		String tmlString = tmlDoc.toString();
					
		// Applying temporal rules...
		if (isRuleSieve()) {
			List<String> eeCausalRule = EventEventCausalRule.getEventEventClinksPerFile(doc);
			tmlString = TimeMLDoc.timeMLFileToString(doc, tmlFile, eeCausalRule);
		}
		
		Doc docSieved = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "liblinear");
			
			//Init the feature vectors...	
			List<PairFeatureVector> eeFvList = EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
					false, Arrays.asList(labels), tlinks, Arrays.asList(tlinkLabels));
			
			List<String> eeClsLabels = eeCls.predict(eeFvList, getCausalModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < eeFvList.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFvList.get(i));
					if (!docSieved.getClinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getClinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						if (eeClsLabels.get(i).equals("CLINK")) {
							docSieved.getClinks().add(new CausalRelation(eefv.getE1().getID(), eefv.getE2().getID()));
						} else if (eeClsLabels.get(i).equals("CLINK-R")) {
							docSieved.getClinks().add(new CausalRelation(eefv.getE2().getID(), eefv.getE1().getID()));
						}
					}				
				}
				
			} else {
				for (int i = 0; i < eeFvList.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFvList.get(i));
					if (eeClsLabels.get(i).equals("CLINK")) {
						docSieved.getClinks().add(new CausalRelation(eefv.getE1().getID(), eefv.getE2().getID()));
					} else if (eeClsLabels.get(i).equals("CLINK-R")) {
						docSieved.getClinks().add(new CausalRelation(eefv.getE2().getID(), eefv.getE1().getID()));
					}
				}
			}
		}
		
		// Causal links to string
		CLINK links = relationToString(doc, docSieved);
		
		return links;
	}
	
	public CLINK relationToString(Doc gold, Doc system) {
		CLINK links = new CLINK();
		Set<String> extracted = new HashSet<String>();
		
		for (CausalRelation clink : system.getClinks()) {
			
			Entity e1 = system.getEntities().get(clink.getSourceID());
			Entity e2 = system.getEntities().get(clink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(system, e1, e2, "CLINK", null, null);
			
			String label = "NONE";
			if (gold.getClinkTypes().containsKey(e1.getID() + "," + e2.getID())) {
				label = gold.getClinkTypes().get(e1.getID() + "," + e2.getID());
			} 
			
			if (!extracted.contains(e1.getID()+","+e2.getID())) {
				if (fv.getPairType().equals(PairType.event_event)) {
					links.getEE().add(e1.getID()
							+ "\t" + e2.getID()
							+ "\t" + label
							+ "\t" + fv.getLabel());
					
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
				}
			}
		}
		
		for (CausalRelation clink : gold.getClinks()) {
			
			Entity e1 = system.getEntities().get(clink.getSourceID());
			Entity e2 = system.getEntities().get(clink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(system, e1, e2, "CLINK", null, null);
			
			if (!extracted.contains(e1.getID()+","+e2.getID())) {
				if (fv.getPairType().equals(PairType.event_event)) {
					links.getEE().add(e1.getID()
							+ "\t" + e2.getID()
							+ "\t" + "CLINK"
							+ "\t" + "NONE");
					
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
				}
			}
		}
		
		return links;
	}
	
	public static Map<String, Map<String, String>> getCausalTempEval3EvalTlinks(String clinkPath) throws Exception {
		Map<String, Map<String, String>> clinkPerFile = new HashMap<String, Map<String, String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(clinkPath)));
		String line;
		String filename, e1, e2, clink;
		
	    while ((line = br.readLine()) != null) {
	    	String[] cols = line.split("\t");
	    	filename = cols[0];
	    	e1 = cols[1]; e2 = cols[2];
	    	clink = cols[3];
	    	
	    	if (!clinkPerFile.containsKey(filename)) {
	    		clinkPerFile.put(filename, new HashMap<String, String>());
	    	}
    		clinkPerFile.get(filename).put(e1+","+e2, clink);
	    }
	    br.close();
		
		return clinkPerFile;
	}
	
	public Causal(String causalModelPath) {
		
		this.setCausalModelPath(causalModelPath);
		
		this.setRuleSieve(true);
		this.setClassifierSieve(true);
		
		this.setTlinkFeature(false);
	}
	
	public Causal(String causalModelPath, boolean ruleSieve, boolean classifierSieve) {
		
		this(causalModelPath);
		this.setRuleSieve(ruleSieve);
		this.setClassifierSieve(classifierSieve);
	}
	
	public Causal(String causalModelPath, boolean ruleSieve, boolean classifierSieve,
			boolean tlinkFeature) {
		
		this(causalModelPath, ruleSieve, classifierSieve);
		this.setTlinkFeature(tlinkFeature);
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

	public boolean isTlinkFeature() {
		return tlinkFeature;
	}

	public void setTlinkFeature(boolean tlinkFeature) {
		this.tlinkFeature = tlinkFeature;
	}

	public String getCausalModelPath() {
		return causalModelPath;
	}

	public void setCausalModelPath(String causalModelPath) {
		this.causalModelPath = causalModelPath;
	}
	
	
}
