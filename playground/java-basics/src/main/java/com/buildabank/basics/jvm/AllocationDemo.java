// playground/java-basics/src/main/java/com/buildabank/basics/jvm/AllocationDemo.java
package com.buildabank.basics.jvm;

/**
 * Generates lots of short-lived garbage so the JVM's <strong>garbage collector</strong> and
 * <strong>JIT compiler</strong> become observable. Run it with diagnostics to SEE the JVM work:
 *
 * <pre>
 *   # GC events (small heap forces frequent young collections):
 *   java -Xmx64m -Xlog:gc -cp target/classes com.buildabank.basics.jvm.AllocationDemo
 *
 *   # JIT compilation of the hot method:
 *   java -XX:+PrintCompilation -cp target/classes com.buildabank.basics.jvm.AllocationDemo
 *
 *   # Java Flight Recorder profile:
 *   java -XX:StartFlightRecording=duration=10s,filename=alloc.jfr -cp target/classes com.buildabank.basics.jvm.AllocationDemo
 *   jfr summary alloc.jfr
 * </pre>
 *
 * <p>Each iteration allocates a 128-byte block (a short-lived object) and folds one byte into a checksum
 * so the JIT cannot optimize the allocation away as dead code.
 */
public final class AllocationDemo {

    private AllocationDemo() { }

    /** Allocates {@code iterations} blocks; returns a deterministic checksum = sum of (i &amp; 0xFF). */
    public static long run(int iterations) {
        // The blocks are stashed in a small ring buffer so they ESCAPE the loop. Without this, the JIT's
        // escape analysis would scalar-replace the byte[128] and no heap allocation (or GC) would happen
        // at all — a real lesson in itself. With it, old entries become unreachable → genuine young-gen GC.
        byte[][] ring = new byte[1024][];
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            byte[] block = new byte[128];
            block[0] = (byte) (i & 0xFF);
            ring[i & 1023] = block;             // escapes → forces real heap allocation
            checksum += block[0] & 0xFF;        // use the result so it isn't eliminated
        }
        return checksum;
    }

    public static void main(String[] args) {
        int iterations = (args.length > 0) ? Integer.parseInt(args[0]) : 20_000_000;
        long start = System.nanoTime();
        long checksum = run(iterations);
        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("Allocated %,d blocks in %,d ms (checksum=%d)%n", iterations, millis, checksum);
    }
}
