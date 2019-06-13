package com.nuix.superutilities;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.joda.time.DateTime;

import com.nuix.superutilities.cases.BulkCaseProcessor;
import com.nuix.superutilities.cases.CaseHistoryHelper;
import com.nuix.superutilities.cases.CaseUtility;
import com.nuix.superutilities.export.JsonExporter;
import com.nuix.superutilities.export.TemplateExporter;
import com.nuix.superutilities.items.SuperItemUtility;
import com.nuix.superutilities.misc.FormatUtility;
import com.nuix.superutilities.misc.FreeSpaceMonitor;
import com.nuix.superutilities.misc.NuixVersion;
import com.nuix.superutilities.regex.RegexScanner;

import nuix.BulkAnnotater;
import nuix.Case;
import nuix.Utilities;

/***
 * Serves as the entry point to most of the functionality.  Follows API Utilities class to some degree, single
 * object with methods to obtain the various other objects.
 * @author Jason Wells
 *
 */
public class SuperUtilities{
	private Utilities util = null;
	private static SuperUtilities instance = null;
	private static NuixVersion currentVersion = null;
	
	protected SuperUtilities(){}
	
	/***
	 * Obtains singleton of this class.
	 * @return Singleton of this class
	 */
	public static SuperUtilities getInstance(){
		if(instance == null){
			instance = new SuperUtilities();
		}
		return instance;
	}
	
	/***
	 * Initializes this class.
	 * @param util Instance of regular API utilities class, in Ruby this would be $utilities
	 * @param nuixVersionString Nuix version string, in Ruby this would be NUIX_VERSION
	 * @return Initialized singleton of this class
	 */
	public static SuperUtilities init(Utilities util, String nuixVersionString){
		SuperUtilities.getInstance().util = util;
		currentVersion = NuixVersion.parse(nuixVersionString);
		return SuperUtilities.getInstance();
	}
	
	/***
	 * Gets the underlying regular API utilities object
	 * @return The result Nuix API Utilities object
	 */
	public Utilities getNuixUtilities(){
		return this.util;
	}
	
	/***
	 * Gets singleton of {@link com.nuix.superutilities.cases.CaseUtility}
	 * @return singleton of {@link com.nuix.superutilities.cases.CaseUtility}
	 */
	public CaseUtility getCaseUtility(){
		return CaseUtility.getInstance();
	}
	
	/***
	 * Gets singleton of {@link com.nuix.superutilities.items.SuperItemUtility}
	 * @return singleton of {@link com.nuix.superutilities.items.SuperItemUtility}
	 */
	public SuperItemUtility getSuperItemUtility(){
		return SuperItemUtility.getInstance();
	}
	
	/***
	 * Gets singleton of {@link com.nuix.superutilities.misc.FormatUtility}
	 * @return singleton of {@link com.nuix.superutilities.misc.FormatUtility}
	 */
	public FormatUtility getFormatUtility(){
		return FormatUtility.getInstance();
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.regex.RegexScanner}
	 * @return a new instance of {@link com.nuix.superutilities.regex.RegexScanner}
	 */
	public RegexScanner createRegexScanner(){
		return new RegexScanner();
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.export.JsonExporter}
	 * @return a new instance of {@link com.nuix.superutilities.export.JsonExporter}
	 */
	public JsonExporter createJsonExporter(){
		return new JsonExporter();
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.export.TemplateExporter} with a template based on the specified file.
	 * @param erbTemplateFile File containing Ruby ERB template
	 * @return a new instance of {@link com.nuix.superutilities.export.TemplateExporter}
	 * @throws Exception if there is an error
	 */
	public TemplateExporter createTemplateExporter(File erbTemplateFile) throws Exception {
		return new TemplateExporter(erbTemplateFile);
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.export.TemplateExporter} with a template based on the specified file.
	 * @param erbTemplateFile File containing Ruby ERB template
	 * @return a new instance of {@link com.nuix.superutilities.export.TemplateExporter}
	 * @throws Exception if there is an error
	 */
	public TemplateExporter createTemplateExporter(String erbTemplateFile) throws Exception {
		return new TemplateExporter(new File(erbTemplateFile));
	}

