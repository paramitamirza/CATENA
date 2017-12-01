package catena;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.parser.entities.CLINK;
import catena.parser.entities.ExtractedLinks;
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
        
        boolean columnFormat = cmd.hasOption("f");
        
        // ---------- TRAIN CATENA MODELS ---------- //
        boolean train = cmd.hasOption("b");
        String tempCorpus = cmd.getOptionValue("tempcorpus");
        String causCorpus = cmd.getOptionValue("causcorpus");
        
        if (train) {
        	if (columnFormat) {
        		if (!cmd.hasOption("tlinks") || !cmd.hasOption("clinks")) {
        			System.err.println("Input files containing gold TLINKs and CLINKs are missing!");
        			return;
        		} else {
        			cat.trainModels(tempCorpus, causCorpus,
        					cmd.getOptionValue("tlinks"), cmd.getOptionValue("clinks"),  
    	        			edModel, etModel, eeModel, eecModel, columnFormat);
        		}
        	} else {
	        	cat.trainModels(tempCorpus, causCorpus,
	        			edModel, etModel, eeModel, eecModel, columnFormat);
        	}
        }
        				
		// ---------- TEST CATENA MODELS ---------- //
        ParserConfig.textProDirpath = cmd.getOptionValue("textpro");
		ParserConfig.mateLemmatizerModel = cmd.getOptionValue("matelemma");
		ParserConfig.mateTaggerModel = cmd.getOptionValue("matetagger");
		ParserConfig.mateParserModel = cmd.getOptionValue("mateparser");
		
		boolean clinkType = cmd.hasOption("y");		//Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve
		
		String input = cmd.getOptionValue("input");
		File file = new File(input);
		String tlinkFilepath = "";
		if (cmd.hasOption("tlinks")) tlinkFilepath = cmd.getOptionValue("tlinks"); 
		String clinkFilepath = "";
		if (cmd.hasOption("clinks")) clinkFilepath = cmd.getOptionValue("clinks");
		if (file.isDirectory()) {
			System.out.println(cat.extractRelationsString(input, 
					tlinkFilepath, clinkFilepath, 
					edModel, etModel, eeModel, eecModel, columnFormat, clinkType));
			
		} else if (file.isFile()) {
			System.out.println(cat.extractRelationsString(new File(input),
					tlinkFilepath, clinkFilepath,
					edModel, etModel, eeModel, eecModel, columnFormat, clinkType));
		}
	}
	
	public Options getCatenaOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input file/directory path (.tml or .col)");
		input.setRequired(true);
        options.addOption(input);
        
        Option inputTlinks = new Option("tl", "tlinks", true, "Input file containing list of temporal links");
        inputTlinks.setRequired(false);
        options.addOption(inputTlinks);
        
        Option inputClinks = new Option("cl", "clinks", true, "Input file containing list of causal links");
        inputClinks.setRequired(false);
        options.addOption(inputClinks);
        
        Option colFiles = new Option("f", "col", false, "Input files are in column (.col) format resulted from converting TimeML files into column format");
        colFiles.setRequired(false);
        options.addOption(colFiles);
        
        Option clinkType = new Option("y", "clinktype", false, "Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve");
        clinkType.setRequired(false);
        options.addOption(clinkType);

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
        
        Option temporaltrain = new Option("m", "tempcorpus", true, "Directory path (containing .tml or .col files) for training temporal classifiers");
        temporaltrain.setRequired(false);
        options.addOption(temporaltrain);
        
        Option causaltrain = new Option("u", "causcorpus", true, "Directory path (containing .tml or .col files) for training causal classifier");
        causaltrain.setRequired(false);
        options.addOption(causaltrain);
        
        return options;
	}
	
	public String extractRelationsString(String dirPath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		StringBuilder results = new StringBuilder();
		File[] files = new File(dirPath).listFiles();
		
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
				System.err.println("Processing " + file.getPath());
				
				results.append(extractRelationsString(file,
						"", "",
						te3CLabelCollapsed, causalLabel, 
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, columnFormat, clinkType));
			}
		}
		
		return results.toString();
	}
	
	public String extractRelationsString(String dirPath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		StringBuilder results = new StringBuilder();
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
				System.err.println("Processing " + file.getPath());
				
				results.append(extractRelationsString(file,
						"", "",
						tempLabels, causLabels, 
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, columnFormat, clinkType));
			}
		}
		
		return results.toString();
	}
	
	public String extractRelationsString(String dirPath,
			String tlinkFilepath, String clinkFilepath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		StringBuilder results = new StringBuilder();
		File[] files = new File(dirPath).listFiles();
		
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
				System.err.println("Processing " + file.getPath());
				
				results.append(extractRelationsString(file,
						tlinkFilepath, clinkFilepath,
						te3CLabelCollapsed, causalLabel,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, columnFormat, clinkType));
			}
		}
		
		return results.toString();
	}
	
	public String extractRelationsString(String dirPath,
			String tlinkFilepath, String clinkFilepath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		StringBuilder results = new StringBuilder();
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
				System.err.println("Processing " + file.getPath());
				
				results.append(extractRelationsString(file,
						tlinkFilepath, clinkFilepath,
						tempLabels, causLabels,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, columnFormat, clinkType));
			}
		}
		
		return results.toString();
	}
	
	public String extractRelationsString(String dirPath,
			String[] fileNames, 
			String tlinkFilepath, String clinkFilepath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		List<String> fileList = Arrays.asList(fileNames);
		StringBuilder results = new StringBuilder();
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((fileList.contains(file.getName())
					|| fileList.contains(file.getName().replace(".col", ".tml"))
					|| fileList.contains(file.getName().replace(".tml", ".col"))
				) &&
				((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml")))
				) {
				System.err.println("Processing " + file.getPath());
				
				results.append(extractRelationsString(file,
						tlinkFilepath, clinkFilepath,
						tempLabels, causLabels,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, columnFormat, clinkType));
			}
		}
		
		return results.toString();
	}
	
	public ExtractedLinks extractRelations(String dirPath,
			String tlinkFilepath, String clinkFilepath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		ExtractedLinks results = new ExtractedLinks();
		File[] files = new File(dirPath).listFiles();
		
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
				System.err.println("Processing " + file.getPath());
				
				results.appendLinks(extractRelations(file,
						tlinkFilepath, clinkFilepath,
						te3CLabelCollapsed, causalLabel,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, 
						columnFormat, clinkType));
			}
		}
		
		return results;
	}
	
	public ExtractedLinks extractRelations(String dirPath,
			String[] fileNames, 
			String tlinkFilepath, String clinkFilepath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel,
			boolean columnFormat, boolean clinkType) throws Exception {
		
		List<String> fileList = Arrays.asList(fileNames);
		ExtractedLinks results = new ExtractedLinks();
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((fileList.contains(file.getName())
					|| fileList.contains(file.getName().replace(".col", ".tml"))
					|| fileList.contains(file.getName().replace(".tml", ".col"))
				) &&
				((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml")))
				) {
				System.err.println("Processing " + file.getPath());
				
				results.appendLinks(extractRelations(file,
						tlinkFilepath, clinkFilepath,
						tempLabels, causLabels,
						eventDctModel, eventTimexModel,
						eventEventModel, causalModel, 
						columnFormat, clinkType));
			}
		}
		
		return results;
	}
	
	public String extractRelationsString(File file,
			String tlinkFilepath, String clinkFilepath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, 
			boolean columnFormat, boolean clinkType) throws Exception {
		
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		ExtractedLinks rl = extractRelations(file,
				tlinkFilepath, clinkFilepath,
				te3CLabelCollapsed, causalLabel,
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel, 
				columnFormat, clinkType);
		
		StringBuilder sb = new StringBuilder();
		sb.append(rl.getTlink().TTLinksToString());
		sb.append(rl.getTlink().EDLinksToString());
		sb.append(rl.getTlink().ETLinksToString());
		sb.append(rl.getTlink().EELinksToString());
		sb.append(rl.getClink().EELinksToString());
		return sb.toString();
	}
	
	public String extractRelationsString(File file,
			String tlinkFilepath, String clinkFilepath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, 
			boolean columnFormat, boolean clinkType) throws Exception {
		
		ExtractedLinks rl = extractRelations(file,
				tlinkFilepath, clinkFilepath,
				tempLabels, causLabels,
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel, 
				columnFormat, clinkType);
		
		StringBuilder sb = new StringBuilder();
		sb.append(rl.getTlink().TTLinksToString());
		sb.append(rl.getTlink().EDLinksToString());
		sb.append(rl.getTlink().ETLinksToString());
		sb.append(rl.getTlink().EELinksToString());
		sb.append(rl.getClink().EELinksToString());
		return sb.toString();
	}
	
	public ExtractedLinks extractRelations(File file,
			String tlinkFilepath, String clinkFilepath,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, 
			boolean columnFormat, boolean clinkType) throws Exception {
		
		// ---------- TEMPORAL ---------- //
//		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
//				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp;
		if (Arrays.asList(tempLabels).contains("VAGUE")) {
			temp = new Temporal(true, tempLabels,
					eventDctModel,
					eventTimexModel,
					eventEventModel,
					true, true, true,
					true, false);
		} else {
			temp = new Temporal(false, tempLabels,
					eventDctModel,
					eventTimexModel,
					eventEventModel,
					true, true, true,
					true, false);
		}
		
		Map<String, String> tlinkPerFile = null;
		if (!tlinkFilepath.equals("")) {
			tlinkPerFile = Temporal.getLinksFromFile(tlinkFilepath).get(file.getName());
		}
		
		// PREDICT
		Map<String, String> relTypeMapping = new HashMap<String, String>();
		relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
		if (Arrays.asList(tempLabels).contains("VAGUE")) {
			relTypeMapping.put("BEGINS", "BEFORE");				//TimebankDense specific!
			relTypeMapping.put("BEGUN_BY", "AFTER");			//TimebankDense specific!
			relTypeMapping.put("ENDS", "AFTER");				//TimebankDense specific!
			relTypeMapping.put("ENDED_BY", "BEFORE");			//TimebankDense specific!
			relTypeMapping.put("DURING", "SIMULTANEOUS");		//TimebankDense specific!
			relTypeMapping.put("DURING_INV", "SIMULTANEOUS");	//TimebankDense specific!
		}
		List<TLINK> tlinks = temp.extractRelations("catena", file, tlinkPerFile, tempLabels, relTypeMapping, columnFormat);
		
		// ---------- CAUSAL ---------- //
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				causalModel,
				true, true);
		
		Map<String, String> clinkPerFile = null;
		if (!clinkFilepath.equals("")) {
			clinkPerFile = Causal.getLinksFromFile(clinkFilepath).get(file.getName());
		}
		
		// PREDICT
		CLINK clinks;
		Map<String, Map<String, String>> tlinksForClinkPerFile = new HashMap<String, Map<String, String>>();
		if (this.isTlinkFeature()) {	
			Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
			relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
			relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
			relTypeMappingTrain.put("IBEFORE", "BEFORE");
			relTypeMappingTrain.put("IAFTER", "AFTER");
			
			for (String s : tlinks.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkPerFile.containsKey(cols[0])) tlinksForClinkPerFile.put(cols[0], new HashMap<String, String>());
				String label = cols[4];
				for (String key : relTypeMappingTrain.keySet()) {
					label = label.replace(key, relTypeMappingTrain.get(key));
				}
				tlinksForClinkPerFile.get(cols[0]).put(cols[1]+","+cols[2], label);
				tlinksForClinkPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(label));
			}
			clinks = causal.extractRelations("catena", file, clinkPerFile, causalLabel, 
					this.isTlinkFeature(), 
					tlinksForClinkPerFile.get(file.getName()), 
					tempLabels, columnFormat, clinkType);
		} else {
			clinks = causal.extractRelations("catena", file, clinkPerFile, causalLabel, columnFormat, clinkType);
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
		
		ExtractedLinks rl = new ExtractedLinks(tlinks.get(1), clinks);
		return rl;
	}
	
	public void trainModels(String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, boolean columnFormat) throws Exception {
		String temporalTrainCorpus = "./data/Catena-train_TML/";
		String causalTrainCorpus = "./data/Causal-TimeBank_TML/";
		String tlinkFilepath = "";
		String clinkFilePath = "";
		if (columnFormat) {
			temporalTrainCorpus = "./data/Catena-train_COL/";
			causalTrainCorpus = "./data/Causal-TimeBank_COL/";
		}
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		trainModels(temporalTrainCorpus, causalTrainCorpus,
				null, null, 
				tlinkFilepath, clinkFilePath, 
				te3CLabelCollapsed, causalLabel, 
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel,
				columnFormat);
	}
	
	public void trainModels(String temporalTrainCorpus, String causalTrainCorpus,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, boolean columnFormat) throws Exception {
		if (columnFormat) {
			System.err.println("Input files containing gold TLINKs and CLINKs are missing!");
			return;
		}
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		trainModels(temporalTrainCorpus, causalTrainCorpus,
				null, null, 
				"", "", 
				te3CLabelCollapsed, causalLabel,
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel, columnFormat);
	}
	
	public void trainModels(String temporalTrainCorpus, String causalTrainCorpus,
			String[] tempLabels, String[] causLabels,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, boolean columnFormat) throws Exception {
		if (columnFormat) {
			System.err.println("Input files containing gold TLINKs and CLINKs are missing!");
			return;
		}
		trainModels(temporalTrainCorpus, causalTrainCorpus,
				null, null, 
				"", "", 
				tempLabels, causLabels,
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel, columnFormat);
	}
	
	public void trainModels(String temporalTrainCorpus, String causalTrainCorpus,
			String tlinkFilepath, String clinkFilepath,
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, boolean columnFormat) throws Exception {
		
		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		trainModels(temporalTrainCorpus, causalTrainCorpus,
				null, null, 
				tlinkFilepath, clinkFilepath, 
				te3CLabelCollapsed, causalLabel,
				eventDctModel, eventTimexModel,
				eventEventModel, causalModel, columnFormat);
	}
	
	public void trainModels(String temporalTrainCorpus, String causalTrainCorpus,
			String[] temporalFileNames, String[] causalFileNames,
			String tlinkFilepath, String clinkFilepath, 
			String[] tempLabels, String[] causLabels, 
			String eventDctModel, String eventTimexModel,
			String eventEventModel, String causalModel, boolean columnFormat) throws Exception {
		
		System.err.println("Train CATENA temporal and causal models...");
		
		// ---------- TEMPORAL ---------- //
//		String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS", 
//				"INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		Temporal temp = new Temporal(false, tempLabels,
				eventDctModel,
				eventTimexModel,
				eventEventModel,
				true, true, true,
				true, false);
		
		Map<String, Map<String, String>> tlinkPerFile = null;
		if (!tlinkFilepath.equals("")) {
			tlinkPerFile = Temporal.getLinksFromFile(tlinkFilepath);
		}
		
		// TRAIN
		Map<String, String> relTypeMappingTrain = new HashMap<String, String>();
		relTypeMappingTrain.put("DURING", "SIMULTANEOUS");
		relTypeMappingTrain.put("DURING_INV", "SIMULTANEOUS");
		relTypeMappingTrain.put("IBEFORE", "BEFORE");
		relTypeMappingTrain.put("IAFTER", "AFTER");
		
		if (temporalFileNames != null) {
//			if (Arrays.asList(tempLabels).contains("VAGUE")) {
//				temp.trainModels("catena", temporalTrainCorpus, temporalFileNames, tlinkPerFile, tempLabels, new HashMap<String, String>(), columnFormat);
//			} else {
				temp.trainModels("catena", temporalTrainCorpus, temporalFileNames, tlinkPerFile, tempLabels, relTypeMappingTrain, columnFormat);
//			}
		} else {
			temp.trainModels("catena", temporalTrainCorpus, tlinkPerFile, tempLabels, relTypeMappingTrain, columnFormat);
		}
		
		// ---------- CAUSAL ---------- //
