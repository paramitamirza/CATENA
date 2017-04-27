package catena.embedding.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.model.classifier.PairClassifier.VectorClassifier;
import catena.model.feature.PairFeatureVector;
import catena.model.classifier.EventEventTemporalClassifier;

public class EvaluateTimeBankDenseCrossVal {
	
	private static String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	private static List<String> labelListDense = Arrays.asList(labelDense);
	
	public static List<Map<Integer,Integer>> getEventEventDenseLabels(String labelFilePath, int numFold) throws IOException {
		List<Map<Integer,Integer>> idxList = new ArrayList<Map<Integer,Integer>>();
		for(int i=0; i<numFold; i++) idxList.add(new HashMap<Integer, Integer>());
		
		List<Integer> idxListBefore = new ArrayList<Integer>();
		List<Integer> idxListAfter = new ArrayList<Integer>();
		List<Integer> idxListSimultaneous = new ArrayList<Integer>();
		List<Integer> idxListIncludes = new ArrayList<Integer>();
		List<Integer> idxListIsIncluded = new ArrayList<Integer>();
		List<Integer> idxListVague = new ArrayList<Integer>();
		
		System.setProperty("line.separator", "\n");
		BufferedReader brlbl = new BufferedReader(new FileReader(labelFilePath));
		String lbl; Integer idx = 0;
		while ((lbl = brlbl.readLine()) != null) {
			switch(lbl) {
	    		case "BEFORE": idxListBefore.add(idx); break;
	    		case "AFTER": idxListAfter.add(idx); break;
	    		case "SIMULTANEOUS": idxListSimultaneous.add(idx); break;
	    		case "INCLUDES": idxListIncludes.add(idx); break;
	    		case "IS_INCLUDED": idxListIsIncluded.add(idx); break;
	    		case "VAGUE": idxListVague.add(idx); break;
	    	}
			idx ++;
		}
		brlbl.close();
		
		Collections.shuffle(idxListBefore);
		Collections.shuffle(idxListAfter);
		Collections.shuffle(idxListSimultaneous);
		Collections.shuffle(idxListIncludes);
		Collections.shuffle(idxListIsIncluded);
		Collections.shuffle(idxListVague);
		
		int numBeforePerFold = (int)Math.floor(idxListBefore.size()/((double)numFold));
		int numAfterPerFold = (int)Math.floor(idxListAfter.size()/((double)numFold));
		int numSimultaneousPerFold = (int)Math.floor(idxListSimultaneous.size()/((double)numFold));
		int numIncludesPerFold = (int)Math.floor(idxListIncludes.size()/((double)numFold));
		int numIsIncludedPerFold = (int)Math.floor(idxListIsIncluded.size()/((double)numFold));
		int numVaguePerFold = (int)Math.floor(idxListVague.size()/((double)numFold));
		
		int idxBefore = 0, idxAfter = 0, idxSimultaneous = 0, idxIncludes = 0, idxIsIncluded = 0, idxVague = 0;
		for (int i=0; i<numFold; i++) {
			for (int j=0; j<numBeforePerFold; j++) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelListDense.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			for (int j=0; j<numAfterPerFold; j++) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelListDense.indexOf("AFTER")+1);
				idxAfter ++;
			}
			for (int j=0; j<numSimultaneousPerFold; j++) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelListDense.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			for (int j=0; j<numIncludesPerFold; j++) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelListDense.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			for (int j=0; j<numIsIncludedPerFold; j++) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelListDense.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			for (int j=0; j<numVaguePerFold; j++) {
				idxList.get(i).put(idxListVague.get(idxVague), labelListDense.indexOf("VAGUE")+1);
				idxVague ++;
			}
		}
		for (int i=0; i<numFold; i++) {
			if (idxBefore < idxListBefore.size()) {
				idxList.get(i).put(idxListBefore.get(idxBefore), labelListDense.indexOf("BEFORE")+1);
				idxBefore ++;
			}
			if (idxAfter < idxListAfter.size()) {
				idxList.get(i).put(idxListAfter.get(idxAfter), labelListDense.indexOf("AFTER")+1);
				idxAfter ++;
			}
			if (idxSimultaneous < idxListSimultaneous.size()) {
				idxList.get(i).put(idxListSimultaneous.get(idxSimultaneous), labelListDense.indexOf("SIMULTANEOUS")+1);
				idxSimultaneous ++;
			}
			if (idxIncludes < idxListIncludes.size()) {
				idxList.get(i).put(idxListIncludes.get(idxIncludes), labelListDense.indexOf("INCLUDES")+1);
				idxIncludes ++;
			}
			if (idxIsIncluded < idxListIsIncluded.size()) {
				idxList.get(i).put(idxListIsIncluded.get(idxIsIncluded), labelListDense.indexOf("IS_INCLUDED")+1);
				idxIsIncluded ++;
			}
			if (idxVague < idxListVague.size()) {
				idxList.get(i).put(idxListVague.get(idxVague), labelListDense.indexOf("VAGUE")+1);
				idxVague ++;
			}
		}
		
		return idxList;
	}
	
	public static List<List<PairFeatureVector>> getEventEventTlinks(String embeddingFilePath, List<Map<Integer,Integer>> idxList,
			String[] arrLabel) throws Exception {
		
		List<List<PairFeatureVector>> fvList = new ArrayList<List<PairFeatureVector>>();
		for(int i=0; i<idxList.size(); i++) fvList.add(new ArrayList<PairFeatureVector>());
		
		List<String> fvListAll = new ArrayList<String>();
		System.setProperty("line.separator", "\n");
		BufferedReader bremb = new BufferedReader(new FileReader(embeddingFilePath));
		String line;
		while ((line = bremb.readLine()) != null) {
			fvListAll.add(line.trim());
		}
		bremb.close();
		
		int numCols; String lbl;
		for (int i=0; i<idxList.size(); i++) {
			for (Integer idx : idxList.get(i).keySet()) {
				numCols = fvListAll.get(idx).split(",").length;
				lbl = arrLabel[idxList.get(i).get(idx)-1];
				PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
		    			null, null);
				int col=0;
				for (String s : fvListAll.get(idx).split(",")) {
		    		fv.getFeatures()[col] = Double.parseDouble(s);
		    		col++;
		    	}		    	
		    	fv.getFeatures()[col] = (double) idxList.get(i).get(idx);
		    	fvList.get(i).add(fv);
			}
		}
		
		return fvList;
	}
	
	public static List<List<PairFeatureVector>> getEventEventTlinksSub(String embeddingFilePath, int embeddingDimension, List<Map<Integer,Integer>> idxList,
			String[] arrLabel) throws Exception {
		
		List<List<PairFeatureVector>> fvList = new ArrayList<List<PairFeatureVector>>();
		for(int i=0; i<idxList.size(); i++) fvList.add(new ArrayList<PairFeatureVector>());
		
		List<String> fvListAll = new ArrayList<String>();
		System.setProperty("line.separator", "\n");
		BufferedReader bremb = new BufferedReader(new FileReader(embeddingFilePath));
		String line;
		while ((line = bremb.readLine()) != null) {
			fvListAll.add(line.trim());
		}
		bremb.close();
		
		int numCols; String lbl;
		for (int i=0; i<idxList.size(); i++) {
			for (Integer idx : idxList.get(i).keySet()) {
				numCols = fvListAll.get(idx).split(",").length / 2;
				lbl = arrLabel[idxList.get(i).get(idx)-1];
				PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
		    			null, null);
				int col=0;
				String[] emb = fvListAll.get(idx).split(",");
				for (int j=0; j<embeddingDimension; j++) {
					fv.getFeatures()[col] = Double.parseDouble(emb[j+embeddingDimension]) - Double.parseDouble(emb[j]);	//w2 - w1
					col++;
				}
				fv.getFeatures()[col] = (double) idxList.get(i).get(idx);
		    	fvList.get(i).add(fv);
			}
		}
		
		return fvList;
	}
	
	public static List<List<PairFeatureVector>> getEventEventTlinksSum(String embeddingFilePath, int embeddingDimension, List<Map<Integer,Integer>> idxList,
			String[] arrLabel) throws Exception {
		
		List<List<PairFeatureVector>> fvList = new ArrayList<List<PairFeatureVector>>();
		for(int i=0; i<idxList.size(); i++) fvList.add(new ArrayList<PairFeatureVector>());
		
		List<String> fvListAll = new ArrayList<String>();
		System.setProperty("line.separator", "\n");
		BufferedReader bremb = new BufferedReader(new FileReader(embeddingFilePath));
		String line;
		while ((line = bremb.readLine()) != null) {
			fvListAll.add(line.trim());
		}
		bremb.close();
		
		int numCols; String lbl;
		for (int i=0; i<idxList.size(); i++) {
			for (Integer idx : idxList.get(i).keySet()) {
				numCols = fvListAll.get(idx).split(",").length / 2;
				lbl = arrLabel[idxList.get(i).get(idx)-1];
				PairFeatureVector fv = new PairFeatureVector(null, null, null, lbl, numCols+1, 
		    			null, null);
				int col=0;
				String[] emb = fvListAll.get(idx).split(",");
				for (int j=0; j<embeddingDimension; j++) {
					fv.getFeatures()[col] = Double.parseDouble(emb[j]) + Double.parseDouble(emb[j+embeddingDimension]);	//w1 + w2
					col++;
				}
		    	fv.getFeatures()[col] = (double) idxList.get(i).get(idx);
		    	fvList.get(i).add(fv);
			}
		}
		
		return fvList;
	}
	
	public static void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public static void runExperiment(String exp, String pair, String feature,
			int numFold, boolean binary,
			List<List<PairFeatureVector>> fvListList) throws Exception {
		
		System.out.println();
		System.out.println("******* EXPERIMENT " + feature + " *******");
		
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(exp+"-stack", "logit");
		
		ensureDirectory(new File("./data/embedding/temporary/"));
		BufferedWriter bwProbs = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-train-probs."+feature+".csv"));
		BufferedWriter bwLabels = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-train-labels."+feature+".csv"));
		BufferedWriter bwFeature = new BufferedWriter(new FileWriter("./data/embedding/temporary/"+exp+"-"+pair+"-train."+feature+".csv"));
		
		// For running significance text
//		BufferedWriter bwSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-"+pair+"-train."+feature+".txt"));
		
		for (int fold=0; fold<numFold; fold++) {
			
			List<PairFeatureVector> evalFvList = fvListList.get(fold);
			
			List<PairFeatureVector> trainFvList = new ArrayList<PairFeatureVector>();
			
			for (int n=0; n<numFold; n++) {
				if (n != fold) {
					trainFvList.addAll(fvListList.get(n));
				}
			}
			
			eeCls.train2(trainFvList, "models/" + eeCls.getName() + "-"+pair+"-"+feature+".model");
			
			List<String> eeClsTest = eeCls.predictProbs2(evalFvList, "models/" + eeCls.getName() + "-"+pair+"-"+feature+".model", labelDense);
			List<String> eeTestList = new ArrayList<String>();
			
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
//					bwProbs.write(discretizeProbs(eeClsTest.get(i).split("#")[1]) + "\n");
					bwProbs.write(eeClsTest.get(i).split("#")[1] + "\n");
				}
				
//				if (evalFvList.get(i).getLabel().equals(eeClsTest.get(i).split("#")[0])) bwSig.write("1 1 1\n");
//				else bwSig.write("0 1 1\n");
			}
			
			//Evaluate
			System.out.println();
			System.out.println("********** FOLD " + (fold+1) + " **********");
			PairEvaluator peeC = new PairEvaluator(eeTestList);
			peeC.evaluatePerLabel(labelDense);
			System.out.println();
		}
		
		bwLabels.close();
		bwFeature.close();
		bwProbs.close();
