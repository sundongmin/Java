package com.github.mvcc;

import sun.misc.Version;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class STMTxn implements Txn {

    private static AtomicInteger txnSeq = new AtomicInteger(0);

    private Map<TxnRef, VersionedRef> inTxnMap = new HashMap<>();

    private Map<TxnRef, Object> writeMap = new HashMap<>();

    private long txnId;

    public STMTxn() {
        this.txnId = txnSeq.incrementAndGet();
    }

    @Override
    public <T> T get(TxnRef<T> ref) {
        if (!inTxnMap.containsKey(ref)) {
            inTxnMap.put(ref, ref.curVersionedRef);
        }

        return (T) inTxnMap.get(ref).value;
    }

    @Override
    public <T> void set(TxnRef ref, T value) {
        if (!inTxnMap.containsKey(ref)) {
            inTxnMap.put(ref, ref.curVersionedRef);
        }

        writeMap.put(ref, value);
    }

    boolean commit() {
        synchronized (STM.commitLock) {
            boolean isValid = true;

            for (Map.Entry<TxnRef, VersionedRef> entry : inTxnMap.entrySet()) {
                VersionedRef curRef = entry.getKey().curVersionedRef;
                VersionedRef readRef = entry.getValue();

                if (curRef.version != readRef.version) {
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                writeMap.forEach((k, v) -> k.curVersionedRef = new VersionedRef(v, txnId));
            }

            return isValid;
        }
    }
}
