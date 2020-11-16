package afdetect;

import java.util.List;

import ecgprocess.EcgPreProcessor;

public class AFDetectEcgPreProcessor extends EcgPreProcessor {
	
	public void process(List<Short> ecgData, int sampleRate) {
		super.process(ecgData, sampleRate);		
	}
}
