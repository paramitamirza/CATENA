package catena.model.rule;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.ColumnParser.Field;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class TestTimexTimexRelationRuleTempEval3 {
	
	public TestTimexTimexRelationRuleTempEval3() {
		
	}
	
	public Map<String,String> getTimexTimexRuleRelation(Doc doc) {
		Object[] entArr = doc.getEntities().keySet().toArray();
		Map<String,String> ttlinks = new HashMap<String,String>();
		String pair = null;
		for (int i = 0; i < entArr.length; i++) {
			for (int j = i; j < entArr.length; j++) {
				if (!entArr[i].equals(entArr[j]) && doc.getEntities().get(entArr[i]) instanceof Timex && 
						doc.getEntities().get(entArr[j]) instanceof Timex) {
					TimexTimexRelationRule timextimex = 
							new TimexTimexRelationRule(((Timex)doc.getEntities().get(entArr[i])), 
							((Timex)doc.getEntities().get(entArr[j])), doc.getDct(), false);
					if (!timextimex.getRelType().equals("O")) {
						pair = ((String) entArr[i]) + "," + ((String) entArr[j]);
						ttlinks.put(pair, timextimex.getRelType());
					}
				}
			}
		}
		return ttlinks;
	}
	
	public List<String> getTimexTimexTlinksPerFile(Doc doc, boolean goldCandidate) throws Exception {
		List<String> tt = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		//Determine the relation type of every timex-timex pair in the document via rules 
		Map<String,String> ttlinks = getTimexTimexRuleRelation(doc);
		
		List<TemporalRelation> candidateTlinks = new ArrayList<TemporalRelation> ();
		if (goldCandidate) candidateTlinks = doc.getTlinks();	//gold annotated pairs
		else candidateTlinks = doc.getCandidateTlinks();		//candidate pairs
		
		for (TemporalRelation tlink : candidateTlinks) {
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& doc.getEntities().containsKey(tlink.getSourceID())
					&& doc.getEntities().containsKey(tlink.getTargetID())
					&& !tlink.getRelType().equals("NONE")
					) {	//classifying the relation task
				
				Entity e1 = doc.getEntities().get(tlink.getSourceID());
				Entity e2 = doc.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.timex_timex)) {
					String st = tlink.getSourceID() + "," + tlink.getTargetID();
					String ts = tlink.getTargetID() + "," + tlink.getSourceID();
					if (ttlinks.containsKey(st)) {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
								tlink.getRelType() + "\t" + ttlinks.get(st));
					} else if (ttlinks.containsKey(ts)) {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
								tlink.getRelType() + "\t" + TemporalRelation.getInverseRelation(ttlinks.get(ts)));
					} else {
						tt.add(tlink.getSourceID() + "\t" + tlink.getTargetID() + "\t" + 
								tlink.getRelType() + "\tNONE");
					}
				}
			}
		}
		return tt;
	}
	
	public List<String> getTimexTimexTlinks(String tmlDirpath, 
			TimeMLToColumns tmlToCol, ColumnParser colParser, 
			boolean goldCandidate) throws Exception {
		
		List<String> tt = new ArrayList<String>();
		
		File[] tmlFiles = new File(tmlDirpath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			System.out.println("Processing " + tmlFile.getPath());
			
			// File pre-processing...
			List<String> columns = tmlToCol.convert(tmlFile, false);
			Doc doc = colParser.parseLines(columns);
			TimeMLParser.parseTimeML(tmlFile, doc);
			ColumnParser.setCandidateTlinks(doc);
			
			// Applying rules...
			List<String> ttPerFile = getTimexTimexTlinksPerFile(doc, goldCandidate);
			tt.addAll(ttPerFile);
			
			PairEvaluator pe = new PairEvaluator(ttPerFile);
			pe.printIncorrectAndSentence(doc);
		}		
		return tt;
	}

	public static void main(String [] args) throws Exception {
		
		TimeMLToColumns tmlToCol = new TimeMLToColumns();
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		String tmlDirpath = "./data/TempEval3-eval_TML/";
		boolean goldCandidate = false;
		TestTimexTimexRelationRuleTempEval3 test = new TestTimexTimexRelationRuleTempEval3();
		List<String> ttResult = test.getTimexTimexTlinks(tmlDirpath, tmlToCol, colParser, goldCandidate);

		PairEvaluator pe = new PairEvaluator(ttResult);
		pe.evaluatePerLabel(); 
		
	}
}
