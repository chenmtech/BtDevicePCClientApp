package ecgprocess;

import java.util.ArrayList;
import java.util.List;

import dsp.filter.FIRFilter;
import dsp.filter.design.FIRDesigner;
import dsp.filter.design.FilterType;
import dsp.filter.design.WinType;
import dsp.filter.structure.StructType;
import dsp.seq.RealSeq;

public class ResampleFrom250To360 {
	 private final int K = 8;
     private final int M = 25;
     private final int L = 36;
     private final int N = K*L*2-1;
     private final int delay = (N-1)/2;
     private final FIRFilter filter;
     
     public ResampleFrom250To360() {
    	double[] wc = {Math.PI/L};
		WinType wType = WinType.HAMMING;
		FilterType fType = FilterType.LOWPASS;
		RealSeq h = FIRDesigner.FIRUsingWindow(N, wc, wType, fType);
		//h = (RealSeq) h.multiple((double) L);
		filter = new FIRFilter(h);
		filter.createStructure(StructType.FIR_LPF);
     }
     
     public List<Short> resample(List<Short> ecgData) {
    	 List<Float> out = new ArrayList<>();
    	 
		for(Short d : ecgData) {
			out.add((float)filter.filter((double)d));
			for(int i = 0; i < L-1; i++) {
				out.add((float)filter.filter(0.0));
			}
		}
		
		List<Short> out1 = new ArrayList<>();
		for(int i = delay; i < out.size(); i+=M) {
			out1.add((short)Math.round(L*out.get(i)));
		}
		
		return out1;    	 
     }
     
     public int getOutSampleRate() {
    	 return 360;
     }
}
