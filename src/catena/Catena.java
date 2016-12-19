package catena;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.parser.entities.CLINK;
import catena.parser.entities.TLINK;
import catena.parser.entities.TemporalRelation;

import org.apache.commons.cli.*;

public class Catena {
	
	private boolean tlinkFeature;
	private boolean clinkPostEditing;
	
	public static void main(String[] args) throws Exception {
		Catena cat = new Catena(true, true);
		Options options = cat.getCatenaOptions();

		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Catena", options);

            System.exit(1);
            return;
        }
        
        String edModel = cmd.getOptionValue("edtemporal");
        String etModel = cmd.getOptionValue("ettemporal");
        String eeModel = cmd.getOptionValue("eetemporal");
        String eecModel = cmd.getOptionValue("eecausal");
        
        // ---------- TRAIN CATENA MODELS ---------- //
        boolean train = cmd.hasOption("b");
        String tempCorpus = cmd.getOptionValue("tempcorpus");
        String causCorpus = cmd.getOptionValue("causcorpus");
        if (train) {
        	cat.trainModels(tempCorpus, causCorpus,
        			edModel, etModel, eeModel, eecModel);
        }
        				
		// ---------- TEST CATENA MODELS ---------- //
        ParserConfig.textProDirpath = cmd.getOptionValue("textpro");
		ParserConfig.mateLemmatizerModel = cmd.getOptionValue("matelemma");
		ParserConfig.mateTaggerModel = cmd.getOptionValue("matetagger");
		ParserConfig.mateParserModel = cmd.getOptionValue("mateparser");
		
