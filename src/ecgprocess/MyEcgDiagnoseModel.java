package ecgprocess;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.json.JSONObject;

import afdetect.AFEvidence.MyAFEvidence;
import application.MyProperties;

/**
 * 心电信号诊断模型
 * 包括信号预处理、心率异常检测、房颤检测，以及心律失常检测
 * @author gdmc
 *
 */
public class MyEcgDiagnoseModel {
	// 诊断模型算法的版本号，通过更新更大的版本号，可以对服务器上的心电信号进行重新诊断
    public static final String VER = "1.1.2";
    
    // 诊断报告提供者
    public static final String REPORT_PROVIDER = "广东医科大学生物医学工程系";
    
    // 缺省的下载心电数据保存的JSON文件名，会放在java tmp目录中
    private static final String DEFAULT_ECG_DATA_FILE_NAME = "ecgData.json";

    private final MyAFEvidence afEvidence;
    
    private String diagnoseResult = "";
    
    private int aveHr;

    public MyEcgDiagnoseModel() {
        afEvidence = new MyAFEvidence();
    }
    
    public String getDiagnoseResult() {
    	return diagnoseResult;
    }
    
    public int getAveHr() {
    	return aveHr;
    }

    public boolean process(List<Short> ecgData, int sampleRate, MyProperties properties) {
    	// 对心电信号进行预处理，包括检测RR间隔，分割每个心动周期
        EcgPreProcessor preProcessor = new EcgPreProcessor();
        preProcessor.process(ecgData, sampleRate);

        // 获取处理后的平均心率值和RR间隔
        aveHr = preProcessor.getAverageHr();
        List<Double> RR = preProcessor.getRRIntervalInMs();

        // 检测心率异常
        String strHrResult = HRAbnormalDetector.process(aveHr);

        // 检测房颤
        afEvidence.process(RR);
        String strAFEResult = afEvidence.getResultStr();
        
        //diagnoseResult = strHrResult + strAFEResult;
        
        // 检测心律失常
        // 生成需要做心律失常检测的记录数据JSON文件名，并准备python运行时需要的其他参数
 		String ecgDataJsonFile = System.getProperty("java.io.tmpdir") + DEFAULT_ECG_DATA_FILE_NAME; 		
 		String[] args = new String[] { properties.getPythonExe(), properties.getEcgScript(), properties.getEcgNNModel(), ecgDataJsonFile};
		boolean isArgsOK = true;
 		for(String str : args) {
			if(str == null || str.equals("")) {
				isArgsOK = false;
				break;
			}
		}
 		String strATMResult = "";
 		if(isArgsOK) { // 如果参数OK
 			File reviewFile = new File(ecgDataJsonFile);
    		try(PrintWriter reviewWriter = new PrintWriter(reviewFile)) {
    			// 将预处理后的心电数据保存到review.json文件中
    			JSONObject rltJson = preProcessor.getResultJson();
    			if(rltJson != null) {
    				reviewWriter.print(rltJson.toString());
    				reviewWriter.flush();    			
			
        			// 用心律失常诊断模型对预处理后的心电数据文件进行检测处理
    				EcgArrhythmiaDetector arrhythmiaDetector = EcgArrhythmiaDetector.getInstance();
    				arrhythmiaDetector.process(args[0], args[1], args[2], args[3]);
    				//System.out.println(arrhythmiaDetector.getDiagnoseResult());		
    				strATMResult = arrhythmiaDetector.getResultStr();		
    				//diagnoseResult += strATMResult;
    			}                
    		} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 		}
 		diagnoseResult = strATMResult + strAFEResult + strHrResult;
 		System.out.println(diagnoseResult);
        return true;
    }
}
