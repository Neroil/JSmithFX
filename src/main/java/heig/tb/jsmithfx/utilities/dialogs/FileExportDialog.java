package heig.tb.jsmithfx.utilities.dialogs;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class FileExportDialog extends Dialog<File> {

    private final TextField folderPathField = new TextField();
    private final Button browseButton = new Button("Browse...");
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    public FileExportDialog() {
        setTitle("Export to Folder");
        setHeaderText("Select the folder to export the file to.");

        // Configure DirectoryChooser
        directoryChooser.setTitle("Select Export Folder");

        // Layout for folder path and browse button
        HBox folderSelectionBox = new HBox(5, folderPathField, browseButton);
        folderSelectionBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(folderPathField, Priority.ALWAYS);

        // Main layout
        VBox content = new VBox(10, folderSelectionBox);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Event handler for the browse button
        browseButton.setOnAction(event -> {
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
            if (button == ButtonType.OK) {
                String path = folderPathField.getText();
                if (path != null && !path.trim().isEmpty()) {
                    return new File(path);
                }
            }
            return null;
        });
    }
}