module heig.tb.jsmithfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires atlantafx.base;
    requires com.pixelduke.fxthemes;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires annotations;
    requires java.logging;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;

    opens heig.tb.jsmithfx to javafx.fxml;
    exports heig.tb.jsmithfx;
    exports heig.tb.jsmithfx.utilities;
    opens heig.tb.jsmithfx.utilities to javafx.fxml;
    exports heig.tb.jsmithfx.model;
    opens heig.tb.jsmithfx.model to javafx.fxml;

    // Open the specific package containing Capacitor, Inductor, etc. to Jackson
    opens heig.tb.jsmithfx.model.Element to com.fasterxml.jackson.databind;
    exports heig.tb.jsmithfx.model.Element.TypicalUnit;
    opens heig.tb.jsmithfx.model.Element.TypicalUnit to javafx.fxml;
    exports heig.tb.jsmithfx.view;
    opens heig.tb.jsmithfx.view to javafx.fxml;
    opens heig.tb.jsmithfx.logic to com.fasterxml.jackson.databind;
}
