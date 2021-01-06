package com.example.mthird;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dmax.dialog.SpotsDialog;
import edmt.dev.edmtdevcognitiveface.Contract.Face;
import edmt.dev.edmtdevcognitiveface.Contract.IdentifyResult;
import edmt.dev.edmtdevcognitiveface.Contract.Person;
import edmt.dev.edmtdevcognitiveface.Contract.TrainingStatus;
import edmt.dev.edmtdevcognitiveface.FaceServiceClient;
import edmt.dev.edmtdevcognitiveface.FaceServiceRestClient;
import edmt.dev.edmtdevcognitiveface.Rest.ClientException;
import edmt.dev.edmtdevcognitiveface.Rest.Utils;

public class AzureActivity extends AppCompatActivity implements ArduinoListener{

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference helm = db.collection("helmets");
    private DocumentReference helmItem = helm.document("hNh8dbIWxENzzOljNBDA");


    private Arduino arduino;
    private final String API_KEY = "d48fdff6e7af4dfd86fbb757c44c6884";
    private final String API_LINK = "https://detectrecogdemo.cognitiveservices.azure.com/face/v1.0";
    String temp ="";
    private FaceServiceClient faceServiceClient = new FaceServiceRestClient(API_LINK, API_KEY);

//    private  final String personGroupID = "randomperson";
    private  final String personGroupID = "alphahelm";
    int ctr=0;
    ImageView img_view;
    Bitmap bmp;
    Face[] faceDetected;

    class detectTask extends AsyncTask<InputStream,String,Face[]> {

//        AlertDialog alertDialog = new SpotsDialog.Builder()
//                .setContext(AzureActivity.this)
//                .setCancelable(false)
//                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected Face[] doInBackground(InputStream... inputStreams) {

            try{
                publishProgress("Detecting...");
                Face[] result = faceServiceClient.detect((inputStreams[0]),true,false,null);
                if(result==null){
                    return null;
                }else{
                    return result;
                }
            }catch (ClientException e){
                e.printStackTrace();
                Intent intent = new Intent(AzureActivity.this, MainActivity.class);
                startActivity(intent);
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Face[] faces) {

            //alertDialog.dismiss();
            if(faces == null){
                Toast.makeText(AzureActivity.this, "No face detected", Toast.LENGTH_SHORT).show();
            }else {
                img_view.setImageBitmap(Utils.drawFaceRectangleOnBitmap(bmp,faces, Color.YELLOW));
                faceDetected = faces;
                try{
                    Thread.sleep(1000);
                    if (faceDetected.length > 0){
                        final UUID[] faceIds = new UUID[faceDetected.length];
                        for (int i = 0; i<faceDetected.length;i++)
                            faceIds[i] = faceDetected[i].faceId;
                        new IdentificationTask().execute(faceIds);
                    }else{
                        Toast.makeText(AzureActivity.this,"No face to detect", Toast.LENGTH_SHORT).show();
                    }
                    Thread.sleep(3000);
                    Intent intent = new Intent(AzureActivity.this, MainActivity.class);
                    startActivity(intent);
                }catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        }
    }

    class IdentificationTask extends AsyncTask<UUID,String, IdentifyResult[]>{

//        AlertDialog alertDialog = new SpotsDialog.Builder()
//                .setContext(AzureActivity.this)
//                .setCancelable(false)
//                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
           // alertDialog.setMessage((values[0]));
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... uuids) {
            try{
                publishProgress("Getting person group status");
                TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(personGroupID);

                if (trainingStatus.status != TrainingStatus.Status.Succeeded){
                    //Log.d("ERROR","Person Group Training status is "+trainingStatus.status);
                    return  null;
                }

                publishProgress("identifying");
                IdentifyResult[] result = faceServiceClient.identity(personGroupID,uuids,2); //max number of candidates

                //Log.d("Hello",String.valueOf(result.length));
                if (result.length > 0){
                    //Log.d("Hello","not null");
                    return result;
                }else{
                    //Log.d("Hello","null ");
                    return null;
                }

            } catch (ClientException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            //alertDialog.dismiss();
            //Log.d("152",String.valueOf(identifyResults.length));
            if (identifyResults != null){
                for(IdentifyResult identifyResult:identifyResults)
//                    Log.d("155",identifyResult.candidates.toString());

                    try {
                        new PersonDetectionTask().execute(identifyResult.candidates.get(0).personId);
                    }catch (Exception ex){
                        //Toast.makeText(MainActivity.this, "Unknown", Toast.LENGTH_SHORT).show();
                        ex.printStackTrace();
                    }



            }
        }
    }

