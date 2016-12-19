package catena;

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
		
		switch(task) {
		
			case "te3" :
				TempEval3();
				break;
				
//			case "tbdense" :
//				TimeBankDense();
//				break;
		}
		
	}
	
	public static void TempEval3() throws Exception {
		
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
		
//		// TRAIN
		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
		relTypeMappingTrain.put("IBEFORE", "BEFORE");
		relTypeMappingTrain.put("IAFTER", "AFTER");
//		temp.trainModels(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMappingTrain);
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		List<TLINK> tlinks = temp.extractRelations(taskName, "./data/TempEval3-eval_TML/", te3CLabelCollapsed, relTypeMapping);
		
		// ---------- CAUSAL ---------- //
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String[] causalLabelEval = {"CLINK", "NONE"};
		
		Causal causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		// TRAIN
		Map<String, Map<String, String>> tlinksForClinkTrainPerFile = new HashMap<String, Map<String, String>>();
		if (cat.isTlinkFeature()) {
			List<TLINK> tlinksTrainRule = temp.extractRelations(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMapping);
			
			for (String s : tlinksTrainRule.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkTrainPerFile.containsKey(cols[0])) tlinksForClinkTrainPerFile.put(cols[0], new HashMap<String, String>());
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[4]);
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[4]));
			}
		}
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel, 
				tlinksForClinkTrainPerFile, te3CLabelCollapsed, relTypeMappingTrain);
		
		// PREDICT
		Map<String, Map<String, String>> tlinksForClinkPerFile = new HashMap<String, Map<String, String>>(); 
		if (cat.isTlinkFeature()) {
			for (String s : tlinks.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkPerFile.containsKey(cols[0])) tlinksForClinkPerFile.put(cols[0], new HashMap<String, String>());
				tlinksForClinkPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[4]);
				tlinksForClinkPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[4]));
			}
		}
		CLINK clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel, 
				tlinksForClinkPerFile, te3CLabelCollapsed);
		
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
		PairEvaluator ptt = new PairEvaluator(tlinks.get(1).getTT());
		ptt.evaluatePerLabel(te3CLabel);
		PairEvaluator ped = new PairEvaluator(tlinks.get(1).getED());
		ped.evaluatePerLabel(te3CLabel);
		PairEvaluator pet = new PairEvaluator(tlinks.get(1).getET());
		pet.evaluatePerLabel(te3CLabel);
		PairEvaluator pee = new PairEvaluator(tlinks.get(1).getEE());
		pee.evaluatePerLabel(te3CLabel);
		PairEvaluator peec = new PairEvaluator(clinks.getEE());
		peec.evaluatePerLabel(causalLabelEval);
	}
	
}
