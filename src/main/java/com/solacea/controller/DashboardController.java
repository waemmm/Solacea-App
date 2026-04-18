package com.solacea.controller;

import com.solacea.util.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.fxml.*;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Tooltip;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.prefs.*;
import java.util.Optional;

import java.time.*;
import java.time.format.*;
import java.sql.*;
import java.util.*;

public class DashboardController {
    private static final Color STATUS_SUCCESS = Color.web("#7A9E7E");
    private static final Color STATUS_ERROR = Color.web("#D57F7E");

    @FXML private VBox wellbeingAlertBanner;
    @FXML private Button homeBtn, logMoodBtn, journalBtn, gardenBtn, historyBtn, settingsBtn;
    @FXML private Button wateringCanButton;
    @FXML private VBox homeView, logMoodView, journalView, gardenView, historyView, settingsView;
    @FXML private ScrollPane mainScrollPane;
    @FXML private FlowPane homeTopFlow, homeBottomFlow, homeStatusFlow, journalStatsFlow, gardenContentFlow, weeklyStreakFlow;
    @FXML private VBox recoveryCard, gardenStatusCard, moodSliderPanel, moodSliderTrackBox, gardenMainColumn, gardenSideColumn;
    @FXML private Label welcomeLabel, recoveryScoreLabel, gardenLevelLabel, insightLabel, gardenSubLabel;
    @FXML private Label journalPromptLabel, journalStatus, journalLogsTrackerLabel, journalRecoveryScoreLabel;
    @FXML private TextArea journalArea;
    @FXML private ImageView gardenPlantImageView;

    @FXML private ImageView moodFaceImageView;
    @FXML private Label moodIndicatorLabel;

    @FXML private Label gardenExpLabel, gardenMaxLabel;
    @FXML private ProgressBar gardenProgress;
    @FXML private Slider intensitySlider;
    @FXML private ComboBox<String> triggerComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label dashboardStatusLabel;
    @FXML private Label settingsStatusLabel;

    @FXML private Label currentDateLabel;

    @FXML private GridPane calendarGrid;
    @FXML private Label calendarMonthYearLabel;
    private YearMonth currentCalendarMonth = YearMonth.now();

    @FXML private Label gardenXpTextLabel;
    @FXML private Label moodTendencyLabel;
    @FXML private Label plantWisdomLabel;
    @FXML private StackPane plantBackgroundPane;

    @FXML private Label totalXpStatLabel;
    @FXML private Label totalMoodsStatLabel;
    @FXML private Label totalJournalsStatLabel;
    @FXML private StackPane day1Pane, day2Pane, day3Pane, day4Pane, day5Pane, day6Pane, day7Pane;

    @FXML private TextField displayNameField;
    @FXML private VBox calendarContainer;
    @FXML private VBox listContainer;
    @FXML private ListView<String> historyList;

    @FXML private Label homeStreakCountLabel;
    @FXML private Label homeTotalXpStatLabel;
    @FXML private Label homeTotalMoodsStatLabel;
    @FXML private Label homeTotalJournalsStatLabel;

    private PauseTransition moodTransition;
    private PauseTransition journalTransition;
    private PauseTransition settingsTransition;

    private Preferences prefs = Preferences.userRoot().node("com.solacea");

    private final String[] prompts = {
            "What emotions did I feel most strongly today, and why?",
            "What is something that made me smile today?",
            "What is currently causing me stress, and how can I manage it?",
            "Describe a recent challenge and what I learned from it?",
            "What are three things I'm grateful for right now?",
            "When do I feel most like myself?",
            "What negative thought keeps repeating in my mind, and how can I reframe it?",
            "What does my ideal peaceful day look like?",
            "How do I usually respond to difficult emotions?",
            "What is one small step I can take tomorrow to improve my well-being?"
    };

    private static final String FIRST_BAD_MOOD_PROMPT =
            "Even on difficult days, growth is still possible. Would you like to water your plant?";
    private static final String RESILIENT_SHIELD_PROMPT =
            "You're having a tough day, but you already took a step to care for yourself earlier. Your roots are strong enough to weather this storm.";
    private static final String WATERING_SUCCESS_WITH_XP_MESSAGE =
            "Watered! (+1 XP) Small care still counts.";
    private static final String WATERING_SUCCESS_NO_XP_MESSAGE =
            "Watered! Plant recovered. Daily mood XP was already counted.";

