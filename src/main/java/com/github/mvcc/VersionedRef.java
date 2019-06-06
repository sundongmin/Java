package com.github.mvcc;

public final class VersionedRef<T> {
    final T value;
    final long version;

    public VersionedRef(T value, long version) {
        this.value = value;
        this.version = version;
    }
}
