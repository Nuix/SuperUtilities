package com.nuix.superutilities.misc;

import java.lang.Package;
import java.util.regex.Pattern;

/***
 * Assists in representing a Nuix version in object form to assist with comparing two versions.  This allows for things such as
 * only executing chunks of code if the version meets a requirement.<br>
 * Ruby example:
 * <pre>
 * {@code
 * current_version = NuixVersion.new(NUIX_VERSION)
 * if current_version.isLessThan("7.8.0.10")
 *     puts "Sorry your version of Nuix is below the minimum required version of 7.8.0.10"
 *     exit 1
 * end
 * }
 * </pre>
 * @author Jason Wells
 *
 */
public class NuixVersion implements Comparable<NuixVersion> {
	private static Pattern previewVersionInfoRemovalPattern = Pattern.compile("[^0-9\\.].*$");
	
	private int major = 0;
	private int minor = 0;
	private int bugfix = 0;
	private int build = 0;
	
	/***
	 * Creates a new instance defaulting to version 0.0.0
	 */
	public NuixVersion(){
		this(0,0,0,0);
	}
	
	/***
	 * Creates a new instance using the provided major version: major.0.0.0
	 * @param majorVersion The major version number
	 */
	public NuixVersion(int majorVersion){
		this(majorVersion,0,0,0);
	}
	/***
	 * Creates a new instance using the provided major and minor versions: major.minor.0.0
	 * @param majorVersion The major version number
	 * @param minorVersion The minor version number
	 */
	public NuixVersion(int majorVersion, int minorVersion){
		this(majorVersion,minorVersion,0,0);
	}
	/***
	 * Creates a new instance using the provided major, minor and bugfix versions: major.minor.bugfix.0
	 * @param majorVersion The major version number
	 * @param minorVersion The minor version number
	 * @param bugfixVersion The bugfix version number
	 */
	public NuixVersion(int majorVersion, int minorVersion, int bugfixVersion){
		this(majorVersion,minorVersion,bugfixVersion,0);
	}
	
	/***
	 * Creates a new instance using the provided major, minor, bugfix and build versions: major.minor.bugfix.build
	 * @param majorVersion The major version number
	 * @param minorVersion The minor version number
	 * @param bugfixVersion The bugfix version number
	 * @param buildVersion The build version number
	 */
	public NuixVersion(int majorVersion, int minorVersion, int bugfixVersion, int buildVersion){
		major = majorVersion;
		minor = minorVersion;
		bugfix = bugfixVersion;
		build = buildVersion;
	}
	
	/***
	 * Parses a version string into a NuixVersion instance.  Supports values such as: 6, 6.2, 6.2.0, 6.2.1-preview6, 7.8.0.10 <br>
	 * When providing a version string such as "6.2.1-preview6", "-preview6" will be trimmed off before parsing.
	 * @param versionString The version string to parse.
	 * @return A NuixVersion instance representing the supplied version string, if there is an error parsing the provided value will return
	 * an instance representing 900.0.0.0
	 */
	public static NuixVersion parse(String versionString){
		try {
			String[] versionParts = NuixVersion.previewVersionInfoRemovalPattern.matcher(versionString.trim()).replaceAll("").split("\\.");
			int[] versionPartInts = new int[versionParts.length];
			for(int i=0;i<versionParts.length;i++){
				versionPartInts[i] = Integer.parseInt(versionParts[i]);
			}
			switch(versionParts.length){
				case 1:
					return new NuixVersion(versionPartInts[0]);
				case 2:
					return new NuixVersion(versionPartInts[0],versionPartInts[1]);
				case 3:
					return new NuixVersion(versionPartInts[0],versionPartInts[1],versionPartInts[2]);
				case 4:
					return new NuixVersion(versionPartInts[0],versionPartInts[1],versionPartInts[2],versionPartInts[3]);
				default:
					return new NuixVersion();
			}
		}catch(Exception exc){
			System.out.println("Error while parsing version: "+versionString);
			System.out.println("Pretending version is 900.0.0.0");
			return new NuixVersion(900,0,0,0);
		}
	}
	
	/***
	 * Gets the determined major portion of this version instance (X.0.0.0)
	 * @return The determined major portion of version
	 */
	public int getMajor() {
		return major;
	}
	
	/***
	 * Sets the major portion of this version instance (X.0.0.0)
	 * @param major The major version value
	 */
	public void setMajor(int major) {
		this.major = major;
	}
	
