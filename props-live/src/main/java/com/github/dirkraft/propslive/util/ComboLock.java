package com.github.dirkraft.propslive.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Simple wrapper around multiple {@link ReadWriteLock}s to facilitate very limited group locking.
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class ComboLock implements ReadWriteLock {

    private static final ThreadLocal<Lock> comboWriteLock_acquireFail = new ThreadLocal<Lock>();

    private final Lock comboReadLock;
    private final Lock comboWriteLock;

    public ComboLock(ReadWriteLock... constituentLocks) {
        this(Arrays.asList(constituentLocks));
    }

    public ComboLock(Collection<ReadWriteLock> constituentLocks) {
        final List<Lock> readLocks = new ArrayList<Lock>(constituentLocks.size());
        final List<Lock> writeLocks = new ArrayList<Lock>(constituentLocks.size());

        for (ReadWriteLock constituentLock : constituentLocks) {
            readLocks.add(constituentLock.readLock());
            writeLocks.add(constituentLock.writeLock());
        }

        // only implementing what we need here
        comboReadLock = new Lock() {
            @Override
            public void lock() {
                for (Lock readLock : readLocks) {
                    readLock.lock();
                }
            }

            @Override
            public void lockInterruptibly() throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean tryLock() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void unlock() {
                // If only lock() is implemented, then the current thread must have successfully taken all locks.
                for (Lock readLock : readLocks) {
                    readLock.unlock();
                }
            }

            @Override
            public Condition newCondition() {
                throw new UnsupportedOperationException();
            }
        };

        comboWriteLock = new Lock() {
            @Override
            public void lock() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void lockInterruptibly() throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean tryLock() {
                boolean acquired = true;
                for (Lock writeLock : writeLocks) {
                    acquired = writeLock.tryLock();
                    if (!acquired) {
                        comboWriteLock_acquireFail.set(writeLock);
                        break;
                    }
                }
                return acquired;
            }

            @Override
            public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void unlock() {
                // If only tryLock is implemented, only can unlock up to before the lock that failed to be acquire.
                Lock failedLock = comboWriteLock_acquireFail.get();
                for (Lock writeLock : writeLocks) {
                    if (writeLock == failedLock) {
                        break;
                    }
                    writeLock.unlock();
                }
                comboWriteLock_acquireFail.remove();
            }

            @Override
            public Condition newCondition() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Lock readLock() {
        return comboReadLock;
    }

    @Override
    public Lock writeLock() {
        return comboWriteLock;
    }

}

