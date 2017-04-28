package catena;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.parser.entities.CLINK;
import catena.parser.entities.TLINK;
import catena.parser.entities.TemporalRelation;

public class TestCatena {

public static void main(String[] args) throws Exception {
		
		String task = "tbdense";
		boolean colFilesAvailable = true;
		boolean train = true;
		
		switch(task) {
		
			case "te3" :
				TempEval3(colFilesAvailable, train);
				break;
				
			case "tbdense" :
				TimeBankDense(colFilesAvailable, train);
				break;
		}
		
	}
	
	public static void TempEval3(boolean colFilesAvailable, boolean train) throws Exception {
		
		String taskName = "te3";
		Catena cat = new Catena(true, true);
		
		// ---------- TEMPORAL ---------- //
		
		String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp = new Temporal(false, te3CLabelCollapsed,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, false);
		
		// TRAIN
		if (train) {
			System.err.println("Train temporal model...");
			
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			temp.trainModels(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMappingTrain, colFilesAvailable);
		}
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		List<TLINK> tlinks = temp.extractRelations(taskName, "./data/TempEval3-eval_TML/", te3CLabelCollapsed, relTypeMapping, colFilesAvailable);
		
		// ---------- CAUSAL ---------- //
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		// TRAIN
		if (train) {
			System.err.println("Train causal model...");
			
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			
			Map<String, Map<String, String>> tlinksForClinkTrainPerFile = new HashMap<String, Map<String, String>>();
			if (cat.isTlinkFeature()) {	
				List<TLINK> tlinksTrain = temp.extractRelations(taskName, "./data/Causal-TimeBank_TML/", te3CLabelCollapsed, relTypeMappingTrain, colFilesAvailable);
				for (String s : tlinksTrain.get(0).getEE()) {
					String[] cols = s.split("\t");
					if (!tlinksForClinkTrainPerFile.containsKey(cols[0])) tlinksForClinkTrainPerFile.put(cols[0], new HashMap<String, String>());
					tlinksForClinkTrainPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[3]);
					tlinksForClinkTrainPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[3]));
				}
				causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel, 
						cat.isTlinkFeature(), tlinksForClinkTrainPerFile, te3CLabelCollapsed, colFilesAvailable);
			} else {
				causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel, colFilesAvailable);
			}
		}
		
		// PREDICT
		CLINK clinks;
		Map<String, Map<String, String>> tlinksForClinkPerFile = new HashMap<String, Map<String, String>>(); 
		if (cat.isTlinkFeature()) {
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			
			for (String s : tlinks.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkPerFile.containsKey(cols[0])) tlinksForClinkPerFile.put(cols[0], new HashMap<String, String>());
				String label = cols[4];
				for (String key : relTypeMappingTrain.keySet()) {
					label = label.replace(key, relTypeMappingTrain.get(key));
				}
				tlinksForClinkPerFile.get(cols[0]).put(cols[1]+","+cols[2], label);
				tlinksForClinkPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(label));
			}
			clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel, 
					cat.isTlinkFeature(), tlinksForClinkPerFile, te3CLabelCollapsed, colFilesAvailable);
		} else {
			clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel, colFilesAvailable);
		}
		
		
		// POST-EDITING
		if (cat.isClinkPostEditing()) {
			for (String key : clinks.getEELinks().keySet()) {
				if (clinks.getEELinks().get(key).equals("CLINK")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "BEFORE");
					}
				} else if (clinks.getEELinks().get(key).equals("CLINK-R")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "AFTER");
					}
				}
			}
		}
		
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		PairEvaluator ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		PairEvaluator ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		PairEvaluator pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		PairEvaluator pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		PairEvaluator peec = new PairEvaluator(clinks.getEE());
		peec.evaluatePerLabel(causalLabel);
	}
	
	public static void TimeBankDense(boolean colFilesAvailable, boolean train) throws Exception {
		
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
		
		Map<String, Map<String, String>> tlinkPerFile = Temporal.getTimeBankDenseTlinks("./data/TimebankDense.T3.txt");
		
		String taskName = "tbdense";
		Catena cat = new Catena(true, true);
		
		// ---------- TEMPORAL ---------- //
		
		String[] tbDenseLabel = {"BEFORE", "AFTER", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "VAGUE"};
		
		String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp = new Temporal(true, tbDenseLabel,
				"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				true, true, true,
				true, false);
		
		// TRAIN
		if (train) {
			System.err.println("Train temporal model...");
			
			temp.trainModels(taskName, "./data/TempEval3-train_TML/", trainDocs, tlinkPerFile, tbDenseLabel, colFilesAvailable);
		}
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		relTypeMapping.put("BEGINS", "BEFORE");
		relTypeMapping.put("BEGUN_BY", "AFTER");
		relTypeMapping.put("ENDS", "AFTER");
		relTypeMapping.put("ENDED_BY", "BEFORE");
		relTypeMapping.put("DURING", "SIMULTANEOUS");
		relTypeMapping.put("DURING_INV", "SIMULTANEOUS");
		List<TLINK> tlinks = temp.extractRelations(taskName, "./data/TempEval3-train_TML/", testDocs, tlinkPerFile, tbDenseLabel, relTypeMapping, colFilesAvailable);
		
		// ---------- CAUSAL ---------- //
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		// TRAIN
		if (train) {
			System.err.println("Train causal model...");
			
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			
			Map<String, Map<String, String>> tlinksForClinkTrainPerFile = new HashMap<String, Map<String, String>>();
			if (cat.isTlinkFeature()) {	
				List<TLINK> tlinksTrain = temp.extractRelations(taskName, "./data/Causal-TimeBank_TML/", te3CLabelCollapsed, relTypeMappingTrain, colFilesAvailable);
				for (String s : tlinksTrain.get(0).getEE()) {
					String[] cols = s.split("\t");
					if (!tlinksForClinkTrainPerFile.containsKey(cols[0])) tlinksForClinkTrainPerFile.put(cols[0], new HashMap<String, String>());
					tlinksForClinkTrainPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[3]);
					tlinksForClinkTrainPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[3]));
				}
				causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel, 
						cat.isTlinkFeature(), tlinksForClinkTrainPerFile, te3CLabelCollapsed, colFilesAvailable);
			} else {
				causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel, colFilesAvailable);
			}
		}
		
		// PREDICT
		CLINK clinks;
		Map<String, Map<String, String>> tlinksForClinkPerFile = new HashMap<String, Map<String, String>>(); 
		if (cat.isTlinkFeature()) {
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			
			for (String s : tlinks.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkPerFile.containsKey(cols[0])) tlinksForClinkPerFile.put(cols[0], new HashMap<String, String>());
				String label = cols[4];
				for (String key : relTypeMappingTrain.keySet()) {
					label = label.replace(key, relTypeMappingTrain.get(key));
				}
				tlinksForClinkPerFile.get(cols[0]).put(cols[1]+","+cols[2], label);
				tlinksForClinkPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(label));
			}
			
			clinks = causal.extractRelations(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel,
					cat.isTlinkFeature(), tlinksForClinkPerFile, te3CLabelCollapsed, colFilesAvailable);
		} else {
			clinks = causal.extractRelations(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel, colFilesAvailable);
		}
		
		
		// POST-EDITING
		if (cat.isClinkPostEditing()) {
			for (String key : clinks.getEELinks().keySet()) {
				if (clinks.getEELinks().get(key).equals("CLINK")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "BEFORE");
					}
				} else if (clinks.getEELinks().get(key).equals("CLINK-R")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "AFTER");
					}
				}
			}
		}
		
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		PairEvaluator ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		PairEvaluator ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		PairEvaluator pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		PairEvaluator pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		PairEvaluator peec = new PairEvaluator(clinks.getEE());
		peec.evaluatePerLabel(causalLabel);
	}
	
}
