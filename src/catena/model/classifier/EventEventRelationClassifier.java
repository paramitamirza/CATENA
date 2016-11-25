package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.classifier.PairClassifier.VectorClassifier;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Doc;
import catena.model.classifier.EventTimexRelationClassifier;
import catena.model.classifier.PairClassifier;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.rule.EventEventRelationRule;
import catena.model.rule.EventTimexRelationRule;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;
import catena.evaluator.PairEvaluator;

import de.bwaldvogel.liblinear.*;

public class EventEventRelationClassifier extends PairClassifier {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	
	private void initFeatureVector() {
		
		super.setPairType(PairType.event_event);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] eeFeatures = {
					FeatureName.tokenSpace, FeatureName.lemmaSpace,
					FeatureName.tokenChunk,
					FeatureName.tempMarkerTextSpace, FeatureName.causMarkerTextSpace
			};
			featureList = Arrays.asList(eeFeatures);
			
		} else {
			FeatureName[] eeFeatures = {
					FeatureName.pos, /*Feature.mainpos,*/
					FeatureName.samePos, /*Feature.sameMainPos,*/
					FeatureName.chunk,
					FeatureName.entDistance, FeatureName.sentDistance,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
					FeatureName.sameEventClass, FeatureName.sameTenseAspect, /*Feature.sameAspect,*/ FeatureName.samePolarity,
					FeatureName.depEvPath,			
					FeatureName.mainVerb,
					FeatureName.hasModal,
//					FeatureName.modalVerb,
//					FeatureName.tempSignalClusText,
//					FeatureName.tempSignalPos,
//					FeatureName.tempSignalDep1Dep2,
//					FeatureName.tempSignal1ClusText,
//					FeatureName.tempSignal1Pos,
//					FeatureName.tempSignal1Dep,
					FeatureName.tempSignal2ClusText,
					FeatureName.tempSignal2Pos,
					FeatureName.tempSignal2Dep,
//					FeatureName.causMarkerClusText,
//					FeatureName.causMarkerPos,
//					/*FeatureName.coref,*/
					FeatureName.wnSim
			};
			featureList = Arrays.asList(eeFeatures);
		}
	}
	
	public List<PairFeatureVector> getEventEventTlinksPerFile(Doc doc, PairClassifier eeRelCls,
			boolean train, boolean goldCandidate, boolean etFeature) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (train || goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();				//candidate pairs
		
		//event-DCT rules
		Map<String, String> eDctRules = new HashMap<String, String>();
		if (etFeature) {
			EventDctRelationClassifier dctCls = new EventDctRelationClassifier("te3", "liblinear");
			List<PairFeatureVector> etFvList = EventDctRelationClassifier.getEventDctTlinksPerFile(doc, dctCls, 
					train, goldCandidate);
			
			for (PairFeatureVector fv : etFvList) {
				EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
				EventTimexRelationRule etRule = new EventTimexRelationRule((Event) etfv.getE1(), (Timex) etfv.getE2(), 
						doc, etfv.getMateDependencyPath());
				if (!etRule.getRelType().equals("O")) {
					eDctRules.put(etfv.getE1().getID(), etRule.getRelType());
				}
			}
		}
		
		for (TemporalRelation tlink : candidateTlinks) {	
			
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					
					//Add features to feature vector
					for (FeatureName f : eeRelCls.featureList) {
						if (eeRelCls.classifier.equals(VectorClassifier.libsvm) ||
								eeRelCls.classifier.equals(VectorClassifier.liblinear)) {								
							eefv.addBinaryFeatureToVector(f);
							
						} else if (eeRelCls.classifier.equals(VectorClassifier.none)) {								
							eefv.addToVector(f);
						}
					}
					
					//Add event-timex/DCT TLINK type feature to feature vector
					if (etFeature) {
						String etRule1 = "O", etRule2 = "O";
						if (eDctRules.containsKey(eefv.getE1().getID())) etRule1 = eDctRules.get(eefv.getE1().getID());
						if (eDctRules.containsKey(eefv.getE2().getID())) etRule2 = eDctRules.get(eefv.getE2().getID());
						if (eeRelCls.classifier.equals(VectorClassifier.libsvm) || 
								eeRelCls.classifier.equals(VectorClassifier.liblinear)) {
							eefv.addBinaryFeatureToVector("etRule1", etRule1, EventEventRelationRule.ruleTlinkTypes);
							eefv.addBinaryFeatureToVector("etRule2", etRule2, EventEventRelationRule.ruleTlinkTypes);
						} else if (eeRelCls.classifier.equals(VectorClassifier.none)){
							eefv.addToVector("etRule1", etRule1);
							eefv.addToVector("etRule2", etRule2);
						}
					}					
					
					if (eeRelCls.classifier.equals(VectorClassifier.libsvm) || 
							eeRelCls.classifier.equals(VectorClassifier.liblinear)) {
						if (train) eefv.addBinaryFeatureToVector(FeatureName.labelCollapsed);
						else eefv.addBinaryFeatureToVector(FeatureName.label);
						
					} else if (eeRelCls.classifier.equals(VectorClassifier.none)){
						if (train) eefv.addToVector(FeatureName.labelCollapsed);
						else eefv.addToVector(FeatureName.label);
					}
					
					if (train && !eefv.getLabel().equals("NONE")) {
						fvList.add(eefv);
					} else if (!train) { //test
						//add all
						fvList.add(eefv);
					}
				}
			}
		}
		return fvList;
	}
	
	public EventEventRelationClassifier(String taskName, 
			String classifier) throws Exception {
		super(taskName, classifier);
		initFeatureVector();
	}
	
	public EventEventRelationClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile);
		initFeatureVector();
	}
	
	public EventEventRelationClassifier(String taskName,
			String classifier, String inconsistency) throws Exception {
		super(taskName, classifier,
				inconsistency);
		initFeatureVector();
	}

	public EventEventRelationClassifier(String taskName, 
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
		
		if (classifier.equals(VectorClassifier.liblinear)
				|| classifier.equals(VectorClassifier.logit)
				) {
			//Prepare training data
			Feature[][] instances = new Feature[nInstances][nFeatures];
			double[] labels = new double[nInstances];
			
			int row = 0;
			for (PairFeatureVector fv : vectors) {				
				int idx = 1, col = 0;
				labels[row] = Double.valueOf(fv.getVectors().get(nFeatures));	//last column is label
				for (int i=0; i<nFeatures; i++) {
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
					System.out.println(((int)labels[i]) + "\t" + ((int)predictions[i]));
				}
				
				PairEvaluator pe = new PairEvaluator(result);
				pe.evaluatePerLabelIdx(label);
			}
		}
	}
	
	public List<String> predict(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
//		System.err.println("Test model...");

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
	
	public List<String> predictProbs(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
//		System.err.println("Test model...");

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
				int[] mLabels = model.getLabels();
				for (int i : mLabels) System.out.print(label[i-1] + " ");
				System.out.println();
				double[] dec_values = new double[model.getNrClass()];
				for (Feature[] instance : instances) {
//					predictionLabels.add(label[(int)Linear.predict(model, instance)-1]);
					predictionLabels.add(label[(int)Linear.predictProbability(model, instance, dec_values)-1]);
//					System.out.print((int)Linear.predict(model, instance) + "\t");
					for (double d : dec_values) System.out.print(d + " ");
					System.out.println();
				}
			}
		}
		
		return predictionLabels;
	}
	
	public List<String> predictProbs2(List<PairFeatureVector> vectors, String modelPath,
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
				
//				int[] mLabels = model.getLabels();
//				for (int i : mLabels) System.out.print(label[i-1] + " ");
//				System.out.println();
				
				double[] dec_values = new double[model.getNrClass()];
				String line, lbl;
				for (Feature[] instance : instances) {
//					predictionLabels.add(label[(int)Linear.predictProbability(model, instance, dec_values)-1]);
//					System.out.print((int)Linear.predictProbability(model, instance, dec_values) 
//							+ " " + label[(int)Linear.predictProbability(model, instance, dec_values)-1]
//							+ ": ");
					line = "";
					lbl = arrLabel[(int)Linear.predictProbability(model, instance, dec_values)-1];
					for (double d : dec_values) line += d + ",";
					predictionLabels.add(lbl + "#" + line.substring(0, line.length()-1));
				}
			}
		}
		
		return predictionLabels;
	}
	
	public List<String> predictBinary(List<PairFeatureVector> vectors, String modelPath) throws Exception {
		
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
					if ((int)Linear.predict(model, instance) == 0) predictionLabels.add("NONE");
					else predictionLabels.add("TLINK");
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
}
