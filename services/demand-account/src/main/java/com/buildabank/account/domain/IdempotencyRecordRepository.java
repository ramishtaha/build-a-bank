// services/demand-account/src/main/java/com/buildabank/account/domain/IdempotencyRecordRepository.java
package com.buildabank.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
