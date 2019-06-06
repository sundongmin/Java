package com.github.mvcc;

public class TxnRef<T> {
    volatile VersionedRef curVersionedRef;

    public TxnRef(T value) {
        this.curVersionedRef = new VersionedRef(value, 0L);
    }

    public T getValue(Txn txn) {
        return txn.get(this);
    }

    public void setValue(T value, Txn txn) {
        txn.set(this, value);
    }
}
