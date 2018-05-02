package com.nuix.superutilities.cases;

/***
 * Represents information parsed from a case lock file.  Used by {@link BulkCaseProcessor}.
 * @author Jason Wells
 *
 */
public class CaseLockInfo {
	private CaseInfo caseInfo = null;
	private String user = null;
	private String host = null;
	private String product = null;
	
	public CaseInfo getCaseInfo() {
		return caseInfo;
	}
	public void setCaseInfo(CaseInfo caseInfo) {
		this.caseInfo = caseInfo;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	@Override
	public String toString() {
		return "CaseLockInfo [user=" + user + ", host=" + host + ", product=" + product
				+ "]";
	}
}
