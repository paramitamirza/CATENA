/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package catena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.sun.jersey.api.client.WebResource;

import catena.parser.TimeMLParser;
import catena.parser.entities.Doc;
import catena.parser.entities.EntityEnum;
import eu.terenceproject.utils.rest.REST;
import eu.terenceproject.utils.rest.Consistency;
import eu.terenceproject.utils.rest.Consistency.State;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author pierpaolo
 */
public class Reasoner extends REST {
	
	private static int numTrueStrict=0;
	private static int numTrueRelaxed=0;
	private static int numFalse=0;
	
	private String readFile( String file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new FileReader (file));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }

	    return stringBuilder.toString();
	}
	
	public static void copyFile(File file, File dir_out) throws Exception {
		Path FROM = Paths.get(file.getPath());
	    Path TO = Paths.get(dir_out.getPath(), file.getName());
	    //overwrite existing file, if exists
	    CopyOption[] options = new CopyOption[]{
	      StandardCopyOption.REPLACE_EXISTING,
	      StandardCopyOption.COPY_ATTRIBUTES
	    }; 
	    Files.copy(FROM, TO, options);
	}
	
	public String deduceTlinksPerFile(String tmlString) throws Exception {
		return deduction(tmlString);
	}
	
	public void deduceTlinksPerFile(String tmlString,
			File file, File dir_out) throws Exception {
		String dedresult = deduction(tmlString);
		
		File output = new File(dir_out.getPath(), file.getName());
		System.setProperty("line.separator", "\n");
		PrintWriter writer = new PrintWriter(output.getPath(), "UTF-8");
		writer.write(dedresult);
		writer.close();
	}
	
	public void deduceTlinks(String filepath, PrintWriter log,
			File dir_out) throws Exception {
		File dir_TML = new File(filepath);
		File[] files_TML = dir_TML.listFiles();
		
		if (files_TML == null) return;
		
		for (File file : files_TML) {
			if (file.isDirectory()){				
				deduceTlinks(file.getPath(), log, dir_out);
				
			} else if (file.isFile()) {
				System.err.println("Checking " + file.getName() + "...");
				
				TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
				Doc docTml = tmlParser.parseDocument(file);
				
				String timeml = readFile(file.getPath());
				
				Consistency conresult = consistency(timeml);
				switch (conresult.getState()) {
					case TRUE_STRICT: numTrueStrict ++; break;
					case TRUE_RELAXED: numTrueRelaxed ++; break;
					case FALSE: 
						log.write(file.getName() + "\n");
						numFalse ++; 
						break;
					case ERROR: break;
				}
				
				if (conresult.getState().equals(State.TRUE_STRICT) ||
						conresult.getState().equals(State.TRUE_RELAXED)) {
					deduceTlinksPerFile(timeml, file, dir_out);
					
				} else {
					copyFile(file, dir_out);
				}
			}
		}
		
		System.out.println("Num true strict : " + numTrueStrict);
		System.out.println("Num true relaxed : " + numTrueRelaxed);
		System.out.println("Num false : " + numFalse);
	}
	
	public static void main(String[] args) {
		Reasoner r = new Reasoner();
		
		try {
			System.setProperty("line.separator", "\n");
			PrintWriter log = new PrintWriter("data/log.csv", "UTF-8");
			
			String dirTimeML = "./data/Thesis_TML/";
			
			File dir = new File(dirTimeML);
			String dir_out_name = Paths.get(dir.getParent(), dir.getName() + "_deduced").toString();
			File dir_out = new File(dir_out_name);
			if (!dir_out.exists()) {
				dir_out.mkdir();
			}
			
			r.deduceTlinks(dirTimeML, log, dir_out);
			
			log.close();
			
			
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public Reasoner() {
        super("http://hixwg.univaq.it/TERENCE-reasoner");
    }
    public Reasoner(String URL) {
        super(URL);
    }

    public Consistency consistency(String s) {
        String r = call("rest/reason/consistency", s);
        switch (r) {
            case "true with strict mapping" : return Consistency.TRUE_STRICT();
            case "true with relaxed mapping": return Consistency.TRUE_RELAXED();
            case "false"                    : return Consistency.FALSE();
            default                         : return Consistency.ERROR();
        }
    }

    public String deduction(String s) {
        return call("rest/reason/deduction", s);
    }

    public enum LANG { EN, IT };
}
