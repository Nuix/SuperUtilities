package com.nuix.superutilities.cases;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.nuix.superutilities.SuperUtilities;

import nuix.Case;

/***
 * Represents some information which can be determined about a Nuix case directory without having
 * yet opened the case through the API.  This includes case store count and information parsed from
 * the case's FBI2 file.
 * @author Jason Wells
 *
 */
public class CaseInfo {
	private static Logger logger = Logger.getLogger(CaseInfo.class);
	private static Pattern storeDirectoryPattern = Pattern.compile("Store-[a-z0-9]+");
	private static DateTimeFormatter dateParser = ISODateTimeFormat.dateTime();
	private static DateTimeFormatter dateToStringFormat = DateTimeFormat.forPattern("yyyy/MM/dd kk:mm aa");
	
	private File directory = null;
	private File fbi2File = null;
	private String name = "";
	private String guid = "";
	private String investigator = "";
	private DateTime creationDateTime = null;
	private DateTime lastAccessedDateTime = null;
	private String savedByProduct = null;
	private String savedByVersion = null;
	
	private int storeCount = 0;
	private boolean isCompound = false;
	private boolean caseExists = false;
	private List<CaseInfo> childCases = new ArrayList<CaseInfo>();
	
	/***
	 * Creates a new instance associated to the specified case directory.
	 * @param caseDirectory Case directory to collect information on.
	 */
	public CaseInfo(File caseDirectory){
		directory = caseDirectory;
		fbi2File = new File(caseDirectory,"case.fbi2");
		caseExists = fbi2File.exists();
		parseFbi2File();
		determineStoreCount();
	}
	
	/***
	 * Creates a new instance associated to the specified case directory.
	 * @param caseDirectoryPath Case directory to collect information on.
	 */
	public CaseInfo(String caseDirectoryPath){
		this(new File(caseDirectoryPath));
	}
	
