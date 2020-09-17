package ecgprocess;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import dsp.filter.FIRFilter;
import dsp.filter.IDigitalFilter;
import dsp.filter.structure.StructType;

public class RWaveDetecter {
	private static final int QRS_WIDTH_MS = 300; // unit:ms
	private static final int SAMPLE_RATE = 360; // sample rate
	private static final int QRS_WIDTH = QRS_WIDTH_MS*SAMPLE_RATE/1000;
	private static final int QRS_HALF_WIDTH = QRS_WIDTH/2;
	
	public List<Long> findRWave(List<Short> ecgData, JSONObject qrsAndBeatPos) {
		List<Long> qrsPos = new ArrayList<>();
		List<Long> beatBegin = new ArrayList<>();
		JSONArray array = (JSONArray)qrsAndBeatPos.get("QrsPos");
		for(int i = 0; i < array.length(); i++) {
			qrsPos.add(array.getLong(i));
		}
		array = (JSONArray)qrsAndBeatPos.get("BeatBegin");
		for(int i = 0; i < array.length(); i++) {
			beatBegin.add(array.getLong(i));
		}
		
		List<Float> d2EcgData = diffFilter(ecgData);

		
		
	}
	
	private List<Float> diffFilter(List<Short> data) {
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
