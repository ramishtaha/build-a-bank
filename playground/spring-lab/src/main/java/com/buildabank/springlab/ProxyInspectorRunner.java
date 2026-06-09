// playground/spring-lab/src/main/java/com/buildabank/springlab/ProxyInspectorRunner.java
package com.buildabank.springlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.buildabank.springlab.account.AccountService;

/**
 * Prints proof that the audited {@code AccountService} is actually a Spring AOP <strong>proxy</strong>
 * (CGLIB by default in Spring Boot), not the bare class. That proxy is what makes {@code @Audited} work.
 */
@Component
public class ProxyInspectorRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProxyInspectorRunner.class);

    private final AccountService accountService;

    public ProxyInspectorRunner(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) {
        log.info("AccountService class : {}", accountService.getClass().getName());
        log.info("is AOP proxy?        : {}", AopUtils.isAopProxy(accountService));
        log.info("is CGLIB proxy?      : {}", AopUtils.isCglibProxy(accountService));
    }
}
