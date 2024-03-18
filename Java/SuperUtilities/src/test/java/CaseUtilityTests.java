import com.nuix.superutilities.cases.CaseUtility;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseUtilityTests extends TestFoundation {
    @Test
    public void testCaseSearch() throws Exception {
        File exampleCaseFbi2 = new File(testDataDirectory, "ExampleCase.fbi2");
        File testDir = new File(testDataDirectory, "CaseSearchTest");
        if (!testDir.exists()) {
            testDir.mkdirs();
            // TestData/Case1234
            File fakeCaseRoot = new File(testDir, "Case1234");
            fakeCaseRoot.mkdirs();

            // TestData/Case1234/case.fbi2
            FileUtils.copyFile(exampleCaseFbi2, new File(fakeCaseRoot, "case.fbi2"));

            // TestData/Case1234/Sub-Directory
            File fakeCaseInner = new File(fakeCaseRoot, "Sub-Directory");
            fakeCaseInner.mkdirs();

            // TestData/Case1234/Sub-Directory/case.fbi2
            FileUtils.copyFile(exampleCaseFbi2, new File(fakeCaseInner, "case.fbi2"));

            // TestData/Case5678
            File fakeCaseRoot2 = new File(testDir, "Case5678");
            fakeCaseRoot2.mkdirs();

            // TestData/Case5678/case.fbi2
            FileUtils.copyFile(exampleCaseFbi2, new File(fakeCaseRoot2, "case.fbi2"));

            // TestData/Case5678/Sub-Directory
            File fakeCaseInner2 = new File(fakeCaseRoot2, "Sub-Directory");
            fakeCaseInner2.mkdirs();

            // TestData/Case5678/Sub-Directory/case.fbi2
            FileUtils.copyFile(exampleCaseFbi2, new File(fakeCaseInner2, "case.fbi2"));
        }

        // Test data should look like:
        // TestData/Case1234/case.fbi2
        // TestData/Case1234/Sub-Directory/case.fbi2
        // TestData/Case5678/case.fbi2
        // TestData/Case5678/Sub-Directory/case.fbi2
        // The deeper case.fbi2 files should NOT be found since traversal logic should halt
        // searching deeper once it finds each upper case.fbi2 file.  The early traversal exit logic
        // will hopefully help address a situation where file system stored binaries are in case directory
        // which, without early exit logic, will do a large amount of unnecessary traversal when the case
        // fbi2 file had been found much higher up.

        Collection<File> caseDirs = CaseUtility.getInstance().findCaseDirectories(testDir);
        for (File caseDir : caseDirs) {
            log.info("Found Case Directory: " + caseDir.getAbsolutePath());
        }
        assertEquals(2, caseDirs.size());
    }
}
