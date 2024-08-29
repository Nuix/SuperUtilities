package com.nuix.superutilities.misc;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/***
 * A wrapper for a {@link Consumer} instance which will only periodically forward call to <code>accept</code>
 * method of wrapped instance.  Created for taking per item events that Nuix publishes and turning them
 * into periodic progress reporters.
 * @param <T> The type of the {@link Consumer} being wrapped
 */
public class PeriodicGatedConsumer<T> implements Consumer<T> {
    private final Consumer<T> wrappedConsumer;
    private long lastPassThru = 0;

    @Getter
    @Setter
    private long intervalMillis = 1000;

    public PeriodicGatedConsumer(@NotNull Consumer<T> wrappedConsumer) {
        this.wrappedConsumer = wrappedConsumer;
    }

    public PeriodicGatedConsumer(@NotNull Consumer<T> wrappedConsumer, long intervalMillis) {
        this.wrappedConsumer = wrappedConsumer;
        this.intervalMillis = intervalMillis;
    }

    /***
     * Convenience method for setting millis interval to a certain number of seconds
     * @param intervalSeconds The number of seconds
     */
    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalMillis = intervalSeconds * 1000;
    }

    /***
     * Convenience method for setting millis interval to a certain number of minutes
     * @param intervalMinutes The number of minutes
     */
    public void setIntervalMinutes(long intervalMinutes) {
        this.intervalMillis = intervalMinutes * 60 * 1000;
    }

    /***
     * Will periodically forward call to wrapped Consumer and then reset current interval
     * @param t the input argument
     */
    @Override
    public void accept(T t) {
        if (intervalMillis < 1 || System.currentTimeMillis() - lastPassThru > intervalMillis) {
            wrappedConsumer.accept(t);
            lastPassThru = System.currentTimeMillis();
        }
    }

    /***
     * Will immediately forward call to wrapped Consumer and then reset current interval
     * @param t the input argument
     */
    public void acceptImmediately(T t) {
        wrappedConsumer.accept(t);
        lastPassThru = System.currentTimeMillis();
    }
}
