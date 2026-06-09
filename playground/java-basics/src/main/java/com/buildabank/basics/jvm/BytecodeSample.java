// playground/java-basics/src/main/java/com/buildabank/basics/jvm/BytecodeSample.java
package com.buildabank.basics.jvm;

/**
 * A deliberately tiny class so its compiled <strong>bytecode</strong> is easy to read.
 *
 * <p>Compile it, then disassemble with {@code javap -c -p com.buildabank.basics.jvm.BytecodeSample}
 * to see the JVM instructions ({@code iload}, {@code iadd}, {@code ireturn}, the loop's {@code goto}/{@code if_icmpgt}).
 * This is the gap between the Java you write and the bytecode the JVM actually executes.
 */
public final class BytecodeSample {

    private BytecodeSample() { }

    /** Compiles to: load arg 0, load arg 1, iadd, ireturn. */
    public static int add(int a, int b) {
        return a + b;
    }

    /** A counted loop — note the bytecode branch instructions in {@code javap -c}. */
    public static long sumTo(int n) {
        long total = 0;
        for (int i = 1; i <= n; i++) {
            total += i;
        }
        return total;
    }

    public static void main(String[] args) {
        System.out.println("add(2, 3)   = " + add(2, 3));
        System.out.println("sumTo(100)  = " + sumTo(100));
    }
}
