package catena.model.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import catena.model.classifier.PairClassifier.VectorClassifier;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import evaluator.PairEvaluator;
import model.feature.CausalSignalList;
import model.feature.PairFeatureVector;
import model.feature.TemporalSignalList;
import model.feature.FeatureEnum.FeatureName;
import parser.TXPParser;
import parser.TimeMLParser;
import parser.entities.EntityEnum;
import server.RemoteServer;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;

public class EventDctRelationClassifier extends EventTimexRelationClassifier {
	
	private String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
	private String[] labelDense = {"BEFORE", "AFTER", "SIMULTANEOUS", 
			"INCLUDES", "IS_INCLUDED", "VAGUE"};
	
	protected void initFeatureVector() {
		
		super.setPairType(PairType.event_timex);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] etFeatures = {
					FeatureName.tokenSpace, FeatureName.lemmaSpace,
					FeatureName.tokenChunk,
					FeatureName.tempMarkerTextSpace
			};
			featureList = Arrays.asList(etFeatures);
			
		} else if (classifier.equals(VectorClassifier.yamcha)) {
			FeatureName[] etFeatures = {
					//FeatureName.tokenSpace, FeatureName.lemmaSpace, FeatureName.tokenChunk,
					/*FeatureName.token,*/ FeatureName.lemma,
					FeatureName.pos, /*FeatureName.mainpos,*/
					/*FeatureName.samePos,*/ /*FeatureName.sameMainPos,*/
					FeatureName.chunk,
//					FeatureName.entDistance, FeatureName.sentDistance, FeatureName.entOrder,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, /*Feature.polarity,*/
//					FeatureName.dct,
					/*FeatureName.timexType,*/ 				
					/*FeatureName.timexValueTemplate,*/
//					FeatureName.depTmxPath,				
					FeatureName.mainVerb,
					FeatureName.modalVerb,
//					FeatureName.tempSignalText,
//					FeatureName.tempSignalPos,
					/*FeatureName.tempMarkerClusTextPos,*/
					/*FeatureName.tempMarkerPos,*/ 
					/*FeatureName.tempMarkerDep1Dep2,*/
					/*FeatureName.timexRule*/
			};
			featureList = Arrays.asList(etFeatures);
			
		} else {
			FeatureName[] etFeatures = {
					FeatureName.pos, /*Feature.mainpos,*/
//					FeatureName.samePos, /*Feature.sameMainPos,*/
					FeatureName.chunk, 
//					FeatureName.entDistance, FeatureName.sentDistance, FeatureName.entOrder,
					FeatureName.eventClass, FeatureName.tense, FeatureName.aspect, FeatureName.polarity,
//					FeatureName.dct,
//					FeatureName.timexType, 				
					FeatureName.mainVerb,
					FeatureName.hasModal,
//					FeatureName.modalVerb, 
//					FeatureName.depTmxPath,
//					FeatureName.tempSignalClusText,
//					FeatureName.tempSignalPos,
//					FeatureName.tempSignalDep1Dep2,
//					FeatureName.tempSignal1ClusText,
//					FeatureName.tempSignal1Pos,
//					FeatureName.tempSignal1Dep
//					FeatureName.tempSignal2ClusText,
//					FeatureName.tempSignal2Pos,
//					FeatureName.tempSignal2Dep,
//					FeatureName.timexRule
			};
			featureList = Arrays.asList(etFeatures);
		}
	}
	
	public EventDctRelationClassifier(String taskName, 
			String classifier) throws Exception {
		super(taskName, classifier);
		initFeatureVector();
	}
	
	public EventDctRelationClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile);
		initFeatureVector();
	}
	
	public EventDctRelationClassifier(String taskName,
			String classifier, String inconsistency) throws Exception {
		super(taskName, classifier,
				inconsistency);
		initFeatureVector();
	}

	public EventDctRelationClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile,
			String inconsistency) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile, inconsistency);
		initFeatureVector();
	}
}
