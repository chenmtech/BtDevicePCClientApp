package ecgprocess;

import java.util.List;


public interface IEcgArrhythmiaDetector {
    String getVer();
    String process(List<Short> ecgData, int sampleRate);
}
