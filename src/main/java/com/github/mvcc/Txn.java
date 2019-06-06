package com.github.mvcc;

public interface Txn {
    <T> T get(TxnRef<T> ref);

    <T> void set(TxnRef ref, T value);
}
