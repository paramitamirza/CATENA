package catena.parser;

import java.io.File;
import java.io.IOException;

import catena.parser.TXPParser.Field;
import catena.parser.entities.*;

public class testParser {
	
	public static void main(String [] args) {
		
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho,
				Field.tense_aspect_pol, Field.coref_event, Field.tlink};
		TXPParser parser = new TXPParser(EntityEnum.Language.EN, fields);
		
		//dir_TXP <-- data/example_TXP
		File dir_TXP = new File(args[0]);
		File[] files_TXP = dir_TXP.listFiles();
		for (File file : files_TXP) {
			if (file.isFile()) {
				
				try {
					Doc doc = parser.parseDocument(file.getPath());
					
					//array of tokens
					for (String tid : doc.getTokenArr()) {
						Token tok = doc.getTokens().get(tid);
						System.out.println(tok.getText() + "\t" + tok.getLemma() + "\t" + tok.getPos() + 
								"\t" + tok.getMainPos() + "\t" + tok.getChunk() + "\t" + tok.getNamedEntity());
						if (tok.getDependencyRel() != null) {
							for (String key : tok.getDependencyRel().keySet()) {
								System.out.println(key + "-" + tok.getDependencyRel().get(key));
							}
						}
					}
					
					//array of sentences
					for (String sid : doc.getSentenceArr()) {
						Sentence sent = doc.getSentences().get(sid);
						System.out.println(sent.getID() + "\t" + sent.getStartTokID() + "\t" + sent.getEndTokID());
						for (String eid : sent.getEntityArr()) {
							System.out.print(eid + " ");
						}
						System.out.println();
					}
					
					//array of entities
					for (String ent_id : doc.getEntities().keySet()) {
						Entity ent = doc.getEntities().get(ent_id);
						if (ent instanceof Timex) {
							System.out.println(ent.getID() + "\tTimex\t" + ent.getStartTokID() + 
									"\t" + ent.getEndTokID());
						} else if (ent instanceof Event) {
							System.out.println(ent.getID() + "\tEvent\t" + ent.getStartTokID() + 
									"\t" + ent.getEndTokID());
							for (String eid : ((Event)ent).getCorefList()) {
								System.out.print(eid + "|");
							}
							System.out.println();
						}
					}
					
					//array of TLINKs
					for (TemporalRelation tlink : doc.getTlinks()) {
						System.out.println(tlink.getSourceID() + "\t" + tlink.getTargetID() + 
								"\t" + tlink.getRelType());
					}
					
					System.out.println();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
