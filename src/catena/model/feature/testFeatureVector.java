package catena.model.feature;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import catena.model.feature.FeatureEnum.*;
import catena.model.rule.TimexTimexRelationRule;
import catena.parser.TXPParser;
import catena.parser.TXPParser.Field;
import catena.parser.entities.*;

public class testFeatureVector {
	
	public static void getFeatureVector(TXPParser parser, String filepath, TemporalSignalList tsignalList, CausalSignalList csignalList) throws Exception {
		File dir_TXP = new File(filepath);
		File[] files_TXP = dir_TXP.listFiles();
		
		if (files_TXP == null) return;
		
		for (File file : files_TXP) {
			if (file.isDirectory()){
				
				getFeatureVector(parser, file.getPath(), tsignalList, csignalList);
				
			} else if (file.isFile()) {				
				Doc doc = parser.parseDocument(file.getPath());
				
				Object[] entArr = doc.getEntities().keySet().toArray();
				
				for (int i = 0; i < entArr.length; i++) {
					for (int j = i; j < entArr.length; j++) {
						if (!entArr[i].equals(entArr[j]) && doc.getEntities().get(entArr[i]) instanceof Timex && 
								doc.getEntities().get(entArr[j]) instanceof Timex) {
							TimexTimexRelationRule tt = new TimexTimexRelationRule(((Timex)doc.getEntities().get(entArr[i])), 
									((Timex)doc.getEntities().get(entArr[j])), doc.getDct(), false);
							//System.out.println(entArr[i] + "\t" + entArr[j] + "\t" + tt.getRelType());
						}
					}
				}
				
				for (TemporalRelation tlink : doc.getTlinks()) {
					if (!tlink.getSourceID().equals(tlink.getTargetID()) &&
							doc.getEntities().containsKey(tlink.getSourceID()) &&
							doc.getEntities().containsKey(tlink.getTargetID())) {
						//System.out.println(file.getName() + "\t " + tlink.getSourceID() + "-" + tlink.getTargetID());
						Entity e1 = doc.getEntities().get(tlink.getSourceID());
						Entity e2 = doc.getEntities().get(tlink.getTargetID());
						
						PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);		
						if (fv.getPairType().equals(PairType.event_event)) {
							fv = new EventEventFeatureVector(fv);
						} else if (fv.getPairType().equals(PairType.event_timex)) {
							fv = new EventTimexFeatureVector(fv);
						}
						
						fv.addToVector(FeatureName.id);
						
						//token attribute features
						fv.addToVector(FeatureName.token);
						fv.addToVector(FeatureName.lemma);
						fv.addToVector(FeatureName.pos);
						fv.addToVector(FeatureName.mainpos);
						fv.addToVector(FeatureName.chunk);
						fv.addToVector(FeatureName.ner);
						fv.addToVector(FeatureName.samePos);
						fv.addToVector(FeatureName.sameMainPos);
						
						//context features
						fv.addToVector(FeatureName.entDistance);
						fv.addToVector(FeatureName.sentDistance);
						
						if (fv instanceof EventEventFeatureVector) {
							//Entity attributes
							fv.addToVector(FeatureName.eventClass);
							fv.addToVector(FeatureName.tense);
							fv.addToVector(FeatureName.aspect);
							fv.addToVector(FeatureName.polarity);
							fv.addToVector(FeatureName.sameEventClass);
							fv.addToVector(FeatureName.sameTense);
							fv.addToVector(FeatureName.sameAspect);
							fv.addToVector(FeatureName.samePolarity);
							
							//dependency information
							fv.addToVector(FeatureName.depPath);
							fv.addToVector(FeatureName.mainVerb);
							
							//temporal connective/signal
							fv.addToVector(FeatureName.tempMarker);
							
							//causal connective/signal/verb
							fv.addToVector(FeatureName.causMarker);
							
							//event co-reference
							fv.addToVector(FeatureName.coref);
							
							//WordNet similarity
							fv.addToVector(FeatureName.wnSim);
							
						} else if (fv instanceof EventTimexFeatureVector) {
							fv.addToVector(FeatureName.entOrder);
							
							//Entity attributes
							fv.addToVector(FeatureName.eventClass);
							fv.addToVector(FeatureName.tense);
							fv.addToVector(FeatureName.aspect);
							fv.addToVector(FeatureName.polarity);
							fv.addToVector(FeatureName.timexType);
							fv.addToVector(FeatureName.timexValueTemplate);
							fv.addToVector(FeatureName.dct);
							
							//dependency information
							fv.addToVector(FeatureName.depPath);
							fv.addToVector(FeatureName.mainVerb);
							
							//temporal connective/signal
							fv.addToVector(FeatureName.tempMarker);
							
							//timex rule type
							fv.addToVector(FeatureName.timexRule);
						}
						
						fv.addToVector(FeatureName.label);
						
						//if (fv instanceof EventEventFeatureVector) {
						//	System.out.println(fv.printVectors());
						//} else if (fv instanceof EventTimexFeatureVector) {
						//	System.out.println(fv.printVectors());
						//}
					}
				}
			}
		}
		
		
	}
	
	public static void main(String [] args) {
		
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, Field.coref_event, Field.tlink};
		TXPParser parser = new TXPParser(EntityEnum.Language.EN, fields);
		
		//dir_TXP <-- data/example_TXP
		try {
			TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
			CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
			
			getFeatureVector(parser, args[0], tsignalList, csignalList);
			
			for (String s : EventEventFeatureVector.fields) {
				System.out.println(s);
			}
			
			for (String s : EventTimexFeatureVector.fields) {
				System.out.println(s);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
