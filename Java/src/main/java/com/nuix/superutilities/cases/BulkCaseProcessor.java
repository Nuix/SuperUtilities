package com.nuix.superutilities.cases;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.nuix.superutilities.SuperUtilities;

import nuix.Case;

/***
 * This class assists with iteratively doing some form of work in multiple cases.
 * This class provides call backs which allow calling code to determine what to do when several errors occur such as:
 * <ul>
 * <li>Case is locked by another process</li>
 * <li>There is an error opening the case</li>
 * <li>User provided code to process each case has an error</li>
 * </ul> 
 * @author Jason Wells
 *
 */
public class BulkCaseProcessor {
	private static Logger logger = Logger.getLogger(BulkCaseProcessor.class);
	
	private List<File> caseDirectories = new ArrayList<File>();
	private boolean allowCaseMigration = false;
	private boolean abortFurtherProcessing = false;
	
	private Consumer<CaseLockedEventInfo> caseLockedCallback = null;
	private Consumer<CaseOpenErrorEvent> caseErrorCallback = null;
	private Consumer<WorkFunctionErrorEvent> userFunctionErrorCallback = null;
	private Consumer<CaseInfo> beforeOpeningCaseCallack = null;
	
	/***
	 * Adds a case directory to be iterated.
	 * @param caseDirectory An additional case directory to be iterated.
	 */
	public void addCaseDirectory(File caseDirectory){
		caseDirectories.add(caseDirectory);
	}
	
	/***
	 * Adds a case directory to be iterated.
	 * @param caseDirectoryPath An additional case directory to be iterated.
	 */
	public void addCaseDirectory(String caseDirectoryPath){
		addCaseDirectory(new File(caseDirectoryPath));
	}
	
	/***
	 * Signals that no further processing should be performed after current case.
	 */
	public void signalAbort(){
		abortFurtherProcessing = true;
	}
	
	/***
	 * Begins iterating provided list of cases, passing the following information for each case to the provided callback:
	 * - The case object
	 * - Information about the case
	 * - Current index of the case provided
	 * - Total number of cases to be iterated
	 * @param caseWorkFunction Callback which does something with each case.
	 */
	public void withEachCase(CaseConsumer caseWorkFunction){
		withEachCase(null,caseWorkFunction);
	}
	
