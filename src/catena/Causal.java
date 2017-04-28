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
import catena.parser.entities.TimeMLDoc;

public class Causal {
	
	private boolean ruleSieve;
	private boolean classifierSieve;
	
	private boolean tlinkFeature;
	
	private String causalModelPath;
	
	public Causal() {
		
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels, boolean colFilesAvailable) throws Exception {
		trainModels(taskName, tmlDirpath, labels, false, null, null, colFilesAvailable);
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels, 
			boolean tlinkAsFeature,
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels, 
			boolean colFilesAvailable) throws Exception {
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "logit");	
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
//				System.err.println("Processing " + tmlFile.getPath());
				
				// File pre-processing...
				Doc doc;
				if (colFilesAvailable) {
					doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
					
				} else {
					tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
					doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
					
					// OR... Parse TimeML file without saving to .col files
//					List<String> columns = tmlToCol.convert(tmlFile, true);
//					doc = colParser.parseLines(columns);
				}				
				
				doc.setFilename(tmlFile.getName());
				
				TimeMLParser.parseTimeML(tmlFile, doc);
				CandidateLinks.setCandidateClinks(doc);
				
				// Get the feature vectors
				if (tlinkAsFeature) {
					eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
							true, Arrays.asList(labels), tlinkAsFeature, tlinks.get(doc.getFilename()), Arrays.asList(tlinkLabels)));
				} else {
					eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
							true, Arrays.asList(labels)));
				}
			}
		}

		eeCls.train(eeFvList, getCausalModelPath());
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] labels, boolean colFilesAvailable) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				labels,
				false, null, null, 
				colFilesAvailable);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] labels,
			boolean tlinkAsFeature,
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels,
			boolean colFilesAvailable) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.err.println("Processing " + tmlFile.getPath());
				
				// PREDICT				
				CLINK links = extractRelations(taskName, tmlFile, null, labels, tlinkAsFeature, tlinks.get(tmlFile.getName()), tlinkLabels, colFilesAvailable);
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
	}	
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			Map<String, Map<String, String>> clinkPerFile,
			String[] labels, boolean colFilesAvailable) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				clinkPerFile,
				labels,
				false, null, null, 
				colFilesAvailable);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			Map<String, Map<String, String>> clinkPerFile,
			String[] labels,
			boolean tlinkAsFeature,
			Map<String, Map<String, String>> tlinks, String[] tlinkLabels,
			boolean colFilesAvailable) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.err.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				Map<String, String> clinks = new HashMap<String, String>();
				if (clinkPerFile.containsKey(tmlFile.getName())) clinks = clinkPerFile.get(tmlFile.getName());
				
				CLINK links;
				if (tlinkAsFeature) {
					links = extractRelations(taskName, tmlFile, clinks, labels, tlinkAsFeature, tlinks.get(tmlFile.getName()), tlinkLabels, colFilesAvailable);
				} else {
					links = extractRelations(taskName, tmlFile, clinks, labels, colFilesAvailable);
				}
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
	}	
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] tmlFileNames, String[] labels,
			boolean colFilesAvailable) throws Exception {
		return extractRelations(taskName, tmlDirpath,
				tmlFileNames, labels, 
				false, null, null, 
				colFilesAvailable);
	}
	
	public CLINK extractRelations(String taskName, String tmlDirpath,
			String[] tmlFileNames, String[] labels, 
			boolean tlinkAsFeature,
			Map<String, String> tlinks, String[] tlinkLabels,
			boolean colFilesAvailable) throws Exception {
		CLINK results = new CLINK();
		List<String> ee = new ArrayList<String>();
		
		List<String> tmlFileList = Arrays.asList(tmlFileNames);
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml") && tmlFileList.contains(tmlFile.getName())) {
				System.err.println("Processing " + tmlFile.getPath());
				
				// PREDICT
				CLINK links = extractRelations(taskName, tmlFile, null, labels, tlinkAsFeature, tlinks, tlinkLabels, colFilesAvailable);
				ee.addAll(links.getEE());
			}
		}
		
		results.setEE(ee);
		
		return results;
	}	
	
	public CLINK extractRelations(String taskName, File tmlFile, Map<String, String> clinks, 
			String[] labels,
			boolean colFilesAvailable) throws Exception {
		return extractRelations(taskName, tmlFile,
				clinks, labels, 
				false, null, null, 
				colFilesAvailable);
	}
	
	public CLINK extractRelations(String taskName, File tmlFile, Map<String, String> clinks, 
			String[] labels,
			boolean tlinkAsFeature,
			Map<String, String> tlinks, String[] tlinkLabels, 
			boolean colFilesAvailable) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
		Doc doc;
		if (colFilesAvailable) {
			doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
			
		} else {
			tmlToCol.convert(tmlFile, new File(tmlFile.getPath().replace(".tml", ".col")), true);
			doc = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
			
			// OR... Parse TimeML file without saving to .col files
//			List<String> columns = tmlToCol.convert(tmlFile, true);
//			doc = colParser.parseLines(columns);
		}				
		
		doc.setFilename(tmlFile.getName());
		
		TimeMLParser.parseTimeML(tmlFile, doc, null, clinks);
		CandidateLinks.setCandidateClinks(doc);
		
		TimeMLDoc tmlDoc = new TimeMLDoc(tmlFile);
		String tmlString = tmlDoc.toString();
					
		// Applying causal rules...
		if (isRuleSieve()) {
			List<String> eeCausalRule = EventEventCausalRule.getEventEventClinksPerFile(doc);
			tmlString = TimeMLDoc.timeMLFileToString(doc, tmlFile, eeCausalRule);
		}
		
//		Doc docSieved = colParser.parseLines(columns);
		Doc docSieved = colParser.parseDocument(new File(tmlFile.getPath().replace(".tml", ".col")), false);
		docSieved.setFilename(tmlFile.getName());
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);
		
		//Applying causal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventEventCausalClassifier eeCls = new EventEventCausalClassifier(taskName, "liblinear");
			
			//Init the feature vectors...
			List<PairFeatureVector> eeFvList;
			if (tlinkAsFeature) {
				eeFvList = EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						false, Arrays.asList(labels), tlinkAsFeature, tlinks, Arrays.asList(tlinkLabels));
			} else {
				eeFvList = EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						false, Arrays.asList(labels));
			}
			
			List<String> eeClsLabels = eeCls.predict(eeFvList, getCausalModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < eeFvList.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFvList.get(i));
					if (!docSieved.getClinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getClinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						
						docSieved.getClinkTypes().put(eefv.getE1().getID() + "," + eefv.getE2().getID(), eeClsLabels.get(i));
						docSieved.getClinkTypes().put(eefv.getE2().getID() + "," + eefv.getE1().getID(), CausalRelation.getInverseRelation(eeClsLabels.get(i)));
						
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
					
					docSieved.getClinkTypes().put(eefv.getE1().getID() + "," + eefv.getE2().getID(), eeClsLabels.get(i));
					docSieved.getClinkTypes().put(eefv.getE2().getID() + "," + eefv.getE1().getID(), CausalRelation.getInverseRelation(eeClsLabels.get(i)));
					
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
		
		for (CausalRelation clink : gold.getClinks()) {
			
			Entity e1 = system.getEntities().get(clink.getSourceID());
			Entity e2 = system.getEntities().get(clink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(system, e1, e2, "CLINK", null, null);
			
			if (!extracted.contains(e1.getID()+","+e2.getID())) {
				if (fv.getPairType().equals(PairType.event_event)) {
					if (system.getClinkTypes().containsKey(e1.getID()+","+e2.getID())) {
						links.getEE().add(system.getFilename()
								+ "\t" + e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + gold.getClinkTypes().get(e1.getID()+","+e2.getID())
								+ "\t" + system.getClinkTypes().get(e1.getID()+","+e2.getID()));
						
					} else {
						links.getEE().add(system.getFilename()
								+ "\t" + e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + gold.getClinkTypes().get(e1.getID()+","+e2.getID())
								+ "\t" + "NONE");
					}
					extracted.add(e1.getID()+","+e2.getID());
					extracted.add(e2.getID()+","+e1.getID());
				}
			}
		}
		
		for (CausalRelation clink : system.getClinks()) {
			
			Entity e1 = system.getEntities().get(clink.getSourceID());
			Entity e2 = system.getEntities().get(clink.getTargetID());
			PairFeatureVector fv = new PairFeatureVector(system, e1, e2, "CLINK", null, null);
			
			if (!extracted.contains(e1.getID()+","+e2.getID())) {
				if (fv.getPairType().equals(PairType.event_event)) {
					links.getEE().add(system.getFilename()
							+ "\t" + e1.getID()
							+ "\t" + e2.getID()
							+ "\t" + "NONE"
							+ "\t" + system.getClinkTypes().get(e1.getID()+","+e2.getID()));
					
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
		
		ensureDirectory(new File(causalModelPath).getParentFile());
		
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
	
	public static void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
}
