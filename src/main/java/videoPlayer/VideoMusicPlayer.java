package videoPlayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * VideoMusicPlayer - A comprehensive media player with playlist support
 * Features: Play/Pause/Stop, Volume control, Seek bar, Playlist management, Video support
 */
public class VideoMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ListView<String> playlistView;
    private ObservableList<String> playlist;
    private List<File> mediaFiles;
    private int currentIndex = -1;

    // UI Controls
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button nextButton;
    private Button prevButton;
    private Slider volumeSlider;
    private Slider seekSlider;
    private Label statusLabel;
    private Label timeLabel;
    private boolean isSeeking = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ArkVideo Music Player");

        // Initialize collections
        playlist = FXCollections.observableArrayList();
        mediaFiles = new ArrayList<>();

        // Create main layout with pure black background
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #000000;");

        // Center: Media View with subtle neon glow
        mediaView = new MediaView();
        mediaView.setFitWidth(640);
        mediaView.setFitHeight(480);
        mediaView.setPreserveRatio(true);
        StackPane mediaContainer = new StackPane(mediaView);
        mediaContainer.setStyle(
                "-fx-background-color: #0a0a0a; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, #00ff41, 8, 0.2, 0, 0);"
        );
        mediaContainer.setMinHeight(480);
        mediaContainer.setPadding(new Insets(15));
        root.setCenter(mediaContainer);

        // Right: Playlist
        VBox playlistBox = createPlaylistPanel();
        root.setRight(playlistBox);

        // Bottom: Controls
        VBox controlsBox = createControlsPanel();
        root.setBottom(controlsBox);

        // Setup scene with CSS
        Scene scene = new Scene(root, 1000, 650);
        // Load external CSS file
        String cssFile = getClass().getResource("/neon-style.css").toExternalForm();
        scene.getStylesheets().add(cssFile);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Cleanup on close
        primaryStage.setOnCloseRequest(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
            }
        });
    }

    private VBox createPlaylistPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setPrefWidth(280);
        box.setStyle(
                "-fx-background-color: #0a0a0a; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #00ff41; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10;"
        );

        Label label = new Label("🎵 PLAYLIST");
        label.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #00ff41; " +
                        "-fx-padding: 5;"
        );

        playlistView = new ListView<>(playlist);
        playlistView.setPrefHeight(400);
        playlistView.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #333333;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
        );
        playlistView.getStyleClass().add("playlist-view");

        // Double-click to play
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selected = playlistView.getSelectionModel().getSelectedIndex();
                if (selected >= 0) {
                    playMedia(selected);
                }
            }
        });

        // Playlist buttons with neon effect
        HBox playlistButtons = new HBox(8);
        playlistButtons.setAlignment(Pos.CENTER);
        Button addButton = createNeonButton("+ Add", "#00ff41");
        Button removeButton = createNeonButton("- Remove", "#ffffff");
        Button clearButton = createNeonButton("Clear", "#ff0055");

        addButton.setOnAction(e -> addFilesToPlaylist());
        removeButton.setOnAction(e -> removeSelectedFromPlaylist());
        clearButton.setOnAction(e -> clearPlaylist());

        playlistButtons.getChildren().addAll(addButton, removeButton, clearButton);

        box.getChildren().addAll(label, playlistView, playlistButtons);
        return box;
    }

    private VBox createControlsPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20, 15, 20, 15));
        box.setStyle(
                "-fx-background-color: #0a0a0a; " +
                        "-fx-background-radius: 15 15 0 0; " +
                        "-fx-border-color: #00ff41; " +
                        "-fx-border-width: 2 0 0 0;"
        );

        // Status label with subtle neon
        statusLabel = new Label("Ready to play");
        statusLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 12px; " +
                        "-fx-text-fill: #00ff41; " +
                        "-fx-padding: 6; " +
                        "-fx-background-color: #000000; " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #00ff41; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6;"
        );

        // Seek slider with neon track
        HBox seekBox = new HBox(15);
        seekBox.setAlignment(Pos.CENTER);
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle(
                "-fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold;"
        );
        seekSlider = new Slider();
        seekSlider.setMin(0);
        seekSlider.setMax(100);
        seekSlider.setValue(0);
        seekSlider.setPrefWidth(500);
        seekSlider.getStyleClass().add("neon-slider");

        seekSlider.setOnMousePressed(e -> isSeeking = true);
        seekSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue()));
            }
            isSeeking = false;
        });

        seekBox.getChildren().addAll(timeLabel, seekSlider);

        // Playback controls with reasonable sizes
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);

        prevButton = createNeonControlButton("◀◀", "#ffffff");
        playButton = createNeonControlButton("▶", "#00ff41");
        pauseButton = createNeonControlButton("||", "#ffffff");
        stopButton = createNeonControlButton("■", "#ff0055");
        nextButton = createNeonControlButton("▶▶", "#ffffff");

        // Make play button slightly larger with subtle glow
        playButton.setStyle(
                playButton.getStyle() +
                        "-fx-font-size: 22px; " +
                        "-fx-min-width: 55px; " +
                        "-fx-min-height: 50px; " +
                        "-fx-effect: dropshadow(gaussian, #00ff41, 10, 0.5, 0, 0);"
        );

        prevButton.setOnAction(e -> playPrevious());
        playButton.setOnAction(e -> play());
        pauseButton.setOnAction(e -> pause());
        stopButton.setOnAction(e -> stopPlayback());
        nextButton.setOnAction(e -> playNext());

        controlButtons.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton);

        // Volume control with neon
        HBox volumeBox = new HBox(15);
        volumeBox.setAlignment(Pos.CENTER);
        Label volumeLabel = new Label("🔊 VOLUME");
        volumeLabel.setStyle(
                "-fx-text-fill: #ffffff; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 11px;"
        );
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(200);
        volumeSlider.getStyleClass().add("neon-slider");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                setMediaVolume(newVal.doubleValue() / 100.0));

        Label volumeValue = new Label("50%");
        volumeValue.setStyle(
                "-fx-text-fill: #00ff41; " +
                        "-fx-font-weight: bold; " +
                        "-fx-min-width: 40px;"
        );
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                volumeValue.setText(String.format("%d%%", newVal.intValue())));

        volumeBox.getChildren().addAll(volumeLabel, volumeSlider, volumeValue);

        box.getChildren().addAll(statusLabel, seekBox, controlButtons, volumeBox);
        return box;
    }

    private void addFilesToPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp3", "*.mp4", "*.wav", "*.m4a", "*.flv", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.flv", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null) {
            for (File file : files) {
                mediaFiles.add(file);
                playlist.add(file.getName());
            }
            statusLabel.setText("Added " + files.size() + " file(s) to playlist");
        }
    }

    private void removeSelectedFromPlaylist() {
        int selected = playlistView.getSelectionModel().getSelectedIndex();
        if (selected >= 0) {
            playlist.remove(selected);
            mediaFiles.remove(selected);
            if (selected == currentIndex) {
                stopPlayback();
                currentIndex = -1;
            } else if (selected < currentIndex) {
                currentIndex--;
            }
        }
    }

    private void clearPlaylist() {
        stopPlayback();
        playlist.clear();
        mediaFiles.clear();
        currentIndex = -1;
        statusLabel.setText("Playlist cleared");
    }

    private void playMedia(int index) {
        if (index < 0 || index >= mediaFiles.size()) {
            return;
        }

        // Dispose of previous player
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        currentIndex = index;
        File file = mediaFiles.get(index);

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // Set volume
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            // Update status
            statusLabel.setText("Playing: " + file.getName());
            playlistView.getSelectionModel().select(index);

            // Setup time update
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!isSeeking) {
                    Platform.runLater(() -> {
                        Duration current = mediaPlayer.getCurrentTime();
                        Duration total = mediaPlayer.getTotalDuration();

                        if (total.greaterThan(Duration.ZERO)) {
                            seekSlider.setMax(total.toSeconds());
                            seekSlider.setValue(current.toSeconds());

                            timeLabel.setText(formatTime(current) + " / " + formatTime(total));
                        }
                    });
                }
            });

            // Auto-play next on end
            mediaPlayer.setOnEndOfMedia(this::playNext);

            // Handle errors
            mediaPlayer.setOnError(() -> {
                statusLabel.setText("Error: " + mediaPlayer.getError().getMessage());
            });

            mediaPlayer.play();

        } catch (Exception e) {
            statusLabel.setText("Error loading: " + file.getName() + " - " + e.getMessage());
            System.err.println("Failed to load media file: " + file.getAbsolutePath());
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            statusLabel.setText("Playing: " + mediaFiles.get(currentIndex).getName());
        } else if (!mediaFiles.isEmpty()) {
            playMedia(0);
        } else {
            statusLabel.setText("No media in playlist");
        }
    }

    private void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            statusLabel.setText("Paused");
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            statusLabel.setText("Stopped");
            seekSlider.setValue(0);
            timeLabel.setText("00:00 / 00:00");
        }
    }

    private void playNext() {
        if (currentIndex < mediaFiles.size() - 1) {
            playMedia(currentIndex + 1);
        } else if (!mediaFiles.isEmpty()) {
            playMedia(0); // Loop to beginning
        }
    }

    private void playPrevious() {
        if (currentIndex > 0) {
            playMedia(currentIndex - 1);
        } else if (!mediaFiles.isEmpty()) {
            playMedia(mediaFiles.size() - 1); // Loop to end
        }
    }

    private void setMediaVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    private Button createNeonButton(String text, String neonColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-text-fill: " + neonColor + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 10px;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + neonColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        );

        String hoverStyle = button.getStyle() +
                "-fx-background-color: " + neonColor + ";" +
                "-fx-text-fill: #000000;";

        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-text-fill: " + neonColor + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 10px;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + neonColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;"
        ));

        return button;
    }

    private Button createNeonControlButton(String symbol, String neonColor) {
        Button button = new Button(symbol);
        button.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-text-fill: " + neonColor + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 45px;" +
                        "-fx-min-height: 42px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-border-color: " + neonColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 50%;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, " + neonColor + ", 5, 0.3, 0, 0);"
        );

        String hoverStyle = button.getStyle() +
                "-fx-background-color: " + neonColor + ";" +
                "-fx-text-fill: #000000;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;" +
                "-fx-effect: dropshadow(gaussian, " + neonColor + ", 8, 0.5, 0, 0);";

        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #000000;" +
                        "-fx-text-fill: " + neonColor + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 45px;" +
                        "-fx-min-height: 42px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-border-color: " + neonColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 50%;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, " + neonColor + ", 5, 0.3, 0, 0);"
        ));

        return button;
    }


    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static void main(String[] args) {
        launch(args);
    }
}