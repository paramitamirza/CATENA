package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bwaldvogel.liblinear.*;

import catena.evaluator.PairEvaluator;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.rule.EventTimexTemporalRule;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;


public class EventTimexTemporalClassifier extends PairClassifier {
	
	protected void initFeatureVector() {
		
		super.setPairType(PairType.event_timex);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] etFeatures = {
					FeatureName.pos
//					FeatureName.tokenSpace, 
//					FeatureName.lemmaSpace,
//					FeatureName.tokenChunk,
//					FeatureName.tempMarkerTextSpace
			};
			featureList = Arrays.asList(etFeatures);
			
		} else {
			if (getName().equals("tbdense")) {
				FeatureName[] etFeatures = {
						FeatureName.pos,
//						Feature.mainpos,
						FeatureName.samePos,
//						Feature.sameMainPos,
						FeatureName.chunk,
						FeatureName.entDistance, FeatureName.sentDistance, FeatureName.entOrder,
						FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
						FeatureName.dct,
						FeatureName.timexType, 				
						FeatureName.mainVerb, 
						FeatureName.hasModal,
//						FeatureName.modalVerb,
//						FeatureName.depTmxPath,
						FeatureName.tempSignalClusText,		//TimeBank-Dense
						FeatureName.tempSignalPos,			//TimeBank-Dense
						FeatureName.tempSignalDep1Dep2,		//TimeBank-Dense
//						FeatureName.tempSignal1ClusText,	//TempEval3
//						FeatureName.tempSignal1Pos,			//TempEval3
//						FeatureName.tempSignal1Dep,			//TempEval3
//						FeatureName.tempSignal2ClusText,
//						FeatureName.tempSignal2Pos,
//						FeatureName.tempSignal2Dep,
//						FeatureName.timexRule
				};
				featureList = Arrays.asList(etFeatures);
			} else {
				FeatureName[] etFeatures = {
						FeatureName.pos,
//						Feature.mainpos,
						FeatureName.samePos,
//						Feature.sameMainPos,
						FeatureName.chunk,
						FeatureName.entDistance, FeatureName.sentDistance, FeatureName.entOrder,
						FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
//						FeatureName.dct,
						FeatureName.timexType, 				
						FeatureName.mainVerb, 
						FeatureName.hasModal,
//						FeatureName.modalVerb,
//						FeatureName.depTmxPath,
//						FeatureName.tempSignalClusText,		//TimeBank-Dense
//						FeatureName.tempSignalPos,			//TimeBank-Dense
//						FeatureName.tempSignalDep1Dep2,		//TimeBank-Dense
						FeatureName.tempSignal1ClusText,	//TempEval3
						FeatureName.tempSignal1Pos,			//TempEval3
						FeatureName.tempSignal1Dep,			//TempEval3
//						FeatureName.tempSignal2ClusText,
//						FeatureName.tempSignal2Pos,
//						FeatureName.tempSignal2Dep,
//						FeatureName.timexRule
				};
				featureList = Arrays.asList(etFeatures);
			}
		}
	}
	
	public static List<PairFeatureVector> getEventTimexTlinksPerFile(Doc doc, PairClassifier etRelCls,
			boolean train, boolean goldCandidate,
			List<String> labelList,
			Map<String, String> ttLinks) throws Exception {
		return getEventTimexTlinksPerFile(doc, etRelCls,
				train, goldCandidate,
				labelList, new HashMap<String, String>(), ttLinks);
	}
	
	public static List<PairFeatureVector> getEventTimexTlinksPerFile(Doc doc, PairClassifier etRelCls,
			boolean train, boolean goldCandidate, List<String> labelList,
			Map<String, String> relTypeMapping,
			Map<String, String> ttLinks) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
		
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (train || goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();				//candidate pairs
		
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
							if (etRelCls.classifier.equals(VectorClassifier.liblinear) ||
									etRelCls.classifier.equals(VectorClassifier.logit)) {								
								etfv.addBinaryFeatureToVector(f);
								
							} else if (etRelCls.classifier.equals(VectorClassifier.none)) {								
								etfv.addToVector(f);
							}
						}
						
						//Add timex-DCT TLINK type feature to feature vector
						if (ttLinks != null) {
							String timexDct = "O";
							if (ttLinks.containsKey(etfv.getE2().getID() + "," + doc.getDct().getID())) {
								timexDct = ttLinks.get(etfv.getE2().getID() + "," + doc.getDct().getID());
							}
							if (etRelCls.classifier.equals(VectorClassifier.liblinear) || 
									etRelCls.classifier.equals(VectorClassifier.logit)) {
								etfv.addBinaryFeatureToVector("timexDct", timexDct, EventTimexTemporalRule.ruleTlinkTypes);
								
							} else if (etRelCls.classifier.equals(VectorClassifier.none)){
								etfv.addToVector("timexDct", timexDct);
							}
						}
						
						String label = etfv.getLabel();
						if (relTypeMapping.containsKey(label)) label = relTypeMapping.get(label);
						if (etRelCls.classifier.equals(VectorClassifier.liblinear) || 
								etRelCls.classifier.equals(VectorClassifier.logit)) {
							etfv.addToVector("label", String.valueOf(labelList.indexOf(label)+1));
							
						} else if (etRelCls.classifier.equals(VectorClassifier.none)){
							etfv.addToVector("label", label);
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
	
	public void evaluate(List<PairFeatureVector> vectors, 
			String modelPath, String[] relTypes) throws Exception {
		
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
				pe.evaluatePerLabelIdx(relTypes);
			}
		}
	}
	
	public List<String> predict(List<PairFeatureVector> vectors, 
			String modelPath, String[] relTypes) throws Exception {
		
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
					predictionLabels.add(relTypes[(int)Linear.predict(model, instance)-1]);
				}
			}
		}
		
		return predictionLabels;
	}
	
	public List<String> predict2(List<PairFeatureVector> vectors, 
			String modelPath, String[] arrLabel) throws Exception {

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
}
