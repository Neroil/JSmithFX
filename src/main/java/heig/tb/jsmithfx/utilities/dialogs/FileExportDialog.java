package heig.tb.jsmithfx.utilities.dialogs;

import heig.tb.jsmithfx.utilities.DialogUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;

public class FileExportDialog extends Dialog<Pair<String, File>> {

    private final TextField folderPathField = new TextField();
    private final TextField fileNameField = new TextField();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    public FileExportDialog() {
        setTitle("Export to Folder");
        setHeaderText("Select the folder to export the file to.");

        // Configure DirectoryChooser
        directoryChooser.setTitle("Select Export Folder");

        // Layout for folder path and browse button
        HBox nameBox = new HBox(5, new Label("File name:"), fileNameField);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Button browseButton = new Button("Browse...");
        HBox folderSelectionBox = new HBox(5, browseButton, folderPathField);
        folderSelectionBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(folderPathField, Priority.ALWAYS);

        // Main layout
        VBox content = new VBox(10, nameBox, folderSelectionBox);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Event handler for the browse button
        browseButton.setOnAction(_ -> {
            File selectedDirectory = directoryChooser.showDialog(getOwner());
            if (selectedDirectory != null) {
                folderPathField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        // Enable OK button only if the text field is not empty
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                Bindings.isEmpty(folderPathField.textProperty())
        );

        // Convert the result to a File object when OK is clicked
        setResultConverter(button -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (button == ButtonType.OK) {
                String path = folderPathField.getText();
                String name = fileNameField.getText();

                if (path != null && !path.trim().isEmpty() && name != null && !name.trim().isEmpty()) {
                    String cleanedName = name.replaceAll("[\\\\/:*?\"<>|]", "");
                    cleanedName = cleanedName.replaceAll("\\s+", "_");
                    if (cleanedName.trim().isEmpty()) {

                        DialogUtils.showErrorAlert("Error", "The file name is invalid after sanitization.",stage);
                        return null;
                    }
                    return new Pair<>(cleanedName, new File(path));
                }
                else DialogUtils.showErrorAlert("Error", "Folder path and/or file name cannot be empty.", stage);
            }
            return null;
        });
    }
}