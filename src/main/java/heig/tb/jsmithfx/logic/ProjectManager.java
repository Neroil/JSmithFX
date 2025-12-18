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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            List<CircuitElement> circuitElements
    ) {}

    public void resetProject(){
        this.projectFile = null;
    }

    public void saveProject(SmithChartViewModel viewModel, Optional<File> optFile) {
        try {
            File file = optFile.orElseGet(this::getProjectFile);

            SmithProjectData data = new SmithProjectData(
                    file.getName().replace(".jsmfx", ""),
                    viewModel.frequencyProperty().get(),
                    viewModel.zo.get(),
                    viewModel.loadImpedance.get(),
                    viewModel.circuitElements.get()
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

    public void loadProject(SmithChartViewModel viewModel, File file) {
        try {
            SmithProjectData data = mapper.readValue(file, SmithProjectData.class);

            viewModel.setProjectName(data.projectName);
            viewModel.zo.set(data.zo);
            viewModel.setFrequency(data.frequency);

            viewModel.circuitElements.clear();
            viewModel.circuitElements.addAll(data.circuitElements);
            viewModel.loadImpedance.set(data.loadImpedance);

            viewModel.setHasBeenSaved(true);
            viewModel.setIsModified(false);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load project", e);
            DialogUtils.showErrorAlert(
                    "Load Error",
                    "Failed to load project:\n" + e.getMessage(),
                    SmithUtilities.getActiveStage());
        }
    }
}