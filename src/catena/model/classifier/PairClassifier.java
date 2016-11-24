package catena.model.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bwaldvogel.liblinear.Predict;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import catena.evaluator.PairEvaluator;
import catena.evaluator.TempEval3;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;

public class PairClassifier {
	
	protected String name;
	
	protected PairType pairType;
	
	protected TemporalSignalList tsignalList;
	protected CausalSignalList csignalList;
	
	protected String dataDirPath;
	
	public static enum VectorClassifier {yamcha, libsvm, weka, liblinear, logit, none};
	public VectorClassifier classifier;
	protected Classifier wekaClassifier;
	
	public List<String> featureNames;
	public List<FeatureName> featureList;
	public int featureVecLen;
	
	private boolean includeInconsistent;
	private List<String> inconsistentFiles;
	
	protected static enum FeatureType {conventional, wordEmbed, 
		wordEmbedConv, phraseEmbed, phraseEmbedConv, convProb, oneHotConv};
	protected FeatureType featureType;
	
	/** for word embedding experiments **/
	protected int labelGrouping = 0;
	protected String probVectorFile = "";
	
	private void initSignal() throws Exception {
		tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		csignalList = new CausalSignalList(EntityEnum.Language.EN);
	}
	
	private void initClassifier() {		
		if (classifier.equals(VectorClassifier.weka)) {
//			wekaClassifier = new LibSVM();
			wekaClassifier = new RandomForest();
		}
	}
	
	private void ensureDataDirectory() {
		if (classifier.equals(VectorClassifier.none)) {
			dataDirPath = "data/tokens/";
		} else if (classifier.equals(VectorClassifier.yamcha)) {
			dataDirPath = "data/yamcha/";
		} else if (classifier.equals(VectorClassifier.libsvm)
				|| classifier.equals(VectorClassifier.liblinear)
				|| classifier.equals(VectorClassifier.logit)
				) {
			dataDirPath = "data/libsvm/";
		} else if (classifier.equals(VectorClassifier.weka)) {
			dataDirPath = "data/weka/";
		}
		File dir = new File(dataDirPath);
		if (!dir.exists()) dir.mkdir();
	}
	
	private void initInconsistentFiles() throws Exception {
		inconsistentFiles = new ArrayList<String>();
		String inconsistentLog = "data/inconsistent.txt";
		BufferedReader br = new BufferedReader(new FileReader(inconsistentLog));
		String line;
		while ((line = br.readLine()) != null) {
			inconsistentFiles.add(line);
		}
		br.close();
	}
	
	private boolean isConsistent(String txpFilename) {
		String tmlFilename = txpFilename.replace(".txp", "");
		if (!inconsistentFiles.contains(tmlFilename)) return true;
		else return false;
	}
	
	public PairClassifier(String taskName, String cls) throws Exception {
		setName(taskName);		
		
		featureNames = new ArrayList<String>();	
		featureType = FeatureType.conventional;
		
		inconsistentFiles = new ArrayList<String>();
		
		switch (cls) {
			case "yamcha": classifier = VectorClassifier.yamcha; break;
			case "libsvm": classifier = VectorClassifier.libsvm; break;
			case "liblinear": classifier = VectorClassifier.liblinear; break;
			case "logit": classifier = VectorClassifier.logit; break;
			case "weka": classifier = VectorClassifier.weka; break;
			default: classifier = VectorClassifier.none; break;
		}
		initClassifier();
		initSignal();
		ensureDataDirectory();		
	}
	
	public PairClassifier(String taskName, String cls, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		this(taskName, cls);
		
		switch (feature) {
			case "conv": featureType = FeatureType.conventional; break;
			case "wembed" : featureType = FeatureType.wordEmbed; break;
			case "wembedconv" : 
				featureType = FeatureType.wordEmbedConv; 
				labelGrouping = Integer.valueOf(lblGrouping); 
				probVectorFile = probVecFile;
				break;
			case "pembed" : featureType = FeatureType.phraseEmbed; break;
			case "pembedconv" : 
				featureType = FeatureType.phraseEmbedConv; 
				labelGrouping = Integer.valueOf(lblGrouping); 
				probVectorFile = probVecFile;
				break;
			case "convprob": 
				featureType = FeatureType.convProb; 
				labelGrouping = Integer.valueOf(lblGrouping); 
				probVectorFile = probVecFile;
				break;
			case "onehotconv":
				featureType = FeatureType.oneHotConv;
				labelGrouping = Integer.valueOf(lblGrouping); 
				probVectorFile = probVecFile;
		}
	}
	
	public PairClassifier(String taskName, String cls, String inconsistency) throws Exception {
		this(taskName, cls);
		
		switch (inconsistency) {
			case "include": includeInconsistent = true; break;
			case "exclude": includeInconsistent = false; break;
		}		
		if (includeInconsistent) {
			inconsistentFiles = new ArrayList<String>();
		} else {
			initInconsistentFiles();
		}
	}
	
	public PairClassifier(String taskName, String cls, String feature, 
			String lblGrouping, String probVecFile,
			String inconsistency) throws Exception {
		this(taskName, cls);
		
		switch (feature) {
			case "conv": featureType = FeatureType.conventional; break;
			case "wembed" : featureType = FeatureType.wordEmbed; break;
			case "wembedconv" : featureType = FeatureType.wordEmbedConv; break;
			case "pembed" : featureType = FeatureType.phraseEmbed; break;
			case "pembedconv" : featureType = FeatureType.phraseEmbedConv; break;
			case "convprob": 
				featureType = FeatureType.convProb; 
				labelGrouping = Integer.valueOf(lblGrouping); 
				probVectorFile = probVecFile;
				break;
		}
		
		switch (inconsistency) {
			case "include": includeInconsistent = true; break;
			case "exclude": includeInconsistent = false; break;
		}		
		if (includeInconsistent) {
			inconsistentFiles = new ArrayList<String>();
		} else {
			initInconsistentFiles();
		}
	}
	
	public String getFeatureTypeString() {
		switch(featureType) {
			case conventional : return "conv";
			case wordEmbed : return "wembed";
			case wordEmbedConv : return "wembedconv";
			case phraseEmbed : return "pembed";
			case phraseEmbedConv : return "pembedconv";
			case convProb : return "convprob";
			default : return "";
		}
	}
	
