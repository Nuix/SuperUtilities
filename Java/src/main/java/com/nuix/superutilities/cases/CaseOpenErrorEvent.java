package com.nuix.superutilities.cases;

/***
 * This class is used to provide a callback information regarding a case encountered by {@link BulkCaseProcessor} which had
 * and error when opened.  This class also provides a way for that callback to instruct {@link BulkCaseProcessor} how to react to this.
 * @author Jason Wells
 *
 */
public class CaseOpenErrorEvent {
	private CaseInfo caseInfo = null;
	private CaseIssueReaction reaction = CaseIssueReaction.SkipCase;
	private Exception error = null;
	
	/***
	 * Creates a new instance
	 * @param erroredCaseInfo Information related to the case which had the error
	 * @param caseOpenError The exception thrown while opening the case
	 */
	public CaseOpenErrorEvent(CaseInfo erroredCaseInfo, Exception caseOpenError){
		this.caseInfo = erroredCaseInfo;
		this.error = caseOpenError;
	}
	
	/***
	 * Gets information related to case which was locked
	 * @return Information related to case which was locked
	 */
	public CaseInfo getCaseInfo() {
		return caseInfo;
	}

	/***
	 * Gets the reaction that should be taken
	 * @return The reaction to take
	 */
	public CaseIssueReaction getReaction() {
		return reaction;
	}
	
	/***
	 * Sets the reaction that should be taken
	 * @param reaction The reaction to take
	 */
	public void setReaction(CaseIssueReaction reaction) {
		this.reaction = reaction;
	}
	
	/***
	 * Gets the exception which was thrown
	 * @return The exception thrown
	 */
	public Exception getError() {
		return error;
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
