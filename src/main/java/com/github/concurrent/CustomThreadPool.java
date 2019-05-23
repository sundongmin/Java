package com.github.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CustomThreadPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomThreadPool.class);

    private final ReentrantLock lock = new ReentrantLock();

    private volatile int minSize;

    private volatile int maxSize;

    private long keepAliveTime;

    private TimeUnit unit;

    private BlockingQueue<Runnable> workQueue;

    private volatile Set<Worker> workers;

    private AtomicBoolean isShutDown = new AtomicBoolean(false);

    private AtomicInteger totalTask = new AtomicInteger();

    private Object shutDownNotify = new Object();

    private Notify notify;

    public CustomThreadPool(int minSize, int maxSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, Notify notify) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.workQueue = workQueue;
        this.notify = notify;

        workers = new ConcurrentHashSet<>();
    }

    public void execute(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable null");
        }

        if (isShutDown.get()) {
            LOGGER.info("thread pool is shutdown");
            return;
        }

        totalTask.incrementAndGet();

        if (workers.size() < minSize) {
            addWorker(runnable);
            return;
        }

        boolean offer = workQueue.offer(runnable);
        if (!offer) {
            if (workers.size() < maxSize) {
                addWorker(runnable);
                return;
            } else {
                LOGGER.error("超过最大线程数");
                try {
                    workQueue.put(runnable);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private void addWorker(Runnable runnable) {
        Worker worker = new Worker(runnable, true);
        worker.startTask();
        workers.add(worker);
    }


    private final class Worker extends Thread {
        private Runnable task;
        private Thread thread;

        private boolean isNewTask;

        public Worker(Runnable task, boolean isNewTask) {
            this.task = task;
            this.isNewTask = isNewTask;
            thread = this;
        }

        public void startTask() {
            thread.start();
        }

        public void close() {
            thread.interrupt();
        }

        @Override
        public void run() {
            Runnable task = null;

            if (isNewTask) {
                task = this.task;
            }

            try {
                while ((task != null || (task = getTask()) != null)) {
                    try {
                        task.run();
                    } finally {
                        task = null;
                        int num = totalTask.decrementAndGet();

                        if (num == 0) {
                            synchronized (shutDownNotify) {
                                shutDownNotify.notify();
                            }
                        }
                    }
                }
            } finally {
                boolean remove = workers.remove(this);
                tryClose(true);
            }

        }

    }

    private Runnable getTask() {
        if (isShutDown.get() && totalTask.get() == 0) {
            return null;
        }

        lock.lock();

        try {
            Runnable task = null;
            if (workers.size() > minSize) {
                task = workQueue.poll(keepAliveTime, unit);
            } else {
                task = workQueue.take();
            }

            if (task != null) {
                return task;
            }

        } catch (InterruptedException e) {
            return null;
        } finally {
            lock.unlock();
        }
        return null;
    }

    private void tryClose(boolean isTry) {
        if (!isTry) {
            closeAllTask();
        } else {
            if (isShutDown.get() && totalTask.get() == 0) {
                closeAllTask();
            }
        }
    }

    private void closeAllTask() {
        for (Worker worker : workers) {
            worker.close();
        }
    }


    public void shutdown() {
        isShutDown.set(true);
        tryClose(true);
    }

    public void shutdownNow() {
        isShutDown.set(true);
        tryClose(false);
    }

    public int getWorkerCount() {
        return workers.size();
    }

    private final class ConcurrentHashSet<T> extends AbstractSet<T> {
        private ConcurrentHashMap<T, Object> map = new ConcurrentHashMap<>();
        private final Object present = new Object();

        private AtomicInteger count = new AtomicInteger();

        @Override
        public Iterator<T> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public boolean add(T t) {
            count.incrementAndGet();
            return map.put(t, present) == null;
        }

        @Override
        public boolean remove(Object o) {
            count.decrementAndGet();
            return map.remove(o) == present;
        }

        @Override
        public int size() {
            return count.get();
        }
    }

}
