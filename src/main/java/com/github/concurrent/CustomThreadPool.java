package com.github.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class CustomThreadPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomThreadPool.class);

    private final ReentrantLock lock = new ReentrantLock();


}
