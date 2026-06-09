// playground/spring-lab/src/test/java/com/buildabank/springlab/aop/AuditAspectSelfInvocationTest.java
package com.buildabank.springlab.aop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.buildabank.springlab.account.AccountService;

/**
 * Proves both that the aspect works AND the self-invocation pitfall — with a counter, not flaky log parsing.
 */
@SpringBootTest
class AuditAspectSelfInvocationTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AuditCounter counter;

    @Test
    void externalCallIsAudited() {
        counter.reset();
        accountService.findById("1");
        assertThat(counter.total()).isEqualTo(1);
    }

    @Test
    void selfInvocationBypassesTheProxyAndIsNotAudited() {
        counter.reset();
        accountService.summaryFor("1"); // summaryFor is advised (+1); its internal this.findById is NOT
        assertThat(counter.total()).isEqualTo(1); // would be 2 if self-calls went through the proxy
        assertThat(counter.calls()).noneMatch(c -> c.contains("findById"));
    }

    @Test
    void auditedServiceBeanIsACglibProxy() {
        assertThat(AopUtils.isAopProxy(accountService)).isTrue();
        assertThat(AopUtils.isCglibProxy(accountService)).isTrue();
    }
}
