package dsp.filter.para;

import dsp.filter.design.AFType;
import dsp.filter.design.FilterType;

public class IIRPara extends DFPara {
	private AFType afType = AFType.NONE;
	
	public IIRPara() {
		
	}
	
	public IIRPara(double[] wp, double[] ws, double Rp, double As, AFType afType, FilterType fType) {
		super(wp, ws, Rp, As, fType);
		this.afType = afType;
	}
	
	public AFType getAFType() {
		return afType;
	}
	
	@Override
	public String toString() {
		return super.toString() + ',' + "AFType=" + afType; 
	}
}
