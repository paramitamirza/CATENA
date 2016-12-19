package catena.evaluator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TempEval3 {
	
	private String goldPath;
	private String systemPath;
	
	private String path_te3_eval_script = "./tools/TempEval3-evaluation-tool/TE3-evaluation.py";
	
	public TempEval3(String gold, String system) {
		this.setGoldPath(gold);
		this.setSystemPath(system);
	}
	
	public void evaluate() throws Exception {
		
		//Run python script
//		PythonInterpreter interp = new PythonInterpreter(null, new PySystemState());
//
//        PySystemState sys = Py.getSystemState();
//        sys.path.append(new PyString("C:/Users/Paramita/.p2/pool/plugins/org.python.pydev.jython_4.5.3.201601211913"));
//        sys.path.append(new PyString("C:/Users/Paramita/.p2/pool/pluginsorg.python.pydev.jython_4.5.3.201601211913/Lib"));
//        sys.argv.append(new PyString(goldPath));
//        sys.argv.append(new PyString(systemPath));
//        
//		interp.execfile(path_te3_eval_script);
//		
//		PyClass funcEvaluate = (PyClass)interp.get("TE3Evaluation", PyClass.class);
//		PyObject timegraph = funcEvaluate.__call__();
	}

	public String getGoldPath() {
		return goldPath;
	}

	public void setGoldPath(String goldPath) {
		this.goldPath = goldPath;
	}

	public String getSystemPath() {
		return systemPath;
	}

	public void setSystemPath(String systemPath) {
		this.systemPath = systemPath;
	}

}
