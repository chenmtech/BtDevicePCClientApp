package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class MyProperties {
	private Properties properties = new Properties();
	private static final String FILE_NAME = "kmsignal.config";
	private static final String PYTHON_EXE_KEY = "python_exe";
	private static final String ECG_PY_SCRIPT_KEY = "ecg_script";
	private static final String ECG_NN_MODEL_KEY = "ecg_model";

	public MyProperties() throws IOException {
		File file = new File(FILE_NAME);
		if(file.exists()) {
			try(InputStream in = new FileInputStream(file)) {
				properties.load(in);
			}
		} else {
			file.createNewFile();
		}		
	}
	
	public void save() throws IOException {
		try(OutputStream output = new FileOutputStream(FILE_NAME)) {
			properties.store(output, "kmsignal configuration");
		}
	}
	
	public String getPythonExe() {
		return properties.getProperty(PYTHON_EXE_KEY);
	}
	
	public void setPythonExe(String pythonExe) {
		properties.setProperty(PYTHON_EXE_KEY, pythonExe);
	}
	
	public String getEcgScript() {
		return properties.getProperty(ECG_PY_SCRIPT_KEY);
	}
	
	public void setEcgScript(String ecgScript) {
		properties.setProperty(ECG_PY_SCRIPT_KEY, ecgScript);
	}
	
	public String getEcgNNModel() {
		return properties.getProperty(ECG_NN_MODEL_KEY);
	}
	
	public void setEcgNNModel(String nnModel) {
		properties.setProperty(ECG_NN_MODEL_KEY, nnModel);
	}
}
