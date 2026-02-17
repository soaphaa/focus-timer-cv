package org.example.focustimercv;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
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
    private MediaPlayerController mediaPlayerController;
    String videoPath = getClass().getResource("/org/example/focustimercv/videos/skeleton_shield_meme.mp4").toExternalForm();

    @FXML
    private Button playBtn;

    @FXML
    private Button pauseBtn;
    //-------- --------------------------- --------------
    private VideoCapture camera;
    private volatile boolean running = false;
    private volatile boolean faceDetectionEnabled = false;  // For the toggle button

    // Timer variables for eye detection
    private long lastEyeDetectedTime = 0; //
    private static final long EYE_DETECTION_THRESHOLD = 2000; // 2 seconds (in milliseconds)
    private boolean eyeWarningActive = false;
    boolean eyesDetectedThisFrame = false;

    //Variables for pomodoro (alot)
    private PomodoroTimer pomodoroTimer; //class object
    private boolean userIsFocused = true;

    // Add FXML components
    @FXML private Label timerLabel;
    @FXML private Label pomodoroCountLabel;
    @FXML private Label sessionTypeLabel;
    @FXML private Button startPomodoroBtn;
    @FXML private Button pausePomodoroBtn;
    @FXML private Button resetPomodoroBtn;
    @FXML private Button skipPomodoroBtn;

    //initialize main stage features
    @FXML
    public void initialize(){
        //to load the tomato image in to the scene 1
        if (tomato != null) { //when loading into scene 2, tomato isnt there so this will only run on scene 1 when tomato isnt null
            loadImage();
        }

        if(mediaView !=null) {
            mediaPlayerController = new MediaPlayerController(mediaView);
            mediaPlayerController.loadAndPlay(videoPath);
        }

        if (webcamView != null) {
            webcamView.setVisible(false);         //hide webcam initially
            initializeCamera(); //set camera
            initializePomodoroTimer(); //set timer to be visible
        }
    }

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
        mediaPlayerController.pause();
    }

    @FXML
    public void onPlayBtn (ActionEvent event){
        mediaPlayerController.play();
    }

    @FXML
    public void onPauseBtn (ActionEvent event){
        mediaPlayerController.pause();
    }

    private void loadImage() {
        tomatoImg = new Image(getClass().getResourceAsStream("/org/example/focustimercv/images/tomato.png"));
        tomato.setImage(tomatoImg);
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

    private void initializePomodoroTimer() {
        pomodoroTimer = new PomodoroTimer();

        //add the timer first
        if (timerLabel != null) {
            pomodoroTimer.timeRemainingProperty().addListener((obs, old, newVal) -> {
                timerLabel.setText(pomodoroTimer.getFormattedTime());
            });
            timerLabel.setText(pomodoroTimer.getFormattedTime());
        }

//        if (pomodoroCountLabel != null) {
//            pomodoroTimer.completedPomodorosProperty().addListener((obs, old, newVal) -> {
//                pomodoroCountLabel.setText("Pomodoros: " + newVal);
//            });
//            pomodoroCountLabel.setText("Pomodoros: 0");
//        }
        //set the initial session type (to work session)
        if (sessionTypeLabel != null) {
            pomodoroTimer.isWorkSessionProperty().addListener((obs, old, isWork) -> {
                sessionTypeLabel.setText(isWork ? "Focus" : "Break");
            });
            sessionTypeLabel.setText("Focus");
        }

    }

    @FXML
    public void onStartPomodoro(ActionEvent event) {
        pomodoroTimer.start();
        webcamView.setVisible(true);
    }

    @FXML
    public void onPausePomodoro(ActionEvent event) {
        pomodoroTimer.pause();
        webcamView.setVisible(false);
    }

    @FXML
    public void initializeCamera(){
        System.out.println("Starting camera");
        lastEyeDetectedTime = System.currentTimeMillis();

        if (running) {
            System.out.println("Camera already running");
            return;
        }

        // ------------- CASCADE DETECTORS INITIALIZING -------------
        cascadeFaceDetector = new CascadeClassifier();
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
                boolean success = camera.read(frame);

                if (!success || frame.empty()) {
                    // Skip this frame silently instead of crashing
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    continue;  // Try next frame
                }

                // Frame is guaranteed good here - no need for second read or empty check!
                Core.flip(frame, frame, 1);
                if (faceDetectionEnabled) {
                    detectAndDisplay(frame);
                }
                enhanceFrame(frame);
                Image image = matToImage(frame);
                Platform.runLater(() -> webcamView.setImage(image));

                try {
                    Thread.sleep(50);
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
        eyesDetectedThisFrame = false;
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
                    faceROI,
                    eyePairs,
                    1.05,        // Thorough search
                    1,           // 1 detection only
                    0,
                    new Size(20, 10),   // ← Changed from (35,20) - smaller minimum size
                    new Size()
            );

//            //Temporary detector indicators
//            if (eyesDetectedThisFrame) {
//                System.out.println("Eyes detected!");
//            }
//            else{
//                System.out.println("Eyes NOT detected. Lock in");
//            }

            Rect[] eyesArray = eyePairs.toArray();

            // ========== CHECK IF EYES WERE DETECTED ==========
            if (eyesArray.length > 0) {
                eyesDetectedThisFrame = true; //eyes are found in the frame
            }
            for (Rect eye : eyesArray) {
                // Draw eye rectangle
                Imgproc.rectangle(
                        frame,
                        new Point(rect.x + eye.x, rect.y + eye.y),
                        new Point(rect.x + eye.x + eye.width, rect.y + eye.y + eye.height),
                        new Scalar(0, 255, 255),  //colour for eyes rectangles
                        1
                );

                // "EYES " label
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
        // ========== Check if eyes haven't been detected for a few seconds WITH A TIMER==========

        if (eyesDetectedThisFrame) {
            // Eyes are visible
            lastEyeDetectedTime = System.currentTimeMillis();
            eyeWarningActive = false;  // Reset timer
            System.out.println("Eyes detected!");

            //pomodoro timer is on when focused --> tells pomodoro that user is focused
            if (!userIsFocused && pomodoroTimer != null) {
                pomodoroTimer.onFocusGained();
                userIsFocused = true;
            }

        } else {
            // No eyes detected in this frame
            long timeSinceLastEyes = System.currentTimeMillis() - lastEyeDetectedTime;
            System.out.println("Eyes NOT detected");

            if (timeSinceLastEyes > EYE_DETECTION_THRESHOLD) {
                // Eyes have been missing for more than 3 seconds --> only triggered once this is active
                System.out.println("for" + timeSinceLastEyes/1000 + "seconds");

                if (userIsFocused && pomodoroTimer != null) {
                    pomodoroTimer.onFocusLost();
                    userIsFocused = false;
                }

                if (!eyeWarningActive) {
                    eyeWarningActive = true;
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("video-window.fxml"));
                            Parent root = loader.load();

                            StackPane wrapper = new StackPane(root);
                            wrapper.setAlignment(Pos.BOTTOM_RIGHT);
                            wrapper.setPadding(new Insets(20, 20, 20, 20));

                            Stage videoStage = new Stage();
                            videoStage.setTitle("Skeleton clashing meme");
                            videoStage.setScene(new Scene(wrapper)); //set scene with the adjustments of position made
                            videoStage.initModality(Modality.APPLICATION_MODAL); // blocks parent window
                            videoStage.setAlwaysOnTop(true); // stays above ALL other windows on your OS
                            videoStage.show();

                            // The video will auto-play

                        } catch (IOException e) {
                            System.out.println("Error opening video window: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }

                // Display warning on screen
                Imgproc.putText(
                        frame,
                        "EYES NOT DETECTED!",
                        new Point(10, 50),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        1.0,
                        new Scalar(0, 0, 255),  // Red
                        3
                );

                // Optional: Show countdown
                long secondsMissing = timeSinceLastEyes / 1000;
                Imgproc.putText(
                        frame,
                        "Time: " + secondsMissing + "s",
                        new Point(10, 90),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.8,
                        new Scalar(0, 0, 255),
                        2
                );
            }
        }

        grayFrame.release();
    }


    //increase saturation & flips if inverted by default
    private void enhanceFrame(Mat frame) {
        Mat hsv = new Mat();
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

    // ---------- GUI METHODS ------------
    @FXML
    private void onOvalButtonHover(MouseEvent event) {
        Node button = (Node) event.getSource();
        button.setScaleX(0.95); //button will shrink a bit when hovered over to indicate button active.
        button.setScaleY(0.95);
    }

    @FXML
    private void onOvalButtonExit(MouseEvent event) {
        Node button = (Node) event.getSource();
        button.setScaleX(1.0);
        button.setScaleY(1.0);
    }
}



