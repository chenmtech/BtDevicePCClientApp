package dsp.filter;

import dsp.seq.ComplexSeq;
import dsp.seq.RealSeq;

public interface IAnalogFilter extends IFilter {
	ComplexSeq freq(double Qmax, int N);
	RealSeq mag(double Qmax, int N);
	RealSeq pha(double Qmax, int N);
}
