package com.example.bankcards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.card.CardMapper;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.BusinessRuleException;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberEncryptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final String CARD_NUMBER = "4111111111111111";

    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardNumberEncryptor encryptor;
    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("user", "hash", "Demo User", Role.USER);
        lenient().when(cardMapper.toResponse(any()))
                .thenReturn(new CardResponse(1L, "**** **** **** 1111", OWNER_ID, "user",
                        LocalDate.now().plusYears(2), CardStatus.ACTIVE, BigDecimal.TEN, null));
    }

    private Card card(CardStatus status, BigDecimal balance, LocalDate expiry) {
        return new Card("enc", "hash", "1111", owner, expiry, status, balance);
    }

    private Card activeCard(BigDecimal balance) {
        return card(CardStatus.ACTIVE, balance, LocalDate.now().plusYears(2));
    }

    @Nested
    class Create {

        @Test
        void issuesCardWhenNumberIsUnique() {
            CreateCardRequest request = new CreateCardRequest(CARD_NUMBER, OWNER_ID,
                    LocalDate.now().plusYears(3), new BigDecimal("100.00"));
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(encryptor.hash(CARD_NUMBER)).thenReturn("hash");
            when(encryptor.encrypt(CARD_NUMBER)).thenReturn("enc");
            when(cardRepository.existsByCardNumberHash("hash")).thenReturn(false);
            when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            cardService.create(request);

            verify(cardRepository).save(any(Card.class));
        }

        @Test
        void rejectsDuplicateCardNumber() {
            CreateCardRequest request = new CreateCardRequest(CARD_NUMBER, OWNER_ID,
                    LocalDate.now().plusYears(3), null);
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(encryptor.hash(CARD_NUMBER)).thenReturn("hash");
            when(cardRepository.existsByCardNumberHash("hash")).thenReturn(true);

            assertThatThrownBy(() -> cardService.create(request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(cardRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownOwner() {
            CreateCardRequest request = new CreateCardRequest(CARD_NUMBER, 999L,
                    LocalDate.now().plusYears(3), null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Transfers {

        @Test
        void movesFundsBetweenOwnCards() {
            Card from = activeCard(new BigDecimal("100.00"));
            Card to = activeCard(new BigDecimal("10.00"));
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(2L, OWNER_ID)).thenReturn(Optional.of(to));

            cardService.transfer(OWNER_ID, new TransferRequest(1L, 2L, new BigDecimal("30.00")));

            assertAll(
                    () -> assertThat(from.getBalance()).isEqualByComparingTo("70.00"),
                    () -> assertThat(to.getBalance()).isEqualByComparingTo("40.00")
            );
        }

        @Test
        void rejectsTransferToSameCard() {
            assertThatThrownBy(() ->
                    cardService.transfer(OWNER_ID, new TransferRequest(1L, 1L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessRuleException.class);
            verify(cardRepository, never()).findByIdAndOwnerId(any(), any());
        }

        @Test
        void rejectsInsufficientFunds() {
            Card from = activeCard(new BigDecimal("5.00"));
            Card to = activeCard(new BigDecimal("0.00"));
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(2L, OWNER_ID)).thenReturn(Optional.of(to));

            assertThatThrownBy(() ->
                    cardService.transfer(OWNER_ID, new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Insufficient");
            assertThat(from.getBalance()).isEqualByComparingTo("5.00");
        }

        @Test
        void rejectsBlockedSourceCard() {
            Card from = card(CardStatus.BLOCKED, new BigDecimal("100.00"), LocalDate.now().plusYears(2));
            Card to = activeCard(new BigDecimal("0.00"));
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(2L, OWNER_ID)).thenReturn(Optional.of(to));

            assertThatThrownBy(() ->
                    cardService.transfer(OWNER_ID, new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsExpiredSourceCard() {
            Card from = card(CardStatus.ACTIVE, new BigDecimal("100.00"), LocalDate.now().minusDays(1));
            Card to = activeCard(new BigDecimal("0.00"));
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(2L, OWNER_ID)).thenReturn(Optional.of(to));

            assertThatThrownBy(() ->
                    cardService.transfer(OWNER_ID, new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsCardNotOwnedByCaller() {
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    cardService.transfer(OWNER_ID, new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void blockSetsBlockedStatus() {
            Card card = activeCard(BigDecimal.ZERO);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            cardService.block(1L);

            assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        }

        @Test
        void activateRejectsExpiredCard() {
            Card card = card(CardStatus.BLOCKED, BigDecimal.ZERO, LocalDate.now().minusDays(1));
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.activate(1L))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void requestBlockBlocksOwnCard() {
            Card card = activeCard(BigDecimal.ZERO);
            when(cardRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(card));

            cardService.requestBlock(1L, OWNER_ID);

            assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        }
    }
}
