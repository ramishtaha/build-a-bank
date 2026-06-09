// services/demand-account/src/main/java/com/buildabank/account/web/PageResponse.java
package com.buildabank.account.web;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * A stable, explicit pagination envelope. We do NOT serialize Spring Data's {@code Page} directly: its JSON
 * shape is an internal implementation detail (Spring even warns against exposing it), and a public API
 * should own its contract. This record is that contract: the items plus the page metadata clients need.
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Map a Spring Data {@link Page} of entities into a DTO page via {@code mapper}. */
    public static <E, T> PageResponse<T> of(Page<E> page, java.util.function.Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