	/***
	 * Gets the determined minor portion of this version instance (0.X.0.0)
	 * @return The determined minor portion of version
	 */
	public int getMinor() {
		return minor;
	}
	
	/***
	 * Sets the minor portion of this version instance (0.X.0.0)
	 * @param minor The minor version value
	 */
	public void setMinor(int minor) {
		this.minor = minor;
	}
	
	/***
	 * Gets the determined bugfix portion of this version instance (0.0.x.0)
	 * @return The determined bugfix portion of version
	 */
	public int getBugfix() {
		return bugfix;
	}

	/***
	 * Sets the determined bugfix portion of this version instance (0.0.x.0)
	 * @param bugfix The determined bugfix portion of version
	 */
	public void setBugfix(int bugfix) {
		this.bugfix = bugfix;
	}
	
	/***
	 * Gets the determined build portion of this version instance (0.0.0.x)
	 * @return The determined build portion of version
	 */
	public int getBuild() {
		return build;
	}
	
	/***
	 * Sets the build portion of this version instance (0.0.0.x)
	 * @param build The build version value
	 */
	public void setBuild(int build) {
		this.build = build;
	}
	
	/***
	 * Attempts to determine current Nuix version by inspecting Nuix packages.  It is preffered to instead use
	 * {@link #parse(String)} when version string is available, such as in Ruby using constant NUIX_VERSION.
	 * @return Best guess at current Nuix version based on package inspection.
	 */
	public static NuixVersion getCurrent(){
		String versionString = "0.0.0.0";
		for(Package p : Package.getPackages()){
			if(p.getName().matches("com\\.nuix\\..*")){
				versionString = p.getImplementationVersion();
				break;
			}
		}
		return NuixVersion.parse(versionString);
	}

	/***
	 * Determines whether another instance's version is less than this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is a lower version, false otherwise
	 */
	public boolean isLessThan(NuixVersion other){
		return this.compareTo(other) < 0;
	}
	/***
	 * Determines whether another instance's version is greater than or equal to this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is greater than or equal to this instance, false otherwise
	 */
	public boolean isAtLeast(NuixVersion other){
		return this.compareTo(other) >= 0;
	}
	/***
	 * Determines whether another instance's version is greater than this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is greater than this instance, false otherwise
	 */
	public boolean isGreaterThan(NuixVersion other){
		return this.compareTo(other) > 0;
	}
	/***
	 * Determines whether another instance's version is greater than this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is greater than this instance, false otherwise
	 */
	public boolean isGreaterThan(String other){
		return this.compareTo(NuixVersion.parse(other)) > 0;
	}
	/***
	 * Determines whether another instance's version is equal to this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is greater than this instance, false otherwise
	 */
	public boolean isEqualTo(NuixVersion other){
		return this.compareTo(other) == 0;
	}
	/***
	 * Determines whether another instance's version is equal to this instance
	 * @param other The other instance to compare against
	 * @return True if the other instance is equal to this instance (major, minor and release are the same), false otherwise
	 */
	public boolean isEqualTo(String other){
		return this.compareTo(NuixVersion.parse(other)) == 0;
	}
	/***
	 * Determines whether another instance's version is less than this instance
	 * @param other A version string to compare against
	 * @return True if the other instance is a lower version, false otherwise
	 */
	public boolean isLessThan(String other){
		return isLessThan(parse(other));
	}
	/***
	 * Determines whether another instance's version is greater than or equal to this instance
	 * @param other A version string to compare against
	 * @return True if the other instance is greater than or equal to this instance, false otherwise
	 */
	public boolean isAtLeast(String other){
		return isAtLeast(parse(other));
	}
	
	/***
	 * Provides comparison logic when comparing two instances.
	 */
	@Override
	public int compareTo(NuixVersion other) {
		if(this.major == other.major){
			if(this.minor == other.minor){
				if(this.bugfix == other.bugfix) {
					return Integer.compare(this.build, other.build);	
				} else {
					return Integer.compare(this.bugfix, other.bugfix);
				}
			} else{
				return Integer.compare(this.minor, other.minor);
			}
		} else{
			return Integer.compare(this.major, other.major);
		}
		
	}
	
	/***
	 * Converts this instance back to a version string from its components, such as: "7.8.0.10"
	 */
	@Override
	public String toString(){
		return Integer.toString(this.major) + "." +
				Integer.toString(this.minor) + "." +
				Integer.toString(this.bugfix) + "." +
				Integer.toString(this.build);
	}
}
