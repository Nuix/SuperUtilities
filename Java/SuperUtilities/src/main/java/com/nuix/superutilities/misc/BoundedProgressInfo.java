package com.nuix.superutilities.misc;

import lombok.Getter;

/***
 * Represents progress of an operation which has a bounded value, that is, we know what progress
 * value is considered maximum/done.
 */
@Getter
public class BoundedProgressInfo extends ProgressInfo {
    /***
     * The maximum progress value
     */
    protected final long maximum;

    public BoundedProgressInfo(String stage, long current, long maximum) {
        super(stage, current);
        this.maximum = maximum;
    }

    /***
     * Provides a percentage completed value by dividing double value of current by
     * double value of maximum.
     * @return A double value representing percentage complete
     */
    public double percentageComplete() {
        return ((double) current) / ((double) maximum);
    }

    @Override
    public String toString() {
        return "BoundedProgressInfo [stage=" + stage + ", " + current + "/" + maximum + "]";
    }
}
