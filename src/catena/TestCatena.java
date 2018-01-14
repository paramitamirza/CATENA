package catena;

import catena.evaluator.PairEvaluator;
import catena.parser.entities.ExtractedLinks;

public class TestCatena {

public static void main(String[] args) throws Exception {
		
		String task = "te3";
		boolean columnFormat = true;
		boolean train = false;
		boolean goldCandidate = true;
		
		switch(task) {
		
			case "te3" :
				TempEval3(columnFormat, train, goldCandidate);
				break;
				
			case "tbdense" :
				TimeBankDense(columnFormat, train, goldCandidate);
				break;
		}
		
	}
	
	public static void TempEval3(boolean columnFormat, boolean train, boolean goldCandidate) throws Exception {
		
		String taskName = "te3";
		Catena cat = new Catena(true, true);
		
		String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
        
        
		// ---------- TRAIN CATENA MODELS ---------- //
        String tempCorpus = "./data/TempEval3-train_TML/";
        String causCorpus = "./data/Causal-TimeBank_TML/";
        String tlinkFilepath = "";
        String clinkFilepath = "";
        
        if (columnFormat) {
    		tempCorpus = "./data/TempEval3-train_COL/";
    		causCorpus = "./data/Causal-TimeBank_COL/";
    		tlinkFilepath = "./data/TempEval3.TLINK.txt";
    		clinkFilepath = "./data/Causal-TimeBank.CLINK.txt";
        }
        
        if (train) {
        	if (columnFormat) {
    			cat.trainModels(tempCorpus, causCorpus,
    					tlinkFilepath, clinkFilepath,  
    					"./models/" + taskName + "-event-dct.model",
    					"./models/" + taskName + "-event-timex.model",
    					"./models/" + taskName + "-event-event.model",
    					"./models/" + taskName + "-causal-event-event.model",
    					columnFormat);
    			
        	} else {
	        	cat.trainModels(tempCorpus, causCorpus,
	        			"./models/" + taskName + "-event-dct.model",
    					"./models/" + taskName + "-event-timex.model",
    					"./models/" + taskName + "-event-event.model",
    					"./models/" + taskName + "-causal-event-event.model",
    					columnFormat);
        	}
        }

        // ---------- TEST CATENA MODELS ---------- //
        boolean clinkType = false;		//Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve
		
        String tempEvalCorpus = "./data/TempEval3-eval_TML/";
        if (columnFormat) {
        	tempEvalCorpus = "./data/TempEval3-eval_COL/";
    		tlinkFilepath = "./data/TempEval3.TLINK.txt";
    		clinkFilepath = "./data/Causal-TimeBank.CLINK.txt";
        }
        ExtractedLinks links = cat.extractRelations(tempEvalCorpus, 
        		tlinkFilepath, clinkFilepath, 
        		"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				"./models/" + taskName + "-causal-event-event.model", 
				columnFormat, goldCandidate, clinkType);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		PairEvaluator ptt = new PairEvaluator(links.getTlink().getTT());
		ptt.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		PairEvaluator ped = new PairEvaluator(links.getTlink().getED());
		ped.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		PairEvaluator pet = new PairEvaluator(links.getTlink().getET());
		pet.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		PairEvaluator pee = new PairEvaluator(links.getTlink().getEE());
		pee.evaluatePerLabel(te3CLabel);
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		PairEvaluator peec = new PairEvaluator(links.getClink().getEE());
		peec.evaluatePerLabel(causalLabel);
	}
	
	public static void TimeBankDense(boolean columnFormat, boolean train, boolean goldCandidate) throws Exception {
		
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
		
		String taskName = "tbdense";
		Catena cat = new Catena(true, true);
		
		String[] tbDenseLabel = {"BEFORE", "AFTER", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "VAGUE"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		// ---------- TRAIN CATENA MODELS ---------- //
        String tempCorpus = "./data/TempEval3-train_TML/";
        String causCorpus = "./data/Causal-TimeBank_TML/";
        
        String tlinkFilepath = "./data/TimebankDense.TLINK.tml.txt";
        String clinkFilepath = "./data/Causal-TimeBank.CLINK.tml.txt";
        
        if (columnFormat) {
    		tempCorpus = "./data/TempEval3-train_COL/";
    		causCorpus = "./data/Causal-TimeBank_COL/";
    		tlinkFilepath = "./data/TimebankDense.TLINK.txt";
    		clinkFilepath = "./data/Causal-TimeBank.CLINK.txt";
    		for (int i=0; i<devDocs.length; i++) { devDocs[i] = devDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<testDocs.length; i++) { testDocs[i] = testDocs[i].replace(".tml", ".col"); }
			for (int i=0; i<trainDocs.length; i++) { trainDocs[i] = trainDocs[i].replace(".tml", ".col"); }
    	}
        
        if (train) {
        	cat.trainModels(tempCorpus, causCorpus,
					trainDocs, trainDocs, 
					tlinkFilepath, clinkFilepath, 
					tbDenseLabel, causalLabel, 
					"./models/" + taskName + "-event-dct.model",
					"./models/" + taskName + "-event-timex.model",
					"./models/" + taskName + "-event-event.model",
					"./models/" + taskName + "-causal-event-event.model",
					columnFormat);
        }
        
        // ---------- TEST CATENA MODELS ---------- //
        boolean clinkType = false;		//Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve
		
        ExtractedLinks links = cat.extractRelations(tempCorpus, 
        		testDocs,  
        		tlinkFilepath, clinkFilepath,
        		tbDenseLabel, causalLabel,
        		"./models/" + taskName + "-event-dct.model",
				"./models/" + taskName + "-event-timex.model",
				"./models/" + taskName + "-event-event.model",
				"./models/" + taskName + "-causal-event-event.model", 
				columnFormat, goldCandidate, clinkType);
		
		// EVALUATE
		System.out.println("********** EVALUATION RESULTS **********");
		System.out.println();
		System.out.println("********** TLINK TIMEX-TIMEX ***********");
		PairEvaluator ptt = new PairEvaluator(links.getTlink().getTT());
		ptt.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("*********** TLINK EVENT-DCT ************");
		PairEvaluator ped = new PairEvaluator(links.getTlink().getED());
		ped.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-TIMEX ***********");
		PairEvaluator pet = new PairEvaluator(links.getTlink().getET());
		pet.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** TLINK EVENT-EVENT ***********");
		PairEvaluator pee = new PairEvaluator(links.getTlink().getEE());
		pee.evaluatePerLabel(tbDenseLabel);
		System.out.println();
		System.out.println("********** CLINK EVENT-EVENT ***********");
		PairEvaluator peec = new PairEvaluator(links.getClink().getEE());
		peec.evaluatePerLabel(causalLabel);
	}
	
}
