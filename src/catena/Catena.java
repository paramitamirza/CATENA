package catena;

import java.util.HashMap;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.parser.entities.CLINK;
import catena.parser.entities.TLINK;

public class Catena {
	
	private boolean tlinkFeature;
	private boolean clinkPostEditing;
	
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
		Catena cat = new Catena(true, false);
		
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
//		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
//		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
//		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
//		relTypeMappingTrain.put("IBEFORE", "BEFORE");
//		relTypeMappingTrain.put("IAFTER", "AFTER");
//		temp.trainModels(taskName, "./data/TempEval3-train_TML/", te3CLabelCollapsed, relTypeMappingTrain);
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		TLINK tlinks = temp.extractRelations(taskName, "./data/TempEval3-eval_TML/", te3CLabelCollapsed, relTypeMapping);
		
		// ---------- CAUSAL ---------- //
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
		// TempEval3 task C
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		String[] causalLabelEval = {"CLINK", "NONE"};
		
		Causal causal = new Causal(
				"./models/" + taskName + "-causal-event-event.model",
				true, true);
		
		Map<String, String> tlinksForClink = null;
		if (cat.isTlinkFeature()) {
			Temporal tempForClink = new Temporal(false, te3CLabelCollapsed,
					"./models/" + taskName + "-event-dct.model",
					"./models/" + taskName + "-event-timex.model",
					"./models/" + taskName + "-event-event.model",
					true, false, true,
					false, false);
			TLINK tlinksRule = tempForClink.extractRelations(taskName, "./data/Causal-TimeBank_TML/", te3CLabelCollapsed, relTypeMapping);
			tlinksForClink = new HashMap<String, String>();
			for (String s : tlinksRule.getEE()) {
				String[] cols = s.split("\t");
				tlinksForClink.put(cols[0]+","+cols[1], cols[3]);
			}
		}
		
		// TRAIN
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", causalLabel, 
				tlinksForClink, te3CLabelCollapsed);
		
		// PREDICT
		CLINK clinks = causal.extractRelations(taskName, "./data/TempEval3-eval_TML/", clinkPerFile, causalLabel, 
				tlinksForClink, te3CLabelCollapsed);
		
		
		// EVALUATE
		PairEvaluator ptt = new PairEvaluator(tlinks.getTT());
		ptt.evaluatePerLabel(te3CLabel);
		PairEvaluator ped = new PairEvaluator(tlinks.getED());
		ped.evaluatePerLabel(te3CLabel);
		PairEvaluator pet = new PairEvaluator(tlinks.getET());
		pet.evaluatePerLabel(te3CLabel);
		PairEvaluator pee = new PairEvaluator(tlinks.getEE());
		pee.evaluatePerLabel(te3CLabel);
		PairEvaluator peec = new PairEvaluator(clinks.getEE());
		peec.evaluatePerLabel(causalLabelEval);
	}
	
	Catena (boolean tlinkFeature, boolean clinkPostEditing) {
		setTlinkFeature(tlinkFeature);
		setClinkPostEditing(clinkPostEditing);
	}

	public boolean isTlinkFeature() {
		return tlinkFeature;
	}

	public void setTlinkFeature(boolean tlinkFeature) {
		this.tlinkFeature = tlinkFeature;
	}

	public boolean isClinkPostEditing() {
		return clinkPostEditing;
	}

	public void setClinkPostEditing(boolean clinkPostEditing) {
		this.clinkPostEditing = clinkPostEditing;
	}
}
