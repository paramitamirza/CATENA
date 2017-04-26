package catena.embedding.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
	
	public static void main(String [] args) throws Exception {
		
		String exp = "tbdense";
		String group = "0";
		int numFold = 10;
		boolean binary = true;
		
		String[] arrLabel = new String[0];
		
		List<Map<Integer, Integer>> idxListList = new ArrayList<Map<Integer, Integer>>();
		List<Map<Integer, Integer>> idxDctListList = new ArrayList<Map<Integer, Integer>>();
		
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(exp+"-stack", "logit");
		
		arrLabel = labelDense;
		idxListList = getEventEventDenseLabels("./data/embedding/"+exp+"-ee-train-labels-str.gr"+group+".csv", 
				numFold);
		idxDctListList = getEventEventDenseLabels("./data/embedding/"+exp+"-ed-train-labels-str.gr"+group+".csv", 
				numFold);
		
		List<List<PairFeatureVector>> fvListListC = 
				getEventEventTlinks("./data/embedding/"+exp+"-ee-train-features.csv", 
						idxListList, arrLabel);
		List<List<PairFeatureVector>> fvListList0 = 
				getEventEventTlinks("./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp0.csv", 
						idxListList, arrLabel);
		List<List<PairFeatureVector>> fvListList2 = 
				getEventEventTlinks("./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp2.csv", 
						idxListList, arrLabel);
		List<List<PairFeatureVector>> fvListList3 = 
				getEventEventTlinks("./data/embedding/"+exp+"-ee-train-embedding-word2vec-300.exp3.csv", 
						idxListList, arrLabel);
		
		List<List<PairFeatureVector>> fvDctListListC = 
				getEventEventTlinks("./data/embedding/"+exp+"-ed-train-features.csv", 
						idxDctListList, arrLabel);
		List<List<PairFeatureVector>> fvDctListList = 
				getEventEventTlinks("./data/embedding/"+exp+"-ed-train-embedding-word2vec-300.csv", 
						idxDctListList, arrLabel);
		
		BufferedWriter bwProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-probs.conv.csv"));
		BufferedWriter bwProbs0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-probs.exp0.csv"));
		BufferedWriter bwProbs2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-probs.exp2.csv"));
		BufferedWriter bwProbs3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-probs.exp3.csv"));
		BufferedWriter bwLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-probs-labels.csv"));
		BufferedWriter bwConv = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train-conv-features.csv"));
		
		BufferedWriter bwDctProbsC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train-probs.conv.csv"));
		BufferedWriter bwDctProbs = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train-probs.csv"));
		BufferedWriter bwDctLabels = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train-probs-labels.csv"));
		BufferedWriter bwDctConv = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train-conv-features.csv"));
		
		BufferedWriter bwSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train.conv.txt"));
		BufferedWriter bwSig0 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train.exp0.txt"));
		BufferedWriter bwSig2 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train.exp2.txt"));
		BufferedWriter bwSig3 = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ee-train.exp3.txt"));
		
		BufferedWriter bwDctSigC = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train.conv.txt"));
		BufferedWriter bwDctSig = new BufferedWriter(new FileWriter("./data/embedding/"+exp+"-ed-train.txt"));
		
		System.out.println(fvListList0.size() + "-" + fvListList3.size());
		
		for (int fold=0; fold<numFold; fold++) {
			
			System.err.println("Fold " + (fold+1) + "...");
			
			List<PairFeatureVector> evalFvListC = fvListListC.get(fold);
			List<PairFeatureVector> evalFvList0 = fvListList0.get(fold);
			List<PairFeatureVector> evalFvList2 = fvListList2.get(fold);
			List<PairFeatureVector> evalFvList3 = fvListList3.get(fold);
			
			List<PairFeatureVector> evalDctFvListC = fvDctListListC.get(fold);
			List<PairFeatureVector> evalDctFvList = fvDctListList.get(fold);
			
			List<PairFeatureVector> trainFvListC = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList0 = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList2 = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainFvList3 = new ArrayList<PairFeatureVector>();
			
			List<PairFeatureVector> trainDctFvListC = new ArrayList<PairFeatureVector>();
			List<PairFeatureVector> trainDctFvList = new ArrayList<PairFeatureVector>();
			
			for (int n=0; n<numFold; n++) {
				if (n != fold) {
					trainFvListC.addAll(fvListListC.get(n));
					trainFvList0.addAll(fvListList0.get(n));
					trainFvList2.addAll(fvListList2.get(n));
					trainFvList3.addAll(fvListList3.get(n));
					
					trainDctFvListC.addAll(fvDctListListC.get(n));
					trainDctFvList.addAll(fvDctListList.get(n));
				}
			}
			
			if (eeCls.classifier.equals(VectorClassifier.logit)) {
				eeCls.train2(trainFvListC, "models/" + eeCls.getName() + "-conv.model");
				eeCls.train2(trainFvList0, "models/" + eeCls.getName() + "-exp0.model");
				eeCls.train2(trainFvList2, "models/" + eeCls.getName() + "-exp2.model");
				eeCls.train2(trainFvList3, "models/" + eeCls.getName() + "-exp3.model");
				
				eeCls.train2(trainDctFvListC, "models/" + eeCls.getName() + "-dct-conv.model");
				eeCls.train2(trainDctFvList, "models/" + eeCls.getName() + "-dct.model");
				
				List<String> eeClsTestC = eeCls.predictProbs2(evalFvListC, "models/" + eeCls.getName() + "-conv.model", arrLabel);
				List<String> eeClsTest0 = eeCls.predictProbs2(evalFvList0, "models/" + eeCls.getName() + "-exp0.model", arrLabel);
				List<String> eeClsTest2 = eeCls.predictProbs2(evalFvList2, "models/" + eeCls.getName() + "-exp2.model", arrLabel);
				List<String> eeClsTest3 = eeCls.predictProbs2(evalFvList3, "models/" + eeCls.getName() + "-exp3.model", arrLabel);
				
				List<String> edClsTestC = eeCls.predictProbs2(evalDctFvListC, "models/" + eeCls.getName() + "-dct-conv.model", arrLabel);
				List<String> edClsTest = eeCls.predictProbs2(evalDctFvList, "models/" + eeCls.getName() + "-dct.model", arrLabel);
				
				List<String> eeTestListC = new ArrayList<String>();
				List<String> eeTestList0 = new ArrayList<String>();
				List<String> eeTestList2 = new ArrayList<String>();
				List<String> eeTestList3 = new ArrayList<String>();
				
				List<String> edTestListC = new ArrayList<String>();
				List<String> edTestList = new ArrayList<String>();
				
				for (PairFeatureVector fv : evalFvListC) {
					bwLabels.write(fv.getLabel() + "\n");
					bwConv.write(fv.toCSVString() + "\n");
				}
				for (int i=0; i<eeClsTestC.size(); i++) {
					eeTestListC.add("-"
							+ "\t" + "-"
							+ "\t" + "-"
							+ "\t" + evalFvListC.get(i).getLabel()
							+ "\t" + eeClsTestC.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(eeClsTestC.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwProbsC.write(discretizeProbs(eeClsTestC.get(i).split("#")[1]) + "\n");
						bwProbsC.write(eeClsTestC.get(i).split("#")[1] + "\n");
					}
					
					if (evalFvListC.get(i).getLabel().equals(eeClsTestC.get(i).split("#")[0])) bwSigC.write("1 1 1\n");
					else bwSigC.write("0 1 1\n");
				}
				for (int i=0; i<eeClsTest0.size(); i++) {
					eeTestList0.add("-"
							+ "\t" + "-"
							+ "\t" + "-"
							+ "\t" + evalFvList0.get(i).getLabel()
							+ "\t" + eeClsTest0.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(eeClsTest0.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwProbs0.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwProbs0.write(discretizeProbs(eeClsTest0.get(i).split("#")[1]) + "\n");
						bwProbs0.write(eeClsTest0.get(i).split("#")[1] + "\n");
					}
					
					if (evalFvList0.get(i).getLabel().equals(eeClsTest0.get(i).split("#")[0])) bwSig0.write("1 1 1\n");
					else bwSig0.write("0 1 1\n");
				}
				for (int i=0; i<eeClsTest2.size(); i++) {
					eeTestList2.add("-"
							+ "\t" + "-"
							+ "\t" + "-"
							+ "\t" + evalFvList2.get(i).getLabel()
							+ "\t" + eeClsTest2.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(eeClsTest2.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwProbs2.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwProbs2.write(discretizeProbs(eeClsTest2.get(i).split("#")[1]) + "\n");
						bwProbs2.write(eeClsTest2.get(i).split("#")[1] + "\n");
					}
					
					if (evalFvList2.get(i).getLabel().equals(eeClsTest2.get(i).split("#")[0])) bwSig2.write("1 1 1\n");
					else bwSig2.write("0 1 1\n");
				}
				for (int i=0; i<eeClsTest3.size(); i++) {
					eeTestList3.add("-"
							+ "\t" + "-"
							+ "\t" + "-"
							+ "\t" + evalFvList3.get(i).getLabel()
							+ "\t" + eeClsTest3.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(eeClsTest3.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwProbs3.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwProbs3.write(discretizeProbs(eeClsTest3.get(i).split("#")[1]) + "\n");
						bwProbs3.write(eeClsTest3.get(i).split("#")[1] + "\n");
					}
					
					if (evalFvList3.get(i).getLabel().equals(eeClsTest3.get(i).split("#")[0])) bwSig3.write("1 1 1\n");
					else bwSig3.write("0 1 1\n");
				}
				
				for (PairFeatureVector fv : evalDctFvListC) {
					bwDctLabels.write(fv.getLabel() + "\n");
					bwDctConv.write(fv.toCSVString() + "\n");
				}
				for (int i=0; i<edClsTestC.size(); i++) {
					edTestListC.add("-"
							+ "\t" + "-"
							+ "\t" + "-"
							+ "\t" + evalDctFvListC.get(i).getLabel()
							+ "\t" + edClsTestC.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(edClsTestC.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwDctProbsC.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwDctProbsC.write(discretizeProbs(edClsTestC.get(i).split("#")[1]) + "\n");
						bwDctProbsC.write(edClsTestC.get(i).split("#")[1] + "\n");
					}
					
					if (evalDctFvListC.get(i).getLabel().equals(edClsTestC.get(i).split("#")[0])) bwDctSigC.write("1 1 1\n");
					else bwDctSigC.write("0 1 1\n");
				}
				for (int i=0; i<edClsTest.size(); i++) {
					edTestList.add("-"
							+ "\t" + "-" 
							+ "\t" + "-"
							+ "\t" + evalDctFvList.get(i).getLabel()
							+ "\t" + edClsTest.get(i).split("#")[0]);
					if (binary) {
						String lineLabels = "";
						for (String l : arrLabel) {
							if (l.equals(edClsTest.get(i).split("#")[0])) lineLabels += "1,";
							else lineLabels += "0,";
						}
						bwDctProbs.write(lineLabels.substring(0, lineLabels.length()-1) + "\n");
					} else {
//						bwDctProbs.write(discretizeProbs(edClsTest.get(i).split("#")[1]) + "\n");
						bwDctProbs.write(edClsTest.get(i).split("#")[1] + "\n");
					}
					
					if (evalDctFvList.get(i).getLabel().equals(edClsTest.get(i).split("#")[0])) bwDctSig.write("1 1 1\n");
					else bwDctSig.write("0 1 1\n");
				}
				
				
				//Evaluate
				PairEvaluator peeC = new PairEvaluator(eeTestListC);
				peeC.evaluatePerLabel(arrLabel);
				PairEvaluator pee0 = new PairEvaluator(eeTestList0);
				pee0.evaluatePerLabel(arrLabel);
				PairEvaluator pee2 = new PairEvaluator(eeTestList2);
				pee2.evaluatePerLabel(arrLabel);
				PairEvaluator pee3 = new PairEvaluator(eeTestList3);
				pee3.evaluatePerLabel(arrLabel);
				
				PairEvaluator pedC = new PairEvaluator(edTestListC);
				pedC.evaluatePerLabel(arrLabel);
				PairEvaluator ped = new PairEvaluator(edTestList);
				ped.evaluatePerLabel(arrLabel);
				
			}
		}
		
		bwLabels.close();
		bwConv.close();
		bwProbsC.close();
		bwProbs0.close();
		bwProbs2.close();
		bwProbs3.close();
		
		bwDctLabels.close();
		bwDctConv.close();
		bwDctProbsC.close();
		bwDctProbs.close();
		
		bwSigC.close();
		bwSig0.close();
		bwSig2.close();
		bwSig3.close();
		
		bwDctSigC.close();
		bwDctSig.close();
		
	}

}
