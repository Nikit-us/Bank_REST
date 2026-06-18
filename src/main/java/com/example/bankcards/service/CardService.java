package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardMapper;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessRuleException;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.specification.CardSpecifications;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMaskUtil;
import com.example.bankcards.util.CardNumberEncryptor;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberEncryptor encryptor;
    private final CardMapper cardMapper;

    public CardResponse create(CreateCardRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.ownerId()));

        String hash = encryptor.hash(request.cardNumber());
        if (cardRepository.existsByCardNumberHash(hash)) {
            throw new DuplicateResourceException("A card with this number already exists");
        }

        Card card = Card.builder()
                .cardNumber(encryptor.encrypt(request.cardNumber()))
                .cardNumberHash(hash)
                .lastFour(CardMaskUtil.lastFour(request.cardNumber()))
                .owner(owner)
                .expiryDate(request.expiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
                .build();

        Card saved = cardRepository.save(card);
        log.info("Card created: id={}, ownerId={}, lastFour={}", saved.getId(), owner.getId(), saved.getLastFour());
        return cardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listAll(CardStatus status, String search, Pageable pageable) {
        return list(null, status, search, pageable);
    }

    @Transactional(readOnly = true)
    public CardResponse getById(Long cardId) {
        return cardMapper.toResponse(findOrThrow(cardId));
    }

    public CardResponse block(Long cardId) {
        Card card = findOrThrow(cardId);
        card.setStatus(CardStatus.BLOCKED);
        log.info("Card blocked by admin: id={}", cardId);
        return cardMapper.toResponse(card);
    }

    public CardResponse activate(Long cardId) {
        Card card = findOrThrow(cardId);
        if (card.isExpired()) {
            throw new BusinessRuleException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        log.info("Card activated by admin: id={}", cardId);
        return cardMapper.toResponse(card);
    }

    public void delete(Long cardId) {
        Card card = findOrThrow(cardId);
        cardRepository.delete(card);
        log.info("Card deleted: id={}", cardId);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listOwn(Long ownerId, CardStatus status, String search, Pageable pageable) {
        return list(ownerId, status, search, pageable);
    }

    private Page<CardResponse> list(Long ownerId, CardStatus status, String search, Pageable pageable) {
        Specification<Card> spec = CardSpecifications.build(ownerId, status, search);
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getOwn(Long cardId, Long ownerId) {
        return cardMapper.toResponse(findOwnedOrThrow(cardId, ownerId));
    }

    @Transactional(readOnly = true)
    public BigDecimal getOwnBalance(Long cardId, Long ownerId) {
        return findOwnedOrThrow(cardId, ownerId).getBalance();
    }

    public CardResponse requestBlock(Long cardId, Long ownerId) {
        Card card = findOwnedOrThrow(cardId, ownerId);
        card.setStatus(CardStatus.BLOCKED);
        log.info("Card block requested by owner: id={}, ownerId={}", cardId, ownerId);
        return cardMapper.toResponse(card);
    }

    public void transfer(Long ownerId, TransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new BusinessRuleException("Source and destination cards must be different");
        }

        Card from = findOwnedOrThrow(request.fromCardId(), ownerId);
        Card to = findOwnedOrThrow(request.toCardId(), ownerId);

        requireUsable(from, "source");
        requireUsable(to, "destination");

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessRuleException("Insufficient funds on the source card");
        }

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));
        log.info("Transfer completed: ownerId={}, fromCardId={}, toCardId={}, amount={}",
                ownerId, from.getId(), to.getId(), request.amount());
    }

    private void requireUsable(Card card, String role) {
        if (!card.isUsable()) {
            throw new BusinessRuleException(
                    "The " + role + " card is not active (status: " + card.effectiveStatus() + ")");
        }
    }

    private Card findOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.of("Card", cardId));
    }

    private Card findOwnedOrThrow(Long cardId, Long ownerId) {
        return cardRepository.findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Card", cardId));
    }
}
