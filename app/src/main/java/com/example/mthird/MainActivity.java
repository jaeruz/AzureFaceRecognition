package com.example.mthird;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;

import org.opencv.imgproc.Imgproc;

import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;

import org.opencv.dnn.Dnn;

import java.io.ByteArrayOutputStream;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;

    boolean startFaces = false;
    boolean firstTimeFaces = false;
    int ctr=0;
    Net detector;
    ImageView img_view;
    public void Faces(/*View Button*/){

        if (startFaces == false){
            startFaces = true;
            if (firstTimeFaces == false){
                firstTimeFaces = true;
                String protoPath = Environment.getExternalStorageDirectory() + "/dnns/deploy.prototxt" ;
                String caffeWeights = Environment.getExternalStorageDirectory() + "/dnns/res10_300x300_ssd_iter_140000.caffemodel";
                detector = Dnn.readNetFromCaffe(protoPath, caffeWeights);
            }
        }
        else{
            startFaces = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

//        img_view = (ImageView)findViewById(R.id.img_view);

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);




            //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            baseLoaderCallback = new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    super.onManagerConnected(status);

                    switch(status){

                        case BaseLoaderCallback.SUCCESS:
                            cameraBridgeViewBase.enableView();
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }

                }

        };


    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        if(ctr==0){
            Faces();
        }

        ctr++;

        if (startFaces == true){

            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

            Mat imageBlob = Dnn.blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0), true, false, CvType.CV_32F);

            detector.setInput(imageBlob); //set the input to network model
            Mat detections = detector.forward(); //feed forward the input to the netwrok to get the output

            int cols = frame.cols();
            int rows = frame.rows();

            double THRESHOLD = 0.85;

            detections = detections.reshape(1, (int)detections.total() / 7);

            Log.d("EXPERIMENT5:ROWS", detections.rows()+"");

//            for (int i = 0; i < detections.rows(); ++i) {
                for (int i = 0; i < 1; ++i) {

                double confidence = detections.get(i, 2)[0];

                Log.d("EXPERIMENT6", i+" "+confidence+" "+THRESHOLD);
                if (confidence > THRESHOLD) {
                    try{
                        Thread.sleep(1000);
                        final Bitmap mybitmap = Bitmap.createBitmap(frame.cols(),frame.rows(), Bitmap.Config.ARGB_8888);

                        Utils.matToBitmap(frame,mybitmap);
                        final Bitmap converetdImage = getResizedBitmap(mybitmap, 500);


                        //serialread from thermal sensor thread

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                converetdImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                              mybitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                byte[] byteArray = stream.toByteArray();
                                Intent intent = new Intent(MainActivity.this, AzureActivity.class);
                                intent.putExtra("picture", byteArray);
                                startActivity(intent);
                                //img_view.setImageBitmap(mybitmap);
                                img_view.setImageBitmap(converetdImage);

                            }
                        });
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }




                    int left   = (int)(detections.get(i, 3)[0] * cols);
                    int top    = (int)(detections.get(i, 4)[0] * rows);
                    int right  = (int)(detections.get(i, 5)[0] * cols);
                    int bottom = (int)(detections.get(i, 6)[0] * rows);

                    if (left<0){
                        left=0;
                    }
                    if (top<0){
                        top=0;
                    }
                    if (right<0){
                        right=0;
                    }
                    if (bottom<0){
                        bottom=0;
                    }
                    int xLim=frame.size(1);
                    int yLim=frame.size(0);

                    if (left>=xLim){
                        left=xLim-2;
                    }
                    if (right>=xLim){
                        right=xLim-2;
                    }

                    if (top>=yLim){
                        top=yLim-2;
                    }
                    if (bottom>=yLim){
                        bottom=yLim-2;
                    }

                    Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),new Scalar(255, 255, 0),2);
                } } }

        return frame;
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        if (startFaces == true){

            String protoPath = Environment.getExternalStorageDirectory() + "/dnns/deploy.prototxt" ;
            String caffeWeights = Environment.getExternalStorageDirectory() + "/dnns/res10_300x300_ssd_iter_140000.caffemodel";

            detector = Dnn.readNetFromDarknet(protoPath, caffeWeights);


        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}