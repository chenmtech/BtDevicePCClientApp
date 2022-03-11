package ecgprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class EcgDiagnoseModel {
	private static final EcgDiagnoseModel instance = new EcgDiagnoseModel();
	
	private static final double NORMAL_THRESH = 0.1;
	
	public static final String VER = "1.0";
	
	private List<List<Double>> predictList = new ArrayList<>();
	private String errStr;
	
	private EcgDiagnoseModel() {
		
	}
	
	public static EcgDiagnoseModel getInstance() {
		return instance;
	}
	
	public List<List<Double>> getPredictList() {
		return predictList;
	}
	
	public String getErrStr() {
		return errStr;
	}
	
	/**
	 * 调用Python脚本，加载神经网络模型，对review.json文件中的心电数据进行处理
	 * 用输入流来截取结果
	 * @param pythonExe -Python.exe文件名
	 * @param diagnoseScript -Python脚本文件名
	 * @param diagNNModel -神经网络模型.h5文件名
	 * @param reviewFile -review.json文件名
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void process(String pythonExe, String diagnoseScript, String diagNNModel, String reviewFile) throws IOException, InterruptedException {
		Process proc;
    	String[] args = {pythonExe, diagnoseScript, diagNNModel, reviewFile};
        proc = Runtime.getRuntime().exec(args);// 执行py文件
        //用输入流来截取结果
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null) {
            builder.append(line);
        }
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        StringBuilder errBuilder = new StringBuilder();
        String errLine = null;
        while ((errLine = err.readLine()) != null) {
            errBuilder.append(errLine);
        }
        in.close();
        err.close();
        proc.waitFor();
        errStr = errBuilder.toString();
        System.out.println(errStr);
        System.out.println(builder.toString());
        JSONObject resultJson = new JSONObject(builder.toString());
        JSONArray predictArr = (JSONArray) resultJson.get("Predict");
        //JSONArray resultArr = (JSONArray) resultJson.get("Result");
        
        predictList.clear();
        
        for(int i = 0; i < predictArr.length(); i++) {
        	List<Double> tmpList = new ArrayList<>();
        	JSONArray arr = (JSONArray)predictArr.get(i);
        	for(int j = 0; j < arr.length(); j++) {
        		tmpList.add((Double)arr.get(j));
        	}
        	predictList.add(tmpList);
        }
	}
	
	public int getAbnormalBeat() {
		int abNum = 0;
		for(int i = 0; i < predictList.size(); i++) {
			List<Double> tmpList = predictList.get(i);
			if(tmpList.get(0) < NORMAL_THRESH) {
				abNum++;
			}
		}
		return abNum;
	}
	
	public List<Integer> getDiagnoseResult() {
		List<Integer> result = new ArrayList<>();
		for(int i = 0; i < predictList.size(); i++) {
			List<Double> tmpList = predictList.get(i);
			if(tmpList.get(0) < NORMAL_THRESH) {
				result.add(1);
			} else {
				result.add(0);
			}
		}
		return result;
	}

}