	/***
	 * Gets {@link com.nuix.superutilities.misc.NuixVersion} object representing version of Nuix passed into {@link SuperUtilities#init(Utilities, String)}
	 * @return object representing current version of Nuix
	 */
	public static NuixVersion getCurrentVersion() {
		return currentVersion;
	}
	
	/***
	 * Get file path of SuperUtilities.jar
	 * @return String representing path to JAR file
	 */
	public String getJarFilePath(){
		try {
			return SuperUtilities.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath().replaceAll("^/", "");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/***
	 * Get file path of SuperUtilities.jar
	 * @return File object representing path to JAR file
	 */
	public File getJarFile(){
		return new File(getJarFilePath()).getParentFile();
	}
	
	/***
	 * Get file path of directory in which SuperUtilities.jar resides
	 * @return File object representing path to directory in which JAR resides
	 */
	public File getJarDirectory(){
		return getJarFile().getParentFile();
	}
	
	/***
	 * Get file path of directory in which SuperUtilities.jar resides
	 * @return String representing path to directory in which JAR resides
	 */
	public String getJarDirectoryPath(){
		return getJarDirectory().getPath();
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.cases.BulkCaseProcessor}.
	 * @return a new instance of {@link com.nuix.superutilities.cases.BulkCaseProcessor}
	 */
	public BulkCaseProcessor createBulkCaseProcessor(){
		return new BulkCaseProcessor();
	}
	
	/***
	 * Saves a diagnostics zip file (similar to same operation in the workbench GUI)
	 * @param zipFile File object specifying where the zip file should be saved to.
	 */
	public void saveDiagnostics(File zipFile){
		List<MBeanServer> beanServers = new ArrayList<MBeanServer>();
		beanServers.add(ManagementFactory.getPlatformMBeanServer());
		beanServers.addAll(MBeanServerFactory.findMBeanServer(null));
		for (MBeanServer mBeanServer : beanServers) {
			Set<ObjectName> objectNames = mBeanServer.queryNames(null, null);
			for (ObjectName beanName : objectNames) {
				if(beanName.toString().contains("DiagnosticsControl")){
					try {
						zipFile.getParentFile().mkdirs();
						mBeanServer.invoke(beanName,"generateDiagnostics",new Object[] {zipFile.getPath()},new String[] {"java.lang.String"});
						break;
					} catch (Exception e) {
						//Ignore, there seems to be a chance of false errors?
					}
				}
			}
		}
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.misc.FreeSpaceMonitor}.
	 * @return a new instance of {@link com.nuix.superutilities.misc.FreeSpaceMonitor}
	 */
	public FreeSpaceMonitor createFreeSpaceMonitor(){
		return new FreeSpaceMonitor();
	}
	
	/***
	 * Saves a diagnostics zip file (similar to same operation in the GUI)
	 * @param zipFile String specifying where the zip file should be saved to.
	 */
	public void saveDiagnostics(String zipFile){
		saveDiagnostics(new File(zipFile));
	}
	
	/***
	 * Creates a new instance of {@link com.nuix.superutilities.cases.CaseHistoryHelper}.
	 * @param nuixCase Nuix case to get history from
	 * @param eventTypes List of Nuix event type names
	 * @param minStart Earliest event start date time to return
	 * @param maxStart Latest event start date time to return
	 * @see <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/HistoryEvent.html#getTypeString--">HistoryEvent.getTypeString</a>
	 * @return a new instance of {@link com.nuix.superutilities.cases.CaseHistoryHelper}
	 * @throws Exception when there is an error
	 */
	public CaseHistoryHelper createCaseHistoryHelper(Case nuixCase, List<String> eventTypes, DateTime minStart, DateTime maxStart)
			throws Exception{
		return new CaseHistoryHelper(nuixCase,eventTypes,minStart,maxStart);
	}
	
	/***
	 * Convenience method to obtain Nuix BulkAnnotater
	 * @return Nuix bulk annotater obtained from Utilities object
	 */
	public static BulkAnnotater getBulkAnnotater(){
		return getInstance().getNuixUtilities().getBulkAnnotater();
	}
}
