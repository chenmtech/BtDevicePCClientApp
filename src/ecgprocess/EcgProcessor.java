package ecgprocess;

import java.io.PrintWriter;
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
	private ResampleFrom250To360 resampler;
	private FIRFilter lpFilter;
	private IIRFilter notch50;
	private List<Short> ecgData;
	private List<Float> normalizedEcgData;
	private JSONObject reviewResult;
	
	public EcgProcessor() {
	}
	
	public FIRFilter designLpFilter(int sampleRate) {
		double[] wp = {2*Math.PI*65/sampleRate};
		double[] ws = {2*Math.PI*85/sampleRate};
		double Rp = 1;
		double As = 50;
		FilterType fType = FilterType.LOWPASS;
		WinType wType = WinType.HAMMING;
		
		FIRFilter filter = FIRDesigner.design(wp, ws, Rp, As, fType, wType);
		return filter;
	}
	
	public IIRFilter designNotch(int f0, int sampleRate) {
		IIRFilter filter = NotchDesigner.design(f0, 2, sampleRate);
		return filter;
	}
	
	public List<Short> getEcgData() {
		return ecgData;
	}

	public List<Float> getNormalizedEcgData() {
		return normalizedEcgData;
	}

	public JSONObject getReviewResult() {
		return reviewResult;
	}

	public JSONArray getBeatBeginPos() {
		return (JSONArray)getReviewResult().get("BeatBegin");
	}

	public void process(List<Short> ecgData, int sampleRate) {
		if(ecgData == null || ecgData.isEmpty()) {
			return;
		}
		
		resampler = new ResampleFrom250To360();
		notch50 = designNotch(50, sampleRate);
		lpFilter = designLpFilter(sampleRate);
		System.out.println(lpFilter);
		
		// do filtering
		List<Short> afterFilter = new ArrayList<Short>();
		for(Short d : ecgData) {
			afterFilter.add((short)Math.round(lpFilter.filter(notch50.filter(d))));
		}		
		ecgData = afterFilter;
		
		// do resampling
		this.ecgData = resampler.resample(ecgData);
		sampleRate = resampler.getOutSampleRate();
		
		// detect the QRS waves and RR interval
		JSONObject qrsAndRRInterval = getQrsPosAndRRInterval(this.ecgData, sampleRate);
		// detect the R wave position and the begin pos of each beat
		JSONObject rPosAndBeatBegin = getRPosAndBeatBegin(this.ecgData, qrsAndRRInterval);

		JSONArray beatBegin = (JSONArray)rPosAndBeatBegin.get("BeatBegin");
		normalizedEcgData = normalizeEcgData(this.ecgData, beatBegin);
		
		reviewResult = rPosAndBeatBegin;
		reviewResult.put("QrsPos", qrsAndRRInterval.get("QrsPos"));
		System.out.println(reviewResult.toString());
	}
	
	public void outputNormalizedEcgData(PrintWriter writer, JSONArray beatBegin) {
		for(int i = 0; i < beatBegin.length()-1; i++) {
			long begin = beatBegin.getLong(i);
			long end = beatBegin.getLong(i+1);
			int length = (int)(end - begin);
			
			int fill = 0;
			if(length > 250) {
				begin += (length-250)/2;
				end = begin + 250;
			} else if(length < 250) {
				fill = 250-length;
			}
			long pos;
			for(pos = begin; pos <end; pos++) {
				writer.printf("%.3f", normalizedEcgData.get((int)pos));
				writer.print(' ');
			}
			float lastNum = normalizedEcgData.get((int)(pos-1));
			for(int j = 0; j < fill; j++) {
				writer.printf("%.3f", lastNum);
				writer.print(' ');
			}
			writer.print("\r\n");
		}
	}
	
	private JSONObject getQrsPosAndRRInterval(List<Short> ecgData, int sampleRate) {
		QrsDetectorWithQRSInfo detector = new QrsDetectorWithQRSInfo(sampleRate);
		for(Short datum : ecgData) {
			detector.outputRRInterval((int)datum);
		}
		List<Long> qrsPos = detector.getQrsPositions();
		List<Integer> rrInterval = detector.getRrIntervals();
		JSONObject json = new JSONObject();
		json.put("QrsPos", qrsPos);
		json.put("RRInterval", rrInterval);
		System.out.println(json);
		return json;
	}
	
	private static JSONObject getRPosAndBeatBegin(List<Short> ecgData, JSONObject qrsAndRRInterval) {
		return RWaveDetecter.findRPosAndBeatBegin(ecgData, qrsAndRRInterval);
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
			
			float ave = MathUtil.floatAve(oneBeat);
			float std = MathUtil.floatStd(oneBeat);
			
			for(int ii = 0; ii < oneBeat.size(); ii++) {
				oneBeat.set(ii, (oneBeat.get(ii)-ave)/std);
			}
			
			for(long begin = beatBegin.getLong(i),  ii = 0; begin < beatBegin.getLong(i+1); begin++, ii++) {
				out.set((int)begin, oneBeat.get((int)ii));
			}
			
			oneBeat.clear();
		}
		return out;
	}
}
