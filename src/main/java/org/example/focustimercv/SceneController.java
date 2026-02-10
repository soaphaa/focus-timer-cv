package org.example.focustimercv;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javafx.scene.shape.Rectangle;


import java.awt.*;
import java.io.File;
import java.io.IOException;


public class SceneController {
    private Stage stage;
    private Scene scene;
    @FXML
    private Parent root;


    private CascadeClassifier cascadeFaceDetector; //classifier object from the opencv folder
    private CascadeClassifier haarEye;
    private CascadeClassifier haarEyeGlasses;
    private CascadeClassifier haarEyePair;
    private MatOfRect matOfRect;
    String haarPath = "src/main/resources/org/example/focustimercv/haarcascade_frontalface_alt.xml"; //path to xml file
    String lbpPath = "src/main/resources/org/example/focustimercv/lbpcascade_frontalface.xml";

    String haarEyePath = "src/main/resources/org/example/focustimercv/haarcascade_eye.xml";
    String haarEyeglassesPath = "src/main/resources/org/example/focustimercv/haarcascade_eye_tree_eyeglasses.xml";
    String haarEyePairPath = "src/main/resources/org/example/focustimercv/haarcascade_mcs_eyepair_big.xml";

    @FXML
    private ImageView webcamView;  // This should match the fx:id in your hello-view.fxml

    private Image tomatoImg;
    @FXML
    private ImageView tomato;


    @FXML
    private RadioButton toggleFaceDetect;

    //-------- Media Video scene variables --------------
    @FXML
    private MediaView mediaView;
    private Media media; //media class
    private MediaPlayer mediaPlayer;

    @FXML
    private Button playBtn;

    @FXML
    private Button pauseBtn;
    //-------- --------------------------- --------------

    private VideoCapture camera;
    private volatile boolean running = false;
    private volatile boolean faceDetectionEnabled = false;  // For the toggle button


    @FXML
    public void onButtonClick(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("video-window.fxml"));

        // Create a new stage for the new window
        Stage newStage = new Stage();
        newStage.setTitle("Skeleton Shield Meme");

