// playground/java-basics/src/main/java/com/buildabank/basics/repo/Repository.java
package com.buildabank.basics.repo;

import java.util.List;
import java.util.Optional;

/**
 * A <strong>generic</strong> repository abstraction: {@code T} is the entity type, {@code ID} its key type.
 *
 * <p>Generics give compile-time type safety without casting. {@link Optional} models "maybe absent" in the
 * type system so callers cannot forget the not-found case (no surprise {@code null}). This is the same shape
 * Spring Data will generate for us automatically from Step 8 — here we build it by hand to see how it works.
 */
public interface Repository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);
}
