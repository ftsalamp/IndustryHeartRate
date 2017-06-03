package com.example.user.industryheartrate;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // SPI Device Name
    private static final String SPI_DEVICE_NAME ="SPI0.0";
    private static final String TAG = "heart rate app";
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 100;
    private Handler mHandler = new Handler();
    private SpiDevice mDevice;
    PulseSensorUtil pulseSensor=new PulseSensorUtil();
    private int lowMax=55, norm=95;
    TextView heartratelabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        heartratelabel = (TextView) findViewById(R.id.heartratelabel);

        // Attempt to access the SPI device
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            mDevice = manager.openSpiDevice(SPI_DEVICE_NAME);
            mDevice.setMode(SpiDevice.MODE0);
            mDevice.setBitsPerWord(8); // 8 BPW
            mDevice.setFrequency(1000000); // 1MHz
            mDevice.setBitJustification(false);
            mHandler.post(deviceReadThread);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access SPI device", e);
        }

        Button but= (Button) findViewById(R.id.button);
        but.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                lowMax= Integer.parseInt(findViewById(R.id.lowMax).toString());
                norm= Integer.parseInt(findViewById(R.id.editNormalMax).toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close SPI device", e);
            }
        }
    }

    private Runnable deviceReadThread = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (mDevice == null) {
                return;
            }
            try {

                Log.i(TAG,"Reading from the SPI");

                byte[] data=new byte[3];
                byte[] response=new byte[3];
                data[0]=1;
                int a2dChannel=0;
                data[1]= (byte) (8 << 4);
                data[2] = 0;

                //full duplex mode
                mDevice.transfer(data,response,3);

                int a2dVal = 0;
                a2dVal = (response[1]<< 8) & 0b1100000000; //merge data[1] & data[2] to get result
                a2dVal |=  (response[2] & 0xff);


                String bpm = pulseSensor.process(a2dVal);
                heartratelabel.setText(bpm);
                int temp=Integer.parseInt(bpm);
                TextView rec= (TextView)findViewById(R.id.speedReccomendation);
                if(temp<lowMax)
                    rec.setText("The production line could go faster");
                else if(temp<norm)
                    rec.setText("The production line is at optimal speed");
                else
                    rec.setText("The production line is going too fast");

                mHandler.postDelayed(deviceReadThread, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };
}