        // Set the scene on a new stage
        scene = new Scene(root);
        newStage.setScene(scene);
        newStage.show();
    }

    @FXML
    public void onReturnButtonClick(ActionEvent event) throws IOException {
        Stage currentStage = (Stage)((Node)event.getSource()).getScene().getWindow();
        // Close this window
        currentStage.close();
    }

    @FXML
    public void onPlayBtn (ActionEvent event){
    }

    @FXML
    public void onPauseBtn (ActionEvent event){
    }

    private void loadImage() {
        tomatoImg = new Image(getClass().getResourceAsStream("/org/example/focustimercv/images/tomato.png"));
        tomato.setImage(tomatoImg);
    }

    //set media on the scene
    @FXML
    public void initialize(){
        loadImage(); //load any images on scene 1
        try {
            String videoPath = getClass().getResource("/org/example/focustimercv/videos/skeleton_shield_meme.mp4").toExternalForm();
            System.out.println("Video path: " + videoPath); //ensure the url exists
            media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);

            // Listen for errors
            mediaPlayer.setOnError(() -> {
                System.out.println("MediaPlayer Error: " + mediaPlayer.getError());
                mediaPlayer.getError().printStackTrace();
            });

            // Listen for ready state
            mediaPlayer.setOnReady(() -> {
            });

            mediaView.setMediaPlayer(mediaPlayer);

            mediaPlayer.play();

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void toggleFaceDetection(ActionEvent event){
        faceDetectionEnabled = toggleFaceDetect.isSelected();

        if (faceDetectionEnabled) {
            System.out.println("Face detection ENABLED");
        } else {
            System.out.println("Face detection DISABLED");
        }
    }

    public void toggleGreyscale(ActionEvent event){
        // Implement similarly for greyscale
    }


    @FXML
    public void startCamera(ActionEvent event){
        System.out.println("Start camera button clicked!");

        if (running) {
            System.out.println("Camera already running");
            return;
        }

        // ------------- CASCADE DETECTORS INITIALIZING -------------
        cascadeFaceDetector = new CascadeClassifier();
        haarEye = new CascadeClassifier(haarEyePath);
        haarEyeGlasses = new CascadeClassifier(haarEyeglassesPath);
        haarEyePair = new CascadeClassifier(haarEyePairPath);
        // For HAAR (more accurate, slower)
        cascadeFaceDetector.load(haarPath);
        // For LBP (faster, less accurate)
        //faceDetector.load(lbpPath);

        //Loading and error checking each detector
        if (cascadeFaceDetector.empty()) {
            System.out.println("Error: Could not load face cascade!");
        } else {
            System.out.println("Face detector loaded successfully!");
        }
        if (haarEye.empty()) {
            System.out.println("Error: Eye detector not loaded!");
        } else {
            System.out.println("Eye detector loaded!");
        }

        if (haarEyeGlasses.empty()) {
            System.out.println("Error: Eyeglasses detector not loaded!");
        } else {
            System.out.println("Eyeglasses detector loaded!");
        }

        if (haarEyePair.empty()) {
            System.out.println("Error: Eye pair detector not loaded!");
        } else {
            System.out.println("Eye pair detector loaded!");
        }

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Error: Camera not found!");
            return;
        }

        System.out.println("Camera opened!");
        running = true;

        Thread captureThread = new Thread(() -> {
            Mat frame = new Mat();

            while (running && camera.isOpened()) {
                camera.read(frame);
                if (!frame.empty()) {
                    // Only detect faces if toggle is enabled
                    if(faceDetectionEnabled){
                        detectAndDisplay(frame);
                    }
                    enhanceFrame(frame); //enhance colors if you have a poor quality webcam like me
                    Image image = matToImage(frame);
                    // Update UI
                    Platform.runLater(() -> webcamView.setImage(image));
                }

                try {
                    Thread.sleep(30); // ~33 FPS
                } catch (InterruptedException e) {
                    break;
                }
            }

            frame.release();
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopCamera() {
        System.out.println("Stopping camera...");
        running = false;
        if (camera != null && camera.isOpened()) {
            camera.release();
            System.out.println("Camera closed.");
        }
    }

    private void detectAndDisplay(Mat frame) {
        MatOfRect faces = new MatOfRect();
        MatOfRect eyePairs = new MatOfRect();
        Mat grayFrame = new Mat();

        //-----------Detection improvements for OpenCV---------
            // Converting to grayscale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // Detect faces!!!
        //how this works:
        cascadeFaceDetector.detectMultiScale(
                grayFrame,           // Input image
                faces,               // Output rectangles
                1.1,                 // Scale factor (1.1 = reduce by 10% each time)
                3,                   // Min neighbors (higher = fewer false positives)
                0,
                new Size(50, 50),    // Min face size
                new Size()           // Max face size (no limit)
        );

        // Draw rectangles around detected faces
        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            Imgproc.rectangle(
                    frame,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(41 ,42, 191),  // Red color (BGR format)
                    2                        // Thickness
            );

            // ---------- DETECTING EYES WITHIN THE FACE ----------
            // Add "FACE" label above rectangle
            Imgproc.putText(
                    frame,
                    "FACE",                              // Text to display
                    new Point(rect.x, rect.y - 10),      // Position (10 pixels above rectangle)
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    new Scalar(41, 42, 191),
                    2                                     // Thickness
            );

            // faceROI means --> Extract stuff from just the face region from the grayscale frame
            Mat faceROI = grayFrame.submat(rect);

            // Detect eye pair
            haarEyePair.detectMultiScale(
                    faceROI,             // Search only in the face region
                    eyePairs,
                    1.1,                 // Scale factor
                    1,
                    0,
                    new Size(35, 20),    // Min eye size ( to avoid nostrils being detected as eyes...
                    new Size() //no max eye size
            );

            //Are the eyes in frame/detected? (To determine focused or not)
            boolean eyesDetected = eyePairs.toArray().length > 0;
            if (eyesDetected) {
                System.out.println("Eyes detected!");
            }

            Rect[] eyesArray = eyePairs.toArray();
            for (Rect eye : eyesArray) {
                // Draw eye rectangle
                Imgproc.rectangle(
                        frame,
                        new Point(rect.x + eye.x, rect.y + eye.y),
                        new Point(rect.x + eye.x + eye.width, rect.y + eye.y + eye.height),
                        new Scalar(0, 255, 255),  //colour for eyes rectangles
                        2
                );

                // "EYE " label
                Imgproc.putText(
                        frame,
                        "eyes",
                        new Point(rect.x + eye.x, rect.y + eye.y - 10),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.5,
                        new Scalar(0, 255, 255),
                        1
                );
            }


        }
        

        grayFrame.release();
    }

    //increase saturation & flips if inverted by default
    private void enhanceFrame(Mat frame) {
        Mat hsv = new Mat();
        Core.flip(frame, frame, 1); // Flips horizontally
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);

        // Split into H, S, V channels
        Core.add(hsv, new org.opencv.core.Scalar(0, 15, 30), hsv); // Increase saturation and brightness

        // Convert back to BGR
        Imgproc.cvtColor(hsv, frame, Imgproc.COLOR_HSV2BGR);
        hsv.release();
    }


    // Convert OpenCV Mat to JavaFX Image to display on GUI
    private Image matToImage(Mat mat) { //mat is
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        if (channels == 1) {
            // Grayscale
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int gray = sourcePixels[y * width + x] & 0xFF;
                    int rgb = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                    pixelWriter.setArgb(x, y, rgb);
                }
            }
        } else {
            // BGR (OpenCV format) to RGB (JavaFX format)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 3;
                    int b = sourcePixels[index] & 0xFF;
                    int g = sourcePixels[index + 1] & 0xFF;
                    int r = sourcePixels[index + 2] & 0xFF;
                    int rgb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    pixelWriter.setArgb(x, y, rgb);
                }
            }
        }

        return image;
    }
}



