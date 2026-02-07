package org.example.focustimercv;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;


import java.io.IOException;


public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;


    private CascadeClassifier cascadeFaceDetector; //classifier object from the opencv folder
    private MatOfRect matOfRect;
    String haarPath = "src/main/resources/org/example/focustimercv/haarcascade_frontalface_alt.xml"; //path to xml file
    String lbpPath = "src/main/resources/org/example/focustimercv/lbpcascade_frontalface.xml";


    @FXML
    private ImageView webcamView;  // This should match the fx:id in your hello-view.fxml


    private VideoCapture camera;
    private volatile boolean running = false;


    @FXML
    public void onButtonClick(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("video-window.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    @FXML
    public void startCamera(ActionEvent event){
        System.out.println("Start camera button clicked!");


        if (running) {
            System.out.println("Camera already running");
            return;
        }


        cascadeFaceDetector = new CascadeClassifier();
        // For HAAR (more accurate, slower)
        cascadeFaceDetector.load(haarPath);

        // For LBP (faster, less accurate)
        //faceDetector.load(lbpPath);

        if (cascadeFaceDetector.empty()) { //error checking of face detector loading properly
            System.out.println("Error: Could not load face cascade!");
        } else {
            System.out.println("Face detector loaded successfully!");
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
                    detectAndDisplay(frame);
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
        Mat grayFrame = new Mat();

        //improving detection accuracy according to how cascade works
        // Convert to grayscale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // Detect faces!!!
        // Parameters: image, objects, scaleFactor, minNeighbors, flags, minSize, maxSize
        cascadeFaceDetector.detectMultiScale(
                grayFrame,           // Input image
                faces,               // Output rectangles
                1.1,                 // Scale factor (1.1 = reduce by 10% each time)
                3,                   // Min neighbors (higher = fewer false positives)
                0,
                new Size(30, 30),    // Min face size
                new Size()           // Max face size (empty = no limit)
        );

        // Draw rectangles around detected faces
        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            Imgproc.rectangle(
                    frame,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),  // Green color (BGR format)
                    3                        // Thickness
            );
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


    // Convert OpenCV Mat to JavaFX Image
    private Image matToImage(Mat mat) {
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



