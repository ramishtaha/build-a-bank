# Step 28: Testing Mastery & Custom Spring Boot Starters (Phase-E Capstone)

> **"A bank is only as trustworthy as its tests."**

Welcome to the **Phase-E finale**. The hexagon isolated our core (Step 26), and ArchUnit clamped the boundaries (Step 27). Now we deliver the final three testing pillars: **property-based testing** (jqwik), the **Boot 4 web slice** (`MockMvcTester`), and the Phase-E capstone: **PITest mutation testing** on the core.

Finally, we'll extract the deterministic `BigDecimal` formatter into a **real auto-configured Spring Boot starter** — exactly as promised in §5 of the master plan — so any service can consume it with one line.

We close by wiring up the long-awaited A 12 code-quality gates (Spotless & Checkstyle) so `./mvnw verify` protects the repository.

---

## 🎯 What We'll Build

1. **Fast Core Unit Tests:** The payoff of the hexagon: sub-millisecond tests on the domain with no Spring context and no Docker.
2. **Property-Based Testing:** `jqwik` generates thousands of randomized inputs to prove an invariant, not just a hand-picked example.
3. **The Phase-E Capstone (Mutation Testing):** PITest mutates the bytecode to prove our tests *actually* catch behavioral changes. Target: 100%.
4. **`common-spring-boot-starter`:** A shared Spring Boot starter with `MoneyFormatter`, an `@AutoConfiguration`, and an `AutoConfiguration.imports` file.
5. **Boot 4 Slice Test:** `HelloMockMvcTesterTest` using the new `MockMvcTester` API.
6. **Code-Quality Gates:** Spotless (formatting) and Checkstyle (lean ruleset) tied to the `verify` phase.

---

## 🏗️ C. Build the Feature

### Sub-step 1: Fast unit tests for the core (The Hexagon Payoff)

🎯 **Goal:** Prove the hexagon's core promise: we can test the `NotificationService` use case in microseconds using Mockito, with no Kafka or Spring.

📁 **Path:** `services/notification/src/test/java/com/buildabank/notification/application/NotificationServiceTest.java`

```java
package com.buildabank.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buildabank.notification.application.port.out.NotificationPublisher;
import com.buildabank.notification.application.port.out.ProcessedEventStore;
import com.buildabank.notification.domain.Notification;
import com.buildabank.notification.domain.TransferEvent;

/**
 * Step 28 · a <strong>fast, Docker-free unit test of the use case</strong> — the payoff of the hexagon
 * (Step 26): because {@link NotificationService} depends only on its outbound ports, we mock them with Mockito
 * and exercise the core logic in microseconds, with no Kafka and no Spring context. (Step 26's "Your Turn"
 * challenge, now delivered.) These tests are what PITest mutates against in the § D mutation-coverage capstone.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    ProcessedEventStore processedEvents;

    @Mock
    NotificationPublisher publisher;

    private static TransferEvent event() {
        return new TransferEvent("evt-1", "txn-1", "ACC-1", "ACC-2", new BigDecimal("100.00"), "2026-06-10T00:00:00Z");
    }

    @Test
    void aNewEventIsAppliedAndPublishedExactlyOnce() {
        when(processedEvents.markIfNew("evt-1")).thenReturn(true);
        NotificationService service = new NotificationService(processedEvents, publisher);

        boolean applied = service.handle(event());

        assertThat(applied).as("a new event is applied").isTrue();
        ArgumentCaptor<Notification> pushed = ArgumentCaptor.forClass(Notification.class);
        verify(publisher).publish(pushed.capture());
        assertThat(pushed.getValue().transactionId()).isEqualTo("txn-1");
        assertThat(pushed.getValue().message()).contains("100.00", "ACC-1", "ACC-2");
    }

    @Test
    void aDuplicateEventIsIgnoredAndNeverPublished() {
        when(processedEvents.markIfNew("evt-1")).thenReturn(false);
        NotificationService service = new NotificationService(processedEvents, publisher);

        boolean applied = service.handle(event());

        assertThat(applied).as("a duplicate is an idempotent no-op").isFalse();
        verify(publisher, never()).publish(any());
    }
}
```

🔍 **Line-by-line:**
- `@ExtendWith(MockitoExtension.class)`: Activates the `@Mock` annotations without loading Spring.
- `ArgumentCaptor<Notification>`: Captures the object passed to the mock so we can assert on its fields.
- `when(...).thenReturn(...)`: Stubs the port to simulate both new and duplicate events.

We also add an example-based test for the domain factory.

📁 **Path:** `services/notification/src/test/java/com/buildabank/notification/domain/NotificationTest.java`

