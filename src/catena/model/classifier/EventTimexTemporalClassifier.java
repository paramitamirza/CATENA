package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bwaldvogel.liblinear.*;
import libsvm.*;

import catena.evaluator.PairEvaluator;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.rule.EventTimexTemporalRule;
import catena.model.rule.TimexTimexTemporalRule;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;


public class EventTimexTemporalClassifier extends PairClassifier {
	
	protected String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	protected String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	
	protected void initFeatureVector() {
		
		super.setPairType(PairType.event_timex);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] etFeatures = {
					FeatureName.tokenSpace, 
					FeatureName.lemmaSpace,
					FeatureName.tokenChunk,
					FeatureName.tempMarkerTextSpace
			};
			featureList = Arrays.asList(etFeatures);
			
		} else {
			FeatureName[] etFeatures = {
					FeatureName.pos,
//					Feature.mainpos,
					FeatureName.samePos,
//					Feature.sameMainPos,
					FeatureName.chunk,
					FeatureName.entDistance, FeatureName.sentDistance, FeatureName.entOrder,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
					FeatureName.dct,
					FeatureName.timexType, 				
					FeatureName.mainVerb, 
					FeatureName.hasModal,
//					FeatureName.modalVerb,
//					FeatureName.depTmxPath,
//					FeatureName.tempSignalClusText,		//TimeBank-Dense
//					FeatureName.tempSignalPos,			//TimeBank-Dense
//					FeatureName.tempSignalDep1Dep2,		//TimeBank-Dense
					FeatureName.tempSignal1ClusText,	//TempEval3
					FeatureName.tempSignal1Pos,			//TempEval3
					FeatureName.tempSignal1Dep			//TempEval3
//					FeatureName.tempSignal2ClusText,
//					FeatureName.tempSignal2Pos,
//					FeatureName.tempSignal2Dep,
//					FeatureName.timexRule
			};
			featureList = Arrays.asList(etFeatures);
		}
	}
	
	public static List<PairFeatureVector> getEventTimexTlinksPerFile(Doc doc, PairClassifier etRelCls,
			boolean train, boolean goldCandidate, boolean ttFeature) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
		
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (train || goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();				//candidate pairs
		
		//timex-DCT rules
		Map<String,String> ttlinks = new HashMap<String, String>();
		if (ttFeature) {
			ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
		}
		
		for (TemporalRelation tlink : candidateTlinks) {
			
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_timex)) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
					
					if (!((Timex) etfv.getE2()).isDct()) {
						
						// Add features to feature vector
						for (FeatureName f : etRelCls.featureList) {
							if (etRelCls.classifier.equals(VectorClassifier.libsvm) ||
									etRelCls.classifier.equals(VectorClassifier.liblinear)) {								
								etfv.addBinaryFeatureToVector(f);
								
							} else if (etRelCls.classifier.equals(VectorClassifier.none)) {								
								etfv.addToVector(f);
							}
						}
						
						//Add timex-DCT TLINK type feature to feature vector
						if (ttFeature) {
							String timexDct = "O";
							if (ttlinks.containsKey(etfv.getE2().getID() + "\t" + doc.getDct().getID())) {
								timexDct = ttlinks.get(etfv.getE2().getID() + "\t" + doc.getDct().getID());
							}
							if (etRelCls.classifier.equals(VectorClassifier.libsvm) || 
									etRelCls.classifier.equals(VectorClassifier.liblinear)) {
								etfv.addBinaryFeatureToVector("timexDct", timexDct, EventTimexTemporalRule.ruleTlinkTypes);
								
							} else if (etRelCls.classifier.equals(VectorClassifier.none)){
								etfv.addToVector("timexDct", timexDct);
							}
						}
						
						if (etRelCls.classifier.equals(VectorClassifier.libsvm) || 
								etRelCls.classifier.equals(VectorClassifier.liblinear)) {
							if (train) etfv.addBinaryFeatureToVector(FeatureName.labelCollapsed);
							else etfv.addBinaryFeatureToVector(FeatureName.label);
							
						} else if (etRelCls.classifier.equals(VectorClassifier.none)){
							if (train) etfv.addToVector(FeatureName.labelCollapsed);
							else etfv.addToVector(FeatureName.label);
						}
						
						if (train && !etfv.getLabel().equals("NONE")) {
							fvList.add(etfv);
						} else if (!train) { //test
							//add all
							fvList.add(etfv);
						}
					}
				}
			}
		}
		
		return fvList;
	}
	
	public EventTimexTemporalClassifier(String taskName, 
			String classifier) throws Exception {
		super(taskName, classifier);
		initFeatureVector();
	}
	
	public EventTimexTemporalClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile);
		initFeatureVector();
	}
	
	public EventTimexTemporalClassifier(String taskName,
			String classifier, String inconsistency) throws Exception {
		super(taskName, classifier,
				inconsistency);
		initFeatureVector();
	}

	public EventTimexTemporalClassifier(String taskName, 
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
		
		System.err.println("Number of instances: " + nInstances);
		System.err.println("Number of features: " + vectors.get(0).getVectors().size());
		
		if (classifier.equals(VectorClassifier.liblinear)
				|| classifier.equals(VectorClassifier.logit)
				) {
			//Prepare training data
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
			
			//Train
			Problem problem = new Problem();
			problem.l = nInstances;
			problem.n = nFeatures;
			problem.x = instances;
			problem.y = labels;
			problem.bias = 1.0;
			
			SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL; // SVM, by default
			
			double C = 1.0;    // cost of constraints violation
			double eps = 0.01; // stopping criteria
			
			if (classifier.equals(VectorClassifier.logit)) {
				solver = SolverType.L2R_LR_DUAL; // Logistic Regression
			}

			Parameter parameter = new Parameter(solver, C, eps);
			Model model = Linear.train(problem, parameter);
			File modelFile = new File(modelPath);
			model.save(modelFile);
		}
	}
	
	public void train2(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		System.err.println("Train model...");

		int nInstances = vectors.size();
		int nFeatures = vectors.get(0).getFeatures().length-1;
		
		if (classifier.equals(VectorClassifier.liblinear)
				|| classifier.equals(VectorClassifier.logit)
				) {
			//Prepare training data
			Feature[][] instances = new Feature[nInstances][nFeatures];
			double[] labels = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				labels[row] = fv.getFeatures()[nFeatures];	//last column is label
				for (int i=0; i<nFeatures; i++) {
					instances[row][col] = new FeatureNode(idx, fv.getFeatures()[i]);
					idx ++;
					col ++;
				}
				row ++;
			}
			
			//Train
			Problem problem = new Problem();
			problem.l = nInstances;
			problem.n = nFeatures;
			problem.x = instances;
			problem.y = labels;
			problem.bias = 1.0;
			
			SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL; // SVM, by default
			
			double C = 1.0;    // cost of constraints violation
			double eps = 0.01; // stopping criteria
			
			if (classifier.equals(VectorClassifier.logit)) {
				solver = SolverType.L2R_LR_DUAL; // Logistic Regression
//				C = Math.pow(2.0, 0.0);
//				eps = Math.pow(2.0, -10.0);
			}

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
		    param.gamma = 0.125;
//		    param.nu = 0.5;
		    param.C = 8;
		    param.svm_type = svm_parameter.C_SVC;
		    param.kernel_type = svm_parameter.RBF; 
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
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getVectors().size()-1;
			
			if (classifier.equals(VectorClassifier.liblinear)
					|| classifier.equals(VectorClassifier.logit)
					) {
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
	}
	
	public List<String> predict(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		List<String> predictionLabels = new ArrayList<String>();
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getVectors().size()-1;
			
			if (classifier.equals(VectorClassifier.liblinear)
					|| classifier.equals(VectorClassifier.logit)
					) {
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
	
	public List<String> predict2(List<PairFeatureVector> vectors, String modelPath,
			String[] arrLabel) throws Exception {
		
//		System.err.println("Test model...");

		List<String> predictionLabels = new ArrayList<String>();
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getFeatures().length-1;
			
			if (classifier.equals(VectorClassifier.liblinear)
					|| classifier.equals(VectorClassifier.logit)
					) {
				//Prepare test data
				Feature[][] instances = new Feature[nInstances][nFeatures];
				double[] labels = new double[nInstances];
				
				int row = 0;
				for (PairFeatureVector fv : vectors) {			
					int idx = 1, col = 0;
					labels[row] = fv.getFeatures()[nFeatures];	//last column is label
					for (int i=0; i<nFeatures; i++) {
						instances[row][col] = new FeatureNode(idx, fv.getFeatures()[i]);
						idx ++;
						col ++;
					}
					row ++;
				}
				
				//Test
				File modelFile = new File(modelPath);
				Model model = Model.load(modelFile);
				for (Feature[] instance : instances) {
					predictionLabels.add(arrLabel[(int)Linear.predict(model, instance)-1]);
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
	
	public List<String> predictDense(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
		System.err.println("Test model...");
		
		List<String> predictionLabels = new ArrayList<String>();
		
		if (vectors.size() > 0) {

			int nInstances = vectors.size();
			int nFeatures = vectors.get(0).getVectors().size()-1;		
			
			if (classifier.equals(VectorClassifier.liblinear)
					|| classifier.equals(VectorClassifier.logit)) {
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
					predictionLabels.add(labelDense[(int)Linear.predict(model, instance)-1]);
				}
			}
		}
		
		return predictionLabels;
	}
	
	public List<String> predictDense(List<PairFeatureVector> vectors, svm_model model) throws Exception {
		
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
				int totalClasses = labelDense.length;
				double[] predictions = new double[nInstances];
				
				for (svm_node[] instance : instances) {
					int[] classes = new int[totalClasses];
			        svm.svm_get_labels(model, classes);
			        double[] probs = new double[totalClasses];
			        predictionLabels.add(labelDense[((int)svm.svm_predict_probability(model, instance, probs))-1]);
				}
			}
		}
		
		return predictionLabels;
	}
}
