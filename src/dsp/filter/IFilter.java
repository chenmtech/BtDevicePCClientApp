package dsp.filter;

import dsp.seq.ComplexSeq;
import dsp.seq.RealSeq;

public interface IFilter {
	RealSeq getB();
	RealSeq getA();
	void setB(RealSeq b);
	void setA(RealSeq a);
	ComplexSeq freq(RealSeq omega);
	RealSeq mag(RealSeq omega);
	RealSeq pha(RealSeq omega);
}