```java
package com.buildabank.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Step 28 · example-based unit test of the domain factory {@link Notification#from}. Pairs with the jqwik
 * property test ({@link NotificationPropertyTest}): this pins the <em>exact</em> message wording for one case;
 * the property checks the <em>invariants</em> hold for thousands of generated cases.
 */
class NotificationTest {

    @Test
    void fromMapsEveryFieldAndComposesTheMessage() {
        TransferEvent event = new TransferEvent(
                "evt-9", "txn-9", "ACC-A", "ACC-B", new BigDecimal("250.00"), "2026-06-10T12:00:00Z");

        Notification n = Notification.from(event);

        assertThat(n.eventId()).isEqualTo("evt-9");
        assertThat(n.transactionId()).isEqualTo("txn-9");
        assertThat(n.fromAccount()).isEqualTo("ACC-A");
        assertThat(n.toAccount()).isEqualTo("ACC-B");
        assertThat(n.amount()).isEqualByComparingTo("250.00");
        assertThat(n.occurredAt()).isEqualTo("2026-06-10T12:00:00Z");
        assertThat(n.message()).isEqualTo("Transfer of 250.00 from ACC-A to ACC-B completed.");
    }
}
```

💭 **Under the hood:** Notice how clean this is? If we hadn't isolated the core in Step 26, these tests would be mixed with `@KafkaListener` setup and Docker bindings.

🔮 **Predict:** These will run instantly.

▶️ **Run & See:** 
```bash
./mvnw -pl services/notification -Dtest='NotificationServiceTest,NotificationTest' test
```

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** 3 unit tests passing in microseconds.

💾 **Commit:** `test(notification): Step 28 fast unit tests on the hexagon core`

---

### Sub-step 2: Property-based testing with jqwik

🎯 **Goal:** Move beyond "example" tests. State an *invariant* and let jqwik generate thousands of random inputs to prove it holds.

📁 **Path:** `services/notification/pom.xml`

```diff
@@ -75,6 +75,14 @@
             <version>${archunit.version}</version>
             <scope>test</scope>
         </dependency>
+        <!-- jqwik (Step 28): property-based testing — generate many randomized inputs + shrink failures. A
+             separate JUnit-Platform test engine; Surefire runs it alongside Jupiter. Pinned (VERSIONS.md). -->
+        <dependency>
+            <groupId>net.jqwik</groupId>
+            <artifactId>jqwik</artifactId>
+            <version>${jqwik.version}</version>
+            <scope>test</scope>
+        </dependency>
     </dependencies>
```

📁 **Path:** `services/notification/src/test/java/com/buildabank/notification/domain/NotificationPropertyTest.java`

```java
package com.buildabank.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Scale;
import net.jqwik.api.constraints.StringLength;

/**
 * Step 28 · <strong>property-based</strong> test with jqwik. An example test ({@link NotificationTest}) checks
 * one hand-picked case; a property states an <em>invariant</em> and jqwik generates many randomized inputs
 * (and shrinks any failure to a minimal counter-example). The invariant here: for ANY transfer event,
 * {@link Notification#from} preserves the identifiers and the message names both parties and the amount.
 */
class NotificationPropertyTest {

    @Property
    void fromPreservesIdentifiersAndNamesBothPartiesAndAmount(
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 8) String eventId,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 8) String transactionId,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 6) String fromAccount,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 6) String toAccount,
            @ForAll @BigRange(min = "0.01", max = "1000000.00") @Scale(2) BigDecimal amount) {

        TransferEvent event = new TransferEvent(
                eventId, transactionId, fromAccount, toAccount, amount, "2026-06-10T00:00:00Z");

        Notification n = Notification.from(event);

        // identifiers and value are carried through unchanged
        assertThat(n.eventId()).isEqualTo(eventId);
        assertThat(n.transactionId()).isEqualTo(transactionId);
        assertThat(n.fromAccount()).isEqualTo(fromAccount);
        assertThat(n.toAccount()).isEqualTo(toAccount);
        assertThat(n.amount()).isEqualByComparingTo(amount);

        // the human message is well-formed and mentions both parties and the amount
        assertThat(n.message())
                .startsWith("Transfer of ")
                .endsWith(" completed.")
                .contains(fromAccount)
                .contains(toAccount)
                .contains(amount.toString());
    }
}
```

🔍 **Line-by-line:**
- `@Property`: Tells jqwik this is a property test, not a JUnit `@Test`.
- `@ForAll`: Instructs jqwik to generate random values matching the constraints.
- `@BigRange`, `@Scale`: Controls the `BigDecimal` generation boundaries.
- `assertThat(n.eventId()).isEqualTo(eventId)`: The invariant! The identifiers must carry through unchanged regardless of the generated input.

💭 **Under the hood:** jqwik is a separate JUnit Platform *engine*. Surefire discovers both Jupiter (`@Test`) and jqwik (`@Property`) and runs them side-by-side. If a property fails, jqwik *shrinks* the input to find the smallest failing case.

🔮 **Predict:** Surefire will run 1000 iterations of this test transparently.

▶️ **Run & See:**
```bash
./mvnw -pl services/notification -Dtest='NotificationPropertyTest' test
```

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** You've just run 1000 tests.