    @FXML
    // Function: initialize - Initializes dashboard controls, listeners, layout behavior, and default Home view.
    public void initialize() {
        if (triggerComboBox != null) {
            triggerComboBox.getItems().addAll("School", "Work", "Family", "Health", "Social", "Personal");
            triggerComboBox.setButtonCell(new ListCell<String>() {
                @Override
                // Function: updateItem - Renders each custom list cell/button-cell item text, style, and optional icon.
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select a trigger...");
                        setTextFill(Color.web("#6A515E")); // Mauve
                    } else {
                        setText(item);
                        setTextFill(Color.web("#592E83")); // Deep Purple
                    }
                }
            });
        }
        LocalDate today = LocalDate.now();
        if (currentDateLabel != null) currentDateLabel.setText(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        if (calendarMonthYearLabel != null) calendarMonthYearLabel.setText(today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        if (intensitySlider != null) {
            updateMoodIndicator((int) intensitySlider.getValue());
            intensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> updateMoodIndicator(newVal.intValue()));

            Rectangle sliderClip = new Rectangle();
            sliderClip.widthProperty().bind(intensitySlider.widthProperty());
            sliderClip.heightProperty().bind(intensitySlider.heightProperty());
            sliderClip.arcWidthProperty().bind(intensitySlider.heightProperty());
            sliderClip.arcHeightProperty().bind(intensitySlider.heightProperty());
            intensitySlider.setClip(sliderClip);
            applyRoundedMoodTrackClip();
        }

        if (moodSliderTrackBox != null) {
            Rectangle trackBoxClip = new Rectangle();
            trackBoxClip.widthProperty().bind(moodSliderTrackBox.widthProperty());
            trackBoxClip.heightProperty().bind(moodSliderTrackBox.heightProperty());
            trackBoxClip.setArcWidth(20);
            trackBoxClip.setArcHeight(20);
            moodSliderTrackBox.setClip(trackBoxClip);
        }

        if (historyList != null) {
            historyList.setCellFactory(listView -> new ListCell<String>() {
                private final ImageView imageView = new ImageView();
                @Override
                // Function: updateItem - Renders each custom list cell/button-cell item text, style, and optional icon.
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setGraphic(null);
                        setStyle("-fx-background-color: transparent; -fx-padding: 10;");
                    } else {
                        setText(item);
                        // Light Lilac borders, Cream background, Deep Purple text
                        setStyle("-fx-text-fill: #592E83; -fx-background-color: #FAF8F0; -fx-border-color: #C1AEDB; -fx-border-width: 0 0 1 0; -fx-padding: 10; -fx-font-weight: bold;");
                        int score = 5;
                        try { score = Integer.parseInt(item.split("\\|")[0].replace("Intensity:", "").trim()); } catch (Exception e) {}
                        Image faceImg = getFaceImage(score);
                        if (faceImg != null) {
                            imageView.setImage(faceImg); imageView.setFitHeight(30); imageView.setFitWidth(30);
                            imageView.setPreserveRatio(true); setGraphic(imageView);
                        } else setGraphic(null);
                    }
                }
            });
        }
        setupResponsiveLayout();
        renderCalendar();
        refreshHomeData();
        handleHomeClick();
    }

    // Function: setupResponsiveLayout - Registers width listeners so dashboard sections adapt to screen size.
    private void setupResponsiveLayout() {
        bindFlowWrapToWidth(homeTopFlow, 220);
        bindFlowWrapToWidth(homeBottomFlow, 220);
        bindFlowWrapToWidth(weeklyStreakFlow, 320);

        if (homeView != null) {
            homeView.widthProperty().addListener((obs, oldVal, newVal) -> updateHomeStatusLayout(newVal.doubleValue()));
        }
        if (journalView != null) {
            journalView.widthProperty().addListener((obs, oldVal, newVal) -> updateJournalStatsLayout(newVal.doubleValue()));
        }
        if (gardenView != null) {
            gardenView.widthProperty().addListener((obs, oldVal, newVal) -> updateGardenResponsiveLayout(newVal.doubleValue()));
        }
        if (logMoodView != null) {
            logMoodView.widthProperty().addListener((obs, oldVal, newVal) -> updateMoodSliderLayout(newVal.doubleValue()));
        }

        Platform.runLater(() -> {
            if (homeView != null) {
                updateHomeStatusLayout(homeView.getWidth());
            }
            if (journalView != null) {
                updateJournalStatsLayout(journalView.getWidth());
            }
            if (gardenView != null) {
                updateGardenResponsiveLayout(gardenView.getWidth());
            }
            if (logMoodView != null) {
                updateMoodSliderLayout(logMoodView.getWidth());
            }
        });
    }

    // Function: bindFlowWrapToWidth - Keeps a FlowPane wrap length synced with available width.
    private void bindFlowWrapToWidth(FlowPane flowPane, double minWrapLength) {
        if (flowPane == null) return;
        flowPane.widthProperty().addListener((obs, oldVal, newVal) ->
                flowPane.setPrefWrapLength(Math.max(minWrapLength, newVal.doubleValue())));
        Platform.runLater(() -> flowPane.setPrefWrapLength(Math.max(minWrapLength, flowPane.getWidth())));
    }

    // Function: updateHomeStatusLayout - Adjusts Home status card sizes for compact and wide layouts.
    private void updateHomeStatusLayout(double homeWidth) {
        if (homeStatusFlow == null) return;

        boolean compact = homeWidth > 0 && homeWidth <= 920;
        double compactPanelWidth = Math.min(540, Math.max(240, homeWidth - 72));
        double panelWidth = compact ? compactPanelWidth : 290;

        homeStatusFlow.setPrefWidth(panelWidth);
        homeStatusFlow.setMinWidth(Math.min(panelWidth, 240));
        homeStatusFlow.setPrefWrapLength(panelWidth);

        double compactCardWidth = (panelWidth - homeStatusFlow.getHgap()) / 2.0;
        applyStatusCardSize(recoveryCard, compact ? compactCardWidth : 250, compact ? 150 : 220);
        applyStatusCardSize(gardenStatusCard, compact ? compactCardWidth : 250, compact ? 150 : 220);
    }

    // Function: updateJournalStatsLayout - Updates Journal stats row wrapping based on current page width.
    private void updateJournalStatsLayout(double journalWidth) {
        if (journalStatsFlow == null) return;
        double effectiveWidth = journalWidth > 0 ? journalWidth : (journalView != null ? journalView.getWidth() : 0);
        if (effectiveWidth <= 0) return;
        journalStatsFlow.setPrefWrapLength(Math.max(420, effectiveWidth - 90));
    }

    // Function: updateGardenResponsiveLayout - Reflows Garden columns and image sizes for narrow or wide screens.
    private void updateGardenResponsiveLayout(double gardenWidth) {
        if (gardenContentFlow == null || gardenMainColumn == null || gardenSideColumn == null) return;

        double effectiveWidth = gardenWidth > 0 ? gardenWidth : (gardenView != null ? gardenView.getWidth() : 0);
        if (effectiveWidth <= 0) return;

        double contentWidth = Math.max(360, effectiveWidth - 64);
        gardenContentFlow.setPrefWrapLength(contentWidth);

        boolean stackColumns = contentWidth < 860;
        if (stackColumns) {
            gardenMainColumn.setPrefWidth(contentWidth);
            gardenSideColumn.setPrefWidth(contentWidth);
            gardenSideColumn.setMaxWidth(Double.MAX_VALUE);

            if (plantBackgroundPane != null) {
                plantBackgroundPane.setMinHeight(320);
                plantBackgroundPane.setPrefHeight(350);
            }
            if (gardenPlantImageView != null) {
                gardenPlantImageView.setFitWidth(285);
                gardenPlantImageView.setFitHeight(285);
            }
        } else {
            double sideWidth = Math.max(280, Math.min(340, contentWidth * 0.30));
            double mainWidth = Math.max(520, Math.min(700, contentWidth - sideWidth - gardenContentFlow.getHgap()));
            gardenMainColumn.setPrefWidth(mainWidth);
            gardenSideColumn.setPrefWidth(sideWidth);
            gardenSideColumn.setMaxWidth(340);

            if (plantBackgroundPane != null) {
                plantBackgroundPane.setMinHeight(290);
                plantBackgroundPane.setPrefHeight(315);
            }
            if (gardenPlantImageView != null) {
                gardenPlantImageView.setFitWidth(245);
                gardenPlantImageView.setFitHeight(245);
            }
        }

        if (weeklyStreakFlow != null) {
            weeklyStreakFlow.setPrefWrapLength(Math.max(420, Math.min(760, contentWidth - 90)));
        }
    }

    // Function: updateMoodSliderLayout - Resizes the mood slider panel and track to fit the current width.
    private void updateMoodSliderLayout(double moodWidth) {
        if (moodSliderPanel == null) return;
        double effectiveWidth = moodWidth > 0 ? moodWidth : (logMoodView != null ? logMoodView.getWidth() : 0);
        if (effectiveWidth <= 0) return;
        double panelWidth = Math.max(320, Math.min(560, effectiveWidth - 84));
        moodSliderPanel.setPrefWidth(panelWidth);

        double trackWidth = Math.max(260, Math.min(500, panelWidth - 32));
        if (moodSliderTrackBox != null) {
            moodSliderTrackBox.setPrefWidth(trackWidth);
            moodSliderTrackBox.setMaxWidth(trackWidth);
        }
        if (intensitySlider != null) {
            intensitySlider.setPrefWidth(trackWidth);
            intensitySlider.setMaxWidth(trackWidth);
        }
        applyRoundedMoodTrackClip();
    }

    // Function: applyRoundedMoodTrackClip - Applies a rounded clip to the slider track for smooth pill styling.
    private void applyRoundedMoodTrackClip() {
        if (intensitySlider == null) return;

        Platform.runLater(() -> {
            Node track = intensitySlider.lookup(".track");
            if (track == null) return;

            Rectangle trackClip = (track.getClip() instanceof Rectangle)
                    ? (Rectangle) track.getClip()
                    : new Rectangle();

            Bounds bounds = track.getLayoutBounds();
            trackClip.setWidth(bounds.getWidth());
            trackClip.setHeight(bounds.getHeight());

            double arc = Math.max(bounds.getHeight() * 3, 30);
            trackClip.setArcWidth(arc);
            trackClip.setArcHeight(arc);
            track.setClip(trackClip);
        });
    }

    // Function: applyStatusCardSize - Applies width constraints to status cards.
    private void applyStatusCardSize(Region card, double prefWidth, double minWidth) {
        if (card == null) return;
        card.setPrefWidth(prefWidth);
        card.setMinWidth(minWidth);
        card.setMaxWidth(Double.MAX_VALUE);
    }

    // Function: getFaceImage - Chooses and loads the mood face image for a score range.
    private Image getFaceImage(int score) {
        String faceFile = "Okay.png";
        if (score <= 2) faceFile = "Awful.png";
        else if (score <= 4) faceFile = "Bad.png";
        else if (score <= 6) faceFile = "Okay.png";
        else if (score <= 8) faceFile = "Good.png";
        else faceFile = "Great.png";

        try {
            java.io.InputStream stream = getClass().getResourceAsStream("/images/" + faceFile);
            if (stream != null) return new Image(stream);
        } catch (Exception e) {
            System.err.println("Could not load image: " + faceFile);
        }
        return null;
    }

    // Function: getMoodLabel - Converts a numeric mood score into a text label.
    private String getMoodLabel(int score) {
        if (score <= 2) return "Awful";
        if (score <= 4) return "Bad";
        if (score <= 6) return "Okay";
        if (score <= 8) return "Good";
        return "Great";
    }

    // Function: getMoodColor - Returns the color used for a mood score range.
    private Color getMoodColor(int score) {
        if (score <= 2) return Color.web("#D57F7E");
        if (score <= 4) return Color.web("#EEAB7B");
        if (score <= 6) return Color.web("#8A75A2");
        if (score <= 8) return Color.web("#7A9E7E");
        return Color.web("#592E83");
    }

    // Function: updateMoodIndicator - Refreshes mood icon and mood text when the slider value changes.
    private void updateMoodIndicator(int score) {
        if (moodFaceImageView != null) {
            moodFaceImageView.setImage(getFaceImage(score));
            moodFaceImageView.setVisible(true);
            moodFaceImageView.setManaged(true);
        }
        if (moodIndicatorLabel != null) {
            moodIndicatorLabel.setText(getMoodLabel(score) + " (" + score + "/10)");
            moodIndicatorLabel.setTextFill(getMoodColor(score));
        }
    }

    // Function: handleHomeClick - Shows Home page and refreshes dashboard summary values.
    @FXML private void handleHomeClick() {
        refreshHomeData();
        setVisibility(homeView, homeBtn);
    }

    // Function: handleLogMoodClick - Shows Log Mood page and refreshes slider layout sizing.
    @FXML private void handleLogMoodClick() {
        setVisibility(logMoodView, logMoodBtn);
        updateMoodSliderLayout(logMoodView != null ? logMoodView.getWidth() : 0);
    }

    // Function: handleJournalClick - Shows Journal page and loads a random writing prompt.
    @FXML private void handleJournalClick() {
        refreshHomeData();
        setVisibility(journalView, journalBtn);
        if (journalPromptLabel != null) {
            journalPromptLabel.setText(prompts[new Random().nextInt(prompts.length)]);
        }
    }

    // Function: handleGardenClick - Shows Garden page and refreshes plant and progress widgets.
    @FXML private void handleGardenClick() {
        setVisibility(gardenView, gardenBtn);
        updateGardenResponsiveLayout(gardenView != null ? gardenView.getWidth() : 0);
        updateGardenUI();
    }

    // Function: handleHistoryClick - Shows History page.
    @FXML private void handleHistoryClick() {
        setVisibility(historyView, historyBtn);
    }

    // Function: handleSettingsClick - Shows Settings page.
    @FXML private void handleSettingsClick() {
        setVisibility(settingsView, settingsBtn);
    }

    @FXML
    // Function: toggleHistoryView - Switches History between calendar view and recent-list view.
    private void toggleHistoryView() {
        if (calendarContainer != null && listContainer != null) {
            boolean isCalendarVisible = calendarContainer.isVisible();
            calendarContainer.setVisible(!isCalendarVisible);
            calendarContainer.setManaged(!isCalendarVisible);
            listContainer.setVisible(isCalendarVisible);
            listContainer.setManaged(isCalendarVisible);

            if (isCalendarVisible && historyList != null) {
                try {
                    historyList.getItems().setAll(DatabaseManager.getHistory(UserSession.getUser()));
                } catch (SQLException e) { }
            }
        }
    }

    @FXML
    // Function: handleChangeDisplayName - Saves a new display name and refreshes welcome text.
    private void handleChangeDisplayName() {
        String newName = displayNameField.getText().trim();
        if (!newName.isEmpty() && newName.length() <= 30) {
            prefs.put(UserSession.getUser() + "_display_name", newName);
            refreshHomeData();
            showTemporaryStatus(settingsStatusLabel, "Display name successfully updated!", Color.web("#8DB084"), "settings");
            displayNameField.clear();
        } else if (newName.length() > 30) {
            showTemporaryStatus(settingsStatusLabel, "Name too long (max 30 chars).", Color.web("#D57F7E"), "settings");
        }
    }
    @FXML
    // Function: handleClearHistoryOnly - Clears timeline history data while preserving XP and garden progress.
    private void handleClearHistoryOnly() {
        boolean confirmed = showStyledSettingsConfirmation(
                "Clear History",
                "Wipe your timeline?",
                "This clears your visual history logs but keeps your Garden XP and streak intact.",
                "Clear History",
                false
        );
        if (!confirmed) return;

        String user = UserSession.getUser();
        prefs.put(user + "_history_cleared_at", LocalDate.now().toString());

        if (historyList != null) historyList.getItems().clear();
        renderCalendar();

        showTemporaryStatus(settingsStatusLabel, "Visual history cleared successfully.", Color.web("#8DB084"), "settings");
    }

    // Function: handlePrevMonth - Moves calendar view to the previous month and re-renders it.
    @FXML private void handlePrevMonth() {
        currentCalendarMonth = currentCalendarMonth.minusMonths(1);
        renderCalendar();
    }

    // Function: handleNextMonth - Moves calendar view to the next month and re-renders it.
    @FXML private void handleNextMonth() {
        currentCalendarMonth = currentCalendarMonth.plusMonths(1);
        renderCalendar();
    }

    // Function: handleCurrentMonth - Jumps calendar view back to the current month.
    @FXML private void handleCurrentMonth() {
        currentCalendarMonth = YearMonth.now();
        renderCalendar();
    }
    @FXML
    // Function: handleResetCalendar - Clears calendar mood/note/streak markers while keeping growth progression.
    private void handleResetCalendar() {
        boolean confirmed = showStyledSettingsConfirmation(
                "Reset Calendar",
                "Clear calendar notes and mood marks?",
                "This removes mood markers, notes, and daily streak logs only. XP, display name, and garden progress are preserved.",
                "Reset Calendar",
                true
        );
        if (!confirmed) return;

        String user = UserSession.getUser();
        if (user == null) return;

        try {
            for (String key : prefs.keys()) {
                boolean isMoodKey = key.startsWith(user + "_mood_");
                boolean isNoteKey = key.startsWith(user + "_note_");
                boolean isDailyLogKey = key.startsWith(user + "_logged_");
                if (isMoodKey || isNoteKey || isDailyLogKey) {
                    prefs.remove(key);
                }
            }
            prefs.flush();

            if (historyList != null) historyList.getItems().clear();
            renderCalendar();
            refreshHomeData();
            updateGardenUI();

            showTemporaryStatus(settingsStatusLabel, "Calendar reset complete. XP and garden progress were kept.", STATUS_SUCCESS, "settings");
        } catch (BackingStoreException e) {
            showTemporaryStatus(settingsStatusLabel, "Could not reset calendar data.", STATUS_ERROR, "settings");
        }
    }

    // Function: getTrueXP - Reads the stored XP total for a user from preferences.
    private int getTrueXP(String user) {
        int storedXp = prefs.getInt(user + "_true_xp", -1);
        if (storedXp == -1) {
            try {
                int dbXp = DatabaseManager.getMoodCount(user) + DatabaseManager.getJournalCount(user);
                prefs.putInt(user + "_true_xp", dbXp);
                return dbXp;
            } catch (SQLException e) {
                return 0;
            }
        }
        return storedXp;
    }

    // Function: awardDailyXP - Awards once-per-day XP for a specific activity type.
    private boolean awardDailyXP(String user, String logType) {
        String today = LocalDate.now().toString();
        boolean alreadyAwarded = prefs.getBoolean(user + "_xp_awarded_" + logType + "_" + today, false);

        if (!alreadyAwarded) {
            prefs.putBoolean(user + "_xp_awarded_" + logType + "_" + today, true);
            int currentXp = getTrueXP(user);
            prefs.putInt(user + "_true_xp", currentXp + 1);
            return true;
        }
        return false;
    }
    @FXML
    // Function: handleResetAccount - Resets account data for testing and refreshes dashboard state.
    private void handleResetAccount() {
        boolean confirmed = showStyledSettingsConfirmation(
                "Reset Account",
                "Start your journey over?",
                "This permanently deletes all mood and journal logs and resets your garden progression.",
                "Reset Account",
                true
        );
        if (!confirmed) return;

        try {
            String user = UserSession.getUser();
            DatabaseManager.temporaryResetAccount(user);

            prefs.put(user + "_join_date", LocalDate.now().toString());
            prefs.remove(user + "_true_xp");
            prefs.remove(user + "_history_cleared_at");
            prefs.remove(user + "_xp_awarded_mood_" + LocalDate.now().toString());
            prefs.remove(user + "_xp_awarded_journal_" + LocalDate.now().toString());
            prefs.remove(user + "_xp_awarded_water_" + LocalDate.now().toString());

            refreshHomeData();
            updateGardenUI();
            if (historyList != null) historyList.getItems().clear();
            renderCalendar();

            showTemporaryStatus(settingsStatusLabel, "Account reset successfully. Your garden has been cleared.", Color.web("#8DB084"), "settings");
        } catch (SQLException e) {
            showTemporaryStatus(settingsStatusLabel, "Error: Could not reset account.", Color.web("#D57F7E"), "settings");
        }
    }

    // Function: showStyledSettingsConfirmation - Shows a styled confirmation dialog for settings actions.
    private boolean showStyledSettingsConfirmation(
            String title,
            String headline,
            String description,
            String confirmLabel,
            boolean highImpact
    ) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        ButtonType confirmType = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(confirmType, cancelType);
        styleCalendarDialog(dialog, 540);
        animateDialogOnShow(dialog);

        VBox content = new VBox(12);
        content.getStyleClass().add("settings-confirm-content");

        Label eyebrow = new Label(highImpact ? "HIGH IMPACT ACTION" : "CONFIRM ACTION");
        eyebrow.getStyleClass().add("settings-confirm-eyebrow");

        Label titleLabel = new Label(headline);
        titleLabel.getStyleClass().add("calendar-dialog-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("calendar-dialog-subtitle");
        descriptionLabel.setWrapText(true);

        Label impactChip = new Label(highImpact ? "Permanent Changes" : "Timeline Only");
        impactChip.getStyleClass().addAll("settings-confirm-chip", highImpact ? "settings-confirm-chip-danger" : "settings-confirm-chip-neutral");

        content.getChildren().addAll(eyebrow, titleLabel, descriptionLabel, impactChip);
        pane.setContent(content);

        Node confirmNode = pane.lookupButton(confirmType);
        Node cancelNode = pane.lookupButton(cancelType);
        if (confirmNode != null) confirmNode.getStyleClass().addAll("danger-button", "calendar-action-button", "settings-confirm-button");
        if (cancelNode != null) cancelNode.getStyleClass().addAll("secondary-button", "calendar-action-button", "settings-confirm-button");

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmType;
    }

    @FXML
    // Function: handleMaxOutGarden - Sets garden XP to max stage and refreshes garden visuals.
    private void handleMaxOutGarden() {
        try {
            String user = UserSession.getUser();
            for (int i = 0; i < 80; i++) {
                DatabaseManager.saveJournal(user, "DEV TOOL", "Auto-generated for max level testing.");
            }
            prefs.putInt(user + "_true_xp", 80);

            refreshHomeData();
            updateGardenUI();
            showTemporaryStatus(settingsStatusLabel, "\u2713 Garden forcefully maxed out! (80 XP Added)", Color.web("#8DB084"), "settings");
        } catch (SQLException e) {
            showTemporaryStatus(settingsStatusLabel, "Error maxing out garden.", Color.web("#D57F7E"), "settings");
            System.err.println("Dev tool error: " + e.getMessage());
        }
    }

    @FXML
    // Function: handleLevelUpGarden - Raises XP to the next stage threshold and refreshes UI.
    private void handleLevelUpGarden() {
        String user = UserSession.getUser();
        if (user == null) {
            showTemporaryStatus(settingsStatusLabel, "No active user session.", Color.web("#D57F7E"), "settings");
            return;
        }

        int currentXp = getTrueXP(user);
        int currentLevel = calculateLevel(currentXp);

        if (currentLevel >= 4) {
            showTemporaryStatus(settingsStatusLabel, "Garden is already at max level.", Color.web("#D57F7E"), "settings");
            return;
        }

        int nextLevel = currentLevel + 1;
        int targetXp = getXPThreshold(nextLevel);
        int gainedXp = Math.max(0, targetXp - currentXp);

        prefs.putInt(user + "_true_xp", targetXp);

        refreshHomeData();
        updateGardenUI();
        showTemporaryStatus(
                settingsStatusLabel,
                "\u2713 Garden leveled up to Stage " + nextLevel + " (+" + gainedXp + " XP).",
                Color.web("#8DB084"),
                "settings"
        );
    }

    // Function: handleSaveEntry - Saves a mood log entry, applies rewards, and refreshes related widgets.
    @FXML private void handleSaveEntry() {
        String user = UserSession.getUser();

        if (triggerComboBox == null || triggerComboBox.getValue() == null) {
            showTemporaryStatus(dashboardStatusLabel, "Please select what triggered this feeling.", Color.web("#D57F7E"), "mood");
            return;
        }

        try {
            int score = (int) intensitySlider.getValue();
            String trigger = triggerComboBox.getValue();
            String notes = notesArea.getText();

            if (DatabaseManager.saveMood(user, score, trigger, notes)) {

                boolean gotXp = awardDailyXP(user, "mood");
                String msg = gotXp ? "Entry saved! (+1 XP) Your garden is reacting." : "Entry saved! (Daily Mood XP cap reached).";
                showTemporaryStatus(dashboardStatusLabel, msg, Color.web("#8DB084"), "mood");

                String today = LocalDate.now().toString();
                prefs.putInt(user + "_mood_" + today, score);
                prefs.putBoolean(user + "_logged_" + today, true);
                renderCalendar();

                intensitySlider.setValue(5);
                triggerComboBox.getSelectionModel().clearSelection();
                triggerComboBox.setValue(null);
                notesArea.clear();

                refreshHomeData();
                updateGardenUI();
                checkEmpatheticTrigger(score);
            }
        } catch (SQLException e) {
            showTemporaryStatus(dashboardStatusLabel, "Database error saving mood.", Color.web("#D57F7E"), "mood");
        }
    }

    // Function: checkEmpatheticTrigger - Shows or hides the gentle support banner based on recent low moods.
    private void checkEmpatheticTrigger(int score) {
        String user = UserSession.getUser();
        if (user == null) return;

        if (score > 4) {
            if (wateringCanButton != null) {
                wateringCanButton.setVisible(false);
                wateringCanButton.setManaged(false);
            }
            return;
        }

        String today = LocalDate.now().toString();
        boolean alreadyWateredToday = prefs.getBoolean(user + "_xp_awarded_water_" + today, false);
        int stage = Math.max(1, calculateLevel(getTrueXP(user)));

        if (!alreadyWateredToday) {
            loadPlantImage(resolvePlantImageName(3, stage, "Sad_Droopy"));
            if (wateringCanButton != null) {
                wateringCanButton.setVisible(true);
                wateringCanButton.setManaged(true);
            }
            if (plantWisdomLabel != null) {
                plantWisdomLabel.setText(FIRST_BAD_MOOD_PROMPT);
            }
            showTemporaryStatus(dashboardStatusLabel, FIRST_BAD_MOOD_PROMPT, STATUS_SUCCESS, "mood");
            return;
        }

        loadPlantImage(resolvePlantImageName(2, stage, "Neutral"));
        if (wateringCanButton != null) {
            wateringCanButton.setVisible(false);
            wateringCanButton.setManaged(false);
        }
        if (plantWisdomLabel != null) {
            plantWisdomLabel.setText(RESILIENT_SHIELD_PROMPT);
        }
        showTemporaryStatus(dashboardStatusLabel, RESILIENT_SHIELD_PROMPT, STATUS_SUCCESS, "mood");
    }

    @FXML
    // Function: handleWateringAction - Handles watering action, optional XP reward, and garden refresh.
    private void handleWateringAction(javafx.event.ActionEvent event) {
        String user = UserSession.getUser();
        if (user == null) return;

        String today = LocalDate.now().toString();
        String moodXpKey = user + "_xp_awarded_mood_" + today;
        String wateringXpKey = user + "_xp_awarded_water_" + today;
        boolean alreadyAwarded = prefs.getBoolean(wateringXpKey, false);

        if (alreadyAwarded) {
            if (plantWisdomLabel != null) {
                plantWisdomLabel.setText(RESILIENT_SHIELD_PROMPT);
            }
            showTemporaryStatus(dashboardStatusLabel, RESILIENT_SHIELD_PROMPT, STATUS_SUCCESS, "mood");
            refreshHomeData();
            updateGardenUI();
            return;
        }

        boolean moodXpAlreadyAwarded = prefs.getBoolean(moodXpKey, false);
        if (!moodXpAlreadyAwarded) {
            int updatedXp = getTrueXP(user) + 1;
            prefs.putInt(user + "_true_xp", updatedXp);
        }
        prefs.putBoolean(wateringXpKey, true);

        String wateringMessage = moodXpAlreadyAwarded
                ? WATERING_SUCCESS_NO_XP_MESSAGE
                : WATERING_SUCCESS_WITH_XP_MESSAGE;
        if (plantWisdomLabel != null) {
            plantWisdomLabel.setText(wateringMessage);
        }
        showTemporaryStatus(dashboardStatusLabel, wateringMessage, STATUS_SUCCESS, "mood");
        refreshHomeData();
        updateGardenUI();
    }

    // Function: handleSaveJournal - Saves journal text and updates stats, status, and garden data.
    @FXML private void handleSaveJournal() {
        if (journalArea == null || journalPromptLabel == null || journalArea.getText().isBlank()) {
            showTemporaryStatus(journalStatus, "Please write something before saving.", Color.web("#D57F7E"), "journal");
            return;
        }
        try {
            String user = UserSession.getUser();
            DatabaseManager.saveJournal(user, journalPromptLabel.getText(), journalArea.getText());

            boolean gotXp = awardDailyXP(user, "journal");
            String msg = gotXp ? "Journal saved! (+1 XP) Your garden is growing. \uD83C\uDF31" : "Journal saved! (Daily Journal XP cap reached).";
            showTemporaryStatus(journalStatus, msg, Color.web("#8DB084"), "journal");

            prefs.putBoolean(user + "_logged_" + LocalDate.now().toString(), true);

            journalArea.clear();
            refreshHomeData();
            updateGardenUI();
        } catch (SQLException e) {
            showTemporaryStatus(journalStatus, "Error saving journal.", Color.web("#D57F7E"), "journal");
        }
    }

    // Function: renderCalendar - Builds the monthly calendar grid with mood colors, notes, and day actions.
    private void renderCalendar() {
        if (calendarGrid == null || calendarMonthYearLabel == null) return;
        calendarMonthYearLabel.setText(currentCalendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calendarGrid.getChildren().clear(); calendarGrid.getRowConstraints().clear();

        LocalDate firstOfMonth = currentCalendarMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int daysInMonth = currentCalendarMonth.lengthOfMonth();
        int totalCellsNeeded = (dayOfWeek - 1) + daysInMonth;
        int rowCount = (int) Math.ceil(totalCellsNeeded / 7.0);

        for (int i = 0; i < rowCount; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rowCount);
            rc.setMinHeight(46);
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            calendarGrid.getRowConstraints().add(rc);
        }

        int row = 0; int col = dayOfWeek - 1; String user = UserSession.getUser();
        if (user == null) return;
        String clearDateStr = prefs.get(user + "_history_cleared_at", "2000-01-01");
        LocalDate historyClearedDate = LocalDate.parse(clearDateStr);

        for (int i = 0; i < col; i++) {
            Region emptyRegion = new Region(); emptyRegion.setStyle("-fx-background-color: transparent;");
            GridPane.setHgrow(emptyRegion, Priority.ALWAYS);
            GridPane.setVgrow(emptyRegion, Priority.ALWAYS);
            calendarGrid.add(emptyRegion, i, row);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentCalendarMonth.atDay(day); StackPane pane = new StackPane();

            String bgColor = "#FAF8F0"; // Cream empty day
            String textColor = "#6A515E"; // Mauve text
            int moodScore = prefs.getInt(user + "_mood_" + date.toString(), -1);
            String emoji = "";

            if (date.isBefore(historyClearedDate)) moodScore = -1;

            if (moodScore != -1) {
                bgColor = getCalendarMoodColor(moodScore);
                textColor = moodScore >= 9 ? "#5B4A14" : "#FFFFFF";
                if (moodScore <= 2) emoji = "\uD83D\uDE16";
                else if (moodScore <= 4) emoji = "\uD83D\uDE1E";
                else if (moodScore <= 6) emoji = "\uD83D\uDE10";
                else if (moodScore <= 8) emoji = "\uD83D\uDE42";
                else emoji = "\uD83D\uDE04";
            } else if (date.isEqual(LocalDate.now())) {
                bgColor = "#C1AEDB"; textColor = "#592E83"; // Highlight today with Lilac
            }

            pane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
            pane.setMinSize(0, 0);
            pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setHgrow(pane, Priority.ALWAYS);
            GridPane.setVgrow(pane, Priority.ALWAYS);
            pane.setCursor(javafx.scene.Cursor.HAND);

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + textColor + "; -fx-padding: 10;");
            StackPane.setAlignment(dayLabel, javafx.geometry.Pos.TOP_LEFT); pane.getChildren().add(dayLabel);
            if (!emoji.isEmpty()) {
                Label emojiLabel = new Label(emoji); emojiLabel.setStyle("-fx-font-size: 20px;");
                StackPane.setAlignment(emojiLabel, javafx.geometry.Pos.CENTER); pane.getChildren().add(emojiLabel);
            }

            String note = getNoteForDate(user, date);
            if (!note.isBlank()) {
                Tooltip noteTooltip = new Tooltip(note);
                noteTooltip.setWrapText(true);
                noteTooltip.setMaxWidth(240);
                noteTooltip.setShowDelay(Duration.millis(120));
                noteTooltip.getStyleClass().add("calendar-note-tooltip");
                Tooltip.install(pane, noteTooltip);

                Circle noteIndicator = new Circle(4.0);
                noteIndicator.setFill(Color.web("#C1AEDB"));
                noteIndicator.setStroke(Color.web("#FAF8F0"));
                noteIndicator.setStrokeWidth(1.1);
                StackPane.setAlignment(noteIndicator, javafx.geometry.Pos.TOP_RIGHT);
                StackPane.setMargin(noteIndicator, new Insets(9, 9, 0, 0));
                pane.getChildren().add(noteIndicator);
            }

            pane.setOnMouseClicked(event -> showDayOptions(date));

            calendarGrid.add(pane, col, row); col++;
            if (col == 7) { col = 0; row++; }
        }

        while (row < rowCount) {
            while (col < 7) {
                Region emptyRegion = new Region(); emptyRegion.setStyle("-fx-background-color: transparent;");
                GridPane.setHgrow(emptyRegion, Priority.ALWAYS);
                GridPane.setVgrow(emptyRegion, Priority.ALWAYS);
                calendarGrid.add(emptyRegion, col, row); col++;
            }
            col = 0; row++;
        }
    }

    // Function: showDayOptions - Opens a dialog with actions for a specific calendar day.
    private void showDayOptions(LocalDate date) {
        String user = UserSession.getUser();
        if (user == null) return;

        String noteKey = getNoteKey(user, date);
        String moodKey = user + "_mood_" + date;
        String loggedKey = user + "_logged_" + date;

        String existingNote = prefs.get(noteKey, "").trim();
        boolean hasNote = !existingNote.isBlank();
        boolean hasMood = prefs.getInt(moodKey, -1) != -1;

        Dialog<ButtonType> menu = new Dialog<>();
        menu.setTitle("Day Actions");
        menu.setHeaderText(null);
        menu.setGraphic(null);

        ButtonType addOrEditNoteButton = new ButtonType(hasNote ? "Edit Note" : "Add Note");
        ButtonType removeNoteButton = new ButtonType("Remove Note");
        ButtonType removeMoodButton = new ButtonType("Remove Mood");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = menu.getDialogPane();
        pane.setContent(buildDayOptionsDialogContent(date, hasNote, hasMood, existingNote));
        pane.getButtonTypes().setAll(addOrEditNoteButton, removeNoteButton, removeMoodButton, cancelButton);
        styleCalendarDialog(menu, 510);
        animateDialogOnShow(menu);

        Node addOrEditNode = pane.lookupButton(addOrEditNoteButton);
        Node removeNoteNode = pane.lookupButton(removeNoteButton);
        Node removeMoodNode = pane.lookupButton(removeMoodButton);
        Node cancelNode = pane.lookupButton(cancelButton);

        if (addOrEditNode != null) addOrEditNode.getStyleClass().addAll("primary-button", "calendar-action-button");
        if (removeNoteNode != null) removeNoteNode.getStyleClass().addAll("danger-button", "calendar-action-button");
        if (removeMoodNode != null) removeMoodNode.getStyleClass().addAll("danger-button", "calendar-action-button");
        if (cancelNode != null) cancelNode.getStyleClass().addAll("secondary-button", "calendar-action-button");
        if (removeNoteNode != null) removeNoteNode.setDisable(!hasNote);
        if (removeMoodNode != null) removeMoodNode.setDisable(!hasMood);

        Optional<ButtonType> menuChoice = menu.showAndWait();
        if (menuChoice.isEmpty() || menuChoice.get() == cancelButton) return;

        if (menuChoice.get() == addOrEditNoteButton) {
            Optional<String> noteResult = showStyledNoteEditor(date, existingNote);
            if (noteResult.isEmpty()) return;

            String updatedNote = noteResult.get().trim();
            if (updatedNote.isEmpty()) prefs.remove(noteKey);
            else prefs.put(noteKey, updatedNote);

            refreshCalendarAndDashboard();
            return;
        }

        if (menuChoice.get() == removeNoteButton) {
            prefs.remove(noteKey);
            refreshCalendarAndDashboard();
            return;
        }

        if (menuChoice.get() == removeMoodButton) {
            prefs.remove(moodKey);
            prefs.remove(loggedKey);
            refreshCalendarAndDashboard();
        }
    }

    // Function: buildDayOptionsDialogContent - Builds the custom content panel used by the day-options dialog.
    private VBox buildDayOptionsDialogContent(LocalDate date, boolean hasNote, boolean hasMood, String existingNote) {
        VBox wrapper = new VBox(12);
        wrapper.getStyleClass().add("calendar-dialog-content");

        Label title = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        title.getStyleClass().add("calendar-dialog-title");

        Label subtitle = new Label("Manage your note and mood marks for this day.");
        subtitle.getStyleClass().add("calendar-dialog-subtitle");
        subtitle.setWrapText(true);

        HBox chipsRow = new HBox(8);
        chipsRow.getStyleClass().add("calendar-status-row");
        chipsRow.getChildren().addAll(
                createDayStatusChip(hasMood ? "Mood Logged" : "No Mood", hasMood),
                createDayStatusChip(hasNote ? "Note Saved" : "No Note", hasNote)
        );

        Label hint = new Label(
                hasNote
                        ? "Tip: Edit note to refine your context before you remove anything."
                        : "Tip: Add a note to remember what happened on this day."
        );
        hint.getStyleClass().add("calendar-dialog-hint");
        hint.setWrapText(true);

        wrapper.getChildren().addAll(title, subtitle, chipsRow, hint);

        if (hasNote) {
            String previewText = existingNote.length() > 110
                    ? existingNote.substring(0, 110) + "..."
                    : existingNote;
            Label preview = new Label("Current note: " + previewText);
            preview.getStyleClass().add("calendar-note-preview");
            preview.setWrapText(true);
            wrapper.getChildren().add(preview);
        }
        return wrapper;
    }

    // Function: createDayStatusChip - Creates a small status chip used in the day-options dialog.
    private Label createDayStatusChip(String text, boolean active) {
        Label chip = new Label(text);
        chip.getStyleClass().add("calendar-status-chip");
        chip.getStyleClass().add(active ? "calendar-status-chip-active" : "calendar-status-chip-inactive");
        return chip;
    }

    // Function: showStyledNoteEditor - Opens styled note editor dialog with character limit and counter.
    private Optional<String> showStyledNoteEditor(LocalDate date, String existingNote) {
        Dialog<String> noteDialog = new Dialog<>();
        noteDialog.setTitle("Edit Note");
        noteDialog.setHeaderText(null);
        noteDialog.setGraphic(null);

        DialogPane pane = noteDialog.getDialogPane();
        ButtonType saveType = new ButtonType("Save Note", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(saveType, cancelType);
        styleCalendarDialog(noteDialog, 560);
        animateDialogOnShow(noteDialog);

        Label title = new Label("Calendar Note");
        title.getStyleClass().add("calendar-dialog-title");

        Label subtitle = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        subtitle.getStyleClass().add("calendar-dialog-subtitle");

        TextArea noteArea = new TextArea(existingNote);
        noteArea.setWrapText(true);
        noteArea.setPromptText("Write what happened today...");
        noteArea.setPrefRowCount(6);
        noteArea.getStyleClass().add("calendar-note-area");

        Label noteHint = new Label("Leave blank and save to remove this note.");
        noteHint.getStyleClass().add("calendar-dialog-hint");

        Label counterLabel = new Label();
        counterLabel.getStyleClass().add("calendar-note-counter");
        final int maxChars = 280;
        counterLabel.setText((existingNote != null ? existingNote.length() : 0) + " / " + maxChars);

        noteArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > maxChars) {
                noteArea.setText(newValue.substring(0, maxChars));
                noteArea.positionCaret(maxChars);
                return;
            }
            counterLabel.setText((newValue == null ? 0 : newValue.length()) + " / " + maxChars);
        });

        HBox counterRow = new HBox(counterLabel);
        counterRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox content = new VBox(12, title, subtitle, noteArea, noteHint, counterRow);
        content.getStyleClass().add("calendar-dialog-content");
        pane.setContent(content);

        Node saveNode = pane.lookupButton(saveType);
        Node cancelNode = pane.lookupButton(cancelType);
        if (saveNode != null) saveNode.getStyleClass().addAll("primary-button", "calendar-action-button");
        if (cancelNode != null) cancelNode.getStyleClass().addAll("secondary-button", "calendar-action-button");

        Platform.runLater(noteArea::requestFocus);

        noteDialog.setResultConverter(buttonType -> {
            if (buttonType == saveType) {
                return noteArea.getText() == null ? "" : noteArea.getText().trim();
            }
            return null;
        });

        return noteDialog.showAndWait();
    }

    // Function: styleCalendarDialog - Applies app CSS and size settings to calendar dialogs.
    private void styleCalendarDialog(Dialog<?> dialog, double minWidth) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("calendar-dialog-pane");
        pane.setMinWidth(minWidth);
        pane.setPrefWidth(minWidth);

        java.net.URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }
    }

    // Function: animateDialogOnShow - Plays fade and scale animation when a dialog appears.
    private void animateDialogOnShow(Dialog<?> dialog) {
        dialog.setOnShown(event -> {
            DialogPane pane = dialog.getDialogPane();
            pane.setOpacity(0);
            pane.setScaleX(0.95);
            pane.setScaleY(0.95);

            FadeTransition fade = new FadeTransition(Duration.millis(170), pane);
            fade.setFromValue(0);
            fade.setToValue(1);

            ScaleTransition scale = new ScaleTransition(Duration.millis(220), pane);
            scale.setFromX(0.95);
            scale.setFromY(0.95);
            scale.setToX(1);
            scale.setToY(1);

            ParallelTransition transition = new ParallelTransition(fade, scale);
            transition.play();
        });
    }

    // Function: getNoteForDate - Reads a saved note for a specific date.
    private String getNoteForDate(String user, LocalDate date) {
        return prefs.get(getNoteKey(user, date), "").trim();
    }

    // Function: getNoteKey - Builds the preference key used to store a date note.
    private String getNoteKey(String user, LocalDate date) {
        return user + "_note_" + date;
    }

    // Function: refreshCalendarAndDashboard - Refreshes calendar, home summary, and garden widgets together.
    private void refreshCalendarAndDashboard() {
        renderCalendar();
        refreshHomeData();
        updateGardenUI();
    }

    // Function: getCalendarMoodColor - Returns calendar color code for each mood score range.
    private String getCalendarMoodColor(int score) {
        if (score <= 2) return "#D57F7E"; // Awful
        if (score <= 4) return "#EEAB7B"; // Bad
        if (score <= 6) return "#8FB9DA"; // Okay (blue face icon tone)
        if (score <= 8) return "#7FC59B"; // Good (green face icon tone)
        return "#E3C44A";                 // Great (yellow face icon tone)
    }

    // Function: refreshHomeData - Reloads greeting, counters, streak, and summary text on Home/Journal cards.
    private void refreshHomeData() {
        String user = UserSession.getUser();
        if (user == null) return;
        String displayName = prefs.get(user + "_display_name", user);
        if (welcomeLabel != null) welcomeLabel.setText("Hi " + displayName + "!");

        try {
            int moodCount    = DatabaseManager.getMoodCount(user);
            int journalCount = DatabaseManager.getJournalCount(user);
            int xp           = getTrueXP(user);
            int level        = calculateLevel(xp);
            String recoveryScore = formatRecoveryScore(moodCount);

            if (recoveryScoreLabel != null)
                recoveryScoreLabel.setText(recoveryScore);
            if (gardenLevelLabel != null)
                gardenLevelLabel.setText("Level " + Math.max(1, level));
            if (gardenSubLabel != null)
                gardenSubLabel.setText(journalCount + " journal" + (journalCount == 1 ? "" : "s") + " completed");
            if (insightLabel != null)
                insightLabel.setText(moodCount == 0
                        ? "Start logging your mood to see insights!"
                        : "You're building a great habit! Keep going. \uD83D\uDCAA");

            if (homeTotalXpStatLabel != null) homeTotalXpStatLabel.setText(String.valueOf(xp));
            if (homeTotalMoodsStatLabel != null) homeTotalMoodsStatLabel.setText(String.valueOf(moodCount));
            if (homeTotalJournalsStatLabel != null) homeTotalJournalsStatLabel.setText(String.valueOf(journalCount));
            if (journalLogsTrackerLabel != null) {
                String suffix = journalCount == 1 ? "entry" : "entries";
                journalLogsTrackerLabel.setText(journalCount + " " + suffix);
            }
            if (journalRecoveryScoreLabel != null) {
                journalRecoveryScoreLabel.setText(recoveryScore);
            }

            if (homeStreakCountLabel != null) {
                int currentStreak = calculateConsecutiveLoggedDays(user);
                homeStreakCountLabel.setText(String.valueOf(currentStreak));
            }

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Function: calculateConsecutiveLoggedDays - Counts the current streak of consecutive logged days.
    private int calculateConsecutiveLoggedDays(String user) {
        LocalDate dateToCheck = LocalDate.now();
        int streak = 0;

        while (prefs.getBoolean(user + "_logged_" + dateToCheck.toString(), false)) {
            streak++;
            dateToCheck = dateToCheck.minusDays(1);
        }
        return streak;
    }

    // Function: formatRecoveryScore - Formats mood log count into a capped percentage value.
    private String formatRecoveryScore(int moodCount) {
        return moodCount == 0 ? "0%" : Math.min(moodCount * 10, 100) + "%";
    }

    // Function: updateGardenUI - Refreshes plant image, level, XP progress, mood tendency, and garden text.
    public void updateGardenUI() {
        try {
            String user = UserSession.getUser();
            if (user == null) return;

            int    moodCount    = DatabaseManager.getMoodCount(user);
            int    journalCount = DatabaseManager.getJournalCount(user);
            int    totalXP      = getTrueXP(user);
            int    level        = calculateLevel(totalXP);

            double progress;
            if (level == 4) {
                progress = 1.0;
                if (gardenXpTextLabel != null) gardenXpTextLabel.setText("MAX XP");
            } else if (level == 0) {
                progress = 0.0;
                if (gardenXpTextLabel != null) gardenXpTextLabel.setText(totalXP + " / " + getXPThreshold(1) + " XP");
            } else {
                int xpThisLevel = (level == 1) ? 0 : getXPThreshold(level);
                int xpNextLevel = getXPThreshold(level + 1);
                progress = (double)(totalXP - xpThisLevel) / (xpNextLevel - xpThisLevel);
                if (gardenXpTextLabel != null) gardenXpTextLabel.setText(totalXP + " / " + xpNextLevel + " XP");
            }

            if (gardenProgress != null) gardenProgress.setProgress(Math.min(progress, 1.0));

            if (gardenExpLabel != null) {
                if (level == 0) gardenExpLabel.setText("Seedling (Stage 0)");
                else if (level == 4) gardenExpLabel.setText("Full Bloom (Stage 4)");
                else gardenExpLabel.setText("Growing (Stage " + level + ")");
            }

            if (gardenMaxLabel != null) {
                boolean isMax = (level == 4);
                gardenMaxLabel.setVisible(isMax);
                gardenMaxLabel.setManaged(isMax);
            }

            if (gardenLevelLabel != null) gardenLevelLabel.setText("Level " + Math.max(1, level));

            if (totalXpStatLabel != null) totalXpStatLabel.setText(String.valueOf(totalXP));
            if (totalMoodsStatLabel != null) totalMoodsStatLabel.setText(String.valueOf(moodCount));
            if (totalJournalsStatLabel != null) totalJournalsStatLabel.setText(String.valueOf(journalCount));

            updateWeeklyStreak();

            if (totalXP == 0) {
                loadPlantImage("0_0_None.jpg");
                if (moodTendencyLabel != null) moodTendencyLabel.setText("Waiting...");
                if (plantWisdomLabel != null) plantWisdomLabel.setText("Your garden is waiting to bloom. Log your first mood to plant a seed!");

                if (plantBackgroundPane != null) plantBackgroundPane.setStyle("-fx-background-color: #3A314D; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);");
                return;
            }

            int lastIntensity = DatabaseManager.getLatestMoodIntensity(user);
            boolean wateredToday = prefs.getBoolean(user + "_xp_awarded_water_" + LocalDate.now(), false);
            int plantType;
            String emotion;

            if (lastIntensity == 0 || (lastIntensity > 4 && lastIntensity <= 6)) {
                plantType = 2;
                emotion = "Neutral";
                if (wateringCanButton != null) {
                    wateringCanButton.setVisible(false);
                    wateringCanButton.setManaged(false);
                }
                if (moodTendencyLabel != null) moodTendencyLabel.setText("Balanced");
                if (plantWisdomLabel != null) plantWisdomLabel.setText("A calm day is a good day. Consistency is the key to a healthy garden.");
                if (plantBackgroundPane != null) plantBackgroundPane.setStyle("-fx-background-color: #3A314D; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);");
            } else if (lastIntensity <= 4) {
                if (wateredToday) {
                    plantType = 2;
                    emotion = "Neutral";
                    if (wateringCanButton != null) {
                        wateringCanButton.setVisible(false);
                        wateringCanButton.setManaged(false);
                    }
                    if (moodTendencyLabel != null) moodTendencyLabel.setText("Recovering");
                    if (plantWisdomLabel != null) plantWisdomLabel.setText(RESILIENT_SHIELD_PROMPT);
                    if (plantBackgroundPane != null) plantBackgroundPane.setStyle("-fx-background-color: #3A314D; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);");
                } else {
                    plantType = 3;
                    emotion = "Sad_Droopy";
                    if (wateringCanButton != null) {
                        wateringCanButton.setVisible(true);
                        wateringCanButton.setManaged(true);
                    }
                    if (moodTendencyLabel != null) moodTendencyLabel.setText("Needing Care");
                    if (plantWisdomLabel != null) plantWisdomLabel.setText("Even in the rain, roots grow deeper. Be gentle with yourself today.");
                    if (plantBackgroundPane != null) plantBackgroundPane.setStyle("-fx-background-color: #3A314D; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);");
                }
            } else {
                plantType = 1;
                emotion = "Happy";
                if (wateringCanButton != null) {
                    wateringCanButton.setVisible(false);
                    wateringCanButton.setManaged(false);
                }
                if (moodTendencyLabel != null) moodTendencyLabel.setText("Thriving");
                if (plantWisdomLabel != null) plantWisdomLabel.setText("Your garden is basking in sunlight! Keep embracing the positive energy.");
                if (plantBackgroundPane != null) plantBackgroundPane.setStyle("-fx-background-color: #3A314D; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 6);");
            }

            String imageName = resolvePlantImageName(plantType, level, emotion);
            loadPlantImage(imageName);

        } catch (SQLException e) {
            System.err.println("Error updating garden UI: " + e.getMessage());
        }
    }

    // Function: updateWeeklyStreak - Colors weekly streak dots based on log status and join date.
    private void updateWeeklyStreak() {
        StackPane[] days = {day1Pane, day2Pane, day3Pane, day4Pane, day5Pane, day6Pane, day7Pane};
        if (days[0] == null) return;
        String user = UserSession.getUser(); LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        String joinDateStr = prefs.get(user + "_join_date", null);
        if (joinDateStr == null) { joinDateStr = today.toString(); prefs.put(user + "_join_date", joinDateStr); }
        LocalDate joinDate = LocalDate.parse(joinDateStr);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            boolean logged = prefs.getBoolean(user + "_logged_" + date.toString(), false);

            if (date.isAfter(today) || date.isBefore(joinDate)) {
                days[i].setStyle("-fx-background-color: #E8E8E8; -fx-background-radius: 20;");
            } else if (date.isEqual(today)) {
                if (logged) days[i].setStyle("-fx-background-color: #7A9E7E; -fx-background-radius: 20;"); // Sage
                else days[i].setStyle("-fx-background-color: #C1AEDB; -fx-background-radius: 20;"); // Lilac
            } else {
                if (logged) days[i].setStyle("-fx-background-color: #7A9E7E; -fx-background-radius: 20;"); // Sage
                else days[i].setStyle("-fx-background-color: #D57F7E; -fx-background-radius: 20;");
            }
        }
    }

    // Function: calculateLevel - Maps XP total to garden level stage.
    private int calculateLevel(int xp) {
        if (xp == 0)  return 0;
        if (xp < 20)  return 1;
        if (xp < 45)  return 2;
        if (xp < 80)  return 3;
        return 4;
    }

    // Function: getXPThreshold - Returns XP requirement for a given level.
    private int getXPThreshold(int level) {
        return switch (level) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 20;
            case 3 -> 45;
            case 4 -> 80;
            default -> Integer.MAX_VALUE;
        };
    }

    // Function: showTemporaryStatus - Shows a status message briefly, then clears it automatically.
    private void showTemporaryStatus(Label label, String text, Color color, String type) {
        if (label != null) {
            label.setText(text);
            label.setTextFill(resolveStatusColor(color));
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> label.setText(""));
            if ("mood".equals(type)) { if (moodTransition != null) moodTransition.stop(); moodTransition = pause; moodTransition.play(); }
            else if ("journal".equals(type)) { if (journalTransition != null) journalTransition.stop(); journalTransition = pause; journalTransition.play(); }
            else if ("settings".equals(type)) { if (settingsTransition != null) settingsTransition.stop(); settingsTransition = pause; settingsTransition.play(); }
        }
    }

    // Function: resolveStatusColor - Normalizes requested status color to app success/error colors.
    private Color resolveStatusColor(Color requestedColor) {
        return STATUS_ERROR.equals(requestedColor) ? STATUS_ERROR : STATUS_SUCCESS;
    }

    // Function: loadPlantImage - Loads plant image asset, with a fallback image when missing.
    private void loadPlantImage(String imageName) {
        if (gardenPlantImageView == null) return;
        try {
            String path = "/images/" + imageName;
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                gardenPlantImageView.setImage(new Image(stream));
            } else {
                java.io.InputStream fallback = getClass().getResourceAsStream("/images/0_0_None.jpg");
                if (fallback != null) gardenPlantImageView.setImage(new Image(fallback));
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
        }
    }

    // Function: setVisibility - Shows selected page, hides others, and updates active sidebar button.
    private void setVisibility(VBox targetView, Button activeBtn) {
        if (dashboardStatusLabel != null) dashboardStatusLabel.setText("");
        if (journalStatus != null) journalStatus.setText("");
        if (settingsStatusLabel != null) settingsStatusLabel.setText("");
        if (moodTransition != null) moodTransition.stop();
        if (journalTransition != null) journalTransition.stop();
        if (settingsTransition != null) settingsTransition.stop();

        VBox[] views = { homeView, logMoodView, journalView, gardenView, historyView, settingsView };
        for (VBox v : views) {
            if (v != null) {
                boolean isActive = v == targetView;
                v.setVisible(isActive);
                v.setManaged(isActive);
            }
        }

        resetButtonStyles();
        if (activeBtn != null) {
            // Active: Sage Green Background, White Text
            activeBtn.setStyle("-fx-background-color: #7A9E7E; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 25 0 0 25; -fx-alignment: center-left; -fx-padding: 12 10 12 30;");
        }

        updateScrollBehavior(targetView);
    }

    // Function: updateScrollBehavior - Sets scroll behavior rules for the active page.
    private void updateScrollBehavior(VBox activeView) {
        if (mainScrollPane == null) return;
        boolean lockVerticalScroll =
                activeView == homeView ||
                activeView == logMoodView ||
                activeView == journalView ||
                activeView == historyView;

        mainScrollPane.setVbarPolicy(lockVerticalScroll
                ? ScrollPane.ScrollBarPolicy.NEVER
                : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setPannable(!lockVerticalScroll);
        if (lockVerticalScroll) mainScrollPane.setVvalue(0.0);
    }

    // Function: resetButtonStyles - Resets sidebar buttons to inactive style.
    private void resetButtonStyles() {
        Button[] btns = { homeBtn, logMoodBtn, journalBtn, gardenBtn, historyBtn, settingsBtn };
        for (Button b : btns) {
            if (b != null) {
                // Inactive: Transparent Background, Mauve Text
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #6A515E; -fx-font-weight: normal; -fx-alignment: center-left; -fx-padding: 12 10 12 30; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    // Function: handleLogout - Clears session/login cache and returns the user to login screen.
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            String user = UserSession.getUser();
            if (user != null) {
                prefs.remove(user + "_true_xp");
            }

            UserSession.cleanUserSession();
            java.util.prefs.Preferences.userRoot().node("com.solacea").remove("savedUsername");

            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/login-view.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Function: resolvePlantImageName - Builds plant image filename from type, stage, and emotion.
    private String resolvePlantImageName(int type, int stage, String emotion) {
        if (stage == 0 || type == 0 || "None".equalsIgnoreCase(emotion)) {
            return "0_0_None.jpg";
        }
        return String.format("%d_%d_%s.jpg", type, stage, emotion);
    }

    @FXML
    // Function: dismissAlert - Hides the wellbeing alert banner.
    private void dismissAlert() {
        if (wellbeingAlertBanner != null) {
            wellbeingAlertBanner.setVisible(false);
            wellbeingAlertBanner.setManaged(false);
        }
    }
}



