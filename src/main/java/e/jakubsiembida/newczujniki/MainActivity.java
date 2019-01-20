package e.jakubsiembida.newczujniki;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
//import android.support.v4.content.ContextCompat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRunning;
    private boolean wasRunning;
    private PowerManager.WakeLock wakeLock;
    private static final double G = 9.81;

    Button buttonMeasurement;
    TextView tvAx, tvAy, tvAz, tvSteps;

    private ArrayList<String> magnitudeData;
    private int stepCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyAPPK:Test czujników");
        wakeLock.acquire(); //niezbędne

        buttonMeasurement = findViewById(R.id.button);
        tvAx = findViewById(R.id.tvAx);
        tvAy = findViewById(R.id.tvAy);
        tvAz = findViewById(R.id.tvAz);
        tvSteps = findViewById(R.id.tvSteps);

        if (savedInstanceState != null) {
            tvAx.setText(savedInstanceState.getString("ax"));
            tvAy.setText(savedInstanceState.getString("ay"));
            tvAz.setText(savedInstanceState.getString("az"));
            isRunning = savedInstanceState.getBoolean("isRunning");
            wasRunning = savedInstanceState.getBoolean("wasRunning");
            magnitudeData = savedInstanceState.getStringArrayList("magnitudeData");
        }

/*
        to do api 27+, mój telefon nie wymaga tego- wręcz z użyciem ContextCompat mój telefon nie spełnia wymagań
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

        }
*/


    }

    @Override
    protected void onStop() {
        super.onStop();
        wasRunning = isRunning;
        isRunning = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(wasRunning){
            isRunning = true;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("ax", String.valueOf(tvAx.getText()));
        savedInstanceState.putString("ay", String.valueOf(tvAy.getText()));
        savedInstanceState.putString("az", String.valueOf(tvAz.getText()));
        savedInstanceState.putBoolean("isRunning", isRunning);
        savedInstanceState.putStringArrayList("magnitudeData", magnitudeData);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Float ax, ay, az;

        if(isRunning){
            int sensorType = event.sensor.getType();
            if(sensorType == Sensor.TYPE_ACCELEROMETER){

                double magnitude;


                ax = event.values[0];
                ay = event.values[1];
                az = event.values[2];

                tvAx.setText(String.valueOf(ax));
                tvAy.setText(String.valueOf(ay));
                tvAz.setText(String.valueOf(az));

                //magnitude of acceleration vector

                magnitude = Math.abs(Math.sqrt(ax*ax  +ay*ay + az*az) - G); //usuwam g, nie wiem czy dobrze, lecz myśle ze to nie wpłynie na wynik

                //jeżeli przekroczy mój wyznaczony próg i lista ma conajmniej 2 elementy
                if(magnitude > 3.5 && magnitudeData.size() > 2){
                    double oneBefore = Double.parseDouble(magnitudeData.get(magnitudeData.size()-1)); //pobieram element przed

                    //jeżeli nie jest prawdą, ze element przed jest większy od 3.5 to dodaj krok
                    if(!(oneBefore > 3.5)){
                        stepCounter++;
                    }

                }

                tvSteps.setText(String.valueOf(stepCounter));

                magnitudeData.add(String.valueOf(magnitude));

                Log.d("onSensorChanged ax:", String.valueOf(ax) +  ", " + String.valueOf(ay)+", " + String.valueOf(az));

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private <K> void saveToFile(ArrayList<K> data, String folder, String fileName) {


        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + folder);
        dir.mkdirs();
        File file = new File(dir, fileName);

        String test = file.getAbsolutePath();
        Log.i("My", "FILE LOCATION: " + test);


        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);


            for (int i = 0; i < data.size(); i++) {
                pw.println(data.get(i));
            }

            pw.flush();
            pw.close();
            f.close();


            Toast.makeText(getApplicationContext(),

                    "Data saved",

                    Toast.LENGTH_LONG).show();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found *********");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String readFromFile(String folder, String fileName) {

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + folder);
        File file = new File(dir, fileName);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found *********");
        } catch (IOException e) {
            //You'll need to add proper error handling here
        } finally {
            return text.toString();
        }


    }



    public void onClickBtn(View view) {
        if(!isRunning){
            //uruchamiam zegar,
            magnitudeData = new ArrayList<>();
            stepCounter = 0;
            wakeLock.acquire();

        } else {
            //wyłączam zegar
            wakeLock.release();
            saveToFile(magnitudeData, "/TEST/", "testMagnitudeStepCounter.txt");

        }

        Log.d("onClickBtn", "button pressed");
        isRunning = !isRunning;


    }
}