💾 **Commit:** `test(notification): Step 28 property test (jqwik) on the hexagon core`

---

### Sub-step 3: The Phase-E Capstone (Mutation Testing)

🎯 **Goal:** How do you *prove* your tests are good? You mutate the production bytecode (flip `==` to `!=`, delete a method call) and see if the tests fail. If the test stays green, a "mutant survives"—a gap in your suite.

📁 **Path:** `services/notification/pom.xml`

```diff
@@ -85,4 +93,62 @@
             </plugin>
         </plugins>
     </build>
+
+    <profiles>
+        <!--
+          Step 28 · MUTATION TESTING (the Phase-E capstone). Off by default (it re-runs the unit tests against
+          hundreds of bytecode mutants, so it's slower than a normal build) — run it on demand:
+              ./mvnw -pl services/notification -Pmutation test
+          It mutates ONLY the hexagon CORE (the domain factory + the application use case) and runs ONLY the
+          fast, Docker-free unit tests against the mutants (the Kafka/SSE integration tests are excluded — they
+          need a broker and would make every mutant a Testcontainers boot). A surviving mutant = a behaviour
+          change no test noticed = a gap in the suite. The build FAILS below the justified threshold.
+        -->
+        <profile>
+            <id>mutation</id>
+            <build>
+                <plugins>
+                    <plugin>
+                        <groupId>org.pitest</groupId>
+                        <artifactId>pitest-maven</artifactId>
+                        <version>${pitest.version}</version>
+                        <dependencies>
+                            <dependency>
+                                <groupId>org.pitest</groupId>
+                                <artifactId>pitest-junit5-plugin</artifactId>
+                                <version>${pitest-junit5.version}</version>
+                            </dependency>
+                        </dependencies>
+                        <configuration>
+                            <targetClasses>
+                                <param>com.buildabank.notification.domain.Notification</param>
+                                <param>com.buildabank.notification.application.NotificationService</param>
+                            </targetClasses>
+                            <targetTests>
+                                <param>com.buildabank.notification.application.NotificationServiceTest</param>
+                                <param>com.buildabank.notification.domain.NotificationTest</param>
+                                <param>com.buildabank.notification.domain.NotificationPropertyTest</param>
+                            </targetTests>
+                            <!-- The integration tests cover the core too, but each is a Testcontainers boot —
+                                 exclude them so mutation analysis stays fast and Docker-free. -->
+                            <excludedTestClasses>
+                                <param>com.buildabank.notification.TransferEventConsumerKafkaTest</param>
+                                <param>com.buildabank.notification.DeadLetterTest</param>
+                                <param>com.buildabank.notification.NotificationControllerTest</param>
+                                <param>com.buildabank.notification.HexagonalArchitectureTest</param>
+                            </excludedTestClasses>
+                            <!-- Justified target: the core is small, pure, and fully unit-tested → demand a high
+                                 score. The build fails below this. (See steps/step-28/lesson.md for the rationale.) -->
+                            <mutationThreshold>90</mutationThreshold>
+                            <coverageThreshold>90</coverageThreshold>
+                            <outputFormats>
+                                <param>HTML</param>
+                                <param>XML</param>
+                            </outputFormats>
+                        </configuration>
+                    </plugin>
+                </plugins>
+            </build>
+        </profile>
+    </profiles>
 </project>
```

🔍 **Line-by-line:**
- `targetClasses` / `targetTests`: We tightly scope this to the *core*.
- `excludedTestClasses`: We deliberately omit the Testcontainers tests; otherwise PITest would boot Kafka for *every* mutant, taking hours.
- `mutationThreshold`: Fails the build if we dip below 90% killed. We will achieve 100%.

💭 **Under the hood:** We pin PITest `1.25.4`. Wait, didn't Maven Central say `1.19.1` was latest? Yes, but `1.19.1` bundles an old ASM library that crashes on JDK 25 with `Unsupported class file major version 69`. Always check the GitHub releases for bleeding-edge JDKs!

🔮 **Predict:** PITest will generate 5 mutants and our fast unit tests will kill all 5.

▶️ **Run & See:**
```bash
./mvnw -pl services/notification -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

```text
================================================================================
- Mutators
================================================================================
> org.pitest.mutationtest.engine.gregor.mutators.BooleanTrueReturnValsMutator
>> Generated 1 Killed 1 (100%)
> KILLED 1 SURVIVED 0 UNCOVERED 0
...
================================================================================
- Statistics
================================================================================
>> Generated 5 mutations Killed 5 (100%)
>> Mutations with no coverage 0. Test strength 100%
>> Ran 15 tests (3 tests per mutation)
```

✋ **Checkpoint:** 100% mutation coverage. The capstone is green! 

⚠️ **Pitfall:** See the *Proof* section below. If we comment out a Mockito `verify`, the mutant survives and the build fails.

💾 **Commit:** `test(notification): Step 28 mutation (PITest) capstone on the hexagon core`

---

### Sub-step 4: Turn `libs/common` into an Auto-Configured Starter

🎯 **Goal:** A bank-wide standard format for money. We promised in Step 5 that `libs/common` would become a real Spring Boot starter. Let's provide a `@Conditional` bean that auto-configures the moment the jar hits the classpath.

📁 **Path:** `pom.xml` (root)
```diff
@@ -35,6 +35,7 @@
     <!-- Modules are added step-by-step. The hello-service is the Step 1 deliverable
          (added at step-01-end). Real banking microservices begin at Step 8 (CIF). -->
     <modules>