//		bwSig.close();
	}
	
	public static void main(String [] args) throws Exception {
		
		String exp = "tbdense";
		String pair = "ed";			//ee for event-event, ed for event-dct
		String group = "0";
		int numFold = 10;			//number of fold for cross validation
		boolean binary = true;		//how to write probability for stack learning, binary=[0,1]
		
		// conv for conventional features, 
		// concat for embeddings concatenated, 
		// sub for embeddings subtracted, 
		// sum for embeddings summed
		
		List<Map<Integer, Integer>> idxListList = getEventEventDenseLabels("./data/embedding/"+exp+"-"+pair+"-train-labels-str.gr"+group+".csv", 
				numFold);		
		List<List<PairFeatureVector>> fvListListConv = getEventEventTlinks("./data/embedding/"+exp+"-"+pair+"-train-features.csv", 
						idxListList, labelDense);
		List<List<PairFeatureVector>> fvListListConcat = getEventEventTlinks("./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv", 
						idxListList, labelDense);
		
		runExperiment(exp, pair, "conv", numFold, binary, fvListListConv);
		runExperiment(exp, pair, "concat", numFold, binary, fvListListConcat);
		
		if (pair.equals("ee")) {
			List<List<PairFeatureVector>> fvListListSub = getEventEventTlinksSub("./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv", 300, 
							idxListList, labelDense);
			List<List<PairFeatureVector>> fvListListSum = getEventEventTlinksSum("./data/embedding/"+exp+"-"+pair+"-train-embedding-word2vec-300.csv", 300, 
							idxListList, labelDense);
			
			runExperiment(exp, pair, "sub", numFold, binary, fvListListSub);
			runExperiment(exp, pair, "sum", numFold, binary, fvListListSum);
		}	
		
		
	}

}
