package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.util.CardMaskUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = CardMaskUtil.class)
public interface CardMapper {

    @Mapping(target = "maskedNumber", expression = "java(CardMaskUtil.mask(card.getLastFour()))")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "status", expression = "java(card.effectiveStatus())")
    CardResponse toResponse(Card card);
}
