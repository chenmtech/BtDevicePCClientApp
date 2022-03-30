package ecgprocess;

import static afdetect.IAFDetector.AF;
import static afdetect.IAFDetector.NON_AF;
import static ecgprocess.EcgPreProcessor.INVALID_HR;

import java.util.List;

import afdetect.AFEvidence.MyAFEvidence;

public class MyEcgArrhythmiaDetector implements IEcgArrhythmiaDetector {
    public static final int HR_TOO_LOW_LIMIT = 50;
    public static final int HR_TOO_HIGH_LIMIT = 100;
    public static final String VER = "0.1.4";

    private final MyAFEvidence afEvidence;

    public MyEcgArrhythmiaDetector() {
        afEvidence = new MyAFEvidence();
    }

    @Override
    public String getVer() {
        return VER;
    }

    @Override
    public String process(List<Short> ecgData, int sampleRate) {
        EcgPreProcessor preProcessor = new EcgPreProcessor();
        preProcessor.process(ecgData, sampleRate);

        int aveHr = preProcessor.getAverageHr();
        List<Double> RR = preProcessor.getRRIntervalInMs();

        String strHrResult = "";
        if(aveHr != INVALID_HR) {
            strHrResult = "平均心率：" + aveHr + "次/分钟，";
            if(aveHr > HR_TOO_HIGH_LIMIT)
                strHrResult += "心动过速。\n";
            else if(aveHr < HR_TOO_LOW_LIMIT)
                strHrResult += "心动过缓。\n";
            else
                strHrResult += "心率正常。\n";
        }

        afEvidence.process(RR);
        int afe = afEvidence.getAFEvidence();
        int classify = afEvidence.getClassifyResult();
        //ViseLog.e("afe:" + afe + "classify:" + classify);

        StringBuilder builder = new StringBuilder();
        if(classify == AF) {
            builder.append("发现房颤风险。");
        } else if(classify == NON_AF){
            builder.append("未发现房颤风险。");
        } else {
            builder.append("由于信号质量问题，无法判断房颤风险。");
        }
        builder.append("(风险值：").append(afe).append(")");
        String strAFEResult = builder.toString();

        return strHrResult + strAFEResult;
    }
}
