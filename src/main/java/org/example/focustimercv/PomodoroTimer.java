package org.example.focustimercv;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.util.Duration;

public class PomodoroTimer {

    // Settings (in minutes)
    private int workMinutes = 25; //default settings
    private int breakMinutes = 5; //default settings
    public static String modeStatus;

    //GUI binds
    private final IntegerProperty timeRemaining = new SimpleIntegerProperty(); //updates the time remaining on the GUI timer.
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private final BooleanProperty isWorkSession = new SimpleBooleanProperty(true); //for when focus detector will be active
    private final IntegerProperty completedPomodoros = new SimpleIntegerProperty(0);

    // Timer class
    private Timeline timeline;
    private boolean autoPausedByFocus = false; //for eyeDetector

    public PomodoroTimer() {
        timeRemaining.set(workMinutes * 60); //Initial default value
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    //updating timer by each tick counting down
    private void tick() {
        if (timeRemaining.get() > 0) {
            timeRemaining.set(timeRemaining.get() - 1);
        } else {
            // Timer finished
            complete();
        }
    }

    public void start() {
        if (!isRunning.get()) {
            isRunning.set(true);
            timeline.play();
        }
    }

    public void pause() {
        if (isRunning.get()) {
            isRunning.set(false);
            timeline.pause();
        }
    }

    //a session whether work or break is completed. (timeline reaches 0)
    private void complete() {
        timeline.stop();
        isRunning.set(false);

        if (isWorkSession.get()) {
            completedPomodoros.set(completedPomodoros.get() + 1);
            System.out.println("Work session complete! 🍅");
            isWorkSession.set(false);
            timeRemaining.set(breakMinutes * 60);
        } else {
            System.out.println("Break complete! Time to work!");
            isWorkSession.set(true);
            timeRemaining.set(workMinutes * 60);
        }
    }


    // ----------- This method will be called when the eyes are NOT detected (focus is lost) and will work by autopausing during the worksession)
    public void onFocusLost() {
        if (isWorkSession.get() && isRunning.get()) {
            autoPausedByFocus = true;
            pause();
            System.out.println("⏸️ Timer auto-paused - eyes not detected");
        }
    }

    //when eyes ARE detected after they werent for 3+ seconds. Theis method will now resume the timer.
    public void onFocusGained() {
        if (autoPausedByFocus && isWorkSession.get()) {
            autoPausedByFocus = false;
            start();
            System.out.println("▶️ Timer auto-resumed - eyes detected");
        }
    }

    // ----- GETTERS --------

    //format the time correctly for GUI
    public String getFormattedTime() {
        int minutes = timeRemaining.get() / 60;
        int seconds = timeRemaining.get() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isWorkSession() {
        return isWorkSession.get();
    }

    public int getCompletedPomodoros() {
        return completedPomodoros.get();
    }

    // Properties for JavaFX binding
    public IntegerProperty timeRemainingProperty() {
        return timeRemaining;
    }

    public BooleanProperty isRunningProperty() {
        return isRunning;
    }

    public BooleanProperty isWorkSessionProperty() {
        return isWorkSession;
    }

    // ============= SETTINGS ============== (user input?)
    // Settings
    public void setWorkMinutes(int minutes) {
        this.workMinutes = minutes;
        if (isWorkSession.get()) {
            timeRemaining.set(minutes * 60);
        }
    }

    public void setBreakMinutes(int minutes) {
        this.breakMinutes = minutes;
        if (!isWorkSession.get() && !isRunning.get()) {
            timeRemaining.set(minutes * 60);
        }
    }

    public int getWorkMinutes() {
        return workMinutes;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void displayStatus(){
        if(isWorkSession() == true){
            modeStatus = "Focus In Session";
        }
        else if(isWorkSession() == false){
            modeStatus = "Break in Session";
        }
    }
}