	public String getPairTypeString() {
		switch(pairType) {
			case event_event : return "ee";
			case event_timex : return "et";
			case timex_timex : return "tt";
			default : return "";
		}
	}
	
//	public void train(List<PairFeatureVector> vectors) throws Exception {
//		
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		System.err.println("Train models...");
//		
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, true);
//			trainModels(filepath);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, true);
//			trainModels(rs, filepath);
//			rs.disconnect();
//		}
//	}
	
//	public void train(List<PairFeatureVector> vectors, boolean labelProbs) throws Exception {
//		
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		System.err.println("Train models...");
//		
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, true);
//			trainModels(filepath, labelProbs);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, true);
//			trainModels(rs, filepath);
//			rs.disconnect();
//		}
//	}
	
	private void trainModels(String filepath) throws Exception {
		String weight = "";
		if (pairType == PairType.event_event) {
			weight = "-w1 34 -w2 22 -w5 14 -w6 10 -w7 9 -w8 5";
		} else if (pairType == PairType.event_timex) {
			weight = "-w1 298 -w2 61 -w6 11 -w7 130 -w8 421 -w9 34 -w10 4 -w11 4 -w12 13 -w13 12 -w14 10";
		}
		
		if (classifier.equals(VectorClassifier.liblinear)) {	//Train models using LibLINEAR
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\train.exe");
			command.add("-s"); command.add("7");
			command.add("-c"); command.add("1.0");
			command.add("-e"); command.add("0.01");
			command.add("-B"); command.add("1.0");
			//for causality
			//command.add("-w3"); command.add("100");
			///////////////
			command.add(filepath + ".libsvm");
			command.add("models/" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			
		} else if (classifier.equals(VectorClassifier.libsvm)) {	//Train models using LibSVM
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-train.exe");
			command.add("-s"); command.add("0");
			command.add("-t"); command.add("2");	//kernel_type, 0:linear, 1:polynomial, 2:RBF
			command.add("-d"); command.add("2");	//degree
			command.add("-g"); command.add("0.0");
			command.add("-r"); command.add("0.0");
			command.add("-c"); command.add("1");
			command.add("-n"); command.add("0.5");
			command.add("-p"); command.add("0.1");
			command.add("-m"); command.add("128");
			command.add("-e"); command.add("0.001");
			command.add(filepath + ".libsvm");
			command.add("models/" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			
		} else if (classifier.equals(VectorClassifier.weka)) {	//Train models using Weka
			Instances train = new DataSource(filepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1); 
		}
	}
	
	private void trainModels(String filepath, boolean labelProbs) throws Exception {
		String weight = "";
		if (pairType == PairType.event_event) {
			weight = "-w1 34 -w2 22 -w5 14 -w6 10 -w7 9 -w8 5";
		} else if (pairType == PairType.event_timex) {
			weight = "-w1 298 -w2 61 -w6 11 -w7 130 -w8 421 -w9 34 -w10 4 -w11 4 -w12 13 -w13 12 -w14 10";
		}
		
		if (classifier.equals(VectorClassifier.liblinear)) {	//Train models using LibLINEAR
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\train.exe");
			command.add("-s"); 
			if (labelProbs) command.add("7");
			else command.add("1");
			command.add("-c"); command.add("1.0");
			command.add("-e"); command.add("0.01");
			command.add("-B"); command.add("1.0");
			command.add(filepath + ".libsvm");
			command.add("models/" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			
		} else if (classifier.equals(VectorClassifier.libsvm)) {	//Train models using LibSVM
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-train.exe");
			command.add("-s"); command.add("0");
			command.add("-t"); command.add("1");	//kernel_type, 0:linear, 1:polynomial, 2:RBF
			command.add("-d"); command.add("4");	//degree
			command.add("-g"); command.add("0.0");
			command.add("-r"); command.add("0.0");
			command.add("-c"); command.add("1");
			command.add("-n"); command.add("0.5");
			command.add("-p"); command.add("0.1");
			command.add("-m"); command.add("128");
			command.add("-e"); command.add("0.001");
			if (labelProbs) { 
				command.add("-b"); command.add("1");
			}
			command.add(filepath + ".libsvm");
			command.add("models/" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			
		} else if (classifier.equals(VectorClassifier.weka)) {	//Train models using Weka
			Instances train = new DataSource(filepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1); 
			
		} else if (classifier.equals(VectorClassifier.yamcha)) {
			
		}
	}
	
//	private void trainModels(RemoteServer rs, String filepath) throws Exception {
//		String weight = "";
//		if (pairType == PairType.event_event) {
//			weight = "-w1 34 -w2 22 -w5 14 -w6 10 -w7 9 -w8 5";
//		} else if (pairType == PairType.event_timex) {
//			weight = "-w1 298 -w2 61 -w6 11 -w7 130 -w8 421 -w9 34 -w10 4 -w11 4 -w12 13 -w13 12 -w14 10";
//		}
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {	//Train models using Yamcha
//			String cmdCd = "cd tools/yamcha-0.33/";
//			String cmdTrain = "make CORPUS=~/" + filepath + " "
//					+ "MULTI_CLASS=2 "	//causality
//					+ "MODEL=~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-yamcha" + " "
//					+ "FEATURE=\"F:0:2..\" "
////					+ "SVM_PARAM=\"-t1 -d4 -c1 -m 512\" train"; //temporal
//					+ "SVM_PARAM=\"-t1 -d2 -c1 -m 512\" train";	//causality
//			rs.executeCommand(cmdCd + " && " + cmdTrain);
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {	//Train models using LibSVM
//			String cmdCd = "cd tools/libsvm-3.20/";
//			String cmdTrain = "./svm-train "
//					+ "-s 0 -t 2 -d 3 -g 0.0 -r 0.0 -c 1 -n 0.5 -p 0.1 -m 128 -e 0.001 "
//					+ "~/" + filepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.model";
//			
//			rs.executeCommand(cmdCd + " && " + cmdTrain);
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {	//Train models using LibLINEAR
//			String cmdCd = "cd tools/liblinear-2.01/";
//			String cmdTrain = "./train "
//					+ "-s 1 -c 1.0 -e 0.01 -B 1.0 "
////					+ weight + " " //label weights
//					+ "~/" + filepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.model";
//			
//			rs.executeCommand(cmdCd + " && " + cmdTrain);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {	//Train models using Weka
//			Instances train = new DataSource(filepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1); 
//		}
//	}
	
//	public void evaluate(List<PairFeatureVector> vectors) 
//			throws Exception {
////		RemoteServer rs = new RemoteServer();
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
//		System.out.println("Evaluate models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			evaluateModels(trainFilepath, filepath);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			evaluateModels(rs, trainFilepath, filepath);
//			rs.disconnect();
//		}
//	}
	
//	public void evaluate(List<PairFeatureVector> vectors, 
//			String probsPath, String modelPath) 
//			throws Exception {
//		
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
//		System.out.println("Evaluate models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			evaluateModels(trainFilepath, filepath, modelPath);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			evaluateModels(rs, trainFilepath, filepath, modelPath);
//			rs.disconnect();
//		}
//	}
	
	private void evaluateModels(String trainFilepath, 
			String testFilepath) throws Exception {
	
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			List<String> result = new ArrayList<String>();
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				result.add(line.split(" ")[0] 
						+ "\t" + lineResult);
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx();
			
		} else if(classifier.equals(VectorClassifier.libsvm)) { 
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			List<String> result = new ArrayList<String>();
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				result.add(line.split(" ")[0] 
						+ "\t" + lineResult);
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx();
		
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
	}
	
	private void evaluateModels(String trainFilepath, 
			String testFilepath,
			String modelPath) throws Exception {
	
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add(modelPath.replace("/", "\\"));
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			List<String> result = new ArrayList<String>();
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				result.add(line.split(" ")[0] 
						+ "\t" + lineResult);
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx();
			
		} else if(classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add(modelPath.replace("/", "\\"));
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			List<String> result = new ArrayList<String>();
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				result.add(line.split(" ")[0] 
						+ "\t" + lineResult);
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx();
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
	}
	
//	private void evaluateModels(RemoteServer rs,
//			String trainFilepath, String testFilepath) throws Exception {
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha -m ~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-yamcha.model "
//					+ "< ~/" + testFilepath + " "
//					+ "| cut -f1,2," + (featureVecLen) + "," + (featureVecLen+1);
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabel();
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabelIdx();
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabelIdx();
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//	}
	
//	private void evaluateModels(RemoteServer rs,
//			String trainFilepath, String testFilepath,
//			String modelPath) throws Exception {
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha -m " + modelPath + " "
//					+ "< ~/" + testFilepath + " "
//					+ "| cut -f1,2," + (featureVecLen) + "," + (featureVecLen+1);
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabel();
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ modelPath + " "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabelIdx();
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ modelPath + " "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			
//			PairEvaluator pe = new PairEvaluator(result);
//			pe.evaluatePerLabelIdx();
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//	}
	
//	public String test(List<PairFeatureVector> vectors, String[] label) 
//			throws Exception {
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
////		System.out.println("Test models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		String result = "";
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			result = testModels(trainFilepath, filepath, label);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			result = testModels(rs, trainFilepath, filepath, label);
//			rs.disconnect();
//		} 
//		
//		return result;
//	}
	
//	public String test(List<PairFeatureVector> vectors, boolean labelProbs, String[] label) 
//			throws Exception {
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
////		System.out.println("Test models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		String result = "";
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			result = testModels(trainFilepath, filepath, labelProbs, label);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			result = testModels(rs, trainFilepath, filepath, labelProbs, label);
//			rs.disconnect();
//		} 
//		
//		return result;
//	}
	
//	public String test(List<PairFeatureVector> vectors) 
//			throws Exception {
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
////		System.out.println("Test models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		String result = "";
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			result = testModels(trainFilepath, filepath);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			result = testModels(rs, trainFilepath, filepath);
//			rs.disconnect();
//		} 
//		
//		return result;
//	}
	
//	public String test(List<PairFeatureVector> vectors, boolean labelProbs) 
//			throws Exception {
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
////		System.out.println("Test models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		String result = "";
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			result = testModels(trainFilepath, filepath, labelProbs);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			result = testModels(rs, trainFilepath, filepath);
//			rs.disconnect();
//		} 
//		
//		return result;
//	}
	
//	public String test(List<PairFeatureVector> vectors, 
//			String modelPath) 
//			throws Exception {
//		String filepath = dataDirPath + name + "-" + getPairTypeString() + "-eval-" + getFeatureTypeString() + ".data";
//		
////		System.out.println("Test models...");
//		String trainFilepath = dataDirPath + name + "-" + getPairTypeString() + "-train-" + getFeatureTypeString() + ".data";
//		
//		String result = "";
//		if (classifier.equals(VectorClassifier.liblinear)
//				|| classifier.equals(VectorClassifier.libsvm)
//				|| classifier.equals(VectorClassifier.weka)) {
//			writeDataset(null, filepath, vectors, false);
//			result = testModels(trainFilepath, filepath, modelPath);
//		} else {
//			RemoteServer rs = new RemoteServer();
//			writeDataset(rs, filepath, vectors, false);
//			result = testModels(rs, trainFilepath, filepath, modelPath);
//			rs.disconnect();
//		}
//		
//		return result;
//	}
	
	private String testModels(String trainFilepath, 
			String testFilepath) throws Exception {
		
		StringBuilder pairResult = new StringBuilder();
		
		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> labelList = Arrays.asList(label);
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if(classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
		
		return pairResult.toString();
	}
	
	private String testModels(String trainFilepath, 
			String testFilepath, String[] label) throws Exception {
		
		StringBuilder pairResult = new StringBuilder();
		List<String> labelList = Arrays.asList(label);
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if(classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
		
		return pairResult.toString();
	}
	
	private String testModels(String trainFilepath, 
			String testFilepath, boolean labelProbs) throws Exception {
		
		StringBuilder pairResult = new StringBuilder();
		
		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> labelList = Arrays.asList(label);
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			if (labelProbs) {
				command.add("-b"); command.add("1");
			}
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			Map<String, Integer> labelIndices = new HashMap<String, Integer>();
			if (labelProbs) {
				String[] headerCols = readerResult.readLine().split(" ");
				for (int i=1; i< headerCols.length; i++) {
					labelIndices.put(headerCols[i], i);
				}
			}
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				List<String> lineResultCols = Arrays.asList(lineResult.split(" "));
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResultCols.get(0));
				if (labelProbs)	pairResult.append("\t" + lineResultCols.get(labelIndices.get(lineResultCols.get(0)) + 1));			
				pairResult.append("\n");
			}
			
		} else if (classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			if (labelProbs) {
				command.add("-b"); command.add("1");
			}
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			Map<String, Integer> labelIndices = new HashMap<String, Integer>();
			if (labelProbs) {
				String[] headerCols = readerResult.readLine().split(" ");
				for (int i=1; i< headerCols.length; i++) {
					labelIndices.put(headerCols[i], i);
				}
			}
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				List<String> lineResultCols = Arrays.asList(lineResult.split(" "));
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResultCols.get(0));
				if (labelProbs)	pairResult.append("\t" + lineResultCols.get(labelIndices.get(lineResultCols.get(0)) + 1));			
				pairResult.append("\n");
			}
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
		
		return pairResult.toString();
	}
	
	private String testModels(String trainFilepath, 
			String testFilepath, boolean labelProbs, String[] label) throws Exception {
		
		StringBuilder pairResult = new StringBuilder();
		
		List<String> labelList = Arrays.asList(label);
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			if (labelProbs) {
				command.add("-b"); command.add("1");
			}
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-liblinear.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			Map<String, Integer> labelIndices = new HashMap<String, Integer>();
			if (labelProbs) {
				String[] headerCols = readerResult.readLine().split(" ");
				for (int i=1; i< headerCols.length; i++) {
					labelIndices.put(headerCols[i], i);
				}
			}
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				List<String> lineResultCols = Arrays.asList(lineResult.split(" "));
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResultCols.get(0));
				if (labelProbs)	pairResult.append("\t" + lineResultCols.get(labelIndices.get(lineResultCols.get(0)) + 1));			
				pairResult.append("\n");
			}
			
		} else if (classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			if (labelProbs) {
				command.add("-b"); command.add("1");
			}
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add("models\\" + name 
					+ "-" + getFeatureTypeString() 
					+ "-" + labelGrouping
					+ "-" + getPairTypeString() 
					+ "-libsvm.model");
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			Map<String, Integer> labelIndices = new HashMap<String, Integer>();
			if (labelProbs) {
				String[] headerCols = readerResult.readLine().split(" ");
				for (int i=1; i< headerCols.length; i++) {
					labelIndices.put(headerCols[i], i);
				}
			}
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				List<String> lineResultCols = Arrays.asList(lineResult.split(" "));
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResultCols.get(0));
				if (labelProbs)	pairResult.append("\t" + lineResultCols.get(labelIndices.get(lineResultCols.get(0))));			
				pairResult.append("\n");
			}
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
		
		return pairResult.toString();
	}
	
	private String testModels(String trainFilepath, 
			String testFilepath,
			String modelPath) throws Exception {
		
		StringBuilder pairResult = new StringBuilder();
		
		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> labelList = Arrays.asList(label);
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\liblinear-2.1\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add(modelPath.replace("/", "\\"));
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-liblinear.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if (classifier.equals(VectorClassifier.libsvm)) {
			
			/*******Windows*******/
			String liblinearPath = "D:\\GitHub\\EventRelationExtractor\\tools\\libsvm-3.21\\";
			String projectPath = "D:\\GitHub\\EventRelationExtractor\\";
			List<String> command = new ArrayList<String>();
			command.add(liblinearPath + "windows\\svm-predict.exe");
			command.add("-q");
			command.add(testFilepath + ".libsvm ");
			command.add(modelPath.replace("/", "\\"));
			command.add("data\\" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged");
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			process.waitFor();
			
			BufferedReader readerInput = new BufferedReader(new FileReader(testFilepath + ".libsvm"));
			BufferedReader readerResult = new BufferedReader(new FileReader("data/" + name 
					+ "-" + getFeatureTypeString()
					+ "-" + labelGrouping 
					+ "-" + getPairTypeString() 
					+ "-libsvm.tagged"));
			String line, lineResult;
			while ((line = readerInput.readLine()) != null) {
				lineResult = readerResult.readLine();
				pairResult.append(line.split(" ")[0] 
						+ "\t" + lineResult
						+ "\n");
			}
			
		} else if (classifier.equals(VectorClassifier.weka)) {
			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
			train.setClassIndex(train.numAttributes() - 1);
			wekaClassifier.buildClassifier(train);
			
			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			Evaluation eval = new Evaluation(train);
		    eval.evaluateModel(wekaClassifier, test);
		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
		}
		
		return pairResult.toString();
	}
	
//	private String testModels(RemoteServer rs,
//			String trainFilepath, String testFilepath, String label[]) throws Exception {
//		
//		StringBuilder pairResult = new StringBuilder();
//		
//		List<String> labelList = Arrays.asList(label);
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha "
//					+ "-V "
//					+ "-m ~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-yamcha.model "
//					+ "< ~/" + testFilepath + " "
//					//+ "| cut -f" + (featureVecLen) + "," + (featureVecLen+1);
//					+ "| cut -f" + (featureVecLen) + "-";
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			int idxLabel, idxPred;
//			Map<String, Double> labelProbs = new HashMap<String, Double>();
//			String lbl; Double prob;
//			for (String s : result) {
//				if (!s.isEmpty()) {
//					String[] cols = s.split("\t");
//					idxLabel = labelList.indexOf(cols[0]) + 1;
//					idxPred = labelList.indexOf(cols[1]) + 1;
//					if (cols.length > 2) {
//						for (int i=2; i<cols.length; i++) {
//							lbl = cols[i].split("/")[0];
//							prob = Double.parseDouble(cols[i].split("/")[1]);
//							labelProbs.put(lbl, prob);
//						}
//					}
//					pairResult.append(idxLabel + "\t" + idxPred + "\t" + labelProbs.get(cols[1]) + "\n");
//				}
//			}
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//		
//		return pairResult.toString();
//	}
	
//	private String testModels(RemoteServer rs,
//			String trainFilepath, String testFilepath, boolean labelProbs, String label[]) throws Exception {
//		
//		StringBuilder pairResult = new StringBuilder();
//		
//		List<String> labelList = Arrays.asList(label);
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha "
//					+ "-V "
//					+ "-m ~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-yamcha.model "
//					+ "< ~/" + testFilepath + " "
//					//+ "| cut -f" + (featureVecLen) + "," + (featureVecLen+1);
//					+ "| cut -f" + (featureVecLen) + "-";
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			int idxLabel, idxPred;
//			Map<String, Double> lblProbs = new HashMap<String, Double>();
//			String lbl; Double prob;
//			for (String s : result) {
//				if (!s.isEmpty()) {
//					String[] cols = s.split("\t");
//					idxLabel = labelList.indexOf(cols[0]) + 1;
//					idxPred = labelList.indexOf(cols[1]) + 1;
//					if (cols.length > 2) {
//						for (int i=2; i<cols.length; i++) {
//							lbl = cols[i].split("/")[0];
//							prob = Double.parseDouble(cols[i].split("/")[1]);
//							lblProbs.put(lbl, prob);
//						}
//					}
//					pairResult.append(idxLabel + "\t" + idxPred + "\t" + lblProbs.get(cols[1]) + "\n");
//				}
//			}
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//		
//		return pairResult.toString();
//	}
	
//	private String testModels(RemoteServer rs,
//			String trainFilepath, String testFilepath) throws Exception {
//		
//		StringBuilder pairResult = new StringBuilder();
//		
//		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
//				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
//		List<String> labelList = Arrays.asList(label);
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha -m ~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-yamcha.model "
//					+ "< ~/" + testFilepath + " "
//					+ "| cut -f" + (featureVecLen) + "," + (featureVecLen+1);
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			int idxLabel, idxPred;
//			for (String s : result) {
//				if (!s.isEmpty()) {
//					String[] cols = s.split("\t");
//					idxLabel = labelList.indexOf(cols[0]) + 1;
//					idxPred = labelList.indexOf(cols[1]) + 1;
//					pairResult.append(idxLabel + "\t" + idxPred + "\n");
//				}
//			}
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "~/models/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.model "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//		
//		return pairResult.toString();
//	}
	
//	private String testModels(RemoteServer rs,
//			String trainFilepath, String testFilepath,
//			String modelPath) throws Exception {
//		
//		StringBuilder pairResult = new StringBuilder();
//		
//		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
//				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
//		List<String> labelList = Arrays.asList(label);
//		
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			String cmdCd = "cd tools/yamcha-0.33/";			
//			String cmdTest = "./usr/local/bin/yamcha -m " + modelPath + " "
//					+ "< ~/" + testFilepath + " "
//					+ "| cut -f1,2," + (featureVecLen) + "," + (featureVecLen+1);
//					//+ " > ~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest);
//			int idxLabel, idxPred;
//			for (String s : result) {
//				String[] cols = s.split("\t");
//				idxLabel = labelList.indexOf(cols[2]);
//				idxPred = labelList.indexOf(cols[3]);
//				pairResult.append(idxLabel + "\t" + idxPred + "\n");
//			}
//			
//		} else if (classifier.equals(VectorClassifier.libsvm)) {
//			String cmdCd = "cd tools/libsvm-3.20/";		
//			String cmdTest = "./svm-predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ modelPath + " "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-libsvm.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);			
//			
//		} else if (classifier.equals(VectorClassifier.liblinear)) {
//			String cmdCd = "cd tools/liblinear-2.01/";		
//			String cmdTest = "./predict -q "
//					+ "~/" + testFilepath + ".libsvm "
//					+ modelPath + " "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			String cmdResult = "cut -d' ' -f1 "
//					+ "~/" + testFilepath + ".libsvm "
//					+ "| paste - "
//					+ "~/data/" + name + "-" + labelGrouping + "-" + getPairTypeString() + "-liblinear.tagged";
//			
//			List<String> result = rs.executeCommand(cmdCd + " && " + cmdTest + " && " + cmdResult);
//			for (String s : result) {
//				pairResult.append(s + "\n");
//			}
//			
//			String rmTagged = "cd ~/data/ && rm *.tagged";
//			rs.executeCommand(rmTagged);
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			Instances train = new DataSource(trainFilepath + ".arff").getDataSet();
//			train.setClassIndex(train.numAttributes() - 1);
//			wekaClassifier.buildClassifier(train);
//			
//			Instances test = new DataSource(testFilepath + ".arff").getDataSet();
//			test.setClassIndex(test.numAttributes() - 1);
//			Evaluation eval = new Evaluation(train);
//		    eval.evaluateModel(wekaClassifier, test);
//		    System.out.println(eval.toClassDetailsString("\n" + getPairTypeString() + " results\n======\n"));
//		}
//		
//		return pairResult.toString();
//	}
	
	public String printFeatureVector(List<PairFeatureVector> vectors) {
		StringBuilder pairFV = new StringBuilder();
		for (PairFeatureVector fv : vectors) {
			if (classifier.equals(VectorClassifier.libsvm) || classifier.equals(VectorClassifier.liblinear)) {
				pairFV.append(fv.printLibSVMVectors() + "\n");
			} else if (classifier.equals(VectorClassifier.weka)) {
				pairFV.append(fv.printCSVVectors() + "\n");
			} else if (classifier.equals(VectorClassifier.yamcha)) {
				pairFV.append(fv.printVectors() + "\n");
			} else if (classifier.equals(VectorClassifier.none)) {
				pairFV.append(fv.printCSVVectors() + "\n");
			}
		}
		if (classifier.equals(VectorClassifier.yamcha)) pairFV.append("\n");
		return pairFV.toString();
	}
	
//	public void writeDataset(RemoteServer rs, String filepath, 
//			List<PairFeatureVector> vectors, 
//			boolean train) throws Exception {
//		int fvSize = vectors.get(0).getVectors().size();
//		int wEmbedDim = 600;
//		int pEmbedDim = 9600;
//		int oneHotTokensDim = 4002;
//		
//		switch (featureType) {
//			case conventional:
//				featureVecLen = fvSize;
//				writeConvFeature(rs, filepath, vectors);		//conventional features
//				break;
//			
//			case wordEmbed:
//				featureVecLen = wEmbedDim;
//				writeProbs(rs, filepath,					//word embedding
//						probVectorFile, wEmbedDim);
//				break;
//				
//			case wordEmbedConv:
//				featureVecLen = fvSize + wEmbedDim;
//				combineFeatureVectorProbs(rs, filepath,	//word embedding + conventional features
//						vectors,
//						probVectorFile, wEmbedDim, 
//						train);	//true for training
//				break;
//				
//			case phraseEmbed:
//				featureVecLen = pEmbedDim;
//				writeProbs(rs, filepath,					//chunk embedding
//						probVectorFile, pEmbedDim);
//				break;
//				
//			case phraseEmbedConv:
//				featureVecLen = fvSize + pEmbedDim;
//				combineFeatureVectorProbs(rs, filepath,	//chunk embedding + conventional features
//						vectors,
//						probVectorFile, pEmbedDim, 
//						train);	//true for training
//				break;
//				
//			case oneHotConv:
//				featureVecLen = fvSize + oneHotTokensDim;
//				combineFeatureVectorProbs(rs, filepath,	//one-hot tokens + conventional features
//						vectors,
//						probVectorFile, oneHotTokensDim, 
//						train);	//true for training
//				break;
//				
//			case convProb:
//				featureVecLen = fvSize + getClassSizeGrouping(labelGrouping);
//				combineFeatureVectorProbs(rs, filepath,	//NN probs + conventional features
//						vectors,
//						probVectorFile, getClassSizeGrouping(labelGrouping), 
//						train);
//				break;
//		}
//	}
	
//	private void writeConvFeature(RemoteServer rs, String filepath, List<PairFeatureVector> vectors) 
//			throws Exception {
//		System.setProperty("line.separator", "\n");
//		if (classifier.equals(VectorClassifier.none)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".csv", "UTF-8");		
//			pairPW.write(printFeatureVector(vectors));
//			pairPW.close();
//			
//		} else if (classifier.equals(VectorClassifier.yamcha)) {
//			PrintWriter pairPW = new PrintWriter(filepath, "UTF-8");		
//			pairPW.write(printFeatureVector(vectors));
//			pairPW.close();
//			
//			//Copy training data to server
//			//System.out.println("Copy training data...");
//			File file = new File(filepath);
//			rs.copyFile(file, "data/yamcha/");
//			
//			//Delete file in local directory
//			file.delete();
//			
//		} else if (classifier.equals(VectorClassifier.libsvm) || classifier.equals(VectorClassifier.liblinear)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".libsvm", "UTF-8");		
//			pairPW.write(printFeatureVector(vectors));
//			pairPW.close();
//			
////			//Copy training data to server
////			//System.out.println("Copy training data...");
////			File file = new File(filepath + ".libsvm");
////			rs.copyFile(file, "data/libsvm/");
////			
////			//Delete file in local directory
////			file.delete();
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".arff", "UTF-8");	
//			writeArffFile(pairPW, vectors, featureNames);
//		}
//	}
	
	private void writeArffFile(PrintWriter pw, List<PairFeatureVector> vectors, List<String> featureNames) {
		//Header
		pw.write("@relation " + name + "\n\n");
		
		//Field/column titles of features
		for (String s : featureNames) {
			if (s!= null) {
				if (s.equals("label")) {
					pw.write("@attribute " + s + " {BEFORE, AFTER, IBEFORE, IAFTER, IDENTITY, "
							+ "SIMULTANEOUS, INCLUDES, IS_INCLUDED, DURING, DURING_INV, "
							+ "BEGINS, BEGUN_BY, ENDS, ENDED_BY}\n");
				} else if (s.equals("wnSim")){
					pw.write("@attribute " + s + " {0.0,0.25,0.75,1.0}\n");
				} else {
					pw.write("@attribute " + s + " {0,1}\n");
				}
			}
		}
		
		//Vectors
		pw.write("\n@data\n");
		pw.write(printFeatureVector(vectors));
		
		pw.close();
	}
	
//	private void writeProbs (RemoteServer rs, String filepath, 
//			String probsPath, int numProbsCols) throws Exception {
//		
//		System.setProperty("line.separator", "\n");
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".data", "UTF-8");
//			String label;
//			
//			BufferedReader br = new BufferedReader(new FileReader(probsPath));
//			String line;
//			while ((line = br.readLine()) != null) {
//		    	String[] cols = line.split(",");
//		    	label = cols[cols.length-1].trim();
//		    	for (int i=0; i<cols.length-1; i++) {
//		    		pairPW.write(cols[i].trim() + "\t");
//		    	}
//		    	if (isNumeric(label)) {
//		    		pairPW.write(getLabelFromNum(label));
//		    	} else {
//		    		pairPW.write(label);
//		    	}
//		    	pairPW.write("\n");
//		    }
//			pairPW.close();
//			br.close();
//			
//			//Copy training data to server
//			//System.out.println("Copy training data...");
//			File file = new File(filepath + ".data");
//			rs.copyFile(file, "data/yamcha/");
//			
//		} else if (classifier.equals(VectorClassifier.libsvm) ||
//				classifier.equals(VectorClassifier.liblinear)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".libsvm", "UTF-8");
//			int idx;
//			String label;
//			
//			BufferedReader br = new BufferedReader(new FileReader(probsPath));
//			String line;
//			while ((line = br.readLine()) != null) {
//		    	String[] cols = line.split(",");
//		    	label = cols[cols.length-1].trim();
//		    	if (isNumeric(label)) {
//		    		pairPW.write(label);
//		    	} else {
//		    		pairPW.write(String.valueOf(getNumFromLabel(label)));
//		    	}
//		    	idx = 1;
//		    	Double d;
//		    	for (int i=0; i<cols.length-1; i++) {
//		    		d = Double.parseDouble(cols[i].trim());
//		    		if (d != 0.0) {
//		    			pairPW.write(" " + idx + ":" + d.toString());
//		    		}
//		    		idx += 1;
//		    	}
//		    	pairPW.write("\n");
//		    }
//			pairPW.close();
//			br.close();
//			
//			//Copy training data to server
//			//System.out.println("Copy training data...");
//			File file = new File(filepath + ".libsvm");
//			rs.copyFile(file, "data/libsvm/");
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".arff", "UTF-8");	
//			pairPW.write("@relation " + name + "-" + getPairTypeString() + "-" + getFeatureTypeString() + "\n\n");
//			
//			int idx = 1;
//			for (int i=0; i<numProbsCols; i++) {
//	    		pairPW.write("@attribute attr" + idx + " numeric\n");
//	    		idx += 1;
//	    	}
//			pairPW.write("@attribute label {BEFORE, AFTER, IBEFORE, IAFTER, IDENTITY, "
//					+ "SIMULTANEOUS, INCLUDES, IS_INCLUDED, DURING, DURING_INV, "
//					+ "BEGINS, BEGUN_BY, ENDS, ENDED_BY}\n");
//			
//			pairPW.write("\n@data\n");
//			
//			BufferedReader br = new BufferedReader(new FileReader(probsPath));
//			String line;
//			while ((line = br.readLine()) != null) {
//				pairPW.write(line + "\n");
//			}
//			pairPW.close();
//			br.close();
//		}
//	}
	
