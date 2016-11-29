package catena.model.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.PairFeatureVector;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.model.feature.Marker;
import catena.parser.entities.CausalRelation;
import catena.parser.entities.Doc;
import catena.model.CandidateLinks;
import catena.model.classifier.PairClassifier;
import catena.model.classifier.PairClassifier.VectorClassifier;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.rule.EventEventTemporalRule;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.evaluator.PairEvaluator;

import de.bwaldvogel.liblinear.*;

public class EventEventCausalClassifier extends PairClassifier {
	
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
	
	public static List<PairFeatureVector> getEventEventClinksPerFile(Doc doc, PairClassifier eeRelCls,
			boolean train, List<String> labelList) throws Exception {
		return getEventEventClinksPerFile(doc, eeRelCls,
				train, labelList, 
				new HashMap<String, String>(),
				null,
				null);
	}
	
	public static List<PairFeatureVector> getEventEventClinksPerFile(Doc doc, PairClassifier eeRelCls,
			boolean train, List<String> labelList, 
			Map<String, String> relTypeMapping,
			Map<String, String> tlinks,
			List<String> tlinkLabels) throws Exception {
		List<PairFeatureVector> fvList = new ArrayList<PairFeatureVector>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<CausalRelation> candidateClinks = doc.getCandidateClinks();	//candidate pairs
		
		for (CausalRelation clink : candidateClinks) {
			
			if (!clink.getSourceID().equals(clink.getTargetID())
					&& doc.getEntities().containsKey(clink.getSourceID())
					&& doc.getEntities().containsKey(clink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(clink.getSourceID());
				Entity e2 = doc.getEntities().get(clink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, 
						CandidateLinks.getClinkType(e1.getID(), e2.getID(), doc), tsignalList, csignalList);
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					
					//Add features to feature vector
					for (FeatureName f : eeRelCls.featureList) {
						if (eeRelCls.classifier.equals(VectorClassifier.liblinear) ||
								eeRelCls.classifier.equals(VectorClassifier.logit)) {								
							eefv.addBinaryFeatureToVector(f);
							
						} else if (eeRelCls.classifier.equals(VectorClassifier.none)) {								
							eefv.addToVector(f);
						}
					}
					
					//Add TLINK type feature to feature vector
					if (tlinks != null) {
						String tlinkType = "O";
						if (tlinks.containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())) 
							tlinkType = tlinks.get(eefv.getE1().getID() + "," + eefv.getE2().getID());
						
						if (eeRelCls.classifier.equals(VectorClassifier.liblinear) || 
								eeRelCls.classifier.equals(VectorClassifier.logit)) {
							eefv.addBinaryFeatureToVector("tlink", tlinkType, tlinkLabels);
						} else if (eeRelCls.classifier.equals(VectorClassifier.none)){
							eefv.addToVector("tlink", tlinkType);
						}
					}
					
					String label = eefv.getLabel();
					if (relTypeMapping.containsKey(label)) label = relTypeMapping.get(label);
					if (eeRelCls.classifier.equals(VectorClassifier.liblinear) || 
							eeRelCls.classifier.equals(VectorClassifier.logit)) {
						eefv.addToVector("label", String.valueOf(labelList.indexOf(label)+1));
						
					} else if (eeRelCls.classifier.equals(VectorClassifier.none)){
						eefv.addToVector("label", label);
					}
					
					String depEvPathStr = eefv.getMateDependencyPath();
					Marker m = fv.getCausalSignal();
					if ((!m.getDepRelE1().equals("O") || !m.getDepRelE2().equals("O"))
							&& (!depEvPathStr.equals("SBJ")
									&& !depEvPathStr.equals("OBJ")
									&& !depEvPathStr.equals("COORD-CONJ")
									&& !depEvPathStr.equals("LOC-PMOD")
									&& !depEvPathStr.equals("VC")
									&& !depEvPathStr.equals("OPRD")
									&& !depEvPathStr.equals("OPRD-IM")
									)
							&& (eefv.getEntityDistance() < 5
//									&& eefv.getEntityDistance() >= 0
									)
							) {
					
						fvList.add(eefv);
					}
				}
			}
		}		
		return fvList;
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
	
	public void evaluate(List<PairFeatureVector> vectors, 
			String modelPath, String[] relTypes) throws Exception {

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
}
