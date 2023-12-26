package com.rudderstack.face_recognition;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.HashMap;
import android.content.Intent;
import android.app.Activity;
import com.google.common.util.concurrent.ListenableFuture;
import android.content.Context;
import java.util.concurrent.ExecutionException;
import androidx.core.content.ContextCompat;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import android.util.Size;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import android.media.Image;
import android.annotation.SuppressLint;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.widget.TextView;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import com.google.mlkit.vision.face.FaceDetector;
import androidx.camera.view.PreviewView;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.YuvImage;
import android.graphics.ImageFormat;
import java.io.ByteArrayOutputStream;
import android.graphics.Rect;
import android.graphics.BitmapFactory;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.IOException;
import org.tensorflow.lite.Interpreter;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import android.os.Bundle;




/** FaceRecognitionPlugin */
public class FaceRecognitionPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;
  private HashMap<String, Recognition> registered = new HashMap<>(); // saved Faces
  boolean start = true, flipX = false;
  float[][] embeedings;
  private static int SELECT_PICTURE = 1;
  ProcessCameraProvider cameraProvider;
  private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
  private Context context;
  CameraSelector cameraSelector;
  int cam_face = CameraSelector.LENS_FACING_BACK; // Default Back Camera
  TextView reco_name,textAbove_preview,preview_info;
  FaceDetector detector;
  PreviewView previewView;
  ImageButton add_face;
  ImageView face_preview;
  Button recognize,actions;
  Interpreter tfLite;
  String modelFile="mobile_face_net.tflite";
  private static final int MY_CAMERA_REQUEST_CODE = 100;
  float distance= 1.0f;
  int OUTPUT_SIZE=192; 

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "face_recognition");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("addFace")) {
      String name = call.argument("faceData");
      System.out.println("My Add Face Method cld");
      System.out.println("Args");
      System.out.println(name);
      addFace(name);
    }else if (call.method.equals("onCreate")){
      onCreate(null);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public void addFace(String name) {
    Recognition result = new Recognition("0", "", -1f);
    result.setExtra(embeedings);
    registered.put(name.toString(), result);
    start = true;
  }

  // @RequiresApi(api = Build.VERSION_CODES.M)
  protected void onCreate(Bundle savedInstanceState) {
      // super.onCreate(savedInstanceState);
      registered=readFromSP(); //Load saved faces from memory when app starts
      // setContentView(R.layout.activity_main);
      // face_preview =findViewById(R.id.imageView);
      // reco_name =findViewById(R.id.textView);
      // preview_info =findViewById(R.id.textView2);
      // textAbove_preview =findViewById(R.id.textAbovePreview);
      // add_face=findViewById(R.id.imageButton);
      add_face.setVisibility(View.INVISIBLE);

      private Context context;

      SharedPreferences sharedPref = getSharedPreferences("Distance",context.MODE_PRIVATE);
      distance = sharedPref.getFloat("distance",1.00f);

      face_preview.setVisibility(View.INVISIBLE);
      // recognize=findViewById(R.id.button3);
      // camera_switch=findViewById(R.id.button5);
      // actions=findViewById(R.id.button2);
      textAbove_preview.setText("Recognized Face:");
//        preview_info.setText("        Recognized Face:");
      //Camera Permission
      if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(new String[]{android.Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
      }
      //On-screen Action Button
      actions.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              AlertDialog.Builder builder = new AlertDialog.Builder(context);
              builder.setTitle("Select Action:");

              // add a checkbox list
              String[] names= {"View Recognition List","Update Recognition List","Save Recognitions","Load Recognitions","Clear All Recognitions","Import Photo (Beta)","Hyperparameters","Developer Mode"};

              builder.setItems(names, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                      switch (which)
                      {
                          case 0:
                              // displaynameListview();
                              break;
                          case 1:
                              // updatenameListview();
                              break;
                          case 2:
                              // insertToSP(registered,0); //mode: 0:save all, 1:clear all, 2:update all
                              break;
                          case 3:
                              // registered.putAll(readFromSP());
                              break;
                          case 4:
                              // clearnameList();
                              break;
                          case 5:
                              loadphoto();
                              break;
                          case 6:
                              // testHyperparameter();
                              break;
                          case 7:
                              // developerMode();
                              break;
                      }

                  }
              });


              builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                  }
              });
              builder.setNegativeButton("Cancel", null);

              // create and show the alert dialog
              AlertDialog dialog = builder.create();
              dialog.show();
          }
      });

      //On-screen switch to toggle between Cameras.
      camera_switch.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              if (cam_face==CameraSelector.LENS_FACING_BACK) {
                  cam_face = CameraSelector.LENS_FACING_FRONT;
                  flipX=true;
              }
              else {
                  cam_face = CameraSelector.LENS_FACING_BACK;
                  flipX=false;
              }
              cameraProvider.unbindAll();
              cameraBind();
          }
      });

      add_face.setOnClickListener((new View.OnClickListener() {
          @Override
          public void onClick(View v) {

              // addFace('hlw');
          }
      }));


      recognize.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              if(recognize.getText().toString().equals("Recognize"))
              {
               start=true;
               textAbove_preview.setText("Recognized Face:");
              recognize.setText("Add Face");
              add_face.setVisibility(View.INVISIBLE);
              reco_name.setVisibility(View.VISIBLE);
              face_preview.setVisibility(View.INVISIBLE);
              preview_info.setText("");
              //preview_info.setVisibility(View.INVISIBLE);
              }
              else
              {
                  textAbove_preview.setText("Face Preview: ");
                  recognize.setText("Recognize");
                  add_face.setVisibility(View.VISIBLE);
                  reco_name.setVisibility(View.INVISIBLE);
                  face_preview.setVisibility(View.VISIBLE);
                  preview_info.setText("1.Bring Face in view of Camera.\n\n2.Your Face preview will appear here.\n\n3.Click Add button to save face.");


              }

          }
      });

      //Load model
      try {
          tfLite=new Interpreter(loadModelFile(MainActivity.this,modelFile));
      } catch (IOException e) {
          e.printStackTrace();
      }
      //Initialize Face Detector
      FaceDetectorOptions highAccuracyOpts =
              new FaceDetectorOptions.Builder()
                      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                      .build();
      detector = FaceDetection.getClient(highAccuracyOpts);

      cameraBind();



  }

  private void cameraBind() {
    cameraProviderFuture = ProcessCameraProvider.getInstance(context);
    // previewView=findViewById(R.id.previewView);
    cameraProviderFuture.addListener(() -> {
      try {
        cameraProvider = cameraProviderFuture.get();

        bindPreview(cameraProvider);
      } catch (ExecutionException | InterruptedException e) {
        // No errors need to be handled for this in Future.
        // This should never be reached.
      }
    }, ContextCompat.getMainExecutor(context));
  }

  void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
    Preview preview = new Preview.Builder()
        .build();

    cameraSelector = new CameraSelector.Builder()
        .requireLensFacing(cam_face)
        .build();

    preview.setSurfaceProvider(previewView.getSurfaceProvider());
    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setTargetResolution(new Size(640, 480))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Latest frame is shown
        .build();

    Executor executor = Executors.newSingleThreadExecutor();
    imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
      @Override
      public void analyze(@NonNull ImageProxy imageProxy) {
        try {
          Thread.sleep(0); // Camera preview refreshed every 10 millisec(adjust as required)
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        InputImage image = null;

        @SuppressLint("UnsafeExperimentalUsageError")
        // Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML
        // kit face detection)

        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
          image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
          // System.out.println("Rotation
          // "+imageProxy.getImageInfo().getRotationDegrees());
        }

        // System.out.println("ANALYSIS");

        // Process acquired image to detect faces
        Task<List<Face>> result = detector.process(image)
            .addOnSuccessListener(
                new OnSuccessListener<List<Face>>() {
                  @Override
                  public void onSuccess(List<Face> faces) {

                    if (faces.size() != 0) {

                      Face face = faces.get(0); // Get first face from detected faces
                      // System.out.println(face);

                      // mediaImage to Bitmap
                      Bitmap frame_bmp = toBitmap(mediaImage);

                      int rot = imageProxy.getImageInfo().getRotationDegrees();

                      // Adjust orientation of Face
                      Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false, false);

                      // Get bounding box of face
                      RectF boundingBox = new RectF(face.getBoundingBox());

                      // Crop out bounding box from whole Bitmap(image)
                      Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                      if (flipX)
                        cropped_face = rotateBitmap(cropped_face, 0, flipX, false);
                      // Scale the acquired Face to 112*112 which is required input for model
                      Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

                      // if (start)
                      // recognizeImage(scaled); // Send scaled bitmap to create face embeddings.
                      // System.out.println(boundingBox);

                    } else {
                      if (registered.isEmpty())
                        reco_name.setText("Add Face");
                      else
                        reco_name.setText("No Face Detected!");
                    }

                  }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {
                    // Task failed with an exception
                    // ...
                  }
                })
            .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
              @Override
              public void onComplete(@NonNull Task<List<Face>> task) {

                imageProxy.close(); // v.important to acquire next frame for analysis
              }
            });

      }
    });

    cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);

  }

  public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleWidth = ((float) newWidth) / width;
    float scaleHeight = ((float) newHeight) / height;
    // CREATE A MATRIX FOR THE MANIPULATION
    Matrix matrix = new Matrix();
    // RESIZE THE BIT MAP
    matrix.postScale(scaleWidth, scaleHeight);

    // "RECREATE" THE NEW BITMAP
    Bitmap resizedBitmap = Bitmap.createBitmap(
        bm, 0, 0, width, height, matrix, false);
    bm.recycle();
    return resizedBitmap;
  }

  private static Bitmap rotateBitmap(
      Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
    Matrix matrix = new Matrix();

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees);

    // Mirror the image along the X or Y axis.
    matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != bitmap) {
      bitmap.recycle();
    }
    return rotatedBitmap;
  }

  private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
    Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
        (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
    Canvas cavas = new Canvas(resultBitmap);

    // draw background
    Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    paint.setColor(Color.WHITE);
    cavas.drawRect(
        new RectF(0, 0, cropRectF.width(), cropRectF.height()),
        paint);

    Matrix matrix = new Matrix();
    matrix.postTranslate(-cropRectF.left, -cropRectF.top);

    cavas.drawBitmap(source, matrix, paint);

    if (source != null && !source.isRecycled()) {
      source.recycle();
    }

    return resultBitmap;
  }

  private Bitmap toBitmap(Image image) {

    byte[] nv21 = YUV_420_888toNV21(image);

    YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

    byte[] imageBytes = out.toByteArray();
    // System.out.println("bytes"+ Arrays.toString(imageBytes));

    // System.out.println("FORMAT"+image.getFormat());

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
  }

  private static byte[] YUV_420_888toNV21(Image image) {

    int width = image.getWidth();
    int height = image.getHeight();
    int ySize = width * height;
    int uvSize = width * height / 4;

    byte[] nv21 = new byte[ySize + uvSize * 2];

    ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
    ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
    ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

    int rowStride = image.getPlanes()[0].getRowStride();
    assert (image.getPlanes()[0].getPixelStride() == 1);

    int pos = 0;

    if (rowStride == width) { // likely
      yBuffer.get(nv21, 0, ySize);
      pos += ySize;
    } else {
      long yBufferPos = -rowStride; // not an actual position
      for (; pos < ySize; pos += width) {
        yBufferPos += rowStride;
        yBuffer.position((int) yBufferPos);
        yBuffer.get(nv21, pos, width);
      }
    }

    rowStride = image.getPlanes()[2].getRowStride();
    int pixelStride = image.getPlanes()[2].getPixelStride();

    assert (rowStride == image.getPlanes()[1].getRowStride());
    assert (pixelStride == image.getPlanes()[1].getPixelStride());

    if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
      // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of
      // uBuffer[0]
      byte savePixel = vBuffer.get(1);
      try {
        vBuffer.put(1, (byte) ~savePixel);
        if (uBuffer.get(0) == (byte) ~savePixel) {
          vBuffer.put(1, savePixel);
          vBuffer.position(0);
          uBuffer.position(0);
          vBuffer.get(nv21, ySize, 1);
          uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

          return nv21; // shortcut
        }
      } catch (ReadOnlyBufferException ex) {
        // unfortunately, we cannot check if vBuffer and uBuffer overlap
      }

      // unfortunately, the check failed. We must save U and V pixel by pixel
      vBuffer.put(1, savePixel);
    }

    // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
    // but performance gain would be less significant

    for (int row = 0; row < height / 2; row++) {
      for (int col = 0; col < width / 2; col++) {
        int vuPos = col * pixelStride + row * rowStride;
        nv21[pos++] = vBuffer.get(vuPos);
        nv21[pos++] = uBuffer.get(vuPos);
      }
    }

    return nv21;
  }

  private void loadphoto()
    {
        start=false;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    private HashMap<String, Recognition> readFromSP(){
      SharedPreferences sharedPreferences = getSharedPreferences("HashMap", context.MODE_PRIVATE);
      String defValue = new Gson().toJson(new HashMap<String, Recognition>());
      String json=sharedPreferences.getString("map",defValue);
     // System.out.println("Output json"+json.toString());
      TypeToken<HashMap<String,Recognition>> token = new TypeToken<HashMap<String,Recognition>>() {};
      HashMap<String,Recognition> retrievedMap=new Gson().fromJson(json,token.getType());
     // System.out.println("Output map"+retrievedMap.toString());

      //During type conversion and save/load procedure,format changes(eg float converted to double).
      //So embeddings need to be extracted from it in required format(eg.double to float).
      for (Map.Entry<String, Recognition> entry : retrievedMap.entrySet())
      {
          float[][] output=new float[1][OUTPUT_SIZE];
          ArrayList arrayList= (ArrayList) entry.getValue().getExtra();
          arrayList = (ArrayList) arrayList.get(0);
          for (int counter = 0; counter < arrayList.size(); counter++) {
              output[0][counter]= ((Double) arrayList.get(counter)).floatValue();
          }
          entry.getValue().setExtra(output);

          //System.out.println("Entry output "+entry.getKey()+" "+entry.getValue().getExtra() );

      }
//        System.out.println("OUTPUT"+ Arrays.deepToString(outut));
      Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT).show();
      return retrievedMap;
  }


}

class Recognition {
  /**
   * A unique identifier for what has been recognized. Specific to the class, not
   * the instance of
   * the object.
   */
  private final String id;
  /** Display name for the recognition. */
  private final String title;

  private final Float distance;
  private Object extra;

  public Recognition(
      final String id, final String title, final Float distance) {
    this.id = id;
    this.title = title;
    this.distance = distance;
    this.extra = null;

  }

  public void setExtra(Object extra) {
    this.extra = extra;
  }

  public Object getExtra() {
    return this.extra;
  }

  @Override
  public String toString() {
    String resultString = "";
    if (id != null) {
      resultString += "[" + id + "] ";
    }

    if (title != null) {
      resultString += title + " ";
    }

    if (distance != null) {
      resultString += String.format("(%.1f%%) ", distance * 100.0f);
    }

    return resultString.trim();
  }

}