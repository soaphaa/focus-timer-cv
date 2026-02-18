package org.example.focustimercv;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class MediaPlayerController {
        private Media media;
        private MediaPlayer mediaPlayer;
        private MediaView mediaView;

        public MediaPlayerController(MediaView mediaView) {
            this.mediaView = mediaView;
            if (this.mediaView == null) {
                System.out.println("Warning: MediaView is null - controller disabled");
            }
        }

        public void loadAndPlay(String videoPath) {
            if (mediaView == null) {
                System.out.println("Cannot load media - MediaView is null");
                return;
            }
            try {
                media = new Media(videoPath);
                mediaPlayer = new MediaPlayer(media);

                mediaPlayer.setOnError(() -> {
                    System.out.println("MediaPlayer Error: " + mediaPlayer.getError());
                    mediaPlayer.getError().printStackTrace();
                });

                mediaPlayer.setOnReady(() -> {
                    System.out.println("Media ready to play");
                });

                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Auto repeat
                mediaPlayer.play();

            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void play() {
            if (mediaPlayer != null) {
                mediaPlayer.play();
            }
        }

        public void pause() {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
        }

        public void dispose() {
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
            }
        }
    }
