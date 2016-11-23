package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import catena.model.classifier.PairClassifier.PairType;
import catena.model.classifier.PairClassifier.VectorClassifier;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import evaluator.PairEvaluator;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import model.feature.PairFeatureVector;
import model.feature.FeatureEnum.FeatureName;

public class EventEventCausalClassifier extends PairClassifier {
	
	private String[] label = {"CLINK", "CLINK-R", "NONE"};
	
	private void initFeatureVector() {
		
		super.setPairType(PairType.event_event);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] eeFeatures = {
					FeatureName.tokenSpace, FeatureName.lemmaSpace,
					FeatureName.tokenChunk,
					FeatureName.tempMarkerTextSpace, FeatureName.causMarkerTextSpace
			};
			featureList = Arrays.asList(eeFeatures);
			
		} else if (classifier.equals(VectorClassifier.yamcha)) {
			FeatureName[] eeFeatures = {
					/*FeatureName.token,*/ /*FeatureName.lemma,*/
//					FeatureName.supersense,
					FeatureName.pos, /*FeatureName.mainpos,*/
					FeatureName.samePos, /*FeatureName.sameMainPos,*/
					FeatureName.chunk,
					FeatureName.entDistance, FeatureName.sentDistance,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
//					FeatureName.sameEventClass, FeatureName.sameTenseAspect, /*FeatureName.sameAspect,*/ FeatureName.samePolarity,
					FeatureName.depEvPath,				
					FeatureName.mainVerb,
					FeatureName.tempSignalClusText,
					FeatureName.tempSignalPos,
					FeatureName.tempSignalDep1Dep2,
					FeatureName.causSignalClusText,
					FeatureName.causSignalPos, 
					FeatureName.causSignalDep1Dep2,
//					FeatureName.causSignal1ClusText,
//					FeatureName.causSignal1Pos,
//					FeatureName.causSignal2ClusText,
//					FeatureName.causSignal2Pos,
//					FeatureName.causVerbClusText,
//					FeatureName.causVerbPos,
//					FeatureName.coref,
					FeatureName.wnSim
			};
			featureList = Arrays.asList(eeFeatures);
			
		} else {
			FeatureName[] eeFeatures = {
//					FeatureName.lemma,
					FeatureName.pos, /*FeatureName.mainpos,*/
					FeatureName.samePos, /*FeatureName.sameMainPos,*/
					FeatureName.chunk,
					FeatureName.entDistance, FeatureName.sentDistance,
//					FeatureName.timexInBetween,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
					FeatureName.sameEventClass, FeatureName.sameTenseAspect, /*FeatureName.sameAspect,*/ FeatureName.samePolarity,
					FeatureName.depEvPath,			
					FeatureName.mainVerb,
					FeatureName.hasModal,
					FeatureName.tempSignalClusText,
					FeatureName.tempSignalPos,
					FeatureName.tempSignalDep1Dep2,
//					FeatureName.tempSignal1ClusText,
//					FeatureName.tempSignal1Pos,
//					FeatureName.tempSignal1Dep,
//					FeatureName.tempSignal2ClusText,
//					FeatureName.tempSignal2Pos,
//					FeatureName.tempSignal2Dep,
					FeatureName.causSignalClusText,
					FeatureName.causSignalPos,
					FeatureName.causSignalDep1Dep2,
//					FeatureName.causSignal1ClusText,
//					FeatureName.causSignal1Pos,
//					FeatureName.causSignal1Dep,
//					FeatureName.causSignal2ClusText,
//					FeatureName.causSignal2Pos,
//					FeatureName.causSignal2Dep,
//					FeatureName.causVerbClusText,
//					FeatureName.causVerbPos,
//					FeatureName.coref
					FeatureName.wnSim
			};
			featureList = Arrays.asList(eeFeatures);
		}
	}
	
	public EventEventCausalClassifier(String taskName, 
			String classifier) throws Exception {
		super(taskName, classifier);
		initFeatureVector();
	}
	
	public EventEventCausalClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile);
		initFeatureVector();
	}
	
	public EventEventCausalClassifier(String taskName,
			String classifier, String inconsistency) throws Exception {
		super(taskName, classifier,
				inconsistency);
		initFeatureVector();
	}

	public EventEventCausalClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile,
			String inconsistency) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile, inconsistency);
		initFeatureVector();
	}
	
	public void train(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		System.err.println("Train model...");

		int nInstances = vectors.size();
		int nFeatures = vectors.get(0).getVectors().size()-1;
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			//Prepare training data
			Feature[][] instances = new Feature[nInstances][nFeatures];
			double[] labels = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				for (int i=0; i<nFeatures; i++) {
					instances[row][col] = new FeatureNode(idx, Double.valueOf(fv.getVectors().get(i)));
					idx ++;
					col ++;
				}
				labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
				row ++;
			}
			
			//Train
			Problem problem = new Problem();
			problem.l = nInstances;
			problem.n = nFeatures;
			problem.x = instances;
			problem.y = labels;
			problem.bias = 1.0;
			
			SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL; // -s 1
			double C = 1.0;    // cost of constraints violation
			double eps = 0.01; // stopping criteria

			Parameter parameter = new Parameter(solver, C, eps);
			Model model = Linear.train(problem, parameter);
			File modelFile = new File(modelPath);
			model.save(modelFile);
			
		}
	}
	
	public svm_model trainSVM(List<PairFeatureVector> vectors) throws Exception {
		
		System.err.println("Train model...");

		int nInstances = vectors.size();
		int nFeatures = vectors.get(0).getVectors().size()-1;
		
		if (classifier.equals(VectorClassifier.libsvm)) {
			//Prepare training data
			svm_problem prob = new svm_problem();
			prob.l = nInstances;
			prob.x = new svm_node[nInstances][nFeatures];
			prob.y = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				for (int i=0; i<nFeatures; i++) {
					svm_node node = new svm_node();
					node.index = idx;
					node.value = Double.valueOf(fv.getVectors().get(i));
					prob.x[row][col] = node;
					idx ++;
					col ++;
				}
				prob.y[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
				row ++;
			}
			
			//Train
			svm_parameter param = new svm_parameter();
		    param.probability = 1;
		    param.gamma = 0.5;
//		    param.nu = 0.5;
		    param.C = 1;
		    param.svm_type = svm_parameter.C_SVC;
		    param.kernel_type = svm_parameter.POLY; 
		    param.degree = 2;
		    param.cache_size = 20000;
		    param.eps = 0.001;
		    
		    svm_model model = svm.svm_train(prob, param);
		    return model;
		}
		return null;
	}
	
	public void evaluate(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		System.err.println("Evaluate model...");

		int nInstances = vectors.size();
		int nFeatures = vectors.get(0).getVectors().size()-1;
		
		if (classifier.equals(VectorClassifier.liblinear)) {
			//Prepare evaluation data
			Feature[][] instances = new Feature[nInstances][nFeatures];
			double[] labels = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				for (int i=0; i<nFeatures; i++) {
					labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
					instances[row][col] = new FeatureNode(idx, Double.valueOf(fv.getVectors().get(i)));
					idx ++;
					col ++;
				}
				row ++;
			}
			
			//Test
			File modelFile = new File(modelPath);
			Model model = Model.load(modelFile);
			double[] predictions = new double[nInstances];
			int p = 0;
			for (Feature[] instance : instances) {
				predictions[p] = Linear.predict(model, instance);
				p ++;
			}
			
			List<String> result = new ArrayList<String>();
			for (int i=0; i<labels.length; i++) {
				result.add(((int)labels[i]) + "\t" + ((int)predictions[i]));
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx(label);
		}
	}
	
	public void evaluate(List<PairFeatureVector> vectors, svm_model model) throws Exception {
		
		System.err.println("Evaluate model...");

		int nInstances = vectors.size();
		int nFeatures = vectors.get(0).getVectors().size()-1;
		
		if (classifier.equals(VectorClassifier.libsvm)) {
			//Prepare evaluation data
			svm_node[][] instances = new svm_node[nInstances][nFeatures];
			double[] labels = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				for (int i=0; i<nFeatures; i++) {
					svm_node node = new svm_node();
					node.index = idx;
					node.value = Double.valueOf(fv.getVectors().get(i));
					instances[row][col] = node;
					idx ++;
					col ++;
				}
				labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
				row ++;
			}
			
			//Test
			int totalClasses = 3;
			double[] predictions = new double[nInstances];
			int p = 0;
			for (svm_node[] instance : instances) {
				int[] classes = new int[totalClasses];
		        svm.svm_get_labels(model, classes);
		        double[] probs = new double[totalClasses];
				predictions[p] = svm.svm_predict_probability(model, instance, probs);
				p ++;
			}
			
			List<String> result = new ArrayList<String>();
			for (int i=0; i<labels.length; i++) {
				result.add(((int)labels[i]) + "\t" + ((int)predictions[i]));
			}
			
			PairEvaluator pe = new PairEvaluator(result);
			pe.evaluatePerLabelIdx(label);
		}
	}
	
	public List<String> predict(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		System.err.println("Test model...");
		
		List<String> predictionLabels = new ArrayList<String>();
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getVectors().size()-1;
			
			if (classifier.equals(VectorClassifier.liblinear)) {
				//Prepare test data
				Feature[][] instances = new Feature[nInstances][nFeatures];
				double[] labels = new double[nInstances];
				
				int row = 0;
				for (PairFeatureVector fv : vectors) {				
					int idx = 1, col = 0;
					for (int i=0; i<nFeatures; i++) {
						labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
						instances[row][col] = new FeatureNode(idx, Double.valueOf(fv.getVectors().get(i)));
						idx ++;
						col ++;
					}
					row ++;
				}
				
				//Test
				File modelFile = new File(modelPath);
				Model model = Model.load(modelFile);
				for (Feature[] instance : instances) {
					predictionLabels.add(label[(int)Linear.predict(model, instance)-1]);
				}
			}
		}
		
		return predictionLabels;
	}
	
	public List<String> predict(List<PairFeatureVector> vectors, svm_model model) throws Exception {
		
		System.err.println("Test model...");

		List<String> predictionLabels = new ArrayList<String>();
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getVectors().size()-1;
		
			if (classifier.equals(VectorClassifier.libsvm)) {
				//Prepare test data
				svm_node[][] instances = new svm_node[nInstances][nFeatures];
				double[] labels = new double[nInstances];
				
				int row = 0;
				for (PairFeatureVector fv : vectors) {				
					int idx = 1, col = 0;
					for (int i=0; i<nFeatures; i++) {
						svm_node node = new svm_node();
						node.index = idx;
						node.value = Double.valueOf(fv.getVectors().get(i));
						instances[row][col] = node;
						idx ++;
						col ++;
					}
					labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
					row ++;
				}
				
				//Test
				int totalClasses = 3;
				double[] predictions = new double[nInstances];
				
				for (svm_node[] instance : instances) {
					int[] classes = new int[totalClasses];
			        svm.svm_get_labels(model, classes);
			        double[] probs = new double[totalClasses];
			        predictionLabels.add(label[((int)svm.svm_predict_probability(model, instance, probs))-1]);
				}
			}
		}
		
		return predictionLabels;
	}
}
