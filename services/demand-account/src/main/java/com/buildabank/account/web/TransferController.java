// services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java
package com.buildabank.account.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.account.domain.Account;
import com.buildabank.account.service.IdempotentTransferService;
import com.buildabank.account.service.TransferService;
import com.buildabank.account.webhook.WebhookPublisher;

/**
 * REST API for accounts and transfers. Step 14 adds <strong>versioned</strong> endpoints under
 * {@code /api/v1}: an <strong>idempotent</strong> transfer ({@code Idempotency-Key} header) that also emits a
 * signed <strong>webhook</strong>, and a <strong>paginated</strong> ledger-entries listing. The original
 * {@code POST /api/transfers} stays for compatibility but is marked <strong>deprecated</strong> (it returns
 * {@code Deprecation}/{@code Sunset}/{@code Link} headers pointing at its successor).
 */
@RestController
public class TransferController {

    private final TransferService transfers;
    private final IdempotentTransferService idempotentTransfers;
    private final WebhookPublisher webhookPublisher;

    public TransferController(TransferService transfers, IdempotentTransferService idempotentTransfers,
                              WebhookPublisher webhookPublisher) {
        this.transfers = transfers;
        this.idempotentTransfers = idempotentTransfers;
        this.webhookPublisher = webhookPublisher;
    }

    /** Open an account → 201 Created. */
    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = transfers.openAccount(
                request.accountNumber(), request.currency(), request.openingBalance());
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getAccountNumber()))
                .body(AccountResponse.from(account));
    }

    /** Read an account (number, currency, balance) → 200, or 404 if it doesn't exist. */
    @GetMapping("/api/accounts/{accountNumber}")
    public ResponseEntity<AccountResponse> balance(@PathVariable String accountNumber) {
        try {
            // Step 32 fix: this used to hand-build the response with currency=null; the SPA's Intl
            // formatter throws on a null currency code. Map the real entity — one source of truth.
            return ResponseEntity.ok(AccountResponse.from(transfers.accountOf(accountNumber)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DEPRECATED transfer (the Step-12 endpoint). Still works, but advertises its replacement via standard
     * deprecation headers so clients can migrate. New integrations should use {@code POST /api/v1/transfers}.
     */
    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        UUID transactionId = transfers.transfer(
                request.from(), request.to(), request.amount(), request.description());
        return ResponseEntity.ok()
                .header("Deprecation", "true")                                       // RFC 8594
                .header("Sunset", "Sat, 31 Oct 2026 23:59:59 GMT")                   // when it will be removed
                .header("Link", "</api/v1/transfers>; rel=\"successor-version\"")     // where to go instead
                .body(new TransferResponse(transactionId));
    }

    /**
     * v1 transfer — <strong>idempotent</strong> (optional {@code Idempotency-Key} header) and emits a signed
     * {@code transfer.completed} webhook after the money moves.
     */
    @PostMapping("/api/v1/transfers")
    public ResponseEntity<TransferResponse> transferV1(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        UUID transactionId = idempotentTransfers.transfer(
                idempotencyKey, request.from(), request.to(), request.amount(), request.description());
        // After the transfer's transaction has committed (this controller is not @Transactional).
        // Webhooks are at-least-once, so a retried request may re-emit — receivers must be idempotent.
        webhookPublisher.transferCompleted(transactionId, request.from(), request.to(), request.amount());
        return ResponseEntity.ok(new TransferResponse(transactionId));
    }

    /**
     * ADMIN-only operational endpoint, guarded by <strong>method security</strong> ({@code @PreAuthorize}) —
     * fine-grained authorization expressed on the method (a USER token gets 403, an ADMIN token 200).
     */
    @GetMapping("/api/v1/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminPing() {
        return Map.of("message", "admin ok");
    }

    /** v1 paginated ledger entries for an account → 200 with a {@link PageResponse} envelope. */
    @GetMapping("/api/v1/accounts/{accountNumber}/entries")
    public PageResponse<LedgerEntryResponse> entries(
            @PathVariable String accountNumber,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.of(transfers.entriesOf(accountNumber, pageable), LedgerEntryResponse::from);
    }
}
