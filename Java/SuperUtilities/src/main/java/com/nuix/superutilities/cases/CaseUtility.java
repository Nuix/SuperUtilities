package com.nuix.superutilities.cases;

import com.nuix.superutilities.misc.ZipHelper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/***
 * Class which provides some additional functionality regarding Nuix cases, mainly finding cases
 * present in directories and their sub directories.
 * @author Jason Wells
 *
 */
public class CaseUtility {
    private static Logger logger = Logger.getLogger(CaseUtility.class);

    protected CaseUtility() {
    }

    protected static CaseUtility instance = null;

    public static CaseUtility getInstance() {
        if (instance == null) {
            instance = new CaseUtility();
        }
        return instance;
    }

    /***
     * Searches for case directories in a given directory and sub-directories.
     * @param rootSearchDirectory The root directory to search.
     * @return A collection of File objects for case directories located.
     */
    public Collection<File> findCaseDirectories(File rootSearchDirectory) {
        logger.info("Searching for cases in: " + rootSearchDirectory.getPath());
        if (!rootSearchDirectory.exists()) {
            logger.info("Directory does not exist: " + rootSearchDirectory.getPath());
            return List.of();
        } else {
            File[] children = rootSearchDirectory.listFiles();
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                if(child.isFile() && child.getName().equalsIgnoreCase("case.fbi2")) {
                    return List.of(child.getParentFile());
                }
            }

            List<File> result = new ArrayList<>();
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                if(child.isDirectory()) {
                    Collection<File> foundFiles = findCaseDirectories(child);
                    if(!foundFiles.isEmpty()) {
                        result.addAll(foundFiles);
                    }
                }
            }
            return result;//.stream().map(File::getParentFile).collect(Collectors.toList());
        }
    }

    /***
     * Searches for case directories in a given directory and sub-directories.
     * @param rootSearchPath The path to the root directory to search.
     * @return A collection of File objects for case directories located.
     */
    public Collection<File> findCaseDirectories(String rootSearchPath) {
        return findCaseDirectories(new File(rootSearchPath));
    }

    /***
     * Searches for case directories in a given directory and sub-directories.
     * @param rootSearchDirectory The root directory to search.
     * @return A collection of String representing case directories located.
     */
    public Collection<String> findCaseDirectoryPaths(File rootSearchDirectory) {
        return findCaseDirectories(rootSearchDirectory)
                .stream().map(f -> f.getPath()).collect(Collectors.toList());
    }

    /***
     * Searches for case directories in a given directory and sub-directories.
     * @param rootSearchPath The root directory to search.
     * @return A collection of String representing case directories located.
     */
    public Collection<String> findCaseDirectoryPaths(String rootSearchPath) {
        return findCaseDirectoryPaths(new File(rootSearchPath));
    }

    /***
     * Scans specified root directory and sub-directories for cases, returning {@link CaseInfo} objects
     * for each case located.
     * @param rootSearchDirectory The root directory to search in
     * @return Case info objects for each case found
     */
    public List<CaseInfo> findCaseInformation(File rootSearchDirectory) {
        List<CaseInfo> result = null;
        result = findCaseDirectories(rootSearchDirectory).stream().map(d -> new CaseInfo(d)).collect(Collectors.toList());
        return result;
    }

    /***
     * Scans specified root directory and sub-directories for cases, returning {@link CaseInfo} objects
     * for each case located.
     * @param rootSearchPath The root directory to search in
     * @return Case info objects for each case found
     */
    public List<CaseInfo> findCaseInformation(String rootSearchPath) {
        return findCaseInformation(new File(rootSearchPath));
    }

    /***
     * Archives a Nuix case into a Zip file, optionally deleting the case once completed.  Based on:
     * https://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
     * @param nuixCaseDirectory Directory of the Nuix case
     * @param archiveFile The Zip file to archive the case into
     * @param deleteCaseOnCompletion Whether to delete the case upon completion
     * @param compressionLevel The compression level (0-9) with 0 being no compression and 9 being full compression, values outside
     * range will be clamped into range.
     * @throws IOException Thrown if there are issues creating the archive or deleting the directory.
     */
    public void archiveCase(String nuixCaseDirectory, String archiveFile, boolean deleteCaseOnCompletion, int compressionLevel) throws IOException {
        archiveCase(new File(nuixCaseDirectory), new File(archiveFile), deleteCaseOnCompletion, compressionLevel);
    }

    /***
     * Archives a Nuix case into a Zip file, optionally deleting the case once completed.  Based on:
     * https://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
     * @param nuixCaseDirectory Directory of the Nuix case
     * @param archiveFile The Zip file to archive the case into
     * @param deleteCaseOnCompletion Whether to delete the case upon completion
     * @param compressionLevel The compression level (0-9) with 0 being no compression and 9 being full compression, values outside
     * range will be clamped into range.
     * @throws IOException Thrown if there are issues creating the archive or deleting the directory.
     */
    public void archiveCase(File nuixCaseDirectory, File archiveFile, boolean deleteCaseOnCompletion, int compressionLevel) throws IOException {
        logger.info("Backing up case at " + nuixCaseDirectory.getAbsolutePath() + " to " + archiveFile.getAbsolutePath());
        archiveFile.getParentFile().mkdirs();
        ZipHelper.compressDirectoryToZipFile(nuixCaseDirectory.getAbsolutePath(), archiveFile.getAbsolutePath(), compressionLevel);
        if (deleteCaseOnCompletion && archiveFile.exists()) {
            logger.info("Deleting now archived case " + nuixCaseDirectory.getAbsolutePath());
            FileUtils.deleteDirectory(nuixCaseDirectory);
        }
    }
}
