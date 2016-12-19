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
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels) throws Exception {
		Map<String, Map<String, String>> tlinks = null;
		String[] tlinkLabels = null;
		trainModels(taskName, tmlDirpath, labels, tlinks, tlinkLabels, new HashMap<String, String>());
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels, 
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels) throws Exception {
		trainModels(taskName, tmlDirpath, labels, 
				tlinks, tlinkLabels, new HashMap<String, String>());
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels, 
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels, 
			Map<String, String> relTypeMappingTrain) throws Exception {
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
				List<String> columns = tmlToCol.convert(tmlFile, false);
				Doc doc = colParser.parseLines(columns);
				
//				tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
//				Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
				
				TimeMLParser.parseTimeML(tmlFile, doc);
				CandidateLinks.setCandidateClinks(doc);
				
				// Get the feature vectors
				if (tlinkLabels != null) {
					if (tlinks == null) {
						eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
								true, Arrays.asList(labels), doc.getTlinkTypes(relTypeMappingTrain), Arrays.asList(tlinkLabels)));
					} else {
						eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
								true, Arrays.asList(labels), tlinks.get(doc.getFilename()), Arrays.asList(tlinkLabels)));
					}
				} else {
					eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
							true, Arrays.asList(labels)));
				}
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
			
			if (tmlFile.getName().contains(".tml") && !tmlFileList.contains(tmlFile.getName())) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// File pre-processing...
				List<String> columns = tmlToCol.convert(tmlFile, false);
				Doc doc = colParser.parseLines(columns);
				
//				tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
//				Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
				
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
			String[] labels) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				labels,
				null, null);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] labels,
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT				
				CLINK links;
				if (tlinks != null) {
					links = extractRelations(taskName, tmlFile, null, labels, tlinks.get(tmlFile.getName()), tlinkLabels);
				} else {
					links = extractRelations(taskName, tmlFile, null, labels, null, null);
				}
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
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
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.out.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				Map<String, String> clinks = new HashMap<String, String>();
				if (clinkPerFile.containsKey(tmlFile.getName())) clinks = clinkPerFile.get(tmlFile.getName());
				
				CLINK links;
				if (tlinks != null) {
					links = extractRelations(taskName, tmlFile, clinks, labels, tlinks.get(tmlFile.getName()), tlinkLabels);
				} else {
					links = extractRelations(taskName, tmlFile, clinks, labels, null, null);
				}
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
		List<String> columns = tmlToCol.convert(tmlFile, false);
		Doc doc = colParser.parseLines(columns);
		
//		tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
//		Doc doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		
		TimeMLParser.parseTimeML(tmlFile, doc, null, clinks);
		CandidateLinks.setCandidateClinks(doc);
		
		TimeMLDoc tmlDoc = new TimeMLDoc(tmlFile);
		String tmlString = tmlDoc.toString();
					
		// Applying causal rules...
		if (isRuleSieve()) {
			List<String> eeCausalRule = EventEventCausalRule.getEventEventClinksPerFile(doc);
			tmlString = TimeMLDoc.timeMLFileToString(doc, tmlFile, eeCausalRule);
		}
		
		Doc docSieved = colParser.parseLines(columns);
//		Doc docSieved = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);
		
		//Applying causal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "liblinear");
			
			//Init the feature vectors...	
			List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
			if (tlinks != null && tlinkLabels != null) {
				eeFvList = EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						false, Arrays.asList(labels), tlinks, Arrays.asList(tlinkLabels));
			} else {
				eeFvList = EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						false, Arrays.asList(labels), null, null);
			}
			
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
					links.getEE().add(system.getFilename()
							+ "\t" + e1.getID()
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
					links.getEE().add(system.getFilename()
							+ "\t" + e1.getID()
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
