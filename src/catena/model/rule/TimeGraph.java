package catena.model.rule;

import java.util.HashMap;
import java.util.HashSet;

import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class TimeGraph {

	private String path_timegraph_script = "./resource/relation_to_timegraph.py";
	public HashMap<String, String> finalRel = new HashMap<String, String>();
	public HashMap<String, String> violatedRel = new HashMap<String, String>();
	public HashMap<String, String> removeRel = new HashMap<String, String>();
	public HashMap<String, String> nonRedundantRel = new HashMap<String, String>();
	
	public TimeGraph(String relText) {
		PythonInterpreter interp = new PythonInterpreter(null, new PySystemState());

        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString("C:/Users/Paramita/.p2/pool/plugins/org.python.pydev.jython_4.5.3.201601211913"));
        sys.path.append(new PyString("C:/Users/Paramita/.p2/pool/pluginsorg.python.pydev.jython_4.5.3.201601211913/Lib"));
        
		interp.execfile(path_timegraph_script);
		
		PyClass funcTimegraph = (PyClass)interp.get("Timegraph", PyClass.class);
		//Instantiate function
		PyFunction funcCreateTimegraph = (PyFunction)interp.get("create_timegraph_from_weight_sorted_relations",PyFunction.class);
		PyFunction funcIntRelTimegraph = (PyFunction)interp.get("interval_rel_X_Y", PyFunction.class);
		PyFunction funcAddRelTimegraph = (PyFunction)interp.get("add_relation_in_timegraph",PyFunction.class);
		
		//graph
		PyObject timegraph = funcTimegraph.__call__();
		timegraph = funcCreateTimegraph.__call__(new PyString(relText),timegraph);
		
		PyString final_relations = (PyString) timegraph.__getattr__("final_relations");
		String final_relations_java = ((PyString) final_relations).getString();
		for (String rel : final_relations_java.split("\n")) {
			if (!rel.isEmpty()) {
				String[] cols = rel.split("\t");
				finalRel.put(cols[1]+"\t"+cols[2], cols[3]);
			}
		}
		
		PyString violated_relations = (PyString) timegraph.__getattr__("violated_relations");
		String violated_relations_java = ((PyString) violated_relations).getString();
		for (String rel : violated_relations_java.split("\n")) {
			if (!rel.isEmpty()) {
				String[] cols = rel.split("\t");
				violatedRel.put(cols[1]+"\t"+cols[2], cols[3]);
			}
		}
		
		PyString remove_relations = (PyString) timegraph.__getattr__("remove_from_reduce");
		String remove_relations_java = ((PyString) remove_relations).getString();
		for (String rel : remove_relations_java.split("\n")) {
			if (!rel.isEmpty()) {
				String[] cols = rel.split("\t");
				removeRel.put(cols[1]+"\t"+cols[2], cols[3]);
			}
		}
		
		PyString non_redundant_relations = (PyString) timegraph.__getattr__("nonredundant");
		String non_redundant_relations_java = ((PyString) non_redundant_relations).getString();
		for (String rel : non_redundant_relations_java.split("\n")) {
			if (!rel.isEmpty()) {
				String[] cols = rel.split("\t");
				nonRedundantRel.put(cols[1]+"\t"+cols[2], cols[3]);
			}
		}
	}
	
}
