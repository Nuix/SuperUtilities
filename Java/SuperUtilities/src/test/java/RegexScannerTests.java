import com.nuix.innovation.enginewrapper.NuixEngine;
import com.nuix.superutilities.regex.RegexMatch;
import com.nuix.superutilities.regex.RegexScanner;
import nuix.Case;
import nuix.Item;
import nuix.Utilities;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RegexScannerTests extends TestFoundation {
    @Test
    public void test() throws Exception {
        extractTestDataZip("TestCase.zip");
        File testCaseDir = new File(testOutputDirectory, "TestCase");
        File outputCsv = new File(testOutputDirectory, "RegexScannerTest.csv");
        CSVFormat csvFormat = CSVFormat.EXCEL;

        try (NuixEngine nuixEngine = constructNuixEngine()) {
            Utilities utilities = nuixEngine.getUtilities();
            try (Case nuixCase = utilities.getCaseFactory().open(testCaseDir, Map.of("migrate", true))) {
                List<Item> items = nuixCase.search("");

                RegexScanner scanner = new RegexScanner();
                scanner.setScanProperties(true);
                scanner.setScanContent(true);
                scanner.setCaseSensitive(false);
                scanner.setCaptureContextualText(true);
                scanner.setContextSize(30);

                scanner.addPattern("Computer","computer[s]?");

                try (
                        FileWriter fileWriter = new FileWriter(outputCsv, StandardCharsets.UTF_8);
                        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)
                ) {
                    csvPrinter.printRecord(
                            "GUID",
                            "Item Name",
                            "ItemKind",
                            "Expression",
                            "Location",
                            "Value",
                            "ValueContext",
                            "Match Start",
                            "Match End");

                    scanner.scanItemsParallel(items, itemRegexMatchCollection -> {
                       Item item = itemRegexMatchCollection.getItem(nuixCase);
                       for(RegexMatch match : itemRegexMatchCollection.getMatches()) {
                           try {
                               csvPrinter.printRecord(item.getGuid(),
                                       item.getLocalisedName(),
                                       item.getType().getKind().getName(),
                                       match.getExpression(),
                                       match.getLocation(),
                                       match.getValue(),
                                       match.getValueContext(),
                                       match.getMatchStart(),
                                       match.getMatchEnd());
                           } catch (IOException e) {
                               throw new RuntimeException(e);
                           }
                       }
                    });
                }
            }
        }
    }
}