//		String[] causalLabel = {"CLINK", "CLINK-R", "NONE"};
		
		Causal causal = new Causal(
				causalModel,
				true, true);
		
		Map<String, Map<String, String>> clinkPerFile = null;
		if (!clinkFilepath.equals("")) {
			clinkPerFile = Causal.getLinksFromFile(clinkFilepath);
		}
		
		// TRAIN
		Map<String, Map<String, String>> tlinksForClinkTrainPerFile = new HashMap<String, Map<String, String>>();
		if (this.isTlinkFeature()) {
			Map<String, String> relTypeMapping = new HashMap<String, String>();
			relTypeMapping.put("IDENTITY", "SIMULTANEOUS");
			if (Arrays.asList(tempLabels).contains("VAGUE")) {
				relTypeMapping.put("BEGINS", "BEFORE");				//TimebankDense specific!
				relTypeMapping.put("BEGUN_BY", "AFTER");			//TimebankDense specific!
				relTypeMapping.put("ENDS", "AFTER");				//TimebankDense specific!
				relTypeMapping.put("ENDED_BY", "BEFORE");			//TimebankDense specific!
				relTypeMapping.put("DURING", "SIMULTANEOUS");		//TimebankDense specific!
				relTypeMapping.put("DURING_INV", "SIMULTANEOUS");	//TimebankDense specific!
			}
			List<TLINK> tlinksTrain;
			if (temporalFileNames != null) {
				tlinksTrain = temp.extractRelations("catena", temporalTrainCorpus, temporalFileNames, tlinkPerFile, tempLabels, relTypeMapping, columnFormat);
			} else {
				tlinksTrain = temp.extractRelations("catena", temporalTrainCorpus, tlinkPerFile, tempLabels, relTypeMapping, columnFormat);
			}
			for (String s : tlinksTrain.get(0).getEE()) {
				String[] cols = s.split("\t");
				if (!tlinksForClinkTrainPerFile.containsKey(cols[0])) tlinksForClinkTrainPerFile.put(cols[0], new HashMap<String, String>());
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[1]+","+cols[2], cols[3]);
				tlinksForClinkTrainPerFile.get(cols[0]).put(cols[2]+","+cols[1], TemporalRelation.getInverseRelation(cols[3]));
			}
			
			causal.trainModels("catena", causalTrainCorpus, clinkPerFile, causLabels, 
					this.isTlinkFeature(), tlinksForClinkTrainPerFile, tempLabels, columnFormat);
		} else {
			causal.trainModels("catena", causalTrainCorpus, causLabels, columnFormat);
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