	/***
	 * Begins iterating provided list of cases, passing the following information for each case to the provided callback:
	 * - The case object
	 * - Information about the case
	 * - Current index of the case provided
	 * - Total number of cases to be iterated
	 * This method differs from {@link #withEachCase(CaseConsumer)} in that you provide the currently open case in the GUI
	 * (in Ruby this would usually be $current_case).  If the case open in the GUI matche a case from the list of cases
	 * to be processed, this method will hand the callback the already open case object rather than trying to open the case
	 * a second time, which will result in an error.  
	 * @param currentCaseFromGui Case currently open in the GUI, in Ruby scripts this is usually $current_case
	 * @param caseWorkFunction Callback which does something with each case.
	 */
	public void withEachCase(Case currentCaseFromGui, CaseConsumer caseWorkFunction){
		if(caseWorkFunction == null){
			throw new IllegalArgumentException("Argument caseWorkFunction cannot be null");
		}
		
		List<CaseInfo> caseInfos = CaseInfo.fromCaseDirectories(caseDirectories);
		abortFurtherProcessing = false;
		boolean isGuiCurrentCase = false;
		
		Map<String,Object> caseOpenSettings = new HashMap<String,Object>();
		caseOpenSettings.put("migrate", allowCaseMigration);
				
		for (int caseIndex = 0; caseIndex < caseInfos.size(); caseIndex++) {
			if(abortFurtherProcessing){
				logger.info("Aborting further bulk case processing");
				break;
			}
			
			CaseInfo caseInfo = caseInfos.get(caseIndex);
			logger.info("Processing case: \n"+caseInfo.toString());
			if(beforeOpeningCaseCallack != null){
				beforeOpeningCaseCallack.accept(caseInfo);
			}
			
			// Determine if this is actually the GUI's $current_case because we have to
			// handle some things differently
			if(currentCaseFromGui != null && caseInfo.hasSameGuidAs(currentCaseFromGui)){
				isGuiCurrentCase = true;
			} else {
				isGuiCurrentCase = false;
			}
			
			// Check if this case is not the currently open case in the GUI and is locked
			if(!isGuiCurrentCase && caseInfo.isLocked()){
				logger.info("Case is locked");
				if(caseLockedCallback != null){
					// On case locked notify callback if was provided
					CaseLockedEventInfo caseLockedInfo = new CaseLockedEventInfo(caseInfo);
					caseLockedCallback.accept(caseLockedInfo);
					// Proceed based on callback reaction
					switch (caseLockedInfo.getReaction()) {
						case Retry:
							logger.info("Callback instructed to retry");
							caseIndex--; // Cause this case to be retried
							break;
						case Abort:
							logger.info("Callback instructed to abort");
							abortFurtherProcessing = true; // Logic to abort
							break;
						default:
							logger.info("Callback instructed to skip case");
							break; // Do nothing to proceed to next case
					}
				}
			}
			else
			{
				// Case was not locked or was GUI currently open case
				
				Case nuixCase = null;
				try {
					// Logic is a bit different depending on whether this is GUI current case
					if(isGuiCurrentCase){
						// If this is $current_case from the GUI don't try to re-open it
						logger.info("Using GUI current case");
						nuixCase = currentCaseFromGui;
					} else {
						// Not the GUI currently open case so we should open it ourselves
						logger.info("Opening case (migration allowed = "+allowCaseMigration+")");
						// If exception is thrown here, outer catch should handle it
						nuixCase = SuperUtilities.getInstance().getNuixUtilities()
								.getCaseFactory().open(caseInfo.getCaseDirectory(),caseOpenSettings);
					}
					
					// If we reach here then we should have an open case to hand off to the user's code
					try {
						// User code can signal to abort processing by yielding false, all other values
						// (true, nil) should be ignored
						Boolean result = null;
						result = caseWorkFunction.acceptCase(nuixCase, caseInfo, caseIndex, caseInfos.size());
						if(result != null && result == false){
							break;
						}
					} catch (Exception e) {
						// User code threw exception but didnt catch it
						logger.error("Error in user provided case work function:");
						logger.error(e);
						if(userFunctionErrorCallback != null){
							WorkFunctionErrorEvent userFunctionErrorInfo = new WorkFunctionErrorEvent(caseInfo, e);
							userFunctionErrorCallback.accept(userFunctionErrorInfo);
							// Proceed based on callback reaction
							switch (userFunctionErrorInfo.getReaction()) {
								case Retry:
									logger.info("Callback instructed to retry, invalid response, aborting instead");
									abortFurtherProcessing = true; // Logic to abort
									break;
								case Abort:
									logger.info("Callback instructed to abort");
									abortFurtherProcessing = true; // Logic to abort
									break;
								default:
									logger.info("Callback instructed to skip case");
									break; // Do nothing to proceed to next case
							}
						}
					}
					
				} catch (Exception e) {
					logger.error("Error opening case: ");
					logger.error(e);
					// On case open error notify callback if was provided
					if(caseErrorCallback != null){
						CaseOpenErrorEvent caseErrorInfo = new CaseOpenErrorEvent(caseInfo, e);
						caseErrorCallback.accept(caseErrorInfo);
						// Proceed based on callback reaction
						switch (caseErrorInfo.getReaction()) {
							case Retry:
								logger.info("Callback instructed to retry");
								caseIndex--; // Cause this case to be retried
								break;
							case Abort:
								logger.info("Callback instructed to abort");
								abortFurtherProcessing = true; // Logic to abort
								break;
							default:
								logger.info("Callback instructed to skip case");
								break; // Do nothing to proceed to next case
						}
					}
				} finally {
					if(nuixCase != null && !isGuiCurrentCase){
						logger.info("Closing case");
						nuixCase.close();
					}
				}
			}
		}
	}
	
	/***
	 * Optional callback which is called when a case is found to be locked in another session of Nuix while being opened.
	 * Callback is provided instance of {@link CaseLockInfo} with information about who is locking that case.
	 * @param callback Callback to handle when a case is locked.
	 */
	public void onCaseIsLocked(Consumer<CaseLockedEventInfo> callback){
		caseLockedCallback = callback;
	}
	
	/***
	 * Optional callback which is called when opening a case has an error.
	 * @param callback Callback to handle when opening a case throws an error.
	 */
	public void onErrorOpeningCase(Consumer<CaseOpenErrorEvent> callback){
		caseErrorCallback = callback;
	}
	
	/***
	 * Optional callback which is called when callback provided to {@link #withEachCase(CaseConsumer)} or {@link #withEachCase(Case, CaseConsumer)}
	 * throws an exception.
	 * @param callback Callback to handle when user provided callback throws an exception.
	 */
	public void onUserFunctionError(Consumer<WorkFunctionErrorEvent> callback){
		userFunctionErrorCallback = callback;
	}
	
	/***
	 * Optional callback which is called before opening each case.
	 * @param callback The callback to invoke before opening each case
	 */
	public void beforeOpeningCase(Consumer<CaseInfo> callback){
		beforeOpeningCaseCallack = callback;
	}
	
	/***
	 * Gets whether Nuix will be allowed to migrate cases as they are opened.
	 * @return True if Nuix will be allowed to migrate cases as they are opened.
	 */
	public boolean getAllowCaseMigration() {
		return allowCaseMigration;
	}

	/***
	 * Sets whether Nuix will be allowed to migrate cases as they are opened.
	 * @param allowCaseMigration True if Nuix will be allowed to migrate cases as they are opened.
	 */
	public void setAllowCaseMigration(boolean allowCaseMigration) {
		this.allowCaseMigration = allowCaseMigration;
	}
}
