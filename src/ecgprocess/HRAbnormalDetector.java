package ecgprocess;

import static ecgprocess.EcgPreProcessor.INVALID_HR;

public class HRAbnormalDetector {
    public static final int HR_TOO_LOW_LIMIT = 50;
    public static final int HR_TOO_HIGH_LIMIT = 100;

    
    public static final String process(int aveHr) {
    	String strHrResult = "";
        if(aveHr != INVALID_HR) {
            if(aveHr > HR_TOO_HIGH_LIMIT)
                strHrResult = "心动过速";
            else if(aveHr < HR_TOO_LOW_LIMIT)
                strHrResult = "心动过缓";
            else
                strHrResult = "心率正常";
            strHrResult += "(平均心率：" + aveHr + "次/分钟);";
        }
        return strHrResult;
    }
}
