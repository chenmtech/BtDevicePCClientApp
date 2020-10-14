package chlg2017;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cmtech.web.btdevice.RecordType;

import AFEvidence.MyAFEvidence;
import application.InfoPane;

public class Chlg2017 {
	private final InfoPane infoPane;
	
	private final MyAFEvidence afEvi = new MyAFEvidence();
	
	public Chlg2017(InfoPane infoPane) {
		this.infoPane = infoPane;
	}
	
	public void processRecord() {
		File file;
        String name = "";
		for(int num = 54; num <= 54; num++) {
			name = "0000" + num;
			name = name.substring(name.length()-5);
			name = "F:\\AFDetect\\origin\\A" + name + ".json";
			System.out.println(name);
			file = new File(name);
			
	        if(file != null){
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
	                JSONArray arr = json.getJSONArray("ecgData");
	            	for(int i = 0; i < arr.length(); i++) {
	            		ecgData.add((short)arr.getInt(i));
	            	}
	                int sampleRate = json.getInt("sampleRate");
	                
	                Chlg2017EcgProcessor ecgProc = new Chlg2017EcgProcessor();
	                ecgProc.process(ecgData, sampleRate);
	                
	                JSONArray RPos = (JSONArray) ecgProc.getReviewResult().get("RPos");
	                List<Double> RR = new ArrayList<>();
	                for(int i = 1; i < RPos.length(); i++) {
	                	double R1 = RPos.getLong(i-1)*1000.0/sampleRate;
	                	double R2 = RPos.getLong(i)*1000.0/sampleRate;
	                	RR.add(R2-R1);
	                	//double R3 = RPos.getLong(i+1)*1000.0/sampleRate;
		                //afEvi.addPoint(R2-R1, R3-R2);
	                }
	                for(int i = 1; i < RR.size()-1; i++) {
	                	afEvi.addPoint(RR.get(i)-RR.get(i-1), RR.get(i+1)-RR.get(i));
	                }
	                System.out.println(afEvi.getAFEvidence());
	                afEvi.clear();
	    			
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
}
