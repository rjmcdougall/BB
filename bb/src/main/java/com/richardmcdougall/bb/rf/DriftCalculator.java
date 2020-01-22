package com.richardmcdougall.bb.rf;

import java.util.ArrayList;

public class DriftCalculator {

    public class Sample {
        public long drift;
        public long roundTripTime;
    }

    private ArrayList<Sample> samples = new ArrayList<Sample>();

    public void AddSample(long drift, long rtt) {
        Sample s = new Sample();
        s.drift = drift;
        s.roundTripTime = rtt;
        samples.add(samples.size(), s);
        // RMC: trying 10 instead of 100 because of long recovery times when time jumps on master
        if (samples.size() > 10)
            samples.remove(0);
    }

    public Sample BestSample() {
        long rtt = Long.MAX_VALUE;
        Sample ret = null;
        for (int i = 0; i < samples.size(); i++) {
            if (samples.get(i).roundTripTime < rtt) {
                rtt = samples.get(i).roundTripTime;
                ret = samples.get(i);
            }
        }
        return ret;
    }

}
