package com.example.user.industryheartrate;

import android.util.Log;

import java.util.Arrays;


public class PulseSensorUtil {

        private int sampleCounter;
        private int lastBeatTime;
        private int threshold;
        private int ibi;
        private int trough;
        private int peak;
        private boolean pulse;
        private boolean secondBeat;
        private int rate[]=new int[10];
        private boolean firstBeat;
        private int bpm;
        private int amp;

        private void pulseSensorUtil() {
            sampleCounter = 0;
            lastBeatTime = 0;
            threshold = 256;
            ibi = 600;
            trough = 256;
            peak = 256;
            pulse = false;
            secondBeat = false;
            rate = new int[10];
            Arrays.fill(rate, 0);
            firstBeat = true;
            bpm = 60;
            amp = 100;
        }

// Code to convert analog values to BPM is from the reference
        //https://github.com/mbientlab-projects/AndroidHeartRateMonitor



        public String process(int signal) {
           Log.i("HRSFagment signal ", String.valueOf(signal));

            // We take a reading every 2 ms
            sampleCounter += 2;                        // keep track of the time in mS with this variable
            int timeInterval = sampleCounter - lastBeatTime;       // monitor the time since the last beat to avoid noise

            Log.i("HRSFragment timeInt ", String.valueOf(timeInterval));

            // Find the peak and trough of the pulse wave
            if (signal < threshold && timeInterval > (ibi / 5) * 3) {       // avoid dichrotic noise by waiting 3/5 of last IBI
                if (signal < trough) {                        // T is the trough
                    trough = signal;                         // keep track of lowest point in pulse wave
                    Log.i("HRSFragment trough", String.valueOf(trough));
                }
            }

            if (signal > threshold && signal > peak) {          // thresh condition helps avoid noise
                peak = signal;                             // P is the peak
                Log.i("HRSFragment Peaks", String.valueOf(peak));
            }                                           // keep track of highest point in pulse wave

            // Look for the heart beat
            // Signal surges up in value every time there is a pulse
            if (timeInterval > 250) {                               // avoid high frequency noise
                if ((signal > threshold) && (!pulse) && (timeInterval > ((ibi / 5) * 3))) {
                    pulse = true;                       // set the Pulse flag when we think there is a pulse
                    ibi = sampleCounter - lastBeatTime; // measure time between beats in mS
                    Log.i("HRSFragment The IBI is ", String.valueOf(ibi));
                    lastBeatTime = sampleCounter;       // keep track of time for next pulse

                    if (firstBeat) {                      // if it's the first time we found a beat, if firstBeat == TRUE
                        Log.i("HSRFragment", "First beat");
                        firstBeat = false;              // clear firstBeat flag
                        secondBeat = true;              // set the second beat flag
                        //return "-1";                         // IBI value is unreliable so discard it
                    } else if (secondBeat) {                     // if this is the second beat, if secondBeat == TRUE
                        Log.i("HSRFragment ", "Second beat");
                        secondBeat = false;             // clear secondBeat flag
                        for (int i = 0; i <= 9; i++) {        // seed the running total to get a realisitic BPM at startup
                            rate[i] = ibi;
                        }
                    } else {

                        // Keep a running total of the last 10 IBI values
                        int runningTotal = 0;               // clear the runningTotal variable

                        for (int i = 0; i <= 8; i++) {            // shift data in the rate array
                            rate[i] = rate[i + 1];
                            runningTotal += rate[i];
                            Log.i("HRSFragment", " Count " + String.valueOf(i) + " from added " + String.valueOf(rate[i]));
                            runningTotal += rate[i];        // add up the 9 oldest IBI values
                        }

                        rate[9] = ibi;

                        runningTotal += rate[9];
                        Log.i("HRSFragment RunningTot", String.valueOf(runningTotal));
                        runningTotal /= 10;                 // average the last 10 IBI values
                        Log.i("HRSFragment RunTot Avg", String.valueOf(runningTotal));
                        bpm = 60000 / runningTotal;           // get the beats per minutes -> BPM
                        Log.i("HRSFragment BMP is ", String.valueOf(bpm));

                        return String.valueOf(bpm);
                    }
                }


                if (signal < threshold && pulse) {      // when the values are going down, the beat is over
                    pulse = false;                          // reset the Pulse flag so we can do it again
                    amp = peak - trough;                            // get amplitude of the pulse wave
                    threshold = amp / 2 + trough;                     // set thresh at 50% of the amplitude
                    peak = threshold;                             // reset these for next time
                    trough = threshold;
                }
            }

            if (timeInterval > 2500 ) {             // if 2.5 seconds go by without a beat -> reset
                threshold = 256;                           // set thresh default
                peak = 512;                                // set P default
                trough = 512;                                // set T default
                lastBeatTime = sampleCounter;           // bring the lastBeatTime up to date
                firstBeat = true;                       // set these to avoid noise
                secondBeat = false;                     // when we get the heartbeat back
            }

            Log.i("HRSFragment BMP is ", String.valueOf(bpm));
            return String.valueOf(bpm);

        }
}
