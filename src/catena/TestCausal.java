package catena;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catena.evaluator.PairEvaluator;
import catena.model.CandidateLinks;
import catena.model.rule.EventEventCausalRule;
import catena.parser.ColumnParser;
import catena.parser.entities.CLINK;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;

public class TestCausal {

public static void main(String[] args) throws Exception {
		
		String task = "science";
		boolean columnFormat = false;
		boolean train = false;
		
		switch(task) {
		
			case "te3" :
				TempEval3(columnFormat, train);
				break;
				
			case "tbdense" :
				TimeBankDense(columnFormat, train);
				break;
				
			case "science" :
				Science(true, false);
				break;
		}
	}
	
	public static void TempEval3(boolean columnFormat, boolean train) throws Exception {
		
		Causal causal;
		PairEvaluator pee;
		CLINK clinks;
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String taskName = "te3";
		
		causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getLinksFromFile("./data/Causal-TempEval3-eval.txt");
		String trainDirpath = "./data/Causal-TimeBank_TML/";
		String evalDirpath = "./data/TempEval3-eval_TML/";
		if (columnFormat) {
			trainDirpath = "./data/Causal-TimeBank_COL/";
			evalDirpath = "./data/TempEval3-eval_COL/";
			clinkPerFile = Causal.getLinksFromFile("./data/Causal-TimeBank.CLINK.txt");
		}
		
		// TRAIN
		if (train) {
			System.err.println("Train causal model...");
			if (columnFormat) causal.trainModels(taskName, trainDirpath, clinkPerFile, causalLabel, columnFormat);
			else causal.trainModels(taskName, trainDirpath, causalLabel, columnFormat);
		}
		
		// PREDICT
		clinks = causal.extractRelations(taskName, evalDirpath, clinkPerFile, causalLabel, columnFormat);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		pee = new PairEvaluator(clinks.getEE());
		pee.evaluatePerLabel(causalLabel);
	}
	
	public static void TimeBankDense(boolean columnFormat, boolean train) throws Exception {
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
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getLinksFromFile("./data/Causal-TimeBank.CLINK.tml.txt");
		String trainDirpath = "./data/Causal-TimeBank_TML/";
		if (columnFormat) {
			trainDirpath = "./data/Causal-TimeBank_COL/";
			clinkPerFile = Causal.getLinksFromFile("./data/Causal-TimeBank.CLINK.txt");
			for (int i=0; i<devDocs.length; i++) { devDocs[i] = devDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<testDocs.length; i++) { testDocs[i] = testDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<trainDocs.length; i++) { trainDocs[i] = trainDocs[i].replace(".tml", ".col"); }
		}
		
		// TRAIN
		if (train) {
			System.err.println("Train causal model...");
			causal.trainModels(taskName, trainDirpath, causalLabel, columnFormat, testDocs);	//train causal model, excluding testDocs from CausalTimeBank
		}
		
		// PREDICT
		clinks = causal.extractRelations(taskName, trainDirpath, testDocs, causalLabel, columnFormat);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		pee = new PairEvaluator(clinks.getEE());
		pee.evaluatePerLabel(causalLabelEval);
	}
	
public static void Science(boolean columnFormat, boolean train) throws Exception {
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getLinksFromFile("./data/Causal-TempEval3-eval.txt");
		
		Causal causal;
		PairEvaluator pee;
		CLINK clinks;
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String taskName = "science";
		
		causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, false);
		
		// TRAIN
//		if (train) {
//			System.err.println("Train causal model...");
//			causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel, columnFormat);
//		}
		
		// PREDICT
//		clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel, columnFormat);
		
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
//		for (int i=0; i<=55; i++) {
//			Doc doc = colParser.parseDocument(new File("./data/Science/science-sentences_" + i + ".col"), false);
			Doc doc = colParser.parseDocument(new File("./data/Science/science-sentences.col"), false);
			CandidateLinks.setCandidateClinks(doc);
			Map<Integer, Set<String>> eeCausalRule = EventEventCausalRule.getEventEventClinksPerFileCombined(doc, true);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter("./data/Science/science-sentences.clink"));
			for (Integer sent : eeCausalRule.keySet()) {
				String links = "";
				for (String link : eeCausalRule.get(sent)) links += "\t" + link;
				bw.write(sent + links + "\n");
			}
			bw.close();
//		}
		
		
		// EVALUATE
//		System.out.println("********** EVALUATION RESULTS **********");
//		System.out.println();
//		System.out.println("********** CLINK EVENT-EVENT ***********");
//		pee = new PairEvaluator(clinks.getEE());
//		pee.evaluatePerLabel(causalLabel);
	}
	
}