	/***
	 * Parses information from FBI2 file
	 */
	protected void parseFbi2File(){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    factory.setNamespaceAware(true);
		    DocumentBuilder builder;
		    Document doc = null;
		    builder = factory.newDocumentBuilder();
		    logger.debug("Parsing: "+getFbi2FilePath());
		    doc = builder.parse(new File(getFbi2FilePath()));
		    XPathFactory xFactory = XPathFactory.newInstance();
		    XPath xpath = xFactory.newXPath();
		    xpath.setNamespaceContext( new NamespaceContext() {
		        public String getNamespaceURI(String prefix) {
		          switch (prefix) {
		            case "nx": return "http://nuix.com/fbi/cases";
		           }
				return prefix;
		        }

				@Override
				public String getPrefix(String arg0) {
					// TODO Auto-generated method stub
					return null;
				}

				@SuppressWarnings("rawtypes")
				@Override
				public Iterator getPrefixes(String arg0) {
					// TODO Auto-generated method stub
					return null;
				}});
		    
		    isCompound = (boolean) xpath.compile("/nx:case/@compound").evaluate(doc, XPathConstants.BOOLEAN);
		    name = (String) xpath.compile("/nx:case/nx:metadata/nx:name/text()[1]").evaluate(doc, XPathConstants.STRING);
		    guid = (String) xpath.compile("/nx:case/nx:metadata/nx:guid/text()[1]").evaluate(doc, XPathConstants.STRING);
		    investigator = (String) xpath.compile("/nx:case/nx:metadata/nx:investigator/text()[1]").evaluate(doc, XPathConstants.STRING);
		    String creationDateString = (String) xpath.compile("/nx:case/nx:metadata/nx:creation-date/text()[1]").evaluate(doc, XPathConstants.STRING);
		    creationDateString = creationDateString.replace(',', '.');
		    creationDateTime = dateParser.parseDateTime(creationDateString);
		    savedByProduct = (String) xpath.compile("/nx:case/nx:metadata/nx:saved-by-product/@name").evaluate(doc, XPathConstants.STRING);
		    savedByVersion = (String) xpath.compile("/nx:case/nx:metadata/nx:saved-by-product/@version").evaluate(doc, XPathConstants.STRING);
		    File analysisDbDirectory = new File(getCaseDirectoryPath()+"\\Stores\\AnalysisDatabase");
		    lastAccessedDateTime = new DateTime(analysisDbDirectory.lastModified());
		   
		    NodeList subCases = (NodeList) xpath.compile("/nx:case/nx:subcases/nx:subcase/@location").evaluate(doc, XPathConstants.NODESET);
		    for(int i=0;i<subCases.getLength();i++){
		    	String path = new File(getCaseDirectoryPath()+"\\"+subCases.item(i).getTextContent().replace('/','\\')).getCanonicalPath();
		    	CaseInfo child = new CaseInfo(path);
		    	childCases.add(child);
		    }
		}catch(Exception e){
			logger.error("Error while parsing case FBI2 file: "+getFbi2FilePath());
			logger.error(e);
		}
	}
	
	/***
	 * Determines case store count by counting store directories in case directory.
	 */
	protected void determineStoreCount(){
		if(isCompound()){
			for(CaseInfo child : childCases){
				storeCount += child.getStoreCount();
			}
		}else{
			File storesDirectory = new File(directory.getAbsolutePath(),"Stores");
			if(storesDirectory.exists()){
				for(File subDir : storesDirectory.listFiles()){
					boolean isStoreDirectory = storeDirectoryPattern.matcher(subDir.getAbsolutePath()).find();
					if(subDir.isDirectory() && isStoreDirectory){
						storeCount += 1;
					}
				}
			} else {
				storeCount = 0;
			}
			
		}
	}
	
	/***
	 * Gets case directory associated with this instance.
	 * @return The case directory associated with this instance as File object.
	 */
	public File getCaseDirectory(){ return directory; }
	
	/***
	 * Gets case directory associated with this instance.
	 * @return The case directory associated with this instance as a String.
	 */
	public String getCaseDirectoryPath(){ return directory.getPath(); }
	
	/***
	 * Gets file path of FBI2 file associated with this instance.
	 * @return The file path of the FBI2 file associated with this instance as File object.
	 */
	public File getFbi2File(){ return fbi2File; }
	
	/***
	 * Gets file path of FBI2 file associated with this instance.
	 * @return The file path of the FBI2 file associated with this instance as a String.
	 */
	public String getFbi2FilePath(){ return getFbi2File().getPath(); }
	
	/***
	 * Whether this case has been determined to be a compound case
	 * @return True if this case has been determined to be compound.
	 */
	public boolean isCompound(){ return isCompound; }
	
	/***
	 * Store count of this case.
	 * @return Determined store count of this case.
	 */
	public int getStoreCount(){ return storeCount; }
	
	/***
	 * The name of this case.
	 * @return The name of this case.
	 */
	public String getName(){ return name; }
	
	/***
	 * The case GUID of this case.
	 * @return The case GUID.
	 */
	public String getGuid() { return guid; }
	
	/***
	 * The case creation date time.
	 * @return The case creation date time.
	 */
	public DateTime getCreationDateTime(){ return creationDateTime; };
	
	/***
	 * The case creation date time as a String.
	 * @return The case creation date time as a String.
	 */
	public String getCreationDateTimeString(){ return dateToStringFormat.print(creationDateTime); };
	
	/***
	 * The case last accessed date time.
	 * @return The case last accessed date time.
	 */
	public DateTime getAccessedDateTime(){ return lastAccessedDateTime; };
	
	/***
	 * The case last accessed date time as a String.
	 * @return The case last accessed date time as a String.
	 */
	public String getAccessedDateTimeString(){ return dateToStringFormat.print(lastAccessedDateTime); };
	
	/***
	 * The investigator value.
	 * @return The investigator value.
	 */
	public String getInvestigator(){ return investigator; }
	
	/***
	 * The saved by name.
	 * @return The saved by name.
	 */
	public String getSavedByName(){ return savedByProduct; }
	
	/***
	 * The saved by version.
	 * @return The saved by version.
	 */
	public String getSavedByVersion(){ return savedByVersion; }
	
	/***
	 * A list of CaseInfo objects representing child cases if this is a compound case.
	 * @return A list of CaseInfo objects representing child cases if this is a compound case.
	 */
	public List<CaseInfo> getChildCases(){ return childCases; }
	
	/***
	 * Count of child cases if this is a compound case.
	 * @return Count of child cases if this is a compound case.
	 */
	public int getChildCaseCount(){ return childCases.size(); }
	
	/***
	 * Whether a case was determined to exist in the directory specified, as determined by presence of
	 * FBI2 file in directory.
	 * @return True if case was determined to exist in the directory specified when this instance was created.
	 */
	public boolean caseExists(){
		return caseExists;
	}
	
	/***
	 * Whether this case was determined to be locked, as determined by the presence of a case.lock file in
	 * the specified case directory.
	 * @return True is case has been determined to be locked.
	 */
	public boolean isLocked(){
		File possibleLockFile = new File(directory,"case.lock");
		return possibleLockFile.exists();
	}
	
	/***
	 * If case is determined to be locked, gets information about who is currently locking the case.
	 * @return A CaseLockInfo object with information about who is locking the case, if the case is locked.
	 */
	public CaseLockInfo getLockProperties(){
		if(isLocked()){
			CaseLockInfo lockInfo = new CaseLockInfo();
			File lockPropertiesFile = new File(directory,"case.lock.properties");
			try(FileInputStream lockFileStream = new FileInputStream(lockPropertiesFile)){
				Properties lockProperties = new Properties();
				lockProperties.load(lockFileStream);
				lockInfo.setCaseInfo(this);
				lockInfo.setUser(lockProperties.getProperty("user"));
				lockInfo.setHost(lockProperties.getProperty("host"));
				lockInfo.setProduct(lockProperties.getProperty("product"));
			} catch (Exception e) {
				logger.error("Error while parsing case lock properties file");
				logger.error(e);
			}
			return lockInfo;
		} else {
			return null;
		}
	}
	
	/***
	 * Opens case and passes it to specified callback.
	 * @param allowMigration Whether to allow Nuix to migrate this case while opening it.
	 * @param caseConsumer Callback which will be provided the case if it opens without error.
	 * @throws Exception Thrown if there is an error.
	 */
	public void withCase(boolean allowMigration, Consumer<Case> caseConsumer) throws Exception {
		Map<String,Object> openCaseSettings = new HashMap<String,Object>();
		openCaseSettings.put("migrate",allowMigration);
		Case nuixCase = null;
		try {
			nuixCase = SuperUtilities.getInstance().getNuixUtilities().getCaseFactory().open(getCaseDirectory(),
					openCaseSettings);
			caseConsumer.accept(nuixCase);
		} finally {
			if(nuixCase != null)
				nuixCase.close();
		}
	}
	
	/***
	 * Opens case and passes it to specified callback.  This differs from {@link #withCase(boolean, Consumer)} in that
	 * this version implictly does not allow Nuix to migrate the case.
	 * @param caseConsumer Callback which will be provided the case if it opens without error.
	 * @throws Exception Thrown if there is an error.
	 */
	public void withCase(Consumer<Case> caseConsumer) throws Exception {
		withCase(false,caseConsumer);
	}
	
	/***
	 * Determines whether a Nuix case object shares the same GUID as this CaseInfo object.
	 * @param nuixCase A Nuix case obtained from the Nuix API.
	 * @return True if this CaseInfo and the provided Case object are found to have the same GUID.
	 */
	public boolean hasSameGuidAs(Case nuixCase){
		String caseGuid = nuixCase.getGuid().replaceAll("\\-","");
		String fbi2Guid = getGuid().replaceAll("\\-","");
		boolean result = !fbi2Guid.isEmpty() && caseGuid.equalsIgnoreCase(fbi2Guid);
		logger.info("Case GUID: "+caseGuid);
		logger.info("FBI2 GUID: "+fbi2Guid);
		logger.info("Same?: "+result);
		return result;
	}
	
	/***
	 * Given a list of case paths (specified as Strings) creates a series of CaseInfo objects.
	 * @param paths String paths to case directories.
	 * @return List of CaseInfo objects for the list of case directories.
	 */
	public static List<CaseInfo> fromCasePaths(List<String> paths){
		return paths.stream().map(p -> new CaseInfo(p)).collect(Collectors.toList());
	}
	
	/***
	 * Given a list of case paths (specified as File objects) creates a series of CaseInfo objects.
	 * @param directories File paths to case directories.
	 * @return List of CaseInfo objects for the list of case directories.
	 */
	public static List<CaseInfo> fromCaseDirectories(List<File> directories){
		return directories.stream().map(d -> new CaseInfo(d)).collect(Collectors.toList());
	}

	/***
	 * Creates a user friendly String "dump" of information contained in this CaseInfo instance.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Case Directory: "+directory+"\n");
		result.append("Case Is Compound: "+isCompound+"\n");
		result.append("Case GUID: "+guid+"\n");
		result.append("Case Name: "+name+"\n");
		result.append("Case Investigator: "+investigator+"\n");
		result.append("Case Store Count: "+storeCount+"\n");
		result.append("Case Saved By: "+savedByProduct+"\n");
		result.append("Case Saved By Version: "+savedByVersion+"\n");
		result.append("Case Created: "+creationDateTime+"\n");
		result.append("Case Last Acessed: "+lastAccessedDateTime+"\n");
		result.append("Case Is Locked: "+isLocked()+"\n");
		
		return result.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directory == null) ? 0 : directory.hashCode());
		result = prime * result + ((guid == null) ? 0 : guid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CaseInfo other = (CaseInfo) obj;
		if (directory == null) {
			if (other.directory != null)
				return false;
		} else if (!directory.equals(other.directory))
			return false;
		if (guid == null) {
			if (other.guid != null)
				return false;
		} else if (!guid.equals(other.guid))
			return false;
		return true;
	}
}
