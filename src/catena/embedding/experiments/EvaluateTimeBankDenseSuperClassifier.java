package catena.embedding.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import catena.model.classifier.EventEventTemporalClassifier;
import catena.evaluator.PairEvaluator;
import catena.model.feature.PairFeatureVector;

public class EvaluateTimeBankDenseSuperClassifier {
	
	private static String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	private static List<String> labelListDense = Arrays.asList(labelDense);
	
	public static List<PairFeatureVector> getEventEventTlinks(String filePath, String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((lbl = brlbl.readLine()) != null) {
			line = br.readLine().trim();
			numCols = line.split(",").length;
	    	
	    	PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
	    			null, null);
	    	int i=0;
	    	for (String s : line.split(",")) {
//	    		fv.getVectors().add(s);
	    		fv.getFeatures()[i] = Double.parseDouble(s);
	    		i++;
	    	}
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;

	    	fvList.add(fv);
		}
		
		brlbl.close();
		br.close();
		
		return fvList;
	}
	
	public static List<PairFeatureVector> getEventEventTlinksSub(String filePath, int embeddingDimension,
			String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((lbl = brlbl.readLine()) != null) {
			line = br.readLine().trim();
			numCols = line.split(",").length / 2;
	    	
	    	PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
	    			null, null);
	    	int i=0;
	    	String[] emb = line.split(",");
			for (int j=0; j<embeddingDimension; j++) {
				fv.getFeatures()[i] = Double.parseDouble(emb[j+embeddingDimension]) - Double.parseDouble(emb[j]);	//w2 - w1
				i++;
			}
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;
	    	
	    	fvList.add(fv);
		}
		
		brlbl.close();
		br.close();
		
		return fvList;
	}
	
	public static List<PairFeatureVector> getEventEventTlinksSum(String filePath, int embeddingDimension, 
			String labelFilePath,
			String[] arrLabel) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		List<String> listLabel = Arrays.asList(arrLabel);
		
		System.setProperty("line.separator", "\n");
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String line, lbl;
		int numCols;
		while ((lbl = brlbl.readLine()) != null) {
			line = br.readLine().trim();
			numCols = line.split(",").length / 2;
	    	
	    	PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
	    			null, null);
	    	int i=0;
	    	String[] emb = line.split(",");
			for (int j=0; j<embeddingDimension; j++) {
				fv.getFeatures()[i] = Double.parseDouble(emb[j+embeddingDimension]) + Double.parseDouble(emb[j]);	//w2 - w1
				i++;
			}
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;
	    	
	    	fvList.add(fv);
		}
		
		brlbl.close();
		br.close();
		
		return fvList;
	}
	
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
	    		fv.getFeatures()[i] = Double.parseDouble(s);
	    		i++;
	    	}
	    	fv.getFeatures()[i] = (double) listLabel.indexOf(lbl)+1;
	    	
	    	fvList.add(fv);
		}
		
		brlbl.close();
		for (BufferedReader br : brList) {
			br.close();
		}
		
		return fvList;
	}
	
	public static void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public static void runExperimentNormal(String exp, String pair, String feature, boolean binary,
			List<PairFeatureVector> trainFvList, List<PairFeatureVector> evalFvList,
			List<Integer> sentDistance) throws Exception {
		
		System.out.println();
		System.out.println("******* EXPERIMENT " + feature + " *******");
		
		EventEventTemporalClassifier cls = new EventEventTemporalClassifier(exp+"-"+pair+"-super", "logit");
		
		List<String> eeClsTest = new ArrayList<String>();
		List<String> eeTestList = new ArrayList<String>();
		
		cls.train2(trainFvList, "models/" + cls.getName() + "-"+pair+"-"+feature+"-normal.model");
		eeClsTest = cls.predictProbs2(evalFvList, "models/" + cls.getName() + "-"+pair+"-"+feature+"-normal.model", labelDense);
		
		ensureDirectory(new File("./data/embedding/temporary/"));
		BufferedWriter bwFeature = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-eval."+feature+".csv"));
		BufferedWriter bwLabels = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-eval-labels.csv"));
		BufferedWriter bwProbs = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-eval-probs."+feature+".csv"));
		
		for (PairFeatureVector fv : evalFvList) {
			bwLabels.write(fv.getLabel() + "\n");
			bwFeature.write(fv.toCSVString() + "\n");
		}
		
		for (int i=0; i<eeClsTest.size(); i++) {
			eeTestList.add("-"
					+ "\t" + "-"
					+ "\t" + "-"
					+ "\t" + evalFvList.get(i).getLabel()
					+ "\t" + eeClsTest.get(i).split("#")[0]);
			if (binary) {
				String lineLabels = "";
				for (String l : labelDense) {
					if (l.equals(eeClsTest.get(i).split("#")[0])) lineLabels += "1,";
					else lineLabels += "0,";
				}
				bwProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
			} else {
//				bwProbs.write(discretizeProbs(eeClsTest.get(i).split("#")[1]) + "\n");
				bwProbs.write(eeClsTest.get(i).split("#")[1] + "\n");
			}
			
//			if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i).split("#")[0])) bwSig.write("1 1 1\n");
//			else bwSig.write("0 1 1\n");
		}
		
		//Evaluate
		System.out.println();
		System.out.println("********** EXPERIMENT RESULT **********");
		PairEvaluator pees = new PairEvaluator(eeTestList);
		pees.evaluatePerLabel(labelDense);
		
		if (pair.equals("ee")) {
			//Same sentence
			List<String> eeTestListSame = new ArrayList<String>();
			for (int i=0; i<eeClsTest.size(); i++) {
				if (sentDistance.get(i) == 0) {
					eeTestListSame.add("-"
							+ "\t" + "-" 
							+ "\t" + "-"
							+ "\t" + evalFvList.get(i).getLabel()
							+ "\t" + eeClsTest.get(i).split("#")[0]);
				}
			}
			
			System.out.println("******* SAME SENTENCE *******");
			pees = new PairEvaluator(eeTestListSame);
			pees.evaluatePerLabel(labelDense);
			
			//Different sentence
			List<String> eeTestListDiff = new ArrayList<String>();
			for (int i=0; i<eeClsTest.size(); i++) {
				if (sentDistance.get(i) != 0) {
					eeTestListDiff.add("-"
							+ "\t" + "-" 
							+ "\t" + "-"
							+ "\t" + evalFvList.get(i).getLabel()
							+ "\t" + eeClsTest.get(i).split("#")[0]);
				}
			}
			System.out.println("******* DIFF SENTENCE *******");
			pees = new PairEvaluator(eeTestListDiff);
			pees.evaluatePerLabel(labelDense);
		}
		
		bwFeature.close();
		bwLabels.close();
		bwProbs.close();
	}
	
	public static void runExperiment(String exp, String pair, String combine, 
			List<PairFeatureVector> trainFvList, List<PairFeatureVector> evalFvList,
			List<Integer> sentDistance) throws Exception {
		
		System.out.println();
		System.out.println("******* EXPERIMENT COMBINE ("+combine+") *******");
		
		EventEventTemporalClassifier cls = new EventEventTemporalClassifier(exp+"-"+pair+"-super", "logit");
		
		List<String> eeClsTest = new ArrayList<String>();
		List<String> eeTestList = new ArrayList<String>();
		
		cls.train2(trainFvList, "models/" + cls.getName() + ".model");
		eeClsTest = cls.predict2(evalFvList, "models/" + cls.getName() + ".model", labelDense);
				
		for (int i=0; i<eeClsTest.size(); i++) {
			eeTestList.add("-"
					+ "\t" + "-"
					+ "\t" + "-"
					+ "\t" + evalFvList.get(i).getLabel()
					+ "\t" + eeClsTest.get(i).split("#")[0]);
		}
		
		PairEvaluator pees = new PairEvaluator(eeTestList);
		pees.evaluatePerLabel(labelDense);
		
		if (pair.equals("ee")) {
			//Same sentence
			List<String> eeTestListSame = new ArrayList<String>();
			for (int i=0; i<eeClsTest.size(); i++) {
				if (sentDistance.get(i) == 0) {
					eeTestListSame.add("-"
							+ "\t" + "-" 
							+ "\t" + "-"
							+ "\t" + evalFvList.get(i).getLabel()
							+ "\t" + eeClsTest.get(i));
				}
			}
			
			System.out.println("******* SAME SENTENCE *******");
			pees = new PairEvaluator(eeTestListSame);
			pees.evaluatePerLabel(labelDense);
			
			//Different sentence
			List<String> eeTestListDiff = new ArrayList<String>();
			for (int i=0; i<eeClsTest.size(); i++) {
				if (sentDistance.get(i) != 0) {
					eeTestListDiff.add("-"
							+ "\t" + "-" 
							+ "\t" + "-"
							+ "\t" + evalFvList.get(i).getLabel()
							+ "\t" + eeClsTest.get(i));
				}
			}
			System.out.println("******* DIFF SENTENCE *******");
			pees = new PairEvaluator(eeTestListDiff);
			pees.evaluatePerLabel(labelDense);
		}
	}
	
	public static void main(String [] args) throws Exception {
		
		String exp = "tbdense";
		String pair = "ee";
		boolean binary = true;			//how to write probability for stack learning, binary=[0,1]
		
		List<Integer> sentDistance = new ArrayList<Integer>();
		if (pair.equals("ee")) {
			BufferedReader brSent = new BufferedReader(new FileReader("./data/embedding/"+exp+"-"+pair+"-eval-features-sent.csv"));
			String distance;
			while ((distance = brSent.readLine()) != null) {
				sentDistance.add(Integer.parseInt(distance));
			}
			brSent.close();
		}
		
		// Run experiments of individual feature sets, i.e., conventional feature (conv), 
		// concatenated embeddings (concat), subtracted embeddings (sub) and
		// summed embeddings (sum)
		
		List<PairFeatureVector> trainFvListConv = getEventEventTlinks(
				"./data/embedding/"+exp+"-"+pair+"-train-features.csv",
				"./data/embedding/"+exp+"-"+pair+"-train-labels-str.csv",
				labelDense);
		List<PairFeatureVector> evalFvListConv = getEventEventTlinks(
				"./data/embedding/"+exp+"-"+pair+"-eval-features.csv",
				"./data/embedding/"+exp+"-"+pair+"-eval-labels-str.csv",
				labelDense);
		
		List<PairFeatureVector> trainFvListConcat = getEventEventTlinks(
				"./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv",
				"./data/embedding/"+exp+"-"+pair+"-train-labels-str.csv",
				labelDense);
		List<PairFeatureVector> evalFvListConcat = getEventEventTlinks(
				"./data/embedding/"+exp+"-"+pair+"-eval-embedding-word2vec-300.csv",
				"./data/embedding/"+exp+"-"+pair+"-eval-labels-str.csv",
				labelDense);
		
		runExperimentNormal(exp, pair, "conv", binary, trainFvListConv, evalFvListConv, sentDistance);
		runExperimentNormal(exp, pair, "concat", binary, trainFvListConcat, evalFvListConcat, sentDistance);
		
		if (pair.equals("ee")) {
			List<PairFeatureVector> trainFvListSub = getEventEventTlinksSub(
					"./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv", 300,
					"./data/embedding/"+exp+"-"+pair+"-train-labels-str.csv",
					labelDense);
			List<PairFeatureVector> evalFvListSub = getEventEventTlinksSub(
					"./data/embedding/"+exp+"-"+pair+"-eval-embedding-word2vec-300.csv", 300,
					"./data/embedding/"+exp+"-"+pair+"-eval-labels-str.csv",
					labelDense);
			
			List<PairFeatureVector> trainFvListSum = getEventEventTlinksSum(
					"./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv", 300,
					"./data/embedding/"+exp+"-"+pair+"-train-labels-str.csv",
					labelDense);
			List<PairFeatureVector> evalFvListSum = getEventEventTlinksSum(
					"./data/embedding/"+exp+"-"+pair+"-eval-embedding-word2vec-300.csv", 300,
					"./data/embedding/"+exp+"-"+pair+"-eval-labels-str.csv",
					labelDense);
			
			runExperimentNormal(exp, pair, "sub", binary, trainFvListSub, evalFvListSub, sentDistance);
			runExperimentNormal(exp, pair, "sum", binary, trainFvListSum, evalFvListSum, sentDistance);
		}
		
		// Run experiments of combining traditional feature sets with embeddings, 
		// stack = in stack learning setting, concat = simple concatenation
		
		List<PairFeatureVector> trainFvList;
		List<PairFeatureVector> evalFvList;
		
		String[] trainFileListConcat = {
				"./data/embedding/temporary/"+exp+"-"+pair+"-train.conv.csv",
				"./data/embedding/temporary/"+exp+"-"+pair+"-train.concat.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-train.sub.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-train.sum.csv",
				};
		trainFvList = 
				getEventEventTlinks(trainFileListConcat, 
						"./data/embedding/temporary/"+exp+"-"+pair+"-train-labels.conv.csv",
						labelDense);
		
		String[] evalFileListConcat = {
				"./data/embedding/temporary/"+exp+"-"+pair+"-eval.conv.csv",
                "./data/embedding/temporary/"+exp+"-"+pair+"-eval.concat.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-eval.sub.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-eval.sum.csv",
				};
		evalFvList = 
				getEventEventTlinks(evalFileListConcat, 
						"./data/embedding/temporary/"+exp+"-"+pair+"-eval-labels.csv",
						labelDense);
		
		runExperiment(exp, pair, "concat", trainFvList, evalFvList, sentDistance);
			
		String[] trainFileListStack = {
				"./data/embedding/temporary/"+exp+"-"+pair+"-train.conv.csv",
				"./data/embedding/temporary/"+exp+"-"+pair+"-train-probs.concat.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-train.sub.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-train.sum.csv",
				};
		trainFvList = 
				getEventEventTlinks(trainFileListStack, 
						"./data/embedding/temporary/"+exp+"-"+pair+"-train-labels.conv.csv",
						labelDense);
		
		String[] evalFileListStack = {
				"./data/embedding/temporary/"+exp+"-"+pair+"-eval.conv.csv",
                "./data/embedding/temporary/"+exp+"-"+pair+"-eval-probs.concat.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-eval.sub.csv",
//		            "./data/embedding/temporary/"+exp+"-"+pair+"-eval.sum.csv",
				};
		evalFvList = 
				getEventEventTlinks(evalFileListStack, 
						"./data/embedding/temporary/"+exp+"-"+pair+"-eval-labels.csv",
						labelDense);
		
		runExperiment(exp, pair, "stack", trainFvList, evalFvList, sentDistance);
		
		
	}

}