    class PersonDetectionTask extends AsyncTask<UUID, String, edmt.dev.edmtdevcognitiveface.Contract.Person> {

//        AlertDialog alertDialog = new SpotsDialog.Builder()
//                .setContext(AzureActivity.this)
//                .setCancelable(false)
//                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //alertDialog.setMessage((values[0]));
        }

        @Override
        protected Person doInBackground(UUID... uuids) {
            try {
//                String s = faceServiceClient.getPerson(personGroupID,uuids[0]).toString();
//                Log.d("190",s);
                if(uuids.length<1 || uuids==null){
                    return null;
                }else{
                    return faceServiceClient.getPerson(personGroupID,uuids[0]);
                }

            } catch (ClientException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Person person) {
            //alertDialog.dismiss();

            if(person!=null){
//                img_view.setImageBitmap(Utils.drawFaceRectangleWithTextOnBitmap(bmp,faceDetected,person.name,Color.YELLOW,100));
                Toast.makeText(AzureActivity.this, person.name, Toast.LENGTH_SHORT).show();
                Toast.makeText(AzureActivity.this, temp, Toast.LENGTH_SHORT).show();
                if(temp!=null && person!=null){
                    if(ctr==0){
                        try {
//                    FirebaseDatabase.getInstance().getReference("helmets").child()
                            Date currentTime = Calendar.getInstance().getTime();
                            Map<String,Object> detectedContent = new HashMap<>();
                            detectedContent.put("name",person.name);
                            detectedContent.put("date",currentTime);
                            detectedContent.put("Temp",temp);
//                    Map<String,Object> detected = new HashMap<>();
//                    detected.put("detected",detectedContent);

                            //helmItem.set(detected, SetOptions.merge());
                            helmItem.update("detected", FieldValue.arrayUnion(detectedContent));
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                        ctr++;
                    }

                }

            }else{
                Toast.makeText(AzureActivity.this, "Undefined Face", Toast.LENGTH_SHORT).show();
            }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //orig
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_azure);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray("picture");

        bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        img_view = (ImageView) findViewById(R.id.imageView1);

        img_view.setImageBitmap(bmp);
        arduino = new Arduino(AzureActivity.this);




    }


    //serialArduino callbacks
    @Override
    protected void onStart() {
        super.onStart();
        arduino.setArduinoListener(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduino.unsetArduinoListener();
        arduino.close();
    }
    @Override
    public void onArduinoAttached(UsbDevice device) {
//        display("arduino attached...");
        arduino.open(device);
    }
    @Override
    public void onArduinoDetached() {
//        display("arduino detached.");
    }
    @Override
    public void onArduinoMessage(byte[] bytes) {
        String recdata = new String(bytes);
        //Toast.makeText(AzureActivity.this, recdata, Toast.LENGTH_SHORT).show();
        temp = recdata;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        new detectTask().execute(inputStream);

//        arduino.unsetArduinoListener();
        arduino.close();
//        display(new String(bytes));
    }
    @Override
    public void onArduinoOpened() {
        String str = "arduino opened...";
        arduino.send(str.getBytes());
    }
    private void display(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                displayTextView.append(message);
            }
        });
    }
}