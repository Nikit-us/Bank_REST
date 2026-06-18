package com.example.bankcards.controller;

import com.example.bankcards.dto.card.BalanceResponse;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.AppUserDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "Card management, viewing and transfers")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue a new card for a user (admin)")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request,
                                               UriComponentsBuilder uriBuilder) {
        CardResponse created = cardService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/cards/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all cards with optional status filter and last-four search (admin)")
    public ResponseEntity<PageResponse<CardResponse>> listAll(@RequestParam(required = false) CardStatus status,
                                                              @RequestParam(required = false) String search,
                                                              @ParameterObject @PageableDefault(size = PageResponse.DEFAULT_PAGE_SIZE) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(cardService.listAll(status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any card by id (admin)")
    public ResponseEntity<CardResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block a card (admin)")
    public ResponseEntity<CardResponse> block(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.block(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a card (admin)")
    public ResponseEntity<CardResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.activate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a card (admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List my cards with optional status filter and last-four search")
    public ResponseEntity<PageResponse<CardResponse>> listMine(@AuthenticationPrincipal AppUserDetails principal,
                                                               @RequestParam(required = false) CardStatus status,
                                                               @RequestParam(required = false) String search,
                                                               @ParameterObject @PageableDefault(size = PageResponse.DEFAULT_PAGE_SIZE) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(cardService.listOwn(principal.getId(), status, search, pageable)));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get one of my cards")
    public ResponseEntity<CardResponse> getMine(@AuthenticationPrincipal AppUserDetails principal,
                                                @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getOwn(id, principal.getId()));
    }

    @GetMapping("/my/{id}/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get the balance of one of my cards")
    public ResponseEntity<BalanceResponse> balance(@AuthenticationPrincipal AppUserDetails principal,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(new BalanceResponse(id, cardService.getOwnBalance(id, principal.getId())));
    }

    @PostMapping("/my/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Request blocking of one of my cards")
    public ResponseEntity<CardResponse> requestBlock(@AuthenticationPrincipal AppUserDetails principal,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(cardService.requestBlock(id, principal.getId()));
    }

    @PostMapping("/my/transfer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Transfer money between my own cards")
    public ResponseEntity<Void> transfer(@AuthenticationPrincipal AppUserDetails principal,
                                         @Valid @RequestBody TransferRequest request) {
        cardService.transfer(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
