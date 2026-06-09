// playground/distributed-lab/src/main/java/com/buildabank/distributed/quorum/QuorumSystem.java
package com.buildabank.distributed.quorum;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A tiny <strong>quorum-replicated register</strong> over {@code N} replicas, each holding a
 * <em>versioned</em> value. A write persists to a chosen set of replicas (the write quorum {@code W}); a read
 * queries a chosen set (the read quorum {@code R}) and returns the highest-versioned value it sees.
 *
 * <p><strong>The theorem this lab proves:</strong> if {@code W + R > N}, then <em>every</em> read quorum and
 * <em>every</em> write quorum must overlap in at least one replica (pigeonhole) — so a read is guaranteed to
 * observe the latest committed write (strong/quorum consistency). If {@code W + R ≤ N}, a read quorum can be
 * chosen disjoint from the last write quorum and return a <strong>stale</strong> value (eventual consistency).
 * This is the dial behind Dynamo-style stores and the read/write side of CAP.
 */
public final class QuorumSystem {

    /** A value tagged with the version at which it was written. */
    public record Versioned(long version, String value) {
        static final Versioned EMPTY = new Versioned(0, null);
    }

    private final Versioned[] replicas;
    private long versionCounter;

    public QuorumSystem(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("need at least one replica");
        }
        this.replicas = new Versioned[n];
        java.util.Arrays.fill(this.replicas, Versioned.EMPTY);
    }

    public int size() {
        return replicas.length;
    }

    /** Write {@code value} to the given write quorum, stamped with the next monotonic version. */
    public long write(Set<Integer> writeQuorum, String value) {
        long version = ++versionCounter;
        for (int replica : writeQuorum) {
            replicas[replica] = new Versioned(version, value);
        }
        return version;
    }

    /** Read from the given read quorum, returning the freshest (highest-version) value seen by the quorum. */
    public Versioned read(Set<Integer> readQuorum) {
        Versioned freshest = Versioned.EMPTY;
        for (int replica : readQuorum) {
            if (replicas[replica].version() > freshest.version()) {
                freshest = replicas[replica];
            }
        }
        return freshest;
    }

    /**
     * Empirically (by enumerating <em>every</em> W-subset and R-subset of {@code {0..n-1}}) decide whether
     * every write quorum necessarily intersects every read quorum. We don't just trust the {@code W+R>N}
     * formula — we check all combinations, which is feasible for the small N used in teaching.
     */
    public static boolean everyWriteAndReadQuorumIntersect(int n, int w, int r) {
        List<Set<Integer>> writeQuorums = subsetsOfSize(n, w);
        List<Set<Integer>> readQuorums = subsetsOfSize(n, r);
        for (Set<Integer> wq : writeQuorums) {
            for (Set<Integer> rq : readQuorums) {
                if (java.util.Collections.disjoint(wq, rq)) {
                    return false;   // found a disjoint pair → a read could miss this write
                }
            }
        }
        return true;
    }

    /** All subsets of {@code {0..n-1}} of exactly size {@code k}. */
    public static List<Set<Integer>> subsetsOfSize(int n, int k) {
        List<Set<Integer>> out = new ArrayList<>();
        combine(0, n, k, new ArrayList<>(), out);
        return out;
    }

    private static void combine(int start, int n, int k, List<Integer> acc, List<Set<Integer>> out) {
        if (acc.size() == k) {
            out.add(new java.util.HashSet<>(acc));
            return;
        }
        for (int i = start; i < n; i++) {
            acc.add(i);
            combine(i + 1, n, k, acc, out);
            acc.removeLast();
        }
    }
}
