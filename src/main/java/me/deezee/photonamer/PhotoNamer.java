/*
 * Part of PhotoNamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import me.deezee.photonamer.format.NamerFormat;
import me.deezee.photonamer.format.NamerFormatCondition;
import me.deezee.photonamer.process.*;
import me.deezee.photonamer.ui.NamerMenuBar;
import me.deezee.photonamer.ui.Resources;
import me.deezee.photonamer.ui.VarTextCompletionCaller;
import me.deezee.photonamer.util.Formatting;
import me.deezee.photonamer.util.Printer;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class PhotoNamer {

    public static final class StartApplication extends Application {

        private static final String DEFAULT_PICS_PATH       = System.getProperty("user.home") + "/Pictures";
        private static final String PREVIEW_CHECK_INPUT     = "Please select a valid input folder";
        private static final String PREVIEW_CHECK_FORMAT    = "Please input a format";
        private static final String PREVIEW_CHECK_OUTPUT    = "Please select a valid output folder or don't use it at all";
        private static final Border DEFAULT_BORDER          = new Border(new BorderStroke(
                Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));

        private static StartApplication instance = null;

        private final StackPane root                = new StackPane();
        private final DirectoryChooser dirChooser   = new DirectoryChooser();
        private final TextField inputDirField       = new TextField();
        private final TextField outputDirField      = new TextField();
        private final TextField formatField         = new TextField();
        private final Label     previewLabel        = new Label("Preview");
        private final Label     outputLabel         = new Label("Output Folder");
        private final Button    rename              = new Button("Rename");
        private final Button    undo                = new Button("Undo");
        private final CheckBox  outputDir           = new CheckBox();
        private final CheckComboBox<String> ccb     = new CheckComboBox<>();

        private NamerFormat currentFormat = null;

        // TODO: Switch to FXML and CSS styling
        @Override
        public void start(Stage primaryStage) {
            if (instance != null) throw new IllegalStateException("Application has already started");
            instance = this;

            // Set up logger
            Printer.setPrefix("");
            Printer.setDebugPrefix(" -> ");
            Printer.log(Printer.BORDER);
            Printer.enableDebugging(true);

            VBox box = new VBox();
            VBox paddedBox = new VBox();
            paddedBox.setPadding(new Insets(20));
            paddedBox.setAlignment(Pos.CENTER);

            MenuBar menu = new NamerMenuBar(this);

            // Input path section
            TitledPane inputPathPane = new TitledPane();
            {
                GridPane grid = new GridPane();
                grid.setVgap(10);
                grid.setHgap(10);

                inputPathPane.setPadding(new Insets(0, 0, 10, 0));
                inputPathPane.setText("Name Formatter");
                inputPathPane.setContent(grid);

                // Input directory section
                {
                    // Input source folder label
                    grid.add(new Label("Image Folder"), 0, 0);

                    // Input location field
                    inputDirField.setPrefWidth(300);
                    inputDirField.setPromptText("Location");
                    inputDirField.textProperty().addListener((obs, oldText, newText) -> {
                        if (validateInput()) {
                            // If correct path, then remember choice
                            dirChooser.setInitialDirectory(new File(inputDirField.getText()));
                        }
                    });
                    grid.add(inputDirField, 1, 0);

                    // Directory choose button
                    dirChooser.setInitialDirectory(new File(DEFAULT_PICS_PATH));
                    Button inputButton = new Button("Select");
                    inputButton.setOnAction(event -> {
                        File selectedDir = dirChooser.showDialog(primaryStage);

                        if (selectedDir != null) {
                            inputDirField.setText(selectedDir.getAbsolutePath());
                            validateInput();
                        }
                    });
                    grid.add(inputButton, 2, 0);
                }

                // Name format styling section
                {
                    // Name format label
                    grid.add(new Label("Name Format:"), 0, 1);

                    // Name format field
                    formatField.setPrefWidth(300);
                    formatField.setPromptText("Exclude extensions");
                    formatField.textProperty().addListener((obs, oldText, newText) -> validateInput());
                    // Add auto-complete for variables
                    VarTextCompletionCaller.createVarCompleter(this);
                    grid.add(formatField, 1, 1);

                    // Formatter rules button
                    Button rulesButton = new Button("Formatter Rules");
                    rulesButton.setOnAction(event -> {
                        Alert rules = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.CLOSE);
                        ((Stage) rules.getDialogPane().getScene().getWindow()).getIcons().add(Resources.MAIN_ICON);
                        rules.setTitle("Formatter Rules");
                        rules.setHeaderText("Type the format of the naming convention for the photos.");
                        rules.initModality(Modality.WINDOW_MODAL);

                        // Alert contents
                        VBox alertContents = new VBox();

                        Font bold = Font.font(null, FontWeight.BOLD, -1);
                        Text top = new Text("A file name can't contain any of the following characters:"
                                + "  \\  /  :  *  ?  \"  <  >  |\n\n");

                        GridPane varGrid = new GridPane();
                        varGrid.setHgap(20);
                        varGrid.add(new Text("Use the following variables:"), 0, 0, 2, 1);

                        ScrollPane scrollPane = new ScrollPane();
                        scrollPane.setBorder(DEFAULT_BORDER);
                        scrollPane.setPadding(new Insets(5));
                        scrollPane.setContent(varGrid);
                        scrollPane.setPrefViewportHeight(300);

                        List<Integer> separatorLines = Arrays.asList(2, 15, 28);

                        int row = 1;
                        for (Map.Entry<NamerFormat.Var, NamerFormatCondition> entry : NamerFormat.getVariables().entrySet()) {

                            // Insert line breaks appropriately to separate list
                            if (separatorLines.contains(row))
                                varGrid.add(new Text(""), 0, row++);

                            NamerFormat.Var var = entry.getKey();
                            varGrid.add(new TextFlow(new Text(" - "), new Text("$" + var.getName()) {{
                                setFont(bold);
                            }}), 0, row);
                            varGrid.add(new Text(var.getDescription()), 1, row++);
                        }

                        LinkedList<Text> bottom = Lists.newLinkedList();

                        bottom.add(new Text("\nExample:\t\t"));
                        bottom.add(new Text("$seq_id. My photo from $c_day/$c_mon/$c_year @ $c_hour2.$c_min $c_ampm") {{
                            setFont(bold);
                        }});
                        bottom.add(new Text("\nBecomes:\t\t"));
                        bottom.add(new Text("4. My photo from 9/07/2020 @ 4.58 pm.jpg") {{
                            setFont(bold);
                        }});

                        alertContents.getChildren().addAll(top, scrollPane, new TextFlow(bottom.toArray(new Text[0])));

                        rules.getDialogPane().setContent(alertContents);
                        rules.show();
                    });
                    grid.add(rulesButton, 2, 1);

                    grid.add(new Text("(Do not include the extension)") {{
                        setFont(Font.font(null, FontPosture.ITALIC, -1));
                    }}, 1, 2, 2, 1);
                }

                // Preview section
                {
                    // Preview label
                    grid.add(new Label("Preview"), 0, 3);

                    // Preview based on first photo
                    grid.add(previewLabel, 1, 3, 2, 1);
                }
            }

            Button outputButton = new Button("Select");
            CheckBox includeSubDirs = new CheckBox();
            CheckBox dateTimeTakenOnly = new CheckBox();
            ComboBox<NamerFormat.Var> groupBy = new ComboBox<>();

            // Advanced settings section
            TitledPane advSettingsPane = new TitledPane();
            {
                GridPane grid = new GridPane();
                grid.setVgap(10);
                grid.setHgap(10);

                advSettingsPane.setPadding(new Insets(0, 0, 10, 0));
                advSettingsPane.collapsibleProperty().setValue(true);
                advSettingsPane.setText("Advanced Settings");
                advSettingsPane.setContent(grid);

                // Checkboxes
                {
                    outputDir.setPadding(new Insets(0, 80, 0, 0));
                    outputDir.setOnAction(event -> {
                        boolean enable = outputDir.isSelected();
                        outputLabel.setDisable(!enable);
                        outputDirField.setDisable(!enable);
                        outputButton.setDisable(!enable);
                        outputDirField.clear();

                        validateInput();
                    });

                    GridPane checkBoxGrid = new GridPane();
                    checkBoxGrid.setVgap(10);
                    checkBoxGrid.setHgap(20);
                    grid.add(checkBoxGrid, 0, 0, 3, 1);

                    // Text Columns
                    checkBoxGrid.add(new Label("Include Sub-Folders"), 0, 0);
                    checkBoxGrid.add(new Label("Custom Output Folder"), 0, 1);

                    checkBoxGrid.add(new Label("Datetime Taken Only"), 2, 0);
                    //checkBoxGrid.add(new Label("<Something Else>"), 2, 1);

                    // Checkbox Columns
                    checkBoxGrid.add(includeSubDirs, 1, 0);
                    checkBoxGrid.add(outputDir, 1, 1);

                    checkBoxGrid.add(dateTimeTakenOnly, 3, 0);
                    //checkBoxGrid.add(new CheckBox(), 3, 1);
                }

                // Output directory
                {
                    // Output source folder label
                    outputLabel.setDisable(true);
                    grid.add(outputLabel, 0, 1);

                    // Output location field
                    outputDirField.setDisable(true);
                    outputDirField.setPrefWidth(300);
                    outputDirField.setPromptText("Location");
                    outputDirField.textProperty().addListener((obs, oldText, newText) -> validateInput());
                    grid.add(outputDirField, 1, 1);

                    // Directory choose button
                    outputButton.setDisable(true);
                    outputButton.setOnAction(event -> {
                        File selectedDir = dirChooser.showDialog(primaryStage);

                        if (selectedDir != null) {
                            outputDirField.setText(selectedDir.getAbsolutePath());
                        }
                    });
                    grid.add(outputButton, 2, 1);
                }

                // Image file extensions
                {
                    grid.add(new Label("Image Extensions"), 0, 2);

                    // Populate with default extensions
                    ccb.getItems().addAll(NamerFormat.DEFAULT_ALLOWED_EXTS);
                    ccb.getCheckModel().checkAll();
                    ccb.setTitle("<Select>");
                    ccb.setShowCheckedCount(true);

                    grid.add(ccb, 1, 2);
                }

                // Group by folder
                {
                    grid.add(new Label("Folder Grouping"), 0, 3);

                    // Initial null value represents no grouping
                    groupBy.getItems().addAll(NamerFormat.Var.availableGroupings());
                    groupBy.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(NamerFormat.Var object) {
                            if (object == null) return "<Select>";
                            return object.getVariable();
                        }

                        @Override
                        public NamerFormat.Var fromString(String string) {
                            if (string == null || string.equals("<Select>")) return null;
                            return NamerFormat.Var.valueOf(string.substring(1)); // Remove initial $ char
                        }
                    });

                    grid.add(groupBy, 1, 3);
                }
            }

            // Bottom buttons
            HBox buttons = new HBox();
            {
                // Rename button
                rename.setDisable(true);
                rename.setOnAction(event -> {
                    Path src = Path.of(inputDirField.getText());
                    Path tgt = Strings.isNullOrEmpty(outputDirField.getText())
                            ? src : Path.of(outputDirField.getText());

                    try {
                        currentFormat.setGrouping(groupBy.getValue());
                    } catch (NamerProcessException e) {
                        alertError(e);
                    }

                    NamerSettings settings = new NamerSettings()
                            .setDirectory(src)
                            .setOutputDirectory(tgt)
                            .setFormatting(currentFormat)
                            .setIncludeSubDirectories(includeSubDirs.isSelected())
                            .setFilterDateTimeTakenOnly(dateTimeTakenOnly.isSelected())
                            .setImageExtensions(ccb.getCheckModel().getCheckedItems());

                    NamerProcessFinishTask onFinish = result -> {
                        NamerProcessResult.Type resType = result.getType();
                        String typeStr = result.getType().toString().toLowerCase();

                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.CLOSE);
                        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(Resources.MAIN_ICON);
                        alert.setTitle(Formatting.capitalise(typeStr));
                        alert.setHeaderText(result.getType().getMessage());

                        Label content = new Label(String.format("%d images have been renamed in %.2fs.",
                                result.getAmountChanged(), result.getTimeCompleted(TimeUnit.MILLISECONDS) / 1000f));
                        content.setPadding(new Insets(10));

                        alert.getDialogPane().setContent(content);
                        alert.show();

                        if (resType == NamerProcessResult.Type.SUCCESS) {
                            undo.setDisable(false);
                            return true;
                        }

                        return false;
                    };

                    try {
                        NamerProcessFactory.getInstance().newProcess(settings, onFinish).start();
                    } catch (NamerProcessException e) {
                        alertError(e);
                    }
                });

                // Undo button
                undo.setDisable(true);
                undo.setOnAction(event -> {
                    Optional<NamerProcess> process = NamerProcessFactory.getInstance().getCurrentProcess();
                    if (process.isPresent()) {
                        try {
                            process.get().undo();
                        } catch (NamerProcessException e) {
                            alertError(e);
                        }
                    }
                });

                buttons.setSpacing(20);
                buttons.setAlignment(Pos.CENTER);
                buttons.getChildren().addAll(rename, undo);
            }

            paddedBox.getChildren().addAll(inputPathPane, advSettingsPane, buttons);
            box.getChildren().addAll(menu, paddedBox);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setMaxWidth(1500);
            scrollPane.setMaxHeight(1000);
            scrollPane.setContent(box);

            root.getChildren().add(scrollPane);

            primaryStage.getIcons().add(Resources.MAIN_ICON);
            primaryStage.setTitle("PhotoNamer");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.sizeToScene();
            primaryStage.show();
        }

        public TextField getFormatField() {
            return formatField;
        }

        private boolean validateInput() {
            String input = inputDirField.getText();
            String format = formatField.getText();

            fail: {
                // Ensure directory input is set
                if (Strings.isNullOrEmpty(input)) {
                    previewLabel.setText(PREVIEW_CHECK_INPUT);
                    break fail;
                }

                // Ensure input path is valid
                Path in;
                try {
                    in = Path.of(input);
                    if (!Files.exists(in) || !Files.isDirectory(in)) {
                        previewLabel.setText(PREVIEW_CHECK_INPUT);
                        break fail;
                    }
                } catch (InvalidPathException ignore) {
                    previewLabel.setText(PREVIEW_CHECK_INPUT);
                    break fail;
                }

                // Ensure format input is set
                if (Strings.isNullOrEmpty(format)) {
                    previewLabel.setText(PREVIEW_CHECK_FORMAT);
                    break fail;
                }

                // Try formatting one of the files as an example
                // Update NamerFormat cache
                try {
                    currentFormat = new NamerFormat(format, null);
                } catch (NamerProcessException e) {
                    previewLabel.setText(PREVIEW_CHECK_FORMAT);
                    break fail;
                }

                // Ensure output path is valid
                if (outputDir.isSelected()) {
                    if (Strings.isNullOrEmpty(outputDirField.getText())) {
                        previewLabel.setText(PREVIEW_CHECK_OUTPUT);
                        break fail;
                    }

                    try {
                        Path out = Path.of(outputDirField.getText());
                        if (!Files.exists(out) || !Files.isDirectory(out)) {
                            previewLabel.setText(PREVIEW_CHECK_OUTPUT);
                            break fail;
                        }
                    } catch (InvalidPathException ignored) {
                        previewLabel.setText(PREVIEW_CHECK_OUTPUT);
                        break fail;
                    }
                }

                // Attempt to grab one image
                try {
                    Optional<Path> img = Files.walk(in, 1)
                            .filter(file -> NamerSettings.isImage(file, NamerFormat.DEFAULT_ALLOWED_EXTS))
                            .findFirst();

                    if (img.isPresent()) {
                        // Generate new file name as an example
                        Pair<String, String> comp = new PhotoWrapper(img.get()).format(currentFormat, 0);
                        previewLabel.setText(comp.getKey() + "." + comp.getValue());
                    }
                } catch (Exception e) {
                    alertError(e);
                }

                // Validation passed
                rename.setDisable(false);
                return true;
            }

            // Button only enables when both text fields were completed and correct
            rename.setDisable(true);
            return false;
        }

        public static StartApplication appInstance() {
            return instance;
        }
    }

    public static void alertError(final Exception exception) {
        // Run on main thread
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);

            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(Resources.MAIN_ICON);
            alert.setTitle(exception.getClass().getName());
            alert.setHeaderText("An error has occurred!");
            alert.setContentText(exception.getMessage());

            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            exception.printStackTrace(print);

            TextArea stacktrace = new TextArea(writer.toString());
            stacktrace.setEditable(false);
            stacktrace.setWrapText(false);
            GridPane.setVgrow(stacktrace, Priority.ALWAYS);
            GridPane.setHgrow(stacktrace, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.add(new Label("Stacktrace:"), 0, 0);
            expContent.add(stacktrace, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        // Let JavaFX handle the launch
        Application.launch(StartApplication.class, args);
    }

    // Disable initialisation
    private PhotoNamer() {}
}