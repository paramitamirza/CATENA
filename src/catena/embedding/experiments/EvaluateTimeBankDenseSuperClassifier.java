package catena.embedding.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import catena.model.classifier.EventEventTemporalClassifier;
import catena.evaluator.PairEvaluator;
import catena.model.feature.PairFeatureVector;

public class EvaluateTimeBankDenseSuperClassifier {
	
	private static String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	private static List<String> labelListDense = Arrays.asList(labelDense);
	
	public static List<PairFeatureVector> getEventEventTlinks(String[] fileList, String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		List<BufferedReader> brList = new ArrayList<BufferedReader>();
		for (String file : fileList) {
			brList.add(new BufferedReader(new FileReader(file)));
		}
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((lbl = brlbl.readLine()) != null) {
			line = "";
			for (BufferedReader br : brList) {
				line += br.readLine().trim() + ",";
			}
			line = line.substring(0, line.length()-1);
			numCols = line.split(",").length;
	    	
	    	PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
	    			null, null);
	    	int i=0;
	    	for (String s : line.split(",")) {
//	    		fv.getVectors().add(s);
	    		fv.getFeatures()[i] = Double.parseDouble(s);
	    		i++;
	    	}
//	    	fv.getVectors().add(String.valueOf(labelList.indexOf(lbl)+1));
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;
	    	
//	    	if (!lbl.equals("VAGUE"))
	    	fvList.add(fv);
		}
		
		brlbl.close();
		for (BufferedReader br : brList) {
			br.close();
		}
		
