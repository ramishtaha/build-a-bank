// services/demand-account/src/main/java/com/buildabank/account/domain/AuditEntryRepository.java
package com.buildabank.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
}
