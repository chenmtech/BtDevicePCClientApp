package ecgprocess;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import dsp.filter.FIRFilter;
import dsp.filter.IDigitalFilter;
import dsp.filter.structure.StructType;
import javafx.util.Pair;
import util.MathUtil;

public class RWaveDetecter {
	private static final int QRS_WIDTH_MS = 300; // unit:ms
	private static final int SAMPLE_RATE = 360; // sample rate
	private static final int QRS_WIDTH = QRS_WIDTH_MS*SAMPLE_RATE/1000;
	private static final int QRS_HALF_WIDTH = QRS_WIDTH/2;
	
	public static JSONObject findRPosAndBeatBegin(List<Short> ecgData, JSONObject qrsAndRRInterval) {
		List<Long> qrsPos = new ArrayList<>();
		List<Integer> rrInterval = new ArrayList<>();
		JSONArray array = (JSONArray)qrsAndRRInterval.get("QrsPos");
		for(int i = 0; i < array.length(); i++) {
			qrsPos.add(array.getLong(i));
		}
		array = (JSONArray)qrsAndRRInterval.get("RRInterval");
		for(int i = 0; i < array.length(); i++) {
			rrInterval.add(array.getInt(i));
		}
		
		List<Float> d2= diffFilter(ecgData);
		List<Long> rPos = new ArrayList<>();
		List<Float> normData = new ArrayList<>();
		for(Short d : ecgData) {
			normData.add((float)d);
		}
		for(int i = 0; i < rrInterval.size()-1; i++) {
			long qrs = qrsPos.get(i+1);
			long qrsBegin = qrs - QRS_HALF_WIDTH;
			long qrsEnd = qrs + QRS_HALF_WIDTH;
			long beatBegin = qrs - Math.round(rrInterval.get(i)*2.0/5);
			long beatEnd = qrs + Math.round(rrInterval.get(i+1)*3.0/5);
			
			List<Short> temp = ecgData.subList((int)beatBegin, (int)beatEnd);
			float ave = MathUtil.shortAve(temp);
			float std = MathUtil.shortStd(temp);
			
			long ii = beatBegin;
			for(Short d : temp) {
				normData.set((int) ii, (d - ave)/std);
				ii++;
			}
			
			Pair<Integer, Float> rlt = MathUtil.floatMin(normData.subList((int)qrsBegin, (int)qrsEnd));
			float minV = rlt.getValue();
			int minI = rlt.getKey();
			
			rlt = MathUtil.floatMax(normData.subList((int)qrsBegin, (int)qrsEnd));
			float maxV = rlt.getValue();
			int maxI = rlt.getKey();
			
			if(Math.abs(maxV) > Math.abs(minV)) {
				rPos.add(qrsBegin + maxI -1);
			} else {
				rlt = MathUtil.floatMin(d2.subList((int)qrsBegin, (int)qrsEnd));
				minV = rlt.getValue();
				minI = rlt.getKey();
				
				rlt = MathUtil.floatMax(d2.subList((int)qrsBegin, (int)qrsEnd));
				maxV = rlt.getValue();
				maxI = rlt.getKey();
				
				if(Math.abs(maxV) > Math.abs(minV)) {
					rPos.add(qrsBegin + maxI -3);
				} else {
					rPos.add(qrsBegin + minI -3);
				}
			}
			
			long rBegin = rPos.get(i) - 5;
			long rEnd = rPos.get(i) + 5;
			List<Float> tmp = new ArrayList<>();
			for(long j = rBegin; j <= rEnd; j++) {
				tmp.add((float)Math.abs(ecgData.get((int)j)));
			}
			
			rlt = MathUtil.floatMax(tmp);
			maxV = rlt.getValue();
			maxI = rlt.getKey();
			rPos.set(i, rBegin+maxI);			
		}	
		
		List<Long> beatBegin = new ArrayList<>();
		for(int i = 0; i < rrInterval.size()-1; i++) {
			beatBegin.add(rPos.get(i) - Math.round(rrInterval.get(i)*2.0/5));
		}
		JSONObject json = new JSONObject();
		json.put("RPos", rPos);
		json.put("BeatBegin", beatBegin);
		return json;
	}
	
	private static List<Float> diffFilter(List<Short> data) {
		double[] b = {1,0,-2,0,1};
		IDigitalFilter diffFilter = new FIRFilter(b);
		diffFilter.createStructure(StructType.FIR_LPF);
		
		List<Float> d2Data = new ArrayList<>();
		for(Short d : data) {
			d2Data.add((float)diffFilter.filter(d));
		}
		
		return d2Data;
	}
}