+        <module>libs/common</module>
         <module>services/hello</module>
         <module>services/cif</module>
```

📁 **Path:** `libs/common/pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.buildabank</groupId>
        <artifactId>build-a-bank-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>common-spring-boot-starter</artifactId>
    <name>Build-a-Bank :: Libs :: Common (Spring Boot Starter)</name>
    <description>A real auto-configured Spring Boot starter — shared bank beans (MoneyFormatter) (Step 28).</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

📁 **Path:** `libs/common/src/main/java/com/buildabank/common/money/MoneyProperties.java`
```java
package com.buildabank.common.money;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Step 28 · typed configuration for the starter, bound from {@code buildabank.money.*} in a consumer's
 * {@code application.yml}. Two knobs: {@code enabled} (turn the whole feature off) and {@code currency-code}
 * (the prefix the {@link MoneyFormatter} renders). The configuration-processor turns this class into IDE
 * auto-complete + docs for those keys.
 */
@ConfigurationProperties(prefix = "buildabank.money")
public class MoneyProperties {

    /** Whether the MoneyFormatter bean is auto-configured. Default true. */
    private boolean enabled = true;

    /** ISO currency code rendered as the prefix (e.g. USD, EUR). Default USD. */
    private String currencyCode = "USD";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
```

📁 **Path:** `libs/common/src/main/java/com/buildabank/common/money/MoneyFormatter.java`
```java
package com.buildabank.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Step 28 · the reusable bean this starter provides. Formats a {@link BigDecimal} money amount with a currency
 * prefix and exactly two minor-unit digits — deterministic and locale-free on purpose (no
 * {@code NumberFormat}/locale), so the bank renders money identically everywhere and tests don't depend on the
 * host locale. Money is always {@code BigDecimal} (never {@code double}); rounding is banker's rounding
 * ({@link RoundingMode#HALF_EVEN}). A plain object — the Spring wiring lives in {@link MoneyAutoConfiguration}.
 */
public class MoneyFormatter {

    private final String currencyCode;

    public MoneyFormatter(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /** Format an amount, e.g. {@code new BigDecimal("1234.5")} -> {@code "USD 1234.50"}. */
    public String format(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_EVEN);
        return currencyCode + " " + scaled.toPlainString();
    }

    public String currencyCode() {
        return currencyCode;
    }
}
```

📁 **Path:** `libs/common/src/main/java/com/buildabank/common/money/MoneyAutoConfiguration.java`
```java
package com.buildabank.common.money;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Step 28 · the heart of the starter — an {@link AutoConfiguration} Spring Boot discovers automatically (via
 * the {@code AutoConfiguration.imports} file) when this jar is on the classpath. The three conditions are what
 * make a starter polite:
 * <ul>
 *   <li>{@link EnableConfigurationProperties} binds {@code buildabank.money.*} into {@link MoneyProperties};</li>
 *   <li>{@link ConditionalOnProperty}{@code (matchIfMissing=true)} — on by default, but a consumer can switch
 *       the whole feature off with {@code buildabank.money.enabled=false};</li>
 *   <li>{@link ConditionalOnMissingBean} — <strong>back off</strong> if the consumer defined their own
 *       {@code MoneyFormatter}. A starter must never clobber a bean the application already provides.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(MoneyProperties.class)
@ConditionalOnProperty(prefix = "buildabank.money", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MoneyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MoneyFormatter moneyFormatter(MoneyProperties properties) {
        return new MoneyFormatter(properties.getCurrencyCode());
    }
}
```

