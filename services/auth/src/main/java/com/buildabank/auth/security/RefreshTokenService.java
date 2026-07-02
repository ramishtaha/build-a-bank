// services/auth/src/main/java/com/buildabank/auth/security/RefreshTokenService.java
package com.buildabank.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Server-side refresh-token store with <strong>rotation</strong> and <strong>reuse detection</strong>
 * (Step 32).
 *
 * <p>Unlike the access JWT (self-contained, verified by signature alone), a refresh token here is an
 * <em>opaque</em> random value whose meaning lives on the server — which is exactly what lets us revoke it,
 * rotate it, and notice theft. Every refresh <em>consumes</em> the presented token and issues a successor in
 * the same <em>family</em> (one family per login). If a token that was already consumed shows up again,
 * someone is replaying a stolen value — we revoke the whole family, forcing a fresh login everywhere.
 *
 * <p>Storage notes: we keep only the SHA-256 <em>hash</em> of each token (a heap dump must not yield
 * replayable credentials), in a {@link ConcurrentHashMap} (in-memory, consistent with the demo
 * {@code UserService}; production would use Redis/DB so restarts and multiple instances share state).
 * Consuming a token is a compare-and-swap, so concurrent refreshes of the SAME token can't both succeed
 * (🧵 Step 11: check-then-act must be atomic).
 */
@Service
public class RefreshTokenService {

    /** One stored refresh token: who it belongs to, its login family, expiry, and whether it was consumed. */
    record RefreshRecord(String username, String familyId, Instant expiresAt, Instant usedAt, boolean revoked) {

        boolean expired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    /**
     * The three ways a rotation can end. A sealed interface (Step 2!) so the controller's handling is
     * exhaustive — the compiler proves no outcome goes unmapped.
     */
    public sealed interface RotationResult {

        /** Success: set {@code rawToken} as the new cookie; mint an access JWT for {@code username}. */
        record Rotated(String rawToken, String username) implements RotationResult {
        }

        /**
         * A benign race: another request (a second browser tab) rotated this token milliseconds ago.
         * → 409; the client retries once — its browser already holds the successor cookie.
         */
        record ConcurrentRotation() implements RotationResult {
        }

        /** Unknown, expired, revoked, or replayed-after-grace (theft) → 401. */
        record Invalid() implements RotationResult {
        }
    }

    private final Map<String, RefreshRecord> byTokenHash = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final Duration rotationGrace;

    public RefreshTokenService(@Value("${bank.auth.refresh-ttl-hours:12}") long ttlHours,
                               @Value("${bank.auth.rotation-grace-seconds:3}") long rotationGraceSeconds) {
        this.ttl = Duration.ofHours(ttlHours);
        this.rotationGrace = Duration.ofSeconds(rotationGraceSeconds);
    }

    /** Start a NEW family (login): mint a random token, store its hash, hand back the raw value once. */
    public String issue(String username) {
        return store(username, UUID.randomUUID().toString());
    }

    /**
     * Rotation with reuse detection. Consumes {@code rawToken} atomically and issues a successor in the same
     * family. A token replayed <em>within the rotation grace</em> is a benign race (two tabs refreshing with
     * the same shared cookie) → {@link RotationResult.ConcurrentRotation}, family intact. Replayed
     * <em>after</em> the grace it means theft or a very stale client → the whole family is revoked and the
     * result is {@link RotationResult.Invalid}: a bank fails closed.
     */
    public RotationResult rotate(String rawToken) {
        Instant now = Instant.now();
        String hash = sha256(rawToken);

        RefreshRecord current = byTokenHash.get(hash);
        if (current == null || current.revoked() || current.expired(now)) {
            return new RotationResult.Invalid();           // unknown, revoked, or expired → 401
        }
        if (current.usedAt() != null) {
            if (Duration.between(current.usedAt(), now).compareTo(rotationGrace) <= 0) {
                return new RotationResult.ConcurrentRotation();   // the other tab won milliseconds ago
            }
            revokeFamily(current.familyId());              // REUSE beyond grace → kill the family
            return new RotationResult.Invalid();
        }

        // Consume via compare-and-swap (records compare by value): of two IN-FLIGHT rotations of the same
        // token, exactly one replace() succeeds — the loser is, by construction, inside the grace window.
        // Check-then-act made atomic without a lock (🧵 Step 11).
        RefreshRecord used = new RefreshRecord(current.username(), current.familyId(),
                current.expiresAt(), now, false);
        if (!byTokenHash.replace(hash, current, used)) {
            return new RotationResult.ConcurrentRotation();
        }
        String successor = store(current.username(), current.familyId());
        return new RotationResult.Rotated(successor, current.username());
    }

    /** Logout: revoke the presented token's whole family (best effort — unknown tokens are a no-op). */
    public void revoke(String rawToken) {
        RefreshRecord record = byTokenHash.get(sha256(rawToken));
        if (record != null) {
            revokeFamily(record.familyId());
        }
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    private String store(String username, String familyId) {
        byte[] bytes = new byte[32];                       // 256 bits of SecureRandom entropy
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        byTokenHash.put(sha256(raw),
                new RefreshRecord(username, familyId, Instant.now().plus(ttl), null, false));
        return raw;
    }

    private void revokeFamily(String familyId) {
        byTokenHash.replaceAll((hash, r) ->
                r.familyId().equals(familyId)
                        ? new RefreshRecord(r.username(), r.familyId(), r.expiresAt(), r.usedAt(), true)
                        : r);
    }

    /** We store only hashes: a stolen memory snapshot must not contain replayable refresh tokens. */
    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
