package com.nuix.superutilities.cases;

import nuix.Case;

/***
 * Interface for callback which accepts cases as they are iterated by {@link BulkCaseProcessor}.
 * @author Jason Wells
 *
 */
public interface CaseConsumer {
	public Boolean acceptCase(Case nuixCase, CaseInfo caseInfo, int currentCaseIndex, int totalCases);
}