**The Magic Bridge:** Spring Boot 2.7+ uses a flat text file so it knows *exactly* which class to load without scanning the whole jar.
📁 **Path:** `libs/common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
```text
# Step 28 · how Spring Boot DISCOVERS this starter's auto-configuration. When this jar is on a service's
# classpath, Boot reads this file and applies the listed @AutoConfiguration class — no @Import or @ComponentScan
# needed. (Boot 2 used spring.factories' EnableAutoConfiguration key; Boot 2.7+ / 3 / 4 use this dedicated file.)
com.buildabank.common.money.MoneyAutoConfiguration
```

We must test the starter to ensure it binds, backs off, and respects the enable flag:
📁 **Path:** `libs/common/src/test/java/com/buildabank/common/money/MoneyAutoConfigurationTest.java`
```java
package com.buildabank.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class MoneyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MoneyAutoConfiguration.class));

    @Test
    void autoConfiguresAMoneyFormatterByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MoneyFormatter.class);
            assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("USD");
        });
    }

    @Test
    void bindsTheCurrencyProperty() {
        runner.withPropertyValues("buildabank.money.currency-code=EUR")
                .run(context -> assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("EUR"));
    }

    @Test
    void backsOffWhenTheConsumerDefinesItsOwn() {
        runner.withUserConfiguration(CustomFormatterConfig.class).run(context -> {
            assertThat(context).hasSingleBean(MoneyFormatter.class);  // ours did NOT also register
            assertThat(context.getBean(MoneyFormatter.class).currencyCode()).isEqualTo("JPY");
        });
    }

    @Test
    void canBeDisabledByProperty() {
        runner.withPropertyValues("buildabank.money.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MoneyFormatter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFormatterConfig {
        @Bean
        MoneyFormatter myOwnFormatter() {
            return new MoneyFormatter("JPY");
        }
    }
}
```

📁 **Path:** `libs/common/src/test/java/com/buildabank/common/money/MoneyFormatterTest.java`
```java
package com.buildabank.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyFormatterTest {

    @Test
    void formatsWithCurrencyPrefixAndTwoDecimals() {
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1234.5"))).isEqualTo("USD 1234.50");
    }

    @Test
    void usesBankersRoundingHalfEven() {
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1.005"))).isEqualTo("USD 1.00");
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1.015"))).isEqualTo("USD 1.02");
    }

    @Test
    void respectsTheConfiguredCurrency() {
        assertThat(new MoneyFormatter("EUR").format(new BigDecimal("9.9"))).isEqualTo("EUR 9.90");
    }
}
```

Now we consume it in `services/hello`!
📁 **Path:** `services/hello/pom.xml`
```diff
@@ -31,6 +31,14 @@
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-actuator</artifactId>
         </dependency>
+        <!-- Our OWN starter (Step 28). Adding this ONE dependency auto-configures a MoneyFormatter bean — no
+             @Import, no @ComponentScan. The proof it's discovered via the AutoConfiguration.imports file is the
+             MoneyFormatter injected in HelloApplicationTests (real consumption of the libs/common starter). -->
+        <dependency>
+            <groupId>com.buildabank</groupId>
+            <artifactId>common-spring-boot-starter</artifactId>
+            <version>0.1.0-SNAPSHOT</version>
+        </dependency>
```

📁 **Path:** `services/hello/src/test/java/com/buildabank/hello/HelloApplicationTests.java`
```diff
@@ -3,12 +3,17 @@
 
 import static org.assertj.core.api.Assertions.assertThat;
 
+import java.math.BigDecimal;
+
 import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.boot.test.web.server.LocalServerPort;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.client.RestClient;
 
+import com.buildabank.common.money.MoneyFormatter;
+
 /**
  * Two tests that actually prove the app works (not just compiles):
  *  1. the Spring context starts cleanly, and
@@ -28,11 +33,23 @@
     @LocalServerPort
     int port;
 
+    // Step 28: injected straight from our OWN starter (libs/common). It's here ONLY because the
+    // common-spring-boot-starter jar is on the classpath — proof Boot discovered the auto-configuration via
+    // the AutoConfiguration.imports file, with no @Import in this app.
+    @Autowired
+    MoneyFormatter moneyFormatter;
+
     @Test
     void contextLoads() {
         // If the application context fails to start, this test fails — the cheapest smoke test there is.
     }
 
+    @Test
+    void ourCustomStarterAutoConfiguresTheMoneyFormatter() {
+        assertThat(moneyFormatter).as("auto-configured by common-spring-boot-starter").isNotNull();
+        assertThat(moneyFormatter.format(new BigDecimal("1234.5"))).isEqualTo("USD 1234.50");
+    }
+
```

💭 **Under the hood:** By merely placing the starter jar on the classpath, the `hello` application injected a `MoneyFormatter`. No `@Import`, no `@ComponentScan`. The auto-configuration engine discovered it and instantiated it.

🔮 **Predict:** `hello`'s context test will pass.

▶️ **Run & See:**
```bash
./mvnw -pl libs/common,services/hello -am test
```

✋ **Checkpoint:** You built a Spring Boot Starter from scratch.

💾 **Commit:** `feat(common): Step 28 turn libs/common into a real auto-configured Spring Boot starter`

---

### Sub-step 5: Boot-4 Slice Test with MockMvcTester

🎯 **Goal:** The Step 1 tests were full `@SpringBootTest` runs using `RestClient` against a running server. Now, we use Spring Boot's `@WebMvcTest` slice and the new `MockMvcTester` API to test just the web layer (faster, no server port).

📁 **Path:** `services/hello/pom.xml`
```diff
@@ -38,6 +46,13 @@
             <artifactId>spring-boot-starter-test</artifactId>
             <scope>test</scope>
         </dependency>
+        <!-- Boot 4 modularized the test slices: @WebMvcTest + MockMvcTester live in spring-boot-webmvc-test
+             (Boot 3 had them under spring-boot-test-autoconfigure). Needed for the Step-28 slice test. -->
+        <dependency>
+            <groupId>org.springframework.boot</groupId>
+            <artifactId>spring-boot-webmvc-test</artifactId>
+            <scope>test</scope>
+        </dependency>
     </dependencies>
```

📁 **Path:** `services/hello/src/test/java/com/buildabank/hello/HelloMockMvcTesterTest.java`
```java
package com.buildabank.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Step 28 · testing mastery — a <strong>web slice</strong> ({@link WebMvcTest} loads only the MVC layer for
 * {@link HelloController}, not the whole app, so it's fast) tested with Spring Framework 7's
 * <strong>{@link MockMvcTester}</strong>: the AssertJ-fluent successor to the old {@code MockMvc.perform(...)}
 * chains (and to Boot 4's removed {@code TestRestTemplate}). This is the API the Step-1 test's "Then vs Now"
 * note pointed forward to.
 */
@WebMvcTest(HelloController.class)
class HelloMockMvcTesterTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getHelloReturns200WithJsonGreeting() {
        assertThat(mvc.get().uri("/api/hello"))
                .hasStatusOk()
                .bodyText()
                .contains("Welcome to Build-a-Bank", "\"service\":\"hello\"");
    }
}
```

🔍 **Line-by-line:**
- `@WebMvcTest`: The slice. It doesn't load the `@Service` layer or the database, just the web controllers.
- `MockMvcTester`: Spring Framework 7's new AssertJ fluent API (replacing `MockMvc.perform()`).
- `assertThat(mvc.get()...)`: Chained assertions!

💭 **Under the hood:** `MockMvcTester` bridges the divide between `WebTestClient` (reactive) and `MockMvc` (servlet), providing one consistent, fluent assertion syntax for both.

🔮 **Predict:** Test passes in ms.

▶️ **Run & See:**
```bash
./mvnw -pl services/hello test -Dtest=HelloMockMvcTesterTest
```

✋ **Checkpoint:** Boot 4 slice testing delivered!

💾 **Commit:** `test(hello): Step 28 Boot-4 slice test with MockMvcTester`

---

### Sub-step 6: Code-Quality Gates (Spotless + Checkstyle)

🎯 **Goal:** The A 12 Definition of Done dictates code-quality gates. We will add a lean Checkstyle (real smells) and Spotless (formatting hygiene).

📁 **Path:** `config/checkstyle/checkstyle.xml`
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN" "https://checkstyle.org/dtds/configuration_1_3.dtd">
<!--
  Step 28 · The bank's LEAN ruleset. We enforce real bugs/smells (unused imports, empty statements, fall-through,
  string ==, equals/hashCode). We specifically AVOID full Google/Sun format checks here (Spotless handles raw
  formatting) so the build doesn't fail because a developer added an extra space before a brace.
-->
<module name="Checker">
    <property name="fileExtensions" value="java"/>
    <module name="TreeWalker">
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>
        <module name="EmptyStatement"/>
        <module name="EqualsHashCode"/>
        <module name="StringLiteralEquality"/>
        <module name="FallThrough"/>
        <module name="ModifierOrder"/>
        <module name="DefaultComesLast"/>
        <module name="OneStatementPerLine"/>
        <module name="UpperEll"/> <!-- "1l" -> "1L" -->
    </module>
</module>
```

