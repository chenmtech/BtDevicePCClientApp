package afdetect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import afdetect.AFEvidence.MyAFEvidence;
import application.InfoPane;

public class AFDetectExecutor {
	private final InfoPane infoPane;
	
	//private final MyAFEvidence afEvi = new MyAFEvidence();
	
	public AFDetectExecutor(InfoPane infoPane) {
		this.infoPane = infoPane;
	}
	
	public void processRecords() {
		String name = "";
		for(int num = 54; num <= 54; num++) {
			name = "0000" + num;
			name = name.substring(name.length()-5);
			name = "F:\\AFDetect\\origin\\A" + name + ".json";
			System.out.println(name);
			File file = new File(name);
			processRecord(file);
		}
	}
	
	public void processRecord(File file) {
		if(file != null && file.exists()){
        	try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
        		String tempString = null;
                StringBuilder builder = new StringBuilder();
                // 一次读入一行，直到读入null为文件结束
                while ((tempString = reader.readLine()) != null) {
                    builder.append(tempString);
                }
                JSONObject json = new JSONObject(builder.toString());
                //System.out.println(json.toString());
                RecordType type = RecordType.fromCode(json.getInt("recordTypeCode"));
                if(type != RecordType.ECG) {
                	infoPane.setInfo("对不起，暂时只能处理心电信号。");
                	return;
                }
                List<Short> ecgData = new ArrayList<>();
                try {
                	String ecgStr = json.getString("ecgData");
                    String[] ecgDataStr = ecgStr.split(",");
                    for(String str : ecgDataStr) {
                    	ecgData.add(Short.parseShort(str));
                    }
                } catch (JSONException ex) {
                	JSONArray arr = json.getJSONArray("ecgData");
                	for(int i = 0; i < arr.length(); i++) {
                		ecgData.add((short)arr.getInt(i));
                	}
                }
                int sampleRate = json.getInt("sampleRate");
                
                AFDetectEcgPreProcessor ecgProc = new AFDetectEcgPreProcessor();
                ecgProc.process(ecgData, sampleRate);
                
                List<Double> RR = ecgProc.getRRIntervalInMs();
                MyAFEvidence afEvi = new MyAFEvidence();
                afEvi.process(RR);
                System.out.println(afEvi.getAFEvidence());
    			
        		/*String srcFileName = file.getAbsolutePath();
        		String tmpFileName = srcFileName.substring(0, srcFileName.lastIndexOf('.'));
        		
        		// save review result to json file
        		String reviewJsonFileName = tmpFileName + "-review.json";
        		File reviewFile = new File(reviewJsonFileName);
        		try(PrintWriter reviewWriter = new PrintWriter(reviewFile)) {
        			reviewWriter.print(ecgProc.getReviewResult().toString());
        		}*/
        		
    			infoPane.setInfo("已将处理结果保存到文件中。");
        	} catch (IOException e) {
        		infoPane.setInfo("处理信号失败");
				e.printStackTrace();
			}
        }
	}
}
