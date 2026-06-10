// services/demand-account/src/test/java/com/buildabank/account/ModularityTest.java
package com.buildabank.account;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Step 27 · Spring Modulith on the richest service. Where ArchUnit (see {@code notification}'s
 * {@code HexagonalArchitectureTest}) enforces hand-written rules about ONE hexagon, Spring Modulith
 * <em>derives</em> the module model from the package structure: each direct sub-package of
 * {@code com.buildabank.account} (batch, client, domain, event, outbox, payment, service, web, webhook)
 * is an application module, and {@link ApplicationModules#verify()} checks the two rules that keep a
 * modular monolith from rotting into a big ball of mud:
 * <ol>
 *   <li><strong>no cyclic dependencies</strong> between modules, and</li>
 *   <li>a module may only touch another module's <strong>API</strong>, never its {@code internal} packages.</li>
 * </ol>
 * This is pure bytecode analysis (ArchUnit under the hood) — it starts no Spring context and needs no Docker.
 */
class ModularityTest {

    /** Derive the module model once from the application's base package. */
    static final ApplicationModules MODULES = ApplicationModules.of(DemandAccountApplication.class);

    @Test
    void module_boundaries_have_no_cycles_and_no_illegal_access() {
        MODULES.verify(); // throws (failing the build) on a cross-module cycle or access to another module's internals
    }

    @Test
    void prints_the_discovered_module_model() {
        MODULES.forEach(module -> System.out.println(module.toString()));
    }

    /**
     * Generate living documentation from the verified model: a C4-style component diagram of all modules,
     * one diagram per module, and a per-module "canvas" (its API, events published/listened-to, dependencies).
     * Output lands in {@code target/spring-modulith-docs} — docs that can never drift from the code.
     */
    @Test
    void writes_living_module_documentation() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