		return fvList;
	}
	
	public static void main(String [] args) throws Exception {
		
		String exp = "tbdense";
		String pair = "ee";
		String group = "0";
		int numFold = 10;
		boolean stack = false;
		
		String[] arrLabel = new String[0];
		
		EventEventTemporalClassifier cls = new EventEventTemporalClassifier(exp+"-"+pair+"-super", "logit");
		
		arrLabel = labelDense;
		
		List<PairFeatureVector> trainFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> evalFvList = new ArrayList<PairFeatureVector>();
		
		if (stack) {
			switch(pair) {
				case "ee":
					String[] trainFileList = {
							"./data/embedding/"+exp+"-ee-train-conv-features.csv",
//							"./data/embedding/"+exp+"-ee-train-probs.conv.csv", 
			                "./data/embedding/"+exp+"-ee-train-probs.exp0.csv", 
//			                "./data/embedding/"+exp+"-ee-train-probs.exp2.csv", 
//			                "./data/embedding/"+exp+"-ee-train-probs.exp3.csv"
							};
					trainFvList = 
							getEventEventTlinks(trainFileList, 
							"./data/embedding/"+exp+"-ee-train-probs-labels.csv",
							arrLabel);
					
					String[] evalFileList = {
							"./data/embedding/"+exp+"-ee-eval-features.csv", 
//							"./data/embedding/"+exp+"-ee-eval-probs.conv.csv", 
							"./data/embedding/"+exp+"-ee-eval-probs.exp0.csv", 
//			                "./data/embedding/"+exp+"-ee-eval-probs.exp2.csv", 
//			                "./data/embedding/"+exp+"-ee-eval-probs.exp3.csv"
							};
					evalFvList = 
							getEventEventTlinks(evalFileList, 
							"./data/embedding/"+exp+"-ee-eval-probs-labels.csv",
							arrLabel);
					break;
				
				case "ed":
					String[] edTrainFileList = {
							"./data/embedding/"+exp+"-ed-train-conv-features.csv",
			                "./data/embedding/"+exp+"-ed-train-probs.csv"
							};
					trainFvList = 
							getEventEventTlinks(edTrainFileList, 
							"./data/embedding/"+exp+"-ed-train-probs-labels.csv",
							arrLabel);
					
					String[] edEvalFileList = {
							"./data/embedding/"+exp+"-ed-eval-features.csv",
							"./data/embedding/"+exp+"-ed-eval-probs.csv"
							};
					evalFvList = 
							getEventEventTlinks(edEvalFileList, 
							"./data/embedding/"+exp+"-ed-eval-probs-labels.csv",
							arrLabel);
					break;
					
				case "et":
					String[] etTrainFileList = {
							"./data/embedding/"+exp+"-et-train-conv-features.csv",
			                "./data/embedding/"+exp+"-et-train-probs.csv"
							};
					trainFvList = 
							getEventEventTlinks(etTrainFileList, 
							"./data/embedding/"+exp+"-et-train-probs-labels.csv",
							arrLabel);
					
					String[] etEvalFileList = {
							"./data/embedding/"+exp+"-et-eval-features.csv",
							"./data/embedding/"+exp+"-et-eval-probs.csv"
							};
					evalFvList = 
							getEventEventTlinks(etEvalFileList, 
							"./data/embedding/"+exp+"-et-eval-probs-labels.csv",
							arrLabel);
					break;
			}
			
		} else {
			switch(pair) {
				case "ee":
					String[] trainFileList = {
							"./data/embedding/"+exp+"-ee-train-features.csv",
			                "./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp0.csv",
//			              "./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp2.csv",
//			              "./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp3.csv",
							};
					trainFvList = 
							getEventEventTlinks(trainFileList, 
							"./data/embedding/"+exp+"-ee-train-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] evalFileList = {
							"./data/embedding/"+exp+"-ee-eval-features.csv",
			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp0.csv",
//			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp2.csv",
//			                "./data/embedding/"+exp+"-ee-eval-embedding-word2vec-300.exp3.csv",
							};
					evalFvList = 
							getEventEventTlinks(evalFileList, 
									"./data/embedding/"+exp+"-ee-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
					
				case "ed":
					String[] edTrainFileList = {
							"./data/embedding/"+exp+"-ed-train-features.csv",
			                "./data/embedding/"+exp+"-ed-train-embedding-word2vec-300.csv"
							};
					trainFvList = 
							getEventEventTlinks(edTrainFileList, 
							"./data/embedding/"+exp+"-ed-train-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] edEvalFileList = {
							"./data/embedding/"+exp+"-ed-eval-features.csv",
			                "./data/embedding/"+exp+"-ed-eval-embedding-word2vec-300.csv"
							};
					evalFvList = 
							getEventEventTlinks(edEvalFileList, 
									"./data/embedding/"+exp+"-ed-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
					
				case "et":
					String[] etTrainFileList = {
							"./data/embedding/"+exp+"-et-train-features.csv",
			                "./data/embedding/"+exp+"-et-train-embedding-word2vec-300.csv"
							};
					trainFvList = 
							getEventEventTlinks(etTrainFileList, 
							"./data/embedding/"+exp+"-et-train-labels-str.gr"+group+".csv",
							arrLabel);
					
					String[] etEvalFileList = {
							"./data/embedding/"+exp+"-et-eval-features.csv",
			                "./data/embedding/"+exp+"-et-eval-embedding-word2vec-300.csv"
							};
					evalFvList = 
							getEventEventTlinks(etEvalFileList, 
									"./data/embedding/"+exp+"-et-eval-labels-str.gr"+group+".csv",
							arrLabel);
					break;
			}
		}
		
		cls.train2(trainFvList, "models/" + cls.getName() + ".model");
		
		List<String> eeClsTest = cls.predict2(evalFvList, "models/" + cls.getName() + ".model", arrLabel);
//		List<String> eeClsTest = 
//				getMaxProbTlinks(evalFileList, 
//						"./data/embedding/"+exp+"-ee-eval-probs-labels.csv");
		
		//Evaluate
		
		List<Integer> sentDistance = new ArrayList<Integer>();
		BufferedReader brSent = new BufferedReader(new FileReader("./data/embedding/"+exp+"-"+pair+"-eval-features-sent.csv"));
		String distance;
		while ((distance = brSent.readLine()) != null) {
			sentDistance.add(Integer.parseInt(distance));
		}
		brSent.close();
		
		String combine = "concat";
		if (stack) combine = "stack";	
		
		BufferedWriter bwSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-"+combine+".txt"));
		BufferedWriter bwSigSame = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-same-"+combine+".txt"));
		BufferedWriter bwSigDiff = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-eval-diff-"+combine+".txt"));
		
		for (int i=0; i<eeClsTest.size(); i++) {
			if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSig.write("1 1 1\n");
			else bwSig.write("0 1 1\n");
		}
		bwSig.close();
		
		//Same sentence
		List<String> eeTestListSame = new ArrayList<String>();
		for (int i=0; i<eeClsTest.size(); i++) {
			if (sentDistance.get(i) == 0) {
				eeTestListSame.add("-"
						+ "\t" + "-"
						+ "\t" + "-"
						+ "\t" + evalFvList.get(i).getLabel()
						+ "\t" + eeClsTest.get(i));
				if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSigSame.write("1 1 1\n");
				else bwSigSame.write("0 1 1\n");
			}
		}
		PairEvaluator pees = new PairEvaluator(eeTestListSame);
		pees.evaluatePerLabel(arrLabel);
		bwSigSame.close();
		
		//Different sentence
		List<String> eeTestListDiff = new ArrayList<String>();
		for (int i=0; i<eeClsTest.size(); i++) {
			if (sentDistance.get(i) != 0) {
				eeTestListDiff.add("-"
						+ "\t" + "-"
						+ "\t" + "-"
						+ "\t" + evalFvList.get(i).getLabel()
						+ "\t" + eeClsTest.get(i));
				if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i))) bwSigDiff.write("1 1 1\n");
				else bwSigDiff.write("0 1 1\n");
			}
		}
		PairEvaluator peed = new PairEvaluator(eeTestListDiff);
		peed.evaluatePerLabel(arrLabel);
		bwSigDiff.close();
		
	}

}
