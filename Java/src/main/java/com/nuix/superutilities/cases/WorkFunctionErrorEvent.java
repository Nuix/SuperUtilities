package com.nuix.superutilities.cases;

/***
 * This class is used to provide a callback information regarding an exception encountered by {@link BulkCaseProcessor} while
 * executing code provided in user code.  This class also provides a way for that callback to instruct {@link BulkCaseProcessor} how to react to this.
 * @author Jason Wells
 *
 */
public class WorkFunctionErrorEvent {
	private CaseInfo caseInfo = null;
	private CaseIssueReaction reaction = CaseIssueReaction.SkipCase;
	private Exception error = null;
	
	/***
	 * Creates a new instance
	 * @param erroredCaseInfo Information about the case open when the error occurred
	 * @param functionError The exception which was thrown
	 */
	public WorkFunctionErrorEvent(CaseInfo erroredCaseInfo, Exception functionError){
		this.caseInfo = erroredCaseInfo;
		this.error = functionError;
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
	 * Note that a value of CaseIssueReaction.Retry makes not sense in this context and should be
	 * considered invalid.
	 * @param reaction The reaction to take
	 */
	public void setReaction(CaseIssueReaction reaction) {
		this.reaction = reaction;
	}
	
	/***
	 * Gets the exception thrown
	 * @return The exception which was thrown
	 */
	public Exception getError() {
		return error;
	}

	/***
	 * Notifies {@link BulkCaseProcessor} that this case should be skipped in response
	 */
	public void skipCase(){ reaction = CaseIssueReaction.SkipCase; }
	
	/***
	 * Notifies {@link BulkCaseProcessor} that all further processing should be aborted
	 */
	public void abort() {reaction = CaseIssueReaction.Abort; }
}
