package heig.tb.jsmithfx.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages saving and loading of Smith Chart projects to and from JSON files.
 * Utilizes Jackson for serialization and deserialization.
 */
public class ProjectManager {

    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    private final ObjectMapper mapper;

    public ProjectManager() {
        this.mapper = JsonMapper.builder()
                .addModule(new Jdk8Module())
                .build();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }


    private File projectFile;

    private File getProjectFile() throws RuntimeException {
        if (projectFile == null) {
            throw new RuntimeException("No project file exists.");
        }
        return projectFile;
    }

    // Smith Project DTO
    record SmithProjectData(
            String projectName,
            double frequency,
            double zo,
            Complex loadImpedance,
            List<List<CircuitElement>> allCircuits
    ) {}

    public void resetProject(){
        this.projectFile = null;
    }

    /**
     * Save the current project from the ViewModel to the specified file. If no file is provided, saves to the last used project file.
     * @param viewModel The SmithChartViewModel containing the project data.
     * @param optFile Optional file to save the project to. If empty, saves to the last used project file.
     */
    public void saveProject(SmithChartViewModel viewModel, Optional<File> optFile) {
        try {
            File file = optFile.orElseGet(this::getProjectFile);

            List<List<CircuitElement>> circuitsData = new ArrayList<>();
            for (ObservableList<CircuitElement> circuit : viewModel.allCircuits) {
                circuitsData.add(new ArrayList<>(circuit));
            }

            SmithProjectData data = new SmithProjectData(
                    file.getName().replace(".jsmfx", ""),
                    viewModel.frequencyProperty().get(),
                    viewModel.zo.get(),
                    viewModel.loadImpedance.get(),
                    circuitsData
            );

            mapper.writeValue(file, data);
            this.projectFile = file;

            viewModel.setHasBeenSaved(true);
            viewModel.setIsModified(false);
            viewModel.setProjectName(data.projectName);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save project", e);
            DialogUtils.showErrorAlert(
                    "Save Error",
                    "Failed to save project:\n" + e.getMessage(),
                    SmithUtilities.getActiveStage());
        }
    }

    /**
     * Load a project from a file into the provided ViewModel. Saves the file reference for future saves.
     * @param viewModel The SmithChartViewModel to load the project into.
     * @param file The file from which to load the project.
     */
    public void loadProject(SmithChartViewModel viewModel, File file) {
        try {
            SmithProjectData data = mapper.readValue(file, SmithProjectData.class);

            viewModel.setProjectName(data.projectName);
            viewModel.zo.set(data.zo);
            viewModel.setFrequency(data.frequency);
            viewModel.loadImpedance.set(data.loadImpedance);

            // CLEAR existing circuits
            viewModel.allCircuits.clear();

            if (data.allCircuits != null) {
                for (List<CircuitElement> rawCircuitList : data.allCircuits) {

                    ObservableList<CircuitElement> observableCircuit = FXCollections.observableArrayList(
                            element -> new javafx.beans.Observable[]{ element.realWorldValueProperty() }
                    );

                    observableCircuit.addAll(rawCircuitList);
                    viewModel.allCircuits.add(observableCircuit);
                }
            }

            // If the file happened to have 0 circuits, ensure at least one exists
            if (viewModel.allCircuits.isEmpty()) {
                viewModel.addCircuit();
            }

            // Reset selection to the first circuit
            viewModel.circuitElementIndex.set(0);

            viewModel.setHasBeenSaved(true);
            viewModel.setIsModified(false);

            this.projectFile = file;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load project", e);
            DialogUtils.showErrorAlert(
                    "Load Error",
                    "Failed to load project:\n" + e.getMessage(),
                    SmithUtilities.getActiveStage());
        }
    }
}