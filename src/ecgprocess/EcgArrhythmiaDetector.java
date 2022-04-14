package ecgprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 心电信号心律失常检测器
 * @author gdmc
 *
 */
public class EcgArrhythmiaDetector {
	private static final EcgArrhythmiaDetector instance = new EcgArrhythmiaDetector();
	
	private static final double NORMAL_THRESH = 0.1;
	
	public static final String VER = "1.0";
	
	private List<List<Double>> predictList = new ArrayList<>();
	private String errStr;
	
	private EcgArrhythmiaDetector() {
		
	}
	
	public static EcgArrhythmiaDetector getInstance() {
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
	 * @param ecgDiagnoseScript -心电诊断Python脚本文件名
	 * @param diagNNModel -神经网络模型.h5文件名
	 * @param ecgDataFile -待诊断的心电数据json文件名
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void process(String pythonExe, String ecgDiagnoseScript, String diagNNModel, String ecgDataFile) throws IOException, InterruptedException {
		// 准备python调用参数
		String[] args = {pythonExe, ecgDiagnoseScript, diagNNModel, ecgDataFile};
    	
		// 执行py文件
		Process proc = Runtime.getRuntime().exec(args);
		
        //用输入流来截取结果
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        StringBuilder outBuilder = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null) {
            outBuilder.append(line);
        }
        
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        StringBuilder errBuilder = new StringBuilder();
        String errLine = null;
        while ((errLine = err.readLine()) != null) {
            errBuilder.append(errLine);
        }
        in.close();
        err.close();
        
        // 等待执行完毕
        proc.waitFor();

        System.out.println(errBuilder.toString());
        System.out.println(outBuilder.toString());
        
        // 获取诊断输出，提取预测结果
        JSONObject outJson = new JSONObject(outBuilder.toString());
        JSONArray predictArr = (JSONArray) outJson.get("Predict");
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
	
	public String getResultStr() {
		int abNum = getAbnormalBeat();
		String strATMResult = (abNum == 0) ? "窦性心律;" : "发现" + abNum + "次异常心动周期;";
		return strATMResult;
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
