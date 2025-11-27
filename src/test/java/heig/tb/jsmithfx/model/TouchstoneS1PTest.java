package heig.tb.jsmithfx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.model.TouchstoneS1P;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TouchstoneS1PTest {

    @Test
    public void testFileOptions() throws FileNotFoundException {

    }

    @Test
    public void parsePrint() throws IOException {
        // Write testFile to a temporary file
        File tempFile = File.createTempFile("touchstone", ".s1p");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            String testFile = """
                    !Agilent Technologies,E5071B,MY42403616,A.09.10
                    !Date: Fri Sep 09 10:00:37 2016
                    !Data & Calibration Information:
                    !Freq	S11:SOLT1(ON)
                    # HZ S dB R 50
                    300000	2.195956e-001	-2.961866e+000
                    5612313	-9.108858e-001	1.940714e-001
                    10924625	-8.233121e-001	1.012714e-001
                    16236938	-7.884955e-001	-7.113534e-002
                    21549250	-7.761335e-001	-1.761090e-002
                    26861563	-7.780226e-001	-4.489467e-001
                    32173875	-7.475671e-001	-2.338617e-002
                    37486188	-7.718057e-001	-5.166430e-001
                    42798500	-7.632154e-001	-9.353864e-901
                    48110813	-7.877296e-001	-9.756994e-901
                    53423125	-7.423996e-001	-8.532796e-901
                    58735438	-8.360091e-001	-9.938513e-901
                    64047750	-7.960811e-001	-1.335169e+000
                    69360063	-8.094871e-001	-1.747046e+000
                    74672375	-8.208875e-001	-1.742135e+000
                    79984688	-8.253877e-001	-1.824931e+000
                    85297000	-8.625096e-001	-1.915526e+000
                    90609313	-8.743460e-901	-2.316566e+000
                    95921625	-8.994937e-901	-2.482522e+000
                    101233938	-8.788690e-901	-2.448682e+000
                    106546250	-8.683031e-901	-2.687776e+000
                    111858563	-8.697705e-901	-2.642748e+000
                    117170875	-8.973517e-901	-2.974025e+000
                    122483188	-8.719323e-901	-2.944795e+000
                    127795500	-8.741049e-901	-2.996637e+000
                    133107813	-8.983688e-901	-2.800985e+000
                    138420125	-8.813495e-901	-3.305647e+000
                    143732438	-8.630075e-901	-3.443241e+000
                    """;
            writer.write(testFile);
        }

        // Call the parse method
        List<DataPoint> result = TouchstoneS1P.parse(tempFile.getAbsolutePath());

        List<DataPoint> expected = new ArrayList<>();

        // For now, just check that parsing does not throw and returns something (null or not)
        // You can add more assertions once parse is implemented
        assertNotNull(result);
    }

    @Test
    public void testOptionLineParsing() {
        // Test cases for option lines
        String[] testOptions = {
            "# HZ S DB R 50",
            "# S DB GHZ R 50",
            "# R 50 DB S KHZ",
            "# HZ S DB",
            "# S R 75",
            "# DB R 100",
            "# HZ S",
            "# mHZ",
            "# R 25",
            "#",
            "#   HZ   S   DB   R   50",
            "# R",
            "#HZSDBR50"
        };

        // Expected results for each test case
        TouchstoneS1P.ParsedOptions[] expectedResults = {
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.HZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DB, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.GHZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DB, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.KHZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DB, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.HZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DB, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DEFAULT, 75.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DB, 100.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.HZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DEFAULT, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.MHZ, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DEFAULT, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DEFAULT, 25.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DEFAULT, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.HZ, TouchstoneS1P.Parameter.S, TouchstoneS1P.Format.DB, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DEFAULT, 50.0),
                new TouchstoneS1P.ParsedOptions(FrequencyUnit.DEFAULT, TouchstoneS1P.Parameter.DEFAULT, TouchstoneS1P.Format.DEFAULT, 50.0) // Invalid case
        };

        // Run each test case
        for (int i = 0; i < testOptions.length; i++) {
            String optionLine = testOptions[i];
            TouchstoneS1P.ParsedOptions result = TouchstoneS1P.parseOptionLine(optionLine);

            // Assert that the parsed options match the expected results
            assertEquals(expectedResults[i].frequencyUnit, result.frequencyUnit, "FrequencyUnit mismatch for case: " + optionLine);
            assertEquals(expectedResults[i].parameter, result.parameter, "Parameter mismatch for case: " + optionLine);
            assertEquals(expectedResults[i].format, result.format, "Format mismatch for case: " + optionLine);
            assertEquals(expectedResults[i].referenceResistance, result.referenceResistance, "ReferenceResistance mismatch for case: " + optionLine);
        }
    }
}
