package com.nuix.superutilities.misc;

import lombok.Getter;

/***
 * Represents progress of an indeterminate operation where the maximum progress value
 * is not known until completion.
 */
@Getter
public class ProgressInfo {
    /***
     * A title representing the current stage, may be null or blank if progress
     * publisher does not populate it!
     */
    protected final String stage;

    /***
     * The current progress value
     */
    protected final long current;

    public ProgressInfo(String stage, long current) {
        this.stage = stage;
        this.current = current;
    }

    @Override
    public String toString() {
        return "ProgressInfo [stage=" + stage + ", current=" + current + "]";
    }
}
