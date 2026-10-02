package com.keni.doctorappointment.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Clock} whose "now" can be moved, so time-dependent rules can be
 * tested without waiting for real hours to pass.
 *
 * <p>Several booking rules only fire once time has moved on - an appointment
 * can be completed only after it ended, and a patient may be marked as no-show
 * only then either. Reaching those states in a test otherwise means editing
 * timestamps in the database behind the API's back, which tests the SQL rather
 * than the application. Pinning this clock instead keeps the whole journey on
 * the public API: book, approve, move time, complete, rate.</p>
 *
 * <p>Because the production code takes its time from the {@code Clock} bean
 * (see {@code TimeConfig}), substituting this one changes the behaviour of the
 * running application without touching any rows.</p>
 */
public class MutableClock extends Clock {

	private final AtomicReference<Instant> current;

	private final ZoneId zone;

	public MutableClock(Instant start, ZoneId zone) {
		this.current = new AtomicReference<>(start);
		this.zone = zone;
	}

	/** Moves the clock forward (or backward, with a negative amount). */
	public void advance(Duration amount) {
		current.updateAndGet(now -> now.plus(amount));
	}

	/**
	 * Jumps to an absolute instant. The Spring context (and therefore this bean)
	 * is shared between test methods of a class, so {@code @BeforeEach} should
	 * reset the clock instead of assuming it started at its initial value.
	 */
	public void set(Instant instant) {
		current.set(instant);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		return new MutableClock(current.get(), newZone);
	}

	@Override
	public Instant instant() {
		return current.get();
	}

}