// services/notification/src/test/java/com/buildabank/notification/HexagonalArchitectureTest.java
package com.buildabank.notification;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Step 27 · <strong>enforce</strong> the Step-26 hexagon with ArchUnit — turning the architecture from a
 * convention into a build-failing rule, so it can't erode. ArchUnit reads the compiled bytecode of
 * {@code com.buildabank.notification} (production only) and checks the dependency rule:
 * <ul>
 *   <li>the <strong>domain</strong> imports no framework/transport (Spring, Kafka, Jackson) — pure;</li>
 *   <li>the <strong>application</strong> never depends on adapters, and is transport-agnostic;</li>
 *   <li>dependencies point <strong>inward</strong> (adapter → application → domain), never the reverse.</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.buildabank.notification", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    /** The dependency rule, expressed as layers. Adapter is one ring, so the documented web→push SSE coupling
     *  (intra-adapter) is allowed, while any adapter→core-inward violation fails the build. */
    @ArchTest
    static final ArchRule hexagonal_layering = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..notification.domain..")
            .layer("Application").definedBy("..notification.application..")
            .layer("Adapter").definedBy("..notification.adapter..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()                       // outer ring: nothing depends on it
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")           // only adapters drive the app
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter"); // domain used inward; depends on none

    /** The domain is pure: no Spring, Kafka, Jackson, or outward (application/adapter) dependencies. */
    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAPackage("..notification.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..notification.application..", "..notification.adapter..",
                    "org.springframework..", "org.apache.kafka..", "tools.jackson..", "com.fasterxml.jackson..");

    /** The application core never reaches into an adapter. */
    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = noClasses()
            .that().resideInAPackage("..notification.application..")
            .should().dependOnClassesThat().resideInAPackage("..notification.adapter..");

    /** The use case is transport-agnostic: no Kafka/Jackson/web — only ports + domain (+ Spring stereotypes). */
    @ArchTest
    static final ArchRule application_is_transport_agnostic = noClasses()
            .that().resideInAPackage("..notification.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.apache.kafka..", "org.springframework.kafka..",
                    "tools.jackson..", "com.fasterxml.jackson..", "org.springframework.web..");
}
