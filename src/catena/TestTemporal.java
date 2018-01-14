package catena;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.parser.entities.TLINK;

public class TestTemporal {

public static void main(String[] args) throws Exception {
		
		String task = "te3-c-rel";
		boolean columnFormat = true;
		boolean train = false;
		
		switch(task) {
			case "te3-c-rel" :
				TempEval3TaskCRelOnly(columnFormat, train);
				break;
			
			case "te3-c" :
				TempEval3TaskC(columnFormat, train);
				break;
				
			case "tbdense" :
				TimeBankDense(columnFormat, train);
				break;
		}
	}
	
	public static void TempEval3TaskC(boolean columnFormat, boolean train) throws Exception {
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		List<TLINK> tlinks;
		
		// TempEval3 task C
		String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String taskName = "te3";
		
		temp = new Temporal(false, te3CLabelCollapsed,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, false);
		
		Map<String, Map<String, String>> tlinkPerFile = null;
		String trainDirpath = "./data/TempEval3-train_TML/";
		String evalDirpath = "./data/TempEval3-eval_TML/";
		if (columnFormat) {
			trainDirpath = "./data/TempEval3-train_COL/";
			evalDirpath = "./data/TempEval3-eval_COL/";
			tlinkPerFile = Temporal.getLinksFromFile("./data/TempEval3.TLINK.txt");
		}
		
		// TRAIN
		if (train) {
			System.err.println("Train temporal models...");
			
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			temp.trainModels(taskName, trainDirpath, tlinkPerFile, te3CLabelCollapsed, relTypeMappingTrain, columnFormat);
		}
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		tlinks = temp.extractRelations(taskName, evalDirpath, tlinkPerFile, te3CLabelCollapsed, relTypeMapping, columnFormat);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(te3CLabel);
	}
	
	public static void TempEval3TaskCRelOnly(boolean columnFormat, boolean train) throws Exception {
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Map<String, String> relTypeMapping;
		List<TLINK> tlinks;
		
		// TempEval3 task C (relation only)
		String[] te3CRelLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String taskName = "te3";
		
		temp = new Temporal(true, te3CLabelCollapsed,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, false);
		
		Map<String, Map<String, String>> tlinkPerFile = null;
		String trainDirpath = "./data/TempEval3-train_TML/";
		String evalDirpath = "./data/TempEval3-eval_TML/";
		if (columnFormat) {
			trainDirpath = "./data/TempEval3-train_COL/";
			evalDirpath = "./data/TempEval3-eval_COL/";
			tlinkPerFile = Temporal.getLinksFromFile("./data/TempEval3.TLINK.txt");
		}
		
		// TRAIN
		if (train) {
			System.err.println("Train temporal models...");
			
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");			
			temp.trainModels(taskName, trainDirpath, tlinkPerFile, te3CLabelCollapsed, relTypeMappingTrain, columnFormat);
			
		}
		
		// PREDICT
		relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		tlinks = temp.extractRelations(taskName, evalDirpath, tlinkPerFile, te3CLabelCollapsed, relTypeMapping, columnFormat);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(te3CRelLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(te3CRelLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(te3CRelLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(te3CRelLabel);
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
		
		Map<String, Map<String, String>> tlinkPerFile = Temporal.getLinksFromFile("./data/TimebankDense.TLINK.tml.txt");
		String dirpath = "./data/TempEval3-train_TML/";
		if (columnFormat) {
			tlinkPerFile = Temporal.getLinksFromFile("./data/TimebankDense.TLINK.txt");
			dirpath = "./data/TempEval3-train_COL/";
			for (int i=0; i<devDocs.length; i++) { devDocs[i] = devDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<testDocs.length; i++) { testDocs[i] = testDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<trainDocs.length; i++) { trainDocs[i] = trainDocs[i].replace(".tml", ".col"); }
		}
		
		Temporal temp;
		PairEvaluator ptt, ped, pet, pee;
		Map<String, String> relTypeMapping;
		List<TLINK> tlinks;
		
		// TimeBank-Dense
		String[] tbDenseLabel = {"BEFORE", "AFTER", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "VAGUE"};
		String taskName = "tbdense";
		
		temp = new Temporal(true, tbDenseLabel,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, false);
		
		// TRAIN
		if (train) {
			System.err.println("Train temporal models...");
			
			temp.trainModels(taskName, dirpath, trainDocs, tlinkPerFile, tbDenseLabel, new HashMap<String, String>(), columnFormat);
		}
		
		// PREDICT
		relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		relTypeMapping.put("BEGINS", "BEFORE");
		relTypeMapping.put("BEGUN_BY", "AFTER");
		relTypeMapping.put("ENDS", "AFTER");
		relTypeMapping.put("ENDED_BY", "BEFORE");
		relTypeMapping.put("DURING", "SIMULTANEOUS");
		relTypeMapping.put("DURING_INV", "SIMULTANEOUS");
		tlinks = temp.extractRelations(taskName, dirpath, testDocs, tlinkPerFile, tbDenseLabel, relTypeMapping, columnFormat);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(tbDenseLabel);
	}
	
}
