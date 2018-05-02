package com.nuix.superutilities.cases;

/***
 * This class is used to provide a callback information regarding a case encountered by {@link BulkCaseProcessor} which was
 * locked.  This class also provides a way for that callback to instruct {@link BulkCaseProcessor} how to react to this.
 * @author Jason Wells
 *
 */
public class CaseLockedEventInfo {
	private CaseInfo caseInfo = null;
	private CaseIssueReaction reaction = CaseIssueReaction.SkipCase;
	
	/***
	 * Creates a new instance
	 * @param lockedCaseInfo Information about the case which was locked
	 */
	public CaseLockedEventInfo(CaseInfo lockedCaseInfo){
		caseInfo = lockedCaseInfo;
	}
	
	/***
	 * Gets information related to case which was locked
	 * @return Information related to case which was locked
	 */
	public CaseInfo getCaseInfo() {
		return caseInfo;
	}

	/***
	 * Gets the reaction that should be taken to this case being locked
	 * @return The reaction to take
	 */
	public CaseIssueReaction getReaction() {
		return reaction;
	}

	/***
	 * Sets the reaction that should be taken to this case being locked
	 * @param reaction The reaction to take
	 */
	public void setReaction(CaseIssueReaction reaction) {
		this.reaction = reaction;
	}
	
	/***
	 * Notifies {@link BulkCaseProcessor} that this case should be skipped in response
	 */
	public void skipCase(){ reaction = CaseIssueReaction.SkipCase; }
	
	/***
	 * Notifies {@link BulkCaseProcessor} that it should try to open this case again
	 */
	public void retry(){ reaction = CaseIssueReaction.Retry; }
	
	/***
	 * Notifies {@link BulkCaseProcessor} that all further processing should be aborted
	 */
	public void abort() {reaction = CaseIssueReaction.Abort; }
}
