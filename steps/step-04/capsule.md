# 🧳 Capsule — Step 4

**Exists now:** `playground/java-basics` module only — packages `lang` (Step 2), `net` (Step 3), `jvm` (new); 22 tests, `./mvnw -pl playground/java-basics -am verify` = BUILD SUCCESS; `steps/step-04/smoke.sh` PASSED. No services, endpoints, ports, Docker, or `requests.http` — the play surface is the JVM CLI.

**This step added:**
- `com.buildabank.basics.jvm.BytecodeSample` — `add`/`sumTo`, disassembled with `javap -c`.
- `com.buildabank.basics.jvm.AllocationDemo` — escaping ring-buffer allocation loop; deterministic checksum (`run(256)` == 32640L, pinned by test).
- `JvmLabTest` — 2 tests (module 20 → 22).
- Captured observations: `-Xlog:gc` G1 young evacuation pauses; `-XX:+PrintCompilation` C1/C2/OSR/deopt; `jfr summary`; `-Xlog:class+load` (~826 classes, CDS "shared objects file"); escape-analysis discovery.

**Gotchas:**
- `AllocationDemo` must KEEP `ring[i & 1023] = block;` — without it escape analysis erases all allocation (zero GC) and smoke.sh's documented behavior breaks.
- GC/JIT output is nondeterministic — never assert on exact `-Xlog:gc`/`PrintCompilation` lines; JFR counts are samples, not totals.
- Build/observe commands are bash-only (`CP=…`, `grep`) — Windows learners must use Git Bash.
- `alloc.jfr` is a build artifact — never commit it.

**Callback hooks:**
- Escape analysis / "the fastest allocation is the one that never happens" → Step 55 (GC tuning, JMH, JFR in depth).
- Thread stacks are confined, the heap is shared → Step 11 (concurrency / JMM).
- Classloader delegation as a trust boundary → Spring bean loading (Steps 5–7) and supply-chain security (Phase H).

**Next step starts:** tag `step-04-end` == step-5-start. Green: BUILD SUCCESS, 22 tests, smoke.sh PASSED. Step 5 = Spring Core & IoC.
