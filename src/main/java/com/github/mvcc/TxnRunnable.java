package com.github.mvcc;

@FunctionalInterface
public interface TxnRunnable {

    void run(Txn txn);
}
