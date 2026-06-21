package com.isxcode.spark.common.locker;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.isxcode.spark.common.cluster.ClusterNodeOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class Locker {

    private static final long LOCK_EXPIRE_HOURS = 24;

    private static final long LOCK_WAIT_INTERVAL_MILLIS = 500;

    private static final long SHORT_LOCK_WAIT_INTERVAL_MILLIS = 50;

    private final LockerRepository lockerRepository;

    private final PlatformTransactionManager transactionManager;

    private final Map<String, Object> localLockMonitors = new ConcurrentHashMap<>();

    /**
     * 加锁.
     */
    public Integer lockOnly(String name) {

        return lock(name);
    }

    /**
     * 加锁.
     */
    public Integer lockOnly(String name, String box) {

        return lock(name, box);
    }

    /**
     * 加锁.
     */
    public Integer lock(String name) {

        return lock(name, null);
    }

    /**
     * 尝试加锁，不等待.
     */
    public Integer tryLock(String name) {

        return tryLock(name, null);
    }

    /**
     * 尝试加锁，不等待.
     */
    public Integer tryLock(String name, String box) {

        clearExpiredLocks();
        return tryAcquire(name, box);
    }

    /**
     * 等待一段时间加锁.
     */
    public Integer waitLock(String name, long waitMillis) {

        long endTime = System.currentTimeMillis() + waitMillis;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() <= endTime) {
            clearExpiredLocks();
            Integer lockId = tryAcquire(name, null);
            if (lockId != null) {
                return lockId;
            }
            log.debug("Waiting for database lock: {}", name);
            sleepQuietly(SHORT_LOCK_WAIT_INTERVAL_MILLIS);
        }

        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private Integer lock(String name, String box) {

        while (!Thread.currentThread().isInterrupted()) {
            clearExpiredLocks();
            Integer lockId = tryAcquire(name, box);
            if (lockId != null) {
                return lockId;
            } else {
                log.debug("Waiting for database lock: {}", name);
                sleepQuietly();
            }
        }

        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for database lock: " + name);
    }

    private Integer tryAcquire(String name, String box) {

        Object localMonitor = localLockMonitors.computeIfAbsent(name, ignored -> new Object());
        synchronized (localMonitor) {
            if (lockerRepository.existsByName(name)) {
                return null;
            }

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(status -> {
                try {
                    return lockerRepository.saveAndFlush(buildLocker(name, box)).getId();
                } catch (DataIntegrityViolationException e) {
                    status.setRollbackOnly();
                    return null;
                }
            });
        }
    }

    /**
     * 是否加锁.
     */
    public Boolean isLocked(String name, Integer id) {

        try {
            Thread.sleep(LOCK_WAIT_INTERVAL_MILLIS);
        } catch (InterruptedException ignored) {
            // 防止没有写进去
            Thread.currentThread().interrupt();
        }

        return lockerRepository.findByName(name).map(lockerEntity -> !Objects.equals(lockerEntity.getId(), id))
            .orElse(false);
    }

    /**
     * 判断是否可行.
     */
    public Boolean isLocked(String name, String box) {

        try {
            Thread.sleep(LOCK_WAIT_INTERVAL_MILLIS);
        } catch (InterruptedException ignored) {
            // 防止没有写进去
            Thread.currentThread().interrupt();
        }

        return lockerRepository.findByName(name).map(lockerEntity -> !Objects.equals(lockerEntity.getBox(), box))
            .orElse(false);
    }

    /**
     * 解锁.
     */
    public void unlock(Integer id) {

        lockerRepository.deleteById(id);
    }

    /**
     * 清理锁.
     */
    public void clearLock(String name) {

        // 将name相关的删掉
        lockerRepository.deleteAll(lockerRepository.findAllByName(name));
    }

    /**
     * 清理锁.
     */
    public void deleteLock(String name) {

        lockerRepository.findByName(name).ifPresent(lockerRepository::delete);
    }

    /**
     * Clear locks that belong to this application instance and expired locks.
     */
    public void clearCurrentOwnerAndExpiredLocks() {

        lockerRepository.deleteAllByOwner(getOwner());
        String hostName = ClusterNodeOwner.getHostName();
        if (hostName != null && !hostName.isBlank()) {
            lockerRepository.deleteAllByOwnerEndingWith("@" + hostName);
        }
        clearExpiredLocks();
    }

    /**
     * Clear all locks when the application starts.
     */
    public void clearStartupLocks() {

        lockerRepository.deleteAllInBatch();
    }

    private void clearExpiredLocks() {

        lockerRepository.deleteAllByExpireTimeBefore(LocalDateTime.now());
    }

    private LockerEntity buildLocker(String name, String box) {

        LocalDateTime now = LocalDateTime.now();
        return LockerEntity.builder().name(name).box(box).owner(getOwner()).createDateTime(now)
            .expireTime(now.plusHours(LOCK_EXPIRE_HOURS)).build();
    }

    private String getOwner() {

        return ClusterNodeOwner.getOwner();
    }

    private void sleepQuietly() {

        sleepQuietly(LOCK_WAIT_INTERVAL_MILLIS);
    }

    private void sleepQuietly(long waitIntervalMillis) {

        try {
            Thread.sleep(waitIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
