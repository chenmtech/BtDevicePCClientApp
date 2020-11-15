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
		
		List<Float> delta2= diffFilter(ecgData); // 二阶差分
		List<Long> rPos = new ArrayList<>();
		List<Float> segNormData = new ArrayList<>();
		List<Float> segDelta2 = new ArrayList<>();
		
		for(int i = 0; i < rrInterval.size()-1; i++) {
			long qrs = qrsPos.get(i+1);
			long tmp = qrs - Math.round(rrInterval.get(i)*2.0/5);
			long beatBegin = (tmp > 0) ? tmp : 0;
			tmp = qrs + Math.round(rrInterval.get(i+1)*3.0/5);
			long beatEnd = (tmp < ecgData.size()) ? tmp : ecgData.size()-1;
			
			for(long i1 = beatBegin; i1 < beatEnd; i1++) {
				segNormData.add((float)ecgData.get((int)i1));
				segDelta2.add(delta2.get((int)i1));
			}
			long qrsSegPos = qrs-beatBegin;
			tmp = qrsSegPos-halfQrsWidth;
			long qrsBegin = (tmp > 0) ? tmp : 0;
			tmp = qrsSegPos + halfQrsWidth;
			long qrsEnd = (tmp < segNormData.size()) ? tmp : segNormData.size()-1;
			
			//List<Short> temp = ecgData.subList((int)beatBegin, (int)beatEnd);
			float ave = MathUtil.floatAve(segNormData);
			float std = MathUtil.floatStd(segNormData);	
			
			Pair<Integer, Float> rlt = MathUtil.floatMin(segNormData.subList((int)qrsBegin, (int)qrsEnd));
			float minV = rlt.second;
			int minI = rlt.first;
			
			rlt = MathUtil.floatMax(segNormData.subList((int)qrsBegin, (int)qrsEnd));
			float maxV = rlt.second;
			int maxI = rlt.first;
			
			if(Math.abs(maxV) > 2*Math.abs(minV)/3) {
				rPos.add(qrs + maxI);
			} else {
				rlt = MathUtil.floatMin(delta2.subList((int)qrsBegin, (int)qrsEnd));
				minV = rlt.second;
				minI = rlt.first;
				
				rlt = MathUtil.floatMax(delta2.subList((int)qrsBegin, (int)qrsEnd));
				maxV = rlt.second;
				maxI = rlt.first;
				
				if(Math.abs(maxV) > Math.abs(minV)) {
					rPos.add(qrs + maxI);
				} else {
					rPos.add(qrs + minI);
				}
			}
			
			long rBegin = rPos.get(i-1) - 5;
			long rEnd = rPos.get(i-1) + 5;
			List<Float> tmpList = new ArrayList<>();
			for(long j = rBegin; j <= rEnd; j++) {
				tmpList.add((float)Math.abs(ecgData.get((int)j)));
			}
			
			rlt = MathUtil.floatMax(tmpList);
			maxV = rlt.second;
			maxI = rlt.first;
			rPos.set(i-1, rBegin + maxI);			
			
			segNormData.clear();
			segDelta2.clear();
		}	
		
		List<Long> beatBegin = new ArrayList<>();
		for(int i = 1; i < rrInterval.size()-1; i++) {
			beatBegin.add(rPos.get(i-1) - Math.round(rrInterval.get(i)*2.0/5));
		}

		if(beatBegin.get(0) < 0) {
            beatBegin.remove(0);
            rPos.remove(0);
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

