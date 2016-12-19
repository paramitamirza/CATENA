package catena;

import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.parser.entities.CLINK;

public class TestCausal {

public static void main(String[] args) throws Exception {
		
		String task = "te3";
		
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
		
		Map<String, Map<String, String>> clinkPerFile = Causal.getCausalTempEval3EvalTlinks("./data/Causal-TempEval3-eval.txt");
		
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
		causal.trainModels(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel);
		
		// PREDICT
		clinks = causal.extractRelations(taskName, "./data/Causal-TimeBank_TML/", testDocs, causalLabel);
		
		// EVALUATE
		pee = new PairEvaluator(clinks.getEE());
		pee.evaluatePerLabel(causalLabelEval);
	}
	
}
