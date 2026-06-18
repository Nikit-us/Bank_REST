package com.example.bankcards.util;

import static org.apache.commons.lang3.StringUtils.getDigits;
import static org.apache.commons.lang3.StringUtils.right;

public final class CardMaskUtil {

    private static final String MASK_GROUPS = "**** **** **** ";
    private static final int LAST_FOUR = 4;

    private CardMaskUtil() {
    }

    public static String mask(String lastFour) {
        return MASK_GROUPS + lastFour;
    }

    public static String lastFour(String cardNumber) {
        String digits = getDigits(cardNumber);
        return right(digits, LAST_FOUR);
    }
}
