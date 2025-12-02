package heig.tb.jsmithfx.model;

//Source on how to use it comes from here :
//https://www.kirkbymicrowave.co.uk/Support/FAQ/What-is-a-Touchstone-file/
//https://docs.keysight.com/display/genesys2010/Touchstone+Format

import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.Complex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/* Example of a file
!Agilent Technologies,E5071B,MY42403616,A.09.10
!Date: Fri Sep 09 10:00:37 2016
!Data & Calibration Information:
!Freq	S11:SOLT1(ON)
# Hz S dB R 50
300000	2.195956e-001	-2.961866e+000
5612313	-9.108858e-001	1.940714e-001
10924625	-8.233121e-001	1.012714e-001
16236938	-7.884955e-001	-7.113534e-002
21549250	-7.761335e-001	-1.761090e-002
26861563	-7.780226e-001	-4.489467e-001
32173875	-7.475671e-001	-2.338617e-002
 */
public class TouchstoneS1P {

    enum Parameter {
        S, Y, Z, H, G;

        public static final Parameter DEFAULT = S;
    }

    enum Format {
        DB, MA, RI;

        public static final Format DEFAULT = MA;
    }

    static class ParsedOptions {
        FrequencyUnit frequencyUnit;
        Parameter parameter;
        Format format;
        double referenceResistance;

        ParsedOptions(FrequencyUnit frequencyUnit, Parameter parameter, Format format, double referenceResistance) {
            this.frequencyUnit = frequencyUnit;
            this.parameter = parameter;
            this.format = format;
            this.referenceResistance = referenceResistance;
        }
    }

    public static ParsedOptions parseOptionLine(String optionLine) {
        FrequencyUnit frequencyUnit = FrequencyUnit.DEFAULT;
        Parameter parameter = Parameter.DEFAULT;
        Format format = Format.DEFAULT;
        double referenceResistance = 50.0;

        String[] tokens = optionLine.split("\\s+");
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            try {
                if (Arrays.stream(FrequencyUnit.values())
                          .map(Enum::name)
                          .anyMatch(token::equalsIgnoreCase)) {
                    frequencyUnit = FrequencyUnit.valueOf(token.toUpperCase());
                } else if (Arrays.stream(Parameter.values())
                                 .map(Enum::name)
                                 .anyMatch(token::equalsIgnoreCase)) {
                    parameter = Parameter.valueOf(token.toUpperCase());
                } else if (Arrays.stream(Format.values())
                                 .map(Enum::name)
                                 .anyMatch(token::equalsIgnoreCase)) {
                    format = Format.valueOf(token.toUpperCase());
                } else if (token.equalsIgnoreCase("R")) {
                    if (i + 1 < tokens.length) {
                        try {
                            referenceResistance = Double.parseDouble(tokens[++i]); // Next token is the value
                        } catch (NumberFormatException e) {
                            System.out.println("Warning: Invalid value for R, using default: " + referenceResistance);
                        }
                    } else {
                        System.out.println("Warning: Missing value for R, using default: " + referenceResistance);
                    }
                }
            } catch (Exception e) {
                System.out.println("Warning: Unrecognized or malformed token: " + token);
            }
        }

