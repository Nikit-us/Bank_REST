package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByCardNumberHash(String cardNumberHash);
}
