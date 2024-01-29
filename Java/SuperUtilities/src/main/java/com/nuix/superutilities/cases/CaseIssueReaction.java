package com.nuix.superutilities.cases;

/***
 * Used to respond to issues opening a case by {@link BulkCaseProcessor}.  Allows callback to
 * inform {@link BulkCaseProcessor} how to proceed when an issue does arise.
 * @author Jason Wells
 *
 */
public enum CaseIssueReaction {
	SkipCase,
	Retry,
	Abort
}