        return new ParsedOptions(frequencyUnit, parameter, format, referenceResistance);
    }

    public static List<DataPoint> parse(File file) {
        // Set up the defaults in case there's no # in the file
        FrequencyUnit frequencyUnit = FrequencyUnit.DEFAULT;
        Parameter parameter = Parameter.DEFAULT;
        Format format = Format.DEFAULT;
        double referenceResistance = 50.0; // Default reference resistance

        List<DataPoint> resList = new LinkedList<>();
        int index = 1;

        try (Scanner myReader = new Scanner(file)) {
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();

                if (data.startsWith("!")) continue; // Ignore comment lines

                if (data.startsWith("#")) {
                    ParsedOptions options = parseOptionLine(data);
                    frequencyUnit = options.frequencyUnit;
                    parameter = options.parameter;
                    format = options.format;
                    referenceResistance = options.referenceResistance;

                } else if (data.matches(("^\\s*\\d+.*"))){ //It's a number
                    String[] parts = data.split("\\s+");
                    if (parts.length >= 3) {
                        double frequency = Double.parseDouble(parts[0]) * frequencyUnit.getFactor();
                        double val1 = Double.parseDouble(parts[1]);
                        double val2 = Double.parseDouble(parts[2]);

                        Complex rawValue = calculateComplexValue(val1, val2, format);

                        Complex impedance = calculateImpedance(rawValue, referenceResistance, parameter);

                        Complex trueGamma = calculateGammaFromZ(impedance, referenceResistance);

                        resList.add(new DataPoint(frequency, "S1P" + index++,
                                impedance, trueGamma, calculateVSWR(trueGamma), calculateReturnLoss(trueGamma)));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return resList;
    }

    /**
     * Export data points to a Touchstone S1P file. The data points are exported in S MA R format.
     * @param toExport the list of data points to export
     * @param z0 the reference impedance
     * @param freqUnit the frequency unit to use in the export
     * @param destination the destination file
     */
    public static void export(List<DataPoint> toExport, double z0, FrequencyUnit freqUnit, File destination) throws IOException {

        if (toExport == null || toExport.isEmpty()) {
            System.err.println("No data points to export.");
            return;
        }

        // Reorder data points by frequency
        toExport.sort(Comparator.comparingDouble(DataPoint::getFrequency));

        // Build the export string
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(freqUnit.name()).append(" S MA R ").append(z0).append("\n");

        sb.append("! Exported data from JSmithFX\n");
        sb.append("! Freq \t MagS11 \t AngS11\n");

        for (DataPoint dataPoint : toExport) {
            double freqInUnit = dataPoint.getFrequency() / freqUnit.getFactor();

            Complex gamma = dataPoint.getGamma();

            double magnitude = gamma.magnitude();
            double angleDeg = Math.toDegrees(gamma.angle());

            sb.append(String.format("%.6e", freqInUnit)).append("\t")
                    .append(String.format("%.6e", magnitude)).append("\t")
                    .append(String.format("%.6e", angleDeg)).append("\n");
        }

        // Write in file
        try (java.io.FileWriter writer = new java.io.FileWriter(destination)) {
            writer.write(sb.toString());
        } catch (java.io.IOException e) {
            System.err.println("Error while exporting to S1P file ! : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static Complex calculateComplexValue(double v1, double v2, Format format) {
        return switch (format) {
            case DB -> {
                double magnitude = Math.pow(10, v1 / 20.0);
                double angleRad = Math.toRadians(v2);
                yield new Complex(magnitude * Math.cos(angleRad), magnitude * Math.sin(angleRad));
            }
            case MA -> {
                double angleRad = Math.toRadians(v2);
                yield new Complex(v1 * Math.cos(angleRad), v1 * Math.sin(angleRad));
            }
            case RI -> new Complex(v1, v2);
        };
    }

    private static Complex calculateImpedance(Complex input, double z0, Parameter inputType) {
        Complex one = new Complex(1, 0);
        return switch (inputType) {
            case S -> {
                Complex numerator = one.add(input);
                Complex denominator = one.subtract(input);
                yield numerator.dividedBy(denominator).multiply(z0);
            }
            case Y -> new Complex(1.0, 0).dividedBy(input);
            case Z -> input; // already Z
            default -> new Complex(0,0);
        };
    }

    private static double calculateVSWR(Complex gamma) {
        double magnitude = gamma.magnitude();
        return (1 + magnitude) / (1 - magnitude);
    }

    private static double calculateReturnLoss(Complex gamma) {
        return -20 * Math.log10(gamma.magnitude());
    }

    private static Complex calculateGammaFromZ(Complex z, double z0) {
        // Gamma = (Z - Z0) / (Z + Z0)
        Complex z0Complex = new Complex(z0, 0);
        return z.subtract(z0Complex).dividedBy(z.add(z0Complex));
    }
}
