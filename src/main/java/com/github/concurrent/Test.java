package com.github.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Test {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws InterruptedException {
        CustomThreadPool pool = new CustomThreadPool(3, 5, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), null);
        for (int i = 0; i < 10; i++) {
            pool.execute(new Worker(i));
        }

        LOGGER.info("====休眠前线程池活跃线程数={}====", pool.getWorkerCount());

        TimeUnit.SECONDS.sleep(5);
        LOGGER.info("====休眠后线程池活跃线程数={}====", pool.getWorkerCount());
        for (int i = 0; i < 3; i++) {
            pool.execute(new Worker(i + 100));
        }

        pool.shutdown();
        LOGGER.info("++++++++++++++++");
    }

    static class Worker implements Runnable {
        private int state;

        public Worker(int state) {
            this.state = state;
        }

        @Override
        public void run() {
            LOGGER.info(toString());
        }

        @Override
        public String toString() {
            return "Worker{" +
                    "state=" + state +
                    '}';
        }
    }

}
