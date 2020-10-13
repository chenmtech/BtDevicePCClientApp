package ecgprocess;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import dsp.filter.FIRFilter;
import dsp.filter.IIRFilter;
import dsp.filter.design.FIRDesigner;
import dsp.filter.design.FilterType;
import dsp.filter.design.NotchDesigner;
import dsp.filter.design.WinType;
import qrsdetbyhamilton.QrsDetectorWithQRSInfo;
import util.MathUtil;

public class EcgProcessor {
	private static final int NUM_BEFORE_R = 99;
	private static final int NUM_AFTER_R = 150;
	private JSONObject reviewResult;
	private List<List<Float>> segEcgData;
	
	public EcgProcessor() {
	}
	
	public JSONObject getReviewResult() {
		return reviewResult;
	}

	public void process(List<Short> ecgData, int sampleRate) {
		if(ecgData == null || ecgData.isEmpty()) {
			reviewResult = null;
			return;
		}
		
		// do filtering
		//IIRFilter dcBlocker = DCBlockDesigner.design(1, sampleRate);
		IIRFilter notch50 = designNotch(50, sampleRate);
		FIRFilter lpFilter = designLpFilter(sampleRate);
		List<Short> afterFilter = new ArrayList<Short>();
		for(Short d : ecgData) {
			afterFilter.add((short)Math.round(lpFilter.filter(notch50.filter(d))));
		}		
		
		// do resampling
		/*ResampleFrom250To360 resampler = new ResampleFrom250To360();
		ecgData = resampler.resample(afterFilter);
		sampleRate = resampler.getOutSampleRate();*/
		ecgData = afterFilter;
		
		// detect the QRS waves and RR interval
		JSONObject qrsAndRRInterval = getQrsPosAndRRInterval(ecgData, sampleRate);
		
		// detect the R wave position and the begin pos of each beat
		JSONObject rPosAndBeatBegin = getRPosAndBeatBegin(ecgData, qrsAndRRInterval);

		// normalize the Ecg data per beat
		JSONArray beatBegin = (JSONArray)rPosAndBeatBegin.get("BeatBegin");
		List<Long> beatBeginPos = new ArrayList<>();
		for(int i = 0; i < beatBegin.length(); i++) {
			beatBeginPos.add(beatBegin.getLong(i));
		}
		List<Float> normalizedEcgData = normalizeEcgData(ecgData, beatBeginPos);
		
		// cut the ecg data into the segments
		JSONArray rPosArr =  (JSONArray)rPosAndBeatBegin.get("RPos");
		List<Long> rPos = new ArrayList<>();
		for(int i = 0; i < rPosArr.length(); i++) {
			rPos.add(rPosArr.getLong(i));
		}
		segEcgData = getSegEcgData(normalizedEcgData, rPos, beatBeginPos);
		
		reviewResult = rPosAndBeatBegin;
		reviewResult.put("QrsPos", qrsAndRRInterval.get("QrsPos"));
		reviewResult.put("EcgData", ecgData);
		reviewResult.put("SegEcgData", segEcgData);
		reviewResult.put("SampleRate", sampleRate);
		System.out.println(reviewResult.toString());
	}	
	
	public String getSegEcgDataString() {
		if(segEcgData == null || segEcgData.isEmpty()) return "";
		
		StringBuilder builder = new StringBuilder();
		
		for(List<Float> seg : segEcgData) {
			for(Float d : seg) {
				builder.append(String.format("%.3f", d));
				builder.append(' ');
			}
			builder.append("\r\n");
		}
		return builder.toString();
	}
	
	private List<List<Float>> getSegEcgData(List<Float> normalizedEcgData, List<Long> rPos, List<Long> beatBeginPos) {
		List<List<Float>> segEcgData = new ArrayList<>();
		
		for(int i = 0; i < beatBeginPos.size()-1; i++) {
			List<Float> oneBeat = new ArrayList<>();
			
			long begin = beatBeginPos.get(i);
			long end = beatBeginPos.get(i+1);
			long r = rPos.get(i);
			long fillBefore = 0;
			long fillAfter = 0;
			float first = 0.0f;
			float last = 0.0f;
			if(r - begin >= NUM_BEFORE_R) {
				begin = r - NUM_BEFORE_R;
			} else {
				fillBefore = NUM_BEFORE_R - (r-begin);
				first = normalizedEcgData.get((int)begin);
			}
			if(end-r > NUM_AFTER_R) {
				end = r + NUM_AFTER_R + 1;
			} else {
				fillAfter = NUM_AFTER_R - (end-r) + 1;
				last = normalizedEcgData.get((int)(end-1));
			}
			for(int j = 0; j < fillBefore; j++) {
				oneBeat.add(first);
			}
			for(long pos = begin; pos < end; pos++) {
				oneBeat.add(normalizedEcgData.get((int)pos));
			}
			for(int j = 0; j < fillAfter; j++) {
				oneBeat.add(last);
			}
			segEcgData.add(oneBeat);
			//System.out.println(oneBeat.size());
		}
		return segEcgData;
	}
	
	private JSONObject getQrsPosAndRRInterval(List<Short> ecgData, int sampleRate) {
		QrsDetectorWithQRSInfo detector = new QrsDetectorWithQRSInfo(sampleRate);
		int n = 0;
		for(Short datum : ecgData) {
			detector.outputRRInterval((int)datum);
			n++;
			if(detector.firstPeakFound()) break;
		}
		for(Short datum : ecgData) {
			detector.outputRRInterval((int)datum);
		}
		
		List<Long> qrsPos = detector.getQrsPositions();
		List<Integer> rrInterval = detector.getRrIntervals();
		
		qrsPos.remove(0);
		rrInterval.remove(0);
		for(int i = 0; i < qrsPos.size(); i++) {
			long p = qrsPos.get(i)-n;
			if(p < 0) p = 0;
			qrsPos.set(i, p);
		}
		
		JSONObject json = new JSONObject();
		json.put("QrsPos", qrsPos);
		json.put("RRInterval", rrInterval);
		//System.out.println(json);
		return json;
	}
	
	private static JSONObject getRPosAndBeatBegin(List<Short> ecgData, JSONObject qrsAndRRInterval) {
		return RWaveDetecter.findRPosAndBeatBegin(ecgData, qrsAndRRInterval);
	}
	
	private List<Float> normalizeEcgData(List<Short> ecgData, List<Long> beatBeginPos) {
		List<Float> normalized = new ArrayList<>();
		
		for(Short d : ecgData) {
			normalized.add((float)d);
		}
		
		List<Float> oneBeat =  new ArrayList<>();
		for(int i = 0; i < beatBeginPos.size()-1; i++) {
			for(long pos = beatBeginPos.get(i); pos < beatBeginPos.get(i+1); pos++) {
				oneBeat.add(normalized.get((int)pos));
			}
			
			float ave = MathUtil.floatAve(oneBeat);
			float std = MathUtil.floatStd(oneBeat);
			
			for(int ii = 0; ii < oneBeat.size(); ii++) {
				oneBeat.set(ii, (oneBeat.get(ii)-ave)/std);
			}
			
			for(long pos = beatBeginPos.get(i),  ii = 0; pos < beatBeginPos.get(i+1); pos++, ii++) {
				normalized.set((int)pos, oneBeat.get((int)ii));
			}
			
			oneBeat.clear();
		}
		return normalized;
	}

	private FIRFilter designLpFilter(int sampleRate) {
		double[] wp = {2*Math.PI*65/sampleRate};
		double[] ws = {2*Math.PI*85/sampleRate};
		double Rp = 1;
		double As = 50;
		FilterType fType = FilterType.LOWPASS;
		WinType wType = WinType.HAMMING;
		
		FIRFilter filter = FIRDesigner.design(wp, ws, Rp, As, fType, wType);
		return filter;
	}
	
	private IIRFilter designNotch(int f0, int sampleRate) {
		IIRFilter filter = NotchDesigner.design(f0, 2, sampleRate);
		return filter;
	}
	
}
