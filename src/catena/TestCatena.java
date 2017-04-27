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
		
		String task = "te3";
		boolean colFilesAvailable = true;
		
		switch(task) {
		
			case "te3" :
				TempEval3(colFilesAvailable);
				break;
				
//			case "tbdense" :
//				TimeBankDense();
//				break;
		}
		
	}
	
	public static void TempEval3(boolean colFilesAvailable) throws Exception {
		
		String taskName = "te3";
		Catena cat = new Catena(true, true);
		boolean train = false;
		
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
		
		// TempEval3 task C
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
	
}
