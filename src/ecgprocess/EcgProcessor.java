package ecgprocess;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import qrsdetbyhamilton.QrsDetectorWithQRSInfo;

public class EcgProcessor {
	private final ResampleFrom250To360 resampler;
	private List<Short> ecgData;
	private List<Float> normalizedEcgData;
	private JSONObject qrsAndBeatPos;
	
	public EcgProcessor() {
		resampler = new ResampleFrom250To360();
	}
	
	
	
	public List<Short> getEcgData() {
		return ecgData;
	}



	public List<Float> getNormalizedEcgData() {
		return normalizedEcgData;
	}



	public JSONObject getQrsAndBeatPos() {
		return qrsAndBeatPos;
	}

	public JSONArray getBeatBeginPos() {
		return (JSONArray)getQrsAndBeatPos().get("BeatBegin");
	}

	public void process(List<Short> ecgData, int sampleRate) {
		if(ecgData == null || ecgData.isEmpty()) {
			return;
		}
		
		this.ecgData = resampler.resample(ecgData);
		
		qrsAndBeatPos = getEcgQrsAndBeatBeginPos(this.ecgData, resampler.getOutSampleRate());

		JSONArray beatBegin = (JSONArray)qrsAndBeatPos.get("BeatBegin");
		normalizedEcgData = normalizeEcgData(this.ecgData, beatBegin);
	}

	private JSONObject getEcgQrsAndBeatBeginPos(List<Short> ecgData, int sampleRate) {
		QrsDetectorWithQRSInfo detector = new QrsDetectorWithQRSInfo(sampleRate);
		for(Short datum : ecgData) {
			detector.outputRRInterval((int)datum);
		}
		List<Long> qrsPos = detector.getQrsPositions();
		List<Integer> rrInterval = detector.getRrIntervals();
		List<Long> beatBegin = new ArrayList<>();
		for(int i = 0; i < rrInterval.size(); i++) {
			beatBegin.add(qrsPos.get(i+1) - Math.round(rrInterval.get(i)*2.0/5));
		}
		JSONObject json = new JSONObject();
		json.put("QrsPos", qrsPos);
		json.put("BeatBegin", beatBegin);
		System.out.println(json);
		return json;
	}
	
	private List<Float> normalizeEcgData(List<Short> ecgData, JSONArray beatBegin) {
		List<Float> out = new ArrayList<>();
		
		for(Short d : ecgData) {
			out.add((float)d);
		}
		
		List<Float> oneBeat =  new ArrayList<>();
		for(int i = 0; i < beatBegin.length()-1; i++) {
			for(long begin = beatBegin.getLong(i); begin < beatBegin.getLong(i+1); begin++) {
				oneBeat.add(out.get((int)begin));
			}
			//System.out.println(oneBeat);
			
			float ave = average(oneBeat);
			float std = standardDiviation(oneBeat);
			
			for(int ii = 0; ii < oneBeat.size(); ii++) {
				oneBeat.set(ii, (oneBeat.get(ii)-ave)/std);
			}
			
			//System.out.println("" + ave + " " + std);
			//System.out.println(oneBeat);
			
			for(long begin = beatBegin.getLong(i),  ii = 0; begin < beatBegin.getLong(i+1); begin++, ii++) {
				out.set((int)begin, oneBeat.get((int)ii));
			}
			
			oneBeat.clear();
		}
		return out;
	}
	
	 //均值
	 public static float average(List<Float> x) { 
		  int m=x.size();
		  float sum=0;
		  for(int i=0;i<m;i++){//求和
			  sum+=x.get(i);
		  }
		 return sum/m; 
	 }
	
	 //标准差σ=sqrt(s^2)
	 public static float standardDiviation(List<Float> x) { 
		  int m=x.size();
		  float sum=0;
		  for(int i=0;i<m;i++){//求和
			  sum+=x.get(i);
		  }
		  double dAve=sum/m;//求平均值
		  double dVar=0;
		  for(int i=0;i<m;i++){
		      dVar+=(x.get(i)-dAve)*(x.get(i)-dAve);
		  }
	   //reture Math.sqrt(dVar/(m-1));
		return (float)Math.sqrt(dVar/(m-1));    
	 }
}
