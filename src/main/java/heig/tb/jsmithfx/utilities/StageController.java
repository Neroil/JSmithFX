package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.SmithChartViewModel;
import javafx.stage.Stage;

public class StageController {

    private final SmithChartViewModel viewModel;
    private final Stage stage;

    public StageController(Stage stage){
        this.viewModel = SmithChartViewModel.getInstance();
        this.stage = stage;

        updateTitle();

        viewModel.projectNameProperty().addListener((_, _, _) -> updateTitle());
        viewModel.isModifiedProperty().addListener((_, _, _) -> updateTitle());
    }

    private void updateTitle() {
        String title = "JSmithFX - " + viewModel.projectNameProperty().get();
        if (viewModel.isModifiedProperty().get()) {
            title += "*";
        }
        stage.setTitle(title);
    }
}