		String input = cmd.getOptionValue("input");
		File file = new File(input);
		if (file.isDirectory()) {
			System.out.println(cat.extractRelations(input, 
					edModel, etModel, eeModel, eecModel));
			
		} else if (file.isFile()) {
			System.out.println(cat.extractRelations(new File(input),
					edModel, etModel, eeModel, eecModel));
		}
	}
	
	public Options getCatenaOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input TimeML file/directory path");
		input.setRequired(true);
        options.addOption(input);

        Option textpro = new Option("x", "textpro", true, "TextPro directory path");
        textpro.setRequired(true);
        options.addOption(textpro);

        Option matelemma = new Option("l", "matelemma", true, "Mate tools' lemmatizer model path");
        matelemma.setRequired(true);
        options.addOption(matelemma);
        
        Option matetagger = new Option("g", "matetagger", true, "Mate tools' PoS tagger model path");
        matetagger.setRequired(true);
        options.addOption(matetagger);
        
        Option mateparser = new Option("p", "mateparser", true, "Mate tools' parser model path");
        mateparser.setRequired(true);
        options.addOption(mateparser);
        
        Option edtemporal = new Option("d", "edtemporal", true, "CATENA model path for E-D temporal classifier");
        edtemporal.setRequired(true);
        options.addOption(edtemporal);
        
        Option ettemporal = new Option("t", "ettemporal", true, "CATENA model path for E-T temporal classifier");
        ettemporal.setRequired(true);
        options.addOption(ettemporal);
        
        Option eetemporal = new Option("e", "eetemporal", true, "CATENA model path for E-E temporal classifier");
        eetemporal.setRequired(true);
        options.addOption(eetemporal);
        
        Option eecausal = new Option("c", "eecausal", true, "CATENA model path for E-E causal classifier");
        eecausal.setRequired(true);
        options.addOption(eecausal);
        
        Option trainmodels = new Option("b", "train", false, "Train the models");
        trainmodels.setRequired(false);
        options.addOption(trainmodels);
        
        Option temporaltrain = new Option("m", "tempcorpus", true, "TimeML directory path for training temporal classifiers");
        temporaltrain.setRequired(false);
        options.addOption(temporaltrain);
        
        Option causaltrain = new Option("u", "causcorpus", true, "TimeML directory path for training causal classifier");
        causaltrain.setRequired(false);
        options.addOption(causaltrain);
        
        return options;
	}
	
	public String extractRelations(String dirPath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel) throws Exception {
		
		StringBuilder results = new StringBuilder();
		File[] tmlFiles = new File(dirPath).listFiles();
		for (File tmlFile : tmlFiles) {	//assuming that there is no sub-directory
			
			if (tmlFile.getName().contains(".tml")) {
				System.err.println("Processing " + tmlFile.getPath());
				
				results.append(extractRelations(tmlFile,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel));
			}
		}
		
		return results.toString();
	}
	
	public String extractRelations(File tmlFile,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel) throws Exception {
		
		// ---------- TEMPORAL ---------- //
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp = new Temporal(false, te3CLabelCollapsed,
				eventDctModel,
				eventTimexModel,
				eventEventModel,
				true, true, true,
				true, false);
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		List<TLINK> tlinks = temp.extractRelations("catena", tmlFile, te3CLabelCollapsed, relTypeMapping);
		
		// ---------- CAUSAL ---------- //
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				causalModel,
				true, true);
		
		// PREDICT
		CLINK clinks;
		Map<String, Map<String, String>> tlinksForClinkPerFile = new HashMap<String, Map<String, String>>();
		if (this.isTlinkFeature()) {			
			for (String s : tlinks.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkPerFile.containsKey(cols[0])) tlinksForClinkPerFile.put(cols[0], new HashMap<String, String>());
				tlinksForClinkPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[4]);
				tlinksForClinkPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[4]));
			}
			clinks = causal.extractRelations("catena", tmlFile, null, causalLabel, 
					tlinksForClinkPerFile.get(tmlFile.getName()), te3CLabelCollapsed);
		} else {
			clinks = causal.extractRelations("catena", tmlFile, null, causalLabel, null, null);
		}
		
		// POST-EDITING
		if (this.isClinkPostEditing()) {
			for (String key : clinks.getEELinks().keySet()) {
				if (clinks.getEELinks().get(key).equals("CLINK")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "BEFORE");
					}
				} else if (clinks.getEELinks().get(key).equals("CLINK-R")) {
					if (tlinks.get(1).getEELinks().containsKey(key)) {
						tlinks.get(1).getEELinks().put(key, "AFTER");
					}
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(tlinks.get(1).TTLinksToString());
		sb.append(tlinks.get(1).EDLinksToString());
		sb.append(tlinks.get(1).ETLinksToString());
		sb.append(tlinks.get(1).EELinksToString());
		sb.append(clinks.EELinksToString());
		return sb.toString();
	}
	
	public void trainModels(String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel) throws Exception {
		trainModels("./data/Catena-train_TML/", "./data/Causal-TimeBank_TML/",
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel);
	}
	
	public void trainModels(String temporalTrainCorpus, String causalTrainCorpus,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel) throws Exception {
		
		// ---------- TEMPORAL ---------- //
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp = new Temporal(false, te3CLabelCollapsed,
				eventDctModel,
				eventTimexModel,
				eventEventModel,
				true, true, true,
				true, false);
		
		// TRAIN
		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
		relTypeMappingTrain.put("IBEFORE", "BEFORE");
		relTypeMappingTrain.put("IAFTER", "AFTER");
		temp.trainModels("catena", temporalTrainCorpus, te3CLabelCollapsed, relTypeMappingTrain);
		
		// ---------- CAUSAL ---------- //
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				causalModel,
				true, true);
		
		// TRAIN
		Map<String, Map<String, String>> tlinksForClinkTrainPerFile = new HashMap<String, Map<String, String>>();
		if (this.isTlinkFeature()) {	
			Map<String, String> relTypeMapping = new HashMap<String, String>();
			relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
			List<TLINK> tlinksTrain = temp.extractRelations("catena", temporalTrainCorpus, te3CLabelCollapsed, relTypeMapping);
			for (String s : tlinksTrain.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkTrainPerFile.containsKey(cols[0])) tlinksForClinkTrainPerFile.put(cols[0], new HashMap<String, String>());
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[3]);
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[3]));
			}
			
			causal.trainModels("catena", causalTrainCorpus, causalLabel, 
					tlinksForClinkTrainPerFile, te3CLabelCollapsed, relTypeMappingTrain);
		} else {
			causal.trainModels("catena", causalTrainCorpus, causalLabel, 
					null, null);
		}
		
	}
	
	Catena (boolean tlinkFeature, boolean clinkPostEditing) {
		setTlinkFeature(tlinkFeature);
		setClinkPostEditing(clinkPostEditing);
	}

	public boolean isTlinkFeature() {
		return tlinkFeature;
	}

	public void setTlinkFeature(boolean tlinkFeature) {
		this.tlinkFeature = tlinkFeature;
	}

	public boolean isClinkPostEditing() {
		return clinkPostEditing;
	}

	public void setClinkPostEditing(boolean clinkPostEditing) {
		this.clinkPostEditing = clinkPostEditing;
	}
}
