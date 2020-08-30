package scw.locks;

import java.util.concurrent.TimeUnit;

import scw.core.GlobalPropertyFactory;

public abstract class AbstractLock implements Lock {
	private static final long DEFAULT_SLEEP_TIME = GlobalPropertyFactory
			.getInstance().getValue("lock.sleep.time", Long.class, 1L);

	public boolean tryLock(long period, TimeUnit timeUnit)
			throws InterruptedException {
		boolean b = false;
		while (!(b = tryLock())) {
			timeUnit.sleep(period);
		}
		return b;
	}

	/**
	 * 默认为ms试一次
	 */
	public void lock() {
		try {
			lockInterruptibly();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void lockInterruptibly() throws InterruptedException {
		while (!tryLock()) {
			TimeUnit.MILLISECONDS.sleep(DEFAULT_SLEEP_TIME);
		}
	}
}