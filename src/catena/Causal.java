package catena;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.model.classifier.EventDctTemporalClassifier;
import catena.model.classifier.EventEventCausalClassifier;
import catena.model.classifier.EventEventTemporalClassifier;
import catena.model.classifier.EventTimexTemporalClassifier;
import catena.model.feature.PairFeatureVector;
import catena.model.rule.TimexTimexTemporalRule;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.CLINK;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TLINK;

public class Causal {
	
	private boolean ruleSieve;
	private boolean classifierSieve;
	
	private boolean tlinkFeature;
	
	private String causalModelPath;
	
	public Causal() {
		
	}

	public static void main(String[] args) throws Exception {
		
		String task = "te3";
		
		switch(task) {
		
			case "te3" :
				TempEval3();
				break;
				
//			case "tbdense" :
//				TimeBankDense();
//				break;
		}
	}
	
//	public static void TimeBankDense() throws Exception {
//		String[] devDocs = { 
//			"APW19980227.0487.tml", 
//			"CNN19980223.1130.0960.tml", 
//			"NYT19980212.0019.tml",  
//			"PRI19980216.2000.0170.tml", 
//			"ed980111.1130.0089.tml" 
//		};
//			
//		String[] testDocs = { 
//			"APW19980227.0489.tml",
//			"APW19980227.0494.tml",
//			"APW19980308.0201.tml",
//			"APW19980418.0210.tml",
//			"CNN19980126.1600.1104.tml",
//			"CNN19980213.2130.0155.tml",
//			"NYT19980402.0453.tml",
//			"PRI19980115.2000.0186.tml",
//			"PRI19980306.2000.1675.tml" 
//		};
//		
//		String[] trainDocs = {
//			"APW19980219.0476.tml",
//			"ea980120.1830.0071.tml",
//			"PRI19980205.2000.1998.tml",
//			"ABC19980108.1830.0711.tml",
//			"AP900815-0044.tml",
//			"CNN19980227.2130.0067.tml",
//			"NYT19980206.0460.tml",
//			"APW19980213.1310.tml",
//			"AP900816-0139.tml",
//			"APW19980227.0476.tml",
//			"PRI19980205.2000.1890.tml",
//			"CNN19980222.1130.0084.tml",
//			"APW19980227.0468.tml",
//			"PRI19980213.2000.0313.tml",
//			"ABC19980120.1830.0957.tml",
//			"ABC19980304.1830.1636.tml",
//			"APW19980213.1320.tml",
//			"PRI19980121.2000.2591.tml",
//			"ABC19980114.1830.0611.tml",
//			"APW19980213.1380.tml",
//			"ea980120.1830.0456.tml",
//			"NYT19980206.0466.tml"
//		};
//		
//		Causal causal;
//		PairEvaluator pee;
//		CLINK clinks;
//		
//		// TimeBank-Dense
//		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
//		String taskName = "tbdense";
//		
//		causal = new Causal(
//				"./models/" + taskName + "-causal-event-event.model",
//				true, false, false);
//		
//		// TRAIN
//		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", trainDocs, causalLabel);
//		
//		// PREDICT
//		clinks = causal.extractRelations(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel);
//		
//		// EVALUATE
//		pee = new PairEvaluator(clinks.getEE());
//		pee.evaluatePerLabel(causalLabel);
//	}
	
	public static void TempEval3() throws Exception {
		Causal causal;
		Temporal temp;
		PairEvaluator pee;
		CLINK clinks;
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String taskName = "te3";
		
		causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		temp = new Temporal(true, te3CLabelCollapsed,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, false, true);
		
		// TRAIN
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel);
		
//		// PREDICT
//		clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", causalLabel);
		
//		// EVALUATE
//		pee = new PairEvaluator(clinks.getEE());
//		pee.evaluatePerLabel(causalLabel);
	}
	
	public void trainModels(String taskName, String tmlDirpath, String[] labels) throws Exception {
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
				
				Map<String, String> tlinks = null;
				if (isTlinkFeature()) tlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				eeFvList.addAll(EventEventCausalClassifier.getEventEventClinksPerFile(doc, eeCls, 
						true, Arrays.asList(labels)));
			}
		}

		eeCls.train(eeFvList, getCausalModelPath());
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