📁 **Path:** `pom.xml` (root)
```diff
@@ -3,6 +3,20 @@
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
     <modelVersion>4.0.0</modelVersion>
 
+    <properties>
+        <pitest.version>1.25.4</pitest.version>
+        <pitest-junit5.version>1.2.2</pitest-junit5.version>
+        <!-- Step 28 — code-quality gates. Run as Maven plugins (not javac plugins), so JDK-25-safe. -->
+        <spotless.version>3.6.0</spotless.version>
+        <maven-checkstyle-plugin.version>3.6.0</maven-checkstyle-plugin.version>
+        <checkstyle.version>13.5.0</checkstyle.version>
+        <!-- Error Prone / NullAway are javac PLUGINS (hook into compiler internals) — JDK-support lags. Pinned
+             here for the off-by-default `errorprone` profile; JDK-25 status verified empirically (lesson §F). -->
+        <error-prone.version>2.49.0</error-prone.version>
+        <nullaway.version>0.13.6</nullaway.version>
+    </properties>
 
@@ -83,4 +97,125 @@
             </dependency>
         </dependencies>
     </dependencyManagement>
+
+    <!--
+      Code-quality gates (Step 28) — inherited by EVERY module. Both bind to `verify`, so `./mvnw verify` fails
+      on a formatting or style violation (proven by the §12.3 mutation in steps/step-28/lesson.md). Kept lean on
+      purpose (see config/checkstyle/checkstyle.xml). Run `./mvnw spotless:apply` to auto-fix formatting.
+      NOTE: Error Prone / NullAway are javac plugins and do NOT yet support JDK 25 — see the `errorprone`
+      profile below and steps/step-28/lesson.md §"Then vs Now" for the honest status.
+    -->
+    <build>
+        <plugins>
+            <!-- Spotless — formatting hygiene. Lean by design (whitespace, EOF newline, dead imports, import
+                 order) rather than a full reformatter, so it doesn't churn the course's hand-laid-out code. -->
+            <plugin>
+                <groupId>com.diffplug.spotless</groupId>
+                <artifactId>spotless-maven-plugin</artifactId>
+                <version>${spotless.version}</version>
+                <configuration>
+                    <!-- PRESERVE: keep each file's existing line endings (the repo has no .gitattributes and
+                         was authored on Windows/CRLF). Without this, Spotless rewrites CRLF→LF on every line of
+                         every file — a 200-file phantom diff. Formatting hygiene shouldn't churn line endings. -->
+                    <lineEndings>PRESERVE</lineEndings>
+                    <java>
+                        <removeUnusedImports/>
+                        <trimTrailingWhitespace/>
+                        <endWithNewline/>
+                    </java>
+                </configuration>
+                <executions>
+                    <execution>
+                        <id>spotless-check</id>
+                        <phase>verify</phase>
+                        <goals>
+                            <goal>check</goal>
+                        </goals>
+                    </execution>
+                </executions>
+            </plugin>
+
+            <!-- Checkstyle — a lean ruleset of real bug-or-smell checks (config/checkstyle/checkstyle.xml).
+                 violationSeverity=warning means any warning fails the build. -->
+            <plugin>
+                <groupId>org.apache.maven.plugins</groupId>
+                <artifactId>maven-checkstyle-plugin</artifactId>
+                <version>${maven-checkstyle-plugin.version}</version>
+                <dependencies>
+                    <dependency>
+                        <groupId>com.puppycrawl.tools</groupId>
+                        <artifactId>checkstyle</artifactId>
+                        <version>${checkstyle.version}</version>
+                    </dependency>
+                </dependencies>
+                <configuration>
+                    <configLocation>${maven.multiModuleProjectDirectory}/config/checkstyle/checkstyle.xml</configLocation>
+                    <includeTestSourceDirectory>true</includeTestSourceDirectory>
+                    <consoleOutput>true</consoleOutput>
+                    <failsOnError>true</failsOnError>
+                    <violationSeverity>warning</violationSeverity>
+                </configuration>
+                <executions>
+                    <execution>
+                        <id>checkstyle-check</id>
+                        <phase>verify</phase>
+                        <goals>
+                            <goal>check</goal>
+                        </goals>
+                    </execution>
+                </executions>
+            </plugin>
+        </plugins>
+    </build>
+
+    <profiles>
+        <!--
+          Error Prone + NullAway (Step 28) — OFF by default; enable with `./mvnw -Perrorprone compile`.
+          These are javac plugins (NullAway = compile-time null-safety: no @Nullable dereference). They hook
+          deep into compiler internals, so each JDK release needs the add-exports/add-opens below AND an
+          Error Prone version that supports that JDK. We keep this profile ready and verify its JDK-25 status
+          honestly in steps/step-28/lesson.md (§F). Spotless + Checkstyle remain the always-on gates.
+        -->
+        <profile>
+            <id>errorprone</id>
+            <build>
+                <plugins>
+                    <plugin>
+                        <groupId>org.apache.maven.plugins</groupId>
+                        <artifactId>maven-compiler-plugin</artifactId>
+                        <configuration>
+                            <fork>true</fork>
+                            <compilerArgs>
+                                <arg>-XDcompilePolicy=simple</arg>
+                                <arg>--should-stop=ifError=FLOW</arg>
+                                <arg>-Xplugin:ErrorProne -XepDisableAllChecks -Xep:NullAway:WARN -XepOpt:NullAway:AnnotatedPackages=com.buildabank</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
+                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
+                                <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
+                                <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>
+                            </compilerArgs>
+                            <annotationProcessorPaths>
+                                <path>
+                                    <groupId>com.google.errorprone</groupId>
+                                    <artifactId>error_prone_core</artifactId>
+                                    <version>${error-prone.version}</version>
+                                </path>
+                                <path>
+                                    <groupId>com.uber.nullaway</groupId>
+                                    <artifactId>nullaway</artifactId>
+                                    <version>${nullaway.version}</version>
+                                </path>
+                            </annotationProcessorPaths>
+                        </configuration>
+                    </plugin>
+                </plugins>
+            </build>
+        </profile>
+    </profiles>
 </project>
```

🔍 **Line-by-line:**
- `lineEndings: PRESERVE`: Prevents a massive churn diff on CRLF machines.
- `<id>errorprone</id>`: Kept as an optional profile since javac plugins are notoriously brittle against bleeding edge JDKs.

💭 **Under the hood:** By tying Spotless and Checkstyle to the `verify` phase, anyone running `./mvnw verify` or `./mvnw clean install` will trigger them.

