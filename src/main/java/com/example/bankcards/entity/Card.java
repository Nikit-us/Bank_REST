package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cards", uniqueConstraints = @UniqueConstraint(name = "uk_cards_card_number_hash", columnNames = "card_number_hash"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "card_number", nullable = false, length = 512)
    private String cardNumber;

    @Column(name = "card_number_hash", nullable = false, length = 64)
    private String cardNumberHash;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cards_owner"))
    private User owner;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Builder
    public Card(String cardNumber, String cardNumberHash, String lastFour, User owner,
                LocalDate expiryDate, CardStatus status, BigDecimal balance) {
        this.cardNumber = cardNumber;
        this.cardNumberHash = cardNumberHash;
        this.lastFour = lastFour;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    public CardStatus effectiveStatus() {
        return isExpired() ? CardStatus.EXPIRED : status;
    }

    public boolean isUsable() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
