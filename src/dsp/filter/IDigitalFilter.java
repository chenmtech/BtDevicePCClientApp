package dsp.filter;

import dsp.filter.structure.StructType;
import dsp.seq.ComplexSeq;
import dsp.seq.RealSeq;

public interface IDigitalFilter extends IFilter {
	ComplexSeq freq(int N);
	RealSeq mag(int N);
	RealSeq pha(int N);
	IDigitalFilter createStructure(StructType sType);
	double filter(double x);
	RealSeq filter(RealSeq seq);
}
