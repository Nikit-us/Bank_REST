package com.example.bankcards.repository.specification;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.Card_;
import com.example.bankcards.entity.User_;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get(Card_.owner).get(User_.id), ownerId);
    }

    public static Specification<Card> lastFourContains(String fragment) {
        return (root, query, cb) -> cb.like(root.get(Card_.lastFour), "%" + fragment + "%");
    }

    public static Specification<Card> hasStatus(CardStatus status) {
        return (root, query, cb) -> {
            if (status == CardStatus.EXPIRED) {
                return cb.lessThan(root.get(Card_.expiryDate), LocalDate.now());
            }
            Predicate stored = cb.equal(root.get(Card_.status), status);
            Predicate notExpired = cb.greaterThanOrEqualTo(root.get(Card_.expiryDate), LocalDate.now());
            return cb.and(stored, notExpired);
        };
    }

    public static Specification<Card> build(Long ownerId, CardStatus status, String search) {
        List<Specification<Card>> parts = new ArrayList<>();
        if (ownerId != null) {
            parts.add(ownedBy(ownerId));
        }
        if (status != null) {
            parts.add(hasStatus(status));
        }
        if (isNotBlank(search)) {
            parts.add(lastFourContains(search.strip()));
        }
        return parts.stream().reduce(Specification.unrestricted(), Specification::and);
    }
}
