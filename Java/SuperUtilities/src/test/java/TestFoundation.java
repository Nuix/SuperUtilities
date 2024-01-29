import com.nuix.innovation.enginewrapper.NuixEngine;
import com.nuix.innovation.enginewrapper.NuixLicenseResolver;
import net.lingala.zip4j.ZipFile;
import nuix.Case;
import nuix.Item;
import nuix.itemtype.ItemTypeBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;


public class TestFoundation {
    protected static Logger log;
    protected static File testOutputDirectory;
    protected static File testDataDirectory;

    public boolean isDebuggerAttached() {
        boolean debuggerAttached = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(arg -> arg.contains("jdwp=") && arg.contains("suspend=y"));
        System.out.println("Debugger Attached: " + debuggerAttached);
        return debuggerAttached;
    }

    public void extractTestDataZip(String fileName) throws Exception {
        File testDataZip = new File(testDataDirectory, fileName);
        if (!testDataZip.exists()) {
            throw new IOException(String.format("Missing '%s', please download from %s and place in the directory %s",
                    testDataZip.getName(), "TODO", testDataDirectory));
        }

        System.out.println(String.format("Extracting %s to %s", testDataZip, testOutputDirectory));
        new ZipFile(testDataZip).extractAll(testOutputDirectory.getAbsolutePath());
        System.out.println("Extraction completed");
    }

    @BeforeAll
    public static void setup() throws Exception {
        // Set default level to INFO
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.INFO);
        ctx.updateLoggers();

        log = LoggerFactory.getLogger("Connector Tests");

        System.out.println("JVM Arguments:");
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = bean.getInputArguments();
        for (String arg : jvmArgs) {
            System.out.println(arg);
        }

        System.out.println("Environment Variables:");
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String varName = entry.getKey();
            String varNameLower = varName.toLowerCase();
            String varValue = entry.getValue();
            if(varNameLower.contains("password") || varNameLower.contains("secret") || varNameLower.contains("token")) {
                varValue = varValue.replaceAll(".","*");
            }
            System.out.println(String.format("%s => %s", varName, varValue));
        }

        testOutputDirectory = new File(System.getenv("TEST_OUTPUT_DIRECTORY"));
        testDataDirectory = new File(System.getenv("TEST_DATA_DIRECTORY"));
    }

    @AfterAll
    public static void breakdown() {
        NuixEngine.closeGlobalContainer();
    }

    public NuixEngine constructNuixEngine(String... additionalRequiredFeatures) {
        List<String> features = List.of("CASE_CREATION");
        if (additionalRequiredFeatures != null && additionalRequiredFeatures.length > 0) {
            features.addAll(List.of(additionalRequiredFeatures));
        }

        NuixLicenseResolver caseCreationCloud = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures(features);

        return NuixEngine.usingFirstAvailableLicense(caseCreationCloud)
                .setEngineDistributionDirectoryFromEnvVar()
                .setLogDirectory(new File(testOutputDirectory, "Logs_" + System.currentTimeMillis()));
    }
}
