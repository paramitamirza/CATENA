package catena.model.classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.model.feature.CausalSignalList;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;

public class EventDctTemporalClassifier extends EventTimexTemporalClassifier {
	
	protected void initFeatureVector() {
		
		super.setPairType(PairType.event_timex);
				
		if (classifier.equals(VectorClassifier.none)) {
			FeatureName[] etFeatures = {
					FeatureName.tokenSpace, FeatureName.lemmaSpace,
					FeatureName.tokenChunk,
					FeatureName.tempMarkerTextSpace
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
	
	public static List<PairFeatureVector> getEventDctTlinksPerFile(Doc doc, PairClassifier etRelCls,
			boolean train, boolean goldCandidate,
			List<String> labelList) throws Exception {
		return getEventDctTlinksPerFile(doc, etRelCls,
				train, goldCandidate,
				labelList, new HashMap<String, String>());
	}
	
	public static List<PairFeatureVector> getEventDctTlinksPerFile(Doc doc, PairClassifier etRelCls,
			boolean train, boolean goldCandidate,
			List<String> labelList,
			Map<String, String> relTypeMapping) throws Exception {
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
					
					if (((Timex) etfv.getE2()).isDct()) {
						
						// Add features to feature vector
						for (FeatureName f : etRelCls.featureList) {
							if (etRelCls.classifier.equals(VectorClassifier.liblinear) ||
									etRelCls.classifier.equals(VectorClassifier.logit)) {								
								etfv.addBinaryFeatureToVector(f);
								
							} else if (etRelCls.classifier.equals(VectorClassifier.none)) {								
								etfv.addToVector(f);
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
	
	public EventDctTemporalClassifier(String taskName, 
			String classifier) throws Exception {
		super(taskName, classifier);
		initFeatureVector();
	}
	
	public EventDctTemporalClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile);
		initFeatureVector();
	}
	
	public EventDctTemporalClassifier(String taskName,
			String classifier, String inconsistency) throws Exception {
		super(taskName, classifier,
				inconsistency);
		initFeatureVector();
	}

	public EventDctTemporalClassifier(String taskName, 
			String classifier, String feature, 
			String lblGrouping, String probVecFile,
			String inconsistency) throws Exception {
		super(taskName, classifier,
				feature, lblGrouping, probVecFile, inconsistency);
		initFeatureVector();
	}
}
