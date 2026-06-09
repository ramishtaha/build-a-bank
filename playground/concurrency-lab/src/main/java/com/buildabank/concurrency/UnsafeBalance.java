// playground/concurrency-lab/src/main/java/com/buildabank/concurrency/UnsafeBalance.java
package com.buildabank.concurrency;

/**
 * BROKEN on purpose. {@code balance += amount} looks atomic but is three steps under the hood — read the
 * field, add, write it back. Two threads can both read the same old value and both write back, so one
 * deposit is silently lost. The field isn't even {@code volatile}, so updates may also be invisible across
 * threads. This is the bug we prove, then fix.
 */
public class UnsafeBalance implements Balance {

    private long balance;

    @Override
    public void deposit(long amount) {
        balance += amount;   // read-modify-write — NOT atomic
    }

    @Override
    public long get() {
        return balance;
    }
}
