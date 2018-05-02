package com.nuix.superutilities.cases;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

/***
 * Class which provides some additional functionality regarding Nuix cases, mainly finding cases
 * present in directories and their sub directories.
 * @author Jason Wells
 *
 */
public class CaseUtility {
	private static Logger logger = Logger.getLogger(CaseUtility.class);
	
	protected CaseUtility(){}
	
	protected static CaseUtility instance = null;
	public static CaseUtility getInstance(){
		if(instance == null){
			instance = new CaseUtility();
		}
		return instance;
	}
	
	/***
	 * Searches for case directories in a given directory and sub-directories.
	 * @param rootSearchDirectory The root directory to search.
	 * @return A collection of File objects for case directories located.
	 */
	public Collection<File> findCaseDirectories(File rootSearchDirectory){
		logger.info("Searching for cases in: "+rootSearchDirectory.getPath());
		return FileUtils
				.listFiles(rootSearchDirectory, new RegexFileFilter("case\\.fbi2"), TrueFileFilter.TRUE)
				.stream().map(f -> f.getParentFile()).collect(Collectors.toList());
	}
	
	/***
	 * Searches for case directories in a given directory and sub-directories.
	 * @param rootSearchPath The path to the root directory to search.
	 * @return A collection of File objects for case directories located.
	 */
	public Collection<File> findCaseDirectories(String rootSearchPath){
		return findCaseDirectories(new File(rootSearchPath));
	}
	
	/***
	 * Searches for case directories in a given directory and sub-directories.
	 * @param rootSearchDirectory The root directory to search.
	 * @return A collection of String representing case directories located.
	 */
	public Collection<String> findCaseDirectoryPaths(File rootSearchDirectory){
		return findCaseDirectories(rootSearchDirectory)
				.stream().map(f -> f.getPath()).collect(Collectors.toList());
	}
	
	/***
	 * Searches for case directories in a given directory and sub-directories.
	 * @param rootSearchPath The root directory to search.
	 * @return A collection of String representing case directories located.
	 */
	public Collection<String> findCaseDirectoryPaths(String rootSearchPath){
		return findCaseDirectoryPaths(new File(rootSearchPath));
	}
	
	/***
	 * Scans specified root directory and sub-directories for cases, returning {@link CaseInfo} objects
	 * for each case located.
	 * @param rootSearchDirectory The root directory to search in
	 * @return Case info objects for each case found
	 */
	public List<CaseInfo> findCaseInformation(File rootSearchDirectory){
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
	public List<CaseInfo> findCaseInformation(String rootSearchPath){
		return findCaseInformation(new File(rootSearchPath));
	}
}
