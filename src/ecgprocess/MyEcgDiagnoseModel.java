package ecgprocess;

import java.util.List;

import afdetect.AFEvidence.MyAFEvidence;

/**
 * 心电信号诊断模型
 * 包括信号预处理、心率异常检测、房颤检测，暂时不包含心律异常检测
 * @author gdmc
 *
 */
public class MyEcgDiagnoseModel {
    public static final String VER = "1.1.0";

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

    public boolean process(List<Short> ecgData, int sampleRate) {
    	// 对心电信号进行预处理，包括检测RR间隔，分割每个心动周期
        EcgPreProcessor preProcessor = new EcgPreProcessor();
        preProcessor.process(ecgData, sampleRate);

        aveHr = preProcessor.getAverageHr();
        List<Double> RR = preProcessor.getRRIntervalInMs();

        // 处理心率异常
        String strHrResult = HRAbnormalDetector.process(aveHr);

        // 处理房颤
        afEvidence.process(RR);
        String strAFEResult = afEvidence.getResultStr();

        diagnoseResult = strHrResult + strAFEResult;
        return true;
    }
}
