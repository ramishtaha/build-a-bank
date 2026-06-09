// playground/java-basics/src/test/java/com/buildabank/basics/jvm/JvmLabTest.java
package com.buildabank.basics.jvm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The JVM-lab programs must be correct as well as good GC/JIT/JFR targets. */
class JvmLabTest {

    @Test
    void bytecodeSampleComputesCorrectly() {
        assertThat(BytecodeSample.add(2, 3)).isEqualTo(5);
        assertThat(BytecodeSample.sumTo(100)).isEqualTo(5050L);
    }

    @Test
    void allocationChecksumIsDeterministic() {
        // sum of (i & 0xFF) for i in [0,256) = 0+1+...+255 = 32640.
        assertThat(AllocationDemo.run(256)).isEqualTo(32640L);
    }
}
