// services/demand-account/src/test/java/com/buildabank/account/service/TransactionPropagationTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AuditEntryRepository;

/**
 * Proves transaction <strong>propagation</strong>: an audit record written with {@code REQUIRES_NEW} commits
 * in its own transaction, so it <em>survives</em> even though the calling (outer) transaction rolls back.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class TransactionPropagationTest {

    @Autowired
    PropagationDemoService demo;

    @Autowired
    AuditEntryRepository auditEntries;

    @BeforeEach
    void clean() {
        auditEntries.deleteAll();
    }

    @Test
    void requiresNew_auditSurvivesOuterRollback() {
        assertThatThrownBy(() -> demo.auditThenFail("transfer-attempt"))
                .isInstanceOf(IllegalStateException.class);

        // The outer transaction rolled back, but the REQUIRES_NEW audit committed independently → it's still here.
        assertThat(auditEntries.count()).isEqualTo(1);
        assertThat(auditEntries.findAll().getFirst().getEvent()).isEqualTo("transfer-attempt");
    }
}
