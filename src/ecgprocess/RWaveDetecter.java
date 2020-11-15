package ecgprocess;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dsp.filter.FIRFilter;
import dsp.filter.IDigitalFilter;
import dsp.filter.structure.StructType;
import util.MathUtil;
import util.Pair;

public class RWaveDetecter {
	private static final int QRS_WIDTH_MS = 300; // QRS波宽度，unit:毫秒
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> findRWaveAndBeatBeginPos(List<Short> ecgData, Map<String, Object> qrsAndRRInterval, int sampleRate) {
		int halfQrsWidth = QRS_WIDTH_MS*sampleRate/1000/2; // QRS波一半宽度点数
		
		List<Long> qrsPos = (List<Long>) qrsAndRRInterval.get("QrsPos");
		List<Integer> rrInterval = (List<Integer>) qrsAndRRInterval.get("RRInterval");
		List<Float> delta2= diffFilter(ecgData); // 信号二阶差分
		
		List<Long> rPos = new ArrayList<>(); 
		//List<Float> segBeat = new ArrayList<>(); // 一次心跳的数据
		//List<Float> segQrsDelta2 = new ArrayList<>(); // 一次QRS波二阶差分数据
		
		for(int i = 1; i < qrsPos.size()-1; i++) {
			long qrs = qrsPos.get(i);
			
			long beatBegin = qrs - Math.round(rrInterval.get(i-1)*2.0/5);
			long qrsBegin = qrs-halfQrsWidth;
			long minTmp = Math.min(beatBegin, qrsBegin);
			if(minTmp < 0) continue;
			beatBegin = minTmp;
			
			long beatEnd = qrs + Math.round(rrInterval.get(i)*3.0/5);
			long qrsEnd = qrs + halfQrsWidth;
			long maxTmp = Math.max(beatEnd, qrsEnd);
			if(maxTmp >= ecgData.size()) continue;
			beatEnd = maxTmp;
			
			List<Float> beat = new ArrayList<>(); // 一次心跳的数据
			for(Short d : ecgData.subList((int)beatBegin, (int)beatEnd)) {
				beat.add((float)d);
			}
			
			float ave = MathUtil.floatAve(beat);
			float std = MathUtil.floatStd(beat);
			
			for(int i1 = 0; i1 < beat.size(); i1++) {
				beat.set(i1, (beat.get(i1)-ave)/std);
			}
			
			Pair<Integer, Float> rlt = MathUtil.floatMin(beat);
			float minV = rlt.second;
			int minI = rlt.first;
			
			rlt = MathUtil.floatMax(beat);
			float maxV = rlt.second;
			int maxI = rlt.first;
			
			if(Math.abs(maxV) > 2*Math.abs(minV)/3) {
				rPos.add(beatBegin + maxI);
			} else {				
				List<Float> qrsDelta2 = delta2.subList((int)qrsBegin, (int)qrsEnd); // 一次QRS波的二阶差分数据
				
				rlt = MathUtil.floatMin(qrsDelta2);
				minV = rlt.second;
				minI = rlt.first;
				
				rlt = MathUtil.floatMax(qrsDelta2);
				maxV = rlt.second;
				maxI = rlt.first;
				
				if(Math.abs(maxV) > Math.abs(minV)) {
					rPos.add(qrsBegin + maxI);
				} else {
					rPos.add(qrsBegin + minI);
				}
			}
			
/*			long rBegin = rPos.get(i-1) - 5;
			long rEnd = rPos.get(i-1) + 5;
			List<Float> tmpList = new ArrayList<>();
			for(long j = rBegin; j <= rEnd; j++) {
				tmpList.add((float)Math.abs(ecgData.get((int)j)));
			}
			
			rlt = MathUtil.floatMax(tmpList);
			maxV = rlt.second;
			maxI = rlt.first;
			rPos.set(i-1, rBegin + maxI);*/
		}	
		
		List<Integer> newRRInterval = new ArrayList<>();
		for(int i = 1; i < rPos.size(); i++) {
			newRRInterval.add((int)(rPos.get(i)-rPos.get(i-1)));
		}

		List<Long> beatBegin = new ArrayList<>();
		for(int i = 1; i < rPos.size(); i++) {
			beatBegin.add(rPos.get(i) - Math.round(newRRInterval.get(i-1)*2.0/5));
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("RPos", rPos);
		map.put("BeatBegin", beatBegin);
		return map;
	}
	
	private static List<Float> diffFilter(List<Short> data) {
		double[] b = {1,0,-2,0,1};
		IDigitalFilter diffFilter = new FIRFilter(b);
		diffFilter.createStructure(StructType.FIR_LPF);
		
		List<Float> d2Data = new ArrayList<>();
		for(Short d : data) {
			d2Data.add((float)diffFilter.filter(d));
		}
		
		// 考虑滤波产生2个数的移位，矫正它
		d2Data.remove(0);
		d2Data.remove(0);
		float tmp = d2Data.get(d2Data.size()-1);
		d2Data.add(tmp);
		d2Data.add(tmp);
		
		return d2Data;
	}
}

