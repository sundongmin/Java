package com.github.mvcc;

public class Account {

    private TxnRef<Integer> balance;

    public Account(int balance) {
        this.balance = new TxnRef<>(balance);
    }

    public void transfer(Account target, int amt) {
        STM.atomic(txn -> {
            Integer from = balance.getValue(txn);
            balance.setValue(from - amt, txn);
            Integer to = target.balance.getValue(txn);
            target.balance.setValue(to + amt, txn);
        });
    }

}