//	private void combineFeatureVectorProbs(RemoteServer rs, String filepath,
//			List<PairFeatureVector> vectors, 
//			String probsPath, int numProbsCols,
//			Boolean train) 
//					throws Exception {
//		
//		List<String> fvLines = new ArrayList<String>();
//		for (PairFeatureVector fv : vectors) {
//			fvLines.add(fv.printCSVVectors());
//		}	
//		
//		List<String> weLines = new ArrayList<String>();
//		if (!probsPath.equals("")) {
//			BufferedReader br = new BufferedReader(new FileReader(probsPath));
//			String line;
//			while ((line = br.readLine()) != null) {
//				if (!line.isEmpty()) {
//					weLines.add(line);
//				}
//			}
//			br.close();
//		}
//		System.out.println(fvLines.size() + ", " + weLines.size());
//		
//		System.setProperty("line.separator", "\n");
//		if (classifier.equals(VectorClassifier.yamcha)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".data", "UTF-8");
//			String label;
//			
//			for (int fvIdx=0; fvIdx<fvLines.size(); fvIdx++) {
//				
//				String[] fvCols = fvLines.get(fvIdx).split(",");
//		    	label = fvCols[fvCols.length-1].trim();
//		    	
//		    	String[] weCols;
//		    	if (train || probsPath.equals("")) {
//		    		weCols = new String[getClassSizeGrouping(labelGrouping)];
//		    		Arrays.fill(weCols, "0");
//					weCols[getLabelGrouping(getLabelFromNum(label), labelGrouping)] = "1";
//		    	} else {
//		    		weCols = weLines.get(fvIdx).split(",");
//		    	}					
//				
//		    	for (int i=0; i<weCols.length-1; i++) {
//		    		pairPW.write(weCols[i].trim() + "\t");
//		    	}
//		    	for (int i=0; i<fvCols.length-1; i++) {
//		    		pairPW.write(fvCols[i].trim() + "\t");
//		    	}
//		    	if (isNumeric(label)) {
//		    		pairPW.write(getLabelFromNum(label));
//		    	} else {
//		    		pairPW.write(label);
//		    	}
//		    	pairPW.write("\n");
//		    }
//			pairPW.close();
//			
//			//Copy training data to server
//			//System.out.println("Copy training data...");
//			File file = new File(filepath + ".data");
//			rs.copyFile(file, "data/yamcha/");
//			
//		} else if (classifier.equals(VectorClassifier.libsvm) ||
//				classifier.equals(VectorClassifier.liblinear)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".libsvm", "UTF-8");
//			int idx;
//			String label;
//			
//			for (int fvIdx=0; fvIdx<fvLines.size(); fvIdx++) {
//
//				String[] fvCols = fvLines.get(fvIdx).split(",");
//		    	label = fvCols[fvCols.length-1].trim();
//		    	
//		    	String[] weCols;
//		    	if (train || probsPath.equals("")) {
//		    		weCols = new String[getClassSizeGrouping(labelGrouping)];
//		    		Arrays.fill(weCols, "0");
//					weCols[getLabelGrouping(getLabelFromNum(label), labelGrouping)] = "1";
//		    	} else {
//		    		weCols = weLines.get(fvIdx).split(",");
//		    	}					
//				
//		    	if (isNumeric(label)) {
//		    		pairPW.write(label);
//		    	} else {
//		    		pairPW.write(String.valueOf(getNumFromLabel(label)));
//		    	}
//		    	
//		    	idx = 1;
//		    	Double d;
//		    	for (int i=0; i<weCols.length-1; i++) {
//		    		d = Double.parseDouble(weCols[i].trim());
//		    		if (d != 0.0) {
//		    			pairPW.write(" " + idx + ":" + d.toString());
//		    		}
//		    		idx += 1;
//		    	}
//		    	for (int i=0; i<fvCols.length-1; i++) {
//		    		d = Double.parseDouble(fvCols[i].trim());
//		    		if (d != 0.0) {
//		    			pairPW.write(" " + idx + ":" + d.toString());
//		    		}
//		    		idx += 1;
//		    	}			    	
//		    	pairPW.write("\n");
//			}
//
//			pairPW.close();
//			
////			//Copy training data to server
////			//System.out.println("Copy training data...");
////			File eeFile = new File(filepath + ".libsvm");
////			rs.copyFile(eeFile, "data/libsvm/");
//			
//		} else if (classifier.equals(VectorClassifier.weka)) {
//			PrintWriter pairPW = new PrintWriter(filepath + ".arff", "UTF-8");	
//			pairPW.write("@relation " + name + "-" + getPairTypeString() + "-" + getFeatureTypeString() + "\n\n");
//			
//			int idx = 1;
//			for (int i=0; i<numProbsCols; i++) {
//	    		pairPW.write("@attribute attr" + idx + " numeric\n");
//	    		idx += 1;
//	    	}
//			for (String s : featureNames) {
//				if (s!= null) {
//					if (s.equals("label")) {
//						pairPW.write("@attribute " + s + " {BEFORE, AFTER, IBEFORE, IAFTER, IDENTITY, "
//								+ "SIMULTANEOUS, INCLUDES, IS_INCLUDED, DURING, DURING_INV, "
//								+ "BEGINS, BEGUN_BY, ENDS, ENDED_BY}\n");
//					} else if (s.equals("wnSim")){
//						pairPW.write("@attribute " + s + " {0.0,0.25,0.75,1.0}\n");
//					} else {
//						pairPW.write("@attribute " + s + " {0,1}\n");
//					}
//				}
//			}
//			
//			pairPW.write("\n@data\n");
//			
//			int weIdx = 0;
//			BufferedReader br = new BufferedReader(new FileReader(probsPath));
//			String line;
//			while ((line = br.readLine()) != null) {
//				if (!line.isEmpty()) {
//					pairPW.write(line + "," + fvLines.get(weIdx) + "\n");
//					weIdx += 1;
//				}
//			}
//			pairPW.close();
//			br.close();
//		}		
//	}
	
	public static Boolean isNumeric(String str) {  
		try {
			Double d = Double.parseDouble(str);  
		} catch(NumberFormatException nfe) {  
			return false;  
		}  
		return true;  
	}
	
	private String getLabelFromNum(String num) {
		String[] temp_rel_type = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> temp_rel_type_list = Arrays.asList(temp_rel_type);
		return temp_rel_type_list.get(Integer.valueOf(num)-1);
	}
	
	private int getNumFromLabel(String label) {
		String[] temp_rel_type = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> temp_rel_type_list = Arrays.asList(temp_rel_type);
		return temp_rel_type_list.indexOf(label) + 1;
	}
	
	public int getClassSizeGrouping(int grouping) {
		switch(grouping) {
			case 0: return 14;
			case 1: return 7;
			case 2: return 6;
			case 3: return 7;
			case 4: return 5;
			case 5: return 6;
			case 6: return 5;
			case 7: return 5;
			case 8: return 4;
		}
		return -1;
	}
	
	public int getLabelGrouping(String label, int grouping) {
		switch(grouping) {
			case 0:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 8;
					case "BEFORE": return 1;
					case "IBEFORE": return 9;
					case "BEGINS": return 2;
					case "BEGUN_BY": return 3;
					case "DURING": return 4;
					case "DURING_INV": return 5;
					case "ENDS": return 7;
					case "ENDED_BY": return 6;
					case "IDENTITY": return 10;
					case "SIMULTANEOUS": return 13;
					case "INCLUDES": return 11;
					case "IS_INCLUDED": return 12;
				}
				break;
			case 1:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 2;
					case "BEGUN_BY": return 2;
					case "DURING": return 3;
					case "DURING_INV": return 3;
					case "ENDS": return 4;
					case "ENDED_BY": return 4;
					case "IDENTITY": return 5;
					case "SIMULTANEOUS": return 5;
					case "INCLUDES": return 6;
					case "IS_INCLUDED": return 6;
				}
				break;
			case 2:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 2;
					case "BEGUN_BY": return 3;
					case "DURING": return 2;
					case "DURING_INV": return 3;
					case "ENDS": return 2;
					case "ENDED_BY": return 3;
					case "IDENTITY": return 4;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 5;
					case "IS_INCLUDED": return 5;
				}
				break;
			case 3:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 2;
					case "BEGUN_BY": return 3;
					case "DURING": return 2;
					case "DURING_INV": return 3;
					case "ENDS": return 2;
					case "ENDED_BY": return 3;
					case "IDENTITY": return 4;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 5;
					case "IS_INCLUDED": return 6;
				}
				break;
			case 4:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 1;
					case "BEGUN_BY": return 0;
					case "DURING": return 4;
					case "DURING_INV": return 4;
					case "ENDS": return 0;
					case "ENDED_BY": return 1;
					case "IDENTITY": return 4;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 2;
					case "IS_INCLUDED": return 3;
				}
				break;
			case 5:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 1;
					case "BEGUN_BY": return 0;
					case "DURING": return 5;
					case "DURING_INV": return 5;
					case "ENDS": return 0;
					case "ENDED_BY": return 1;
					case "IDENTITY": return 5;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 2;
					case "IS_INCLUDED": return 3;
				}
				break;
			case 6:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 1;
					case "BEGUN_BY": return 0;
					case "DURING": return 3;
					case "DURING_INV": return 2;
					case "ENDS": return 0;
					case "ENDED_BY": return 1;
					case "IDENTITY": return 4;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 2;
					case "IS_INCLUDED": return 3;
				}
				break;
			case 7:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 3;
					case "BEGUN_BY": return 2;
					case "DURING": return 3;
					case "DURING_INV": return 2;
					case "ENDS": return 3;
					case "ENDED_BY": return 2;
					case "IDENTITY": return 4;
					case "SIMULTANEOUS": return 4;
					case "INCLUDES": return 2;
					case "IS_INCLUDED": return 3;
				}
				break;
			case 8:
				switch(label) {
					case "AFTER": return 0;
					case "IAFTER": return 0;
					case "BEFORE": return 1;
					case "IBEFORE": return 1;
					case "BEGINS": return 1;
					case "BEGUN_BY": return 0;
					case "DURING": return 2;
					case "DURING_INV": return 2;
					case "ENDS": return 0;
					case "ENDED_BY": return 1;
					case "IDENTITY": return 3;
					case "SIMULTANEOUS": return 2;
					case "INCLUDES": return 2;
					case "IS_INCLUDED": return 2;
				}
				break;
		}
		return -1;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public PairType getPairType() {
		return pairType;
	}

	public void setPairType(PairType pairType) {
		this.pairType = pairType;
	}
}