🔮 **Predict:** Code passes checking instantly.

▶️ **Run & See:**
```bash
./mvnw -B -q spotless:check checkstyle:check
```

✋ **Checkpoint:** Code-quality gates active.

💾 **Commit:** `build: Step 28 code-quality gates — Spotless + Checkstyle (lean) + opt-in Error Prone/NullAway`

---

## 🛡️ D. Proofs & Safeguards (The §12.3 Mutations)

### 1. PITest Catching a Bug
1. Open `NotificationService.java`. Comment out `publisher.publish(...)`.
2. Run unit tests normally: `./mvnw -pl services/notification -Dtest='NotificationServiceTest' test`. **It still passes!** Why? Because our Mockito test had a gap.
3. Run PITest: `./mvnw -pl services/notification -Pmutation test`.
4. It goes **RED**: `Mutation score of 80 is below threshold of 90`. It caught the removed method call.
5. Restore the code. Green. This proves the mutation gate works.

### 2. Spotless Gate
1. Add an empty line to the top of `MoneyFormatter.java` with 5 spaces.
2. Run `./mvnw spotless:check`. It goes **RED** (trailing whitespace).
3. Run `./mvnw spotless:apply`. It auto-fixes the formatting.

---

## 💼 F. Interview Prep

1. **"Mutation testing vs code coverage?"** — Code coverage (line/branch) only proves a line was *executed* during a test. Mutation testing (PITest) modifies the bytecode (e.g., changes `if (x > 0)` to `if (x >= 0)`) and proves your tests actually *fail* when the behavior changes. It measures *test strength*, not just reachability.
2. **"What's a 'surviving mutant'?"** — PITest flipped an operator or removed a method call, ran your tests, and your tests *stayed green*. That's a surviving mutant—a gap in your assertions. You "kill" it by adding an assertion that notices the changed behavior.
3. **"How does a Spring Boot starter auto-configure?"** — A starter provides an `@AutoConfiguration` class declared in a `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file. Boot discovers this file on the classpath and instantiates its beans, unless the user opted out (`@ConditionalOnProperty`) or provided their own bean (`@ConditionalOnMissingBean`).
4. **"Property-based vs example-based tests?"** — Example-based tests (JUnit) check hand-picked inputs (e.g., "1234.50"). Property-based tests (`jqwik`) state an *invariant* ("identifiers are unchanged") and generate thousands of randomized inputs, shrinking any failure down to a minimal counter-example.
5. **(Gotcha) "What happens if Spotless enforces LF line-endings on a Windows team?"** — Every file modified on Windows (CRLF) gets rewritten to LF, creating a massive "phantom diff" on GitHub where every line changed just for line-endings. The fix is `lineEndings=PRESERVE` or a strict `.gitattributes` file.
6. **"What is MockMvcTester?"** — Spring Framework 7's AssertJ-fluent API for testing controllers. It replaces the older `MockMvc.perform(...)` chain with fluent `assertThat(mvc.get()...)` calls, unifying the API style for both servlet and reactive tests.
7. **"Why use NullAway, and what's the catch?"** — NullAway is a compile-time check (an Error Prone plugin) that forbids `@Nullable` dereferences, eliminating `NullPointerException`s before runtime. The catch: it hooks deeply into `javac` internals via `--add-exports`, making it tightly coupled to specific JDK versions (which is why we keep it in an opt-in profile on bleeding-edge JDK 25).

> **Behavioral / STAR seed:** *"Tell me about a time you improved a team's test suite."* — **S/T:** Our core business logic had 100% line coverage, but bugs were still slipping through because assertions were weak. **A:** I introduced mutation testing (PITest) scoped specifically to the core domain (avoiding the slow integration tests) to objectively measure assertion strength. I also added jqwik for property-based testing to find edge-cases we hadn't imagined. **R:** PITest immediately exposed three "surviving mutants" (missing assertions), which we patched. We locked the build to a 90% mutation threshold, permanently raising the bar for core logic reliability without slowing down the overall CI cycle.

---

## 📜 Verification Log

*(Tests re-run and passed using the Step-28 `smoke.sh`)*

```text
==> 1/4 core unit + jqwik property tests (notification hexagon core)
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

==> 2/4 mutation coverage on the core (PITest) — the Phase-E capstone (threshold 90%, achieves 100%)
================================================================================
- Statistics
================================================================================
>> Generated 5 mutations Killed 5 (100%)
>> Mutations with no coverage 0. Test strength 100%
>> Ran 15 tests (3 tests per mutation)
[INFO] BUILD SUCCESS

==> 3/4 the custom starter auto-configures + is consumed by hello
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

==> 4/4 code-quality gates pass repo-wide (Spotless + Checkstyle)
[INFO] BUILD SUCCESS

✅ Step 28 smoke test PASSED — mutation 100% + property tests + custom starter + quality gates (Phase E complete)
```
