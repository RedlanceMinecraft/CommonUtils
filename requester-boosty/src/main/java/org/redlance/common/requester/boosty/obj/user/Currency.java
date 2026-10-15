package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Currency of a price, by its ISO 4217 code; the currencies the Boosty site knows.
 */
public enum Currency {
    RUB,
    EUR,
    USD,
    UAH,
    BRL,
    CHF,
    SEK,
    GBP,
    AED,
    AUD,
    JPY,
    UZS,
    KZT,

    /**
     * A currency this wrapper does not know yet.
     */
    UNKNOWN;

    /**
     * Reads a currency code in any case; one this wrapper does not know becomes {@link #UNKNOWN}.
     */
    @JsonCreator
    public static Currency of(String value) {
        return BoostyValues.parse(values(), value, UNKNOWN);
    }

    /**
     * Leaves out the prices in currencies this wrapper does not know: they would all collide on {@link #UNKNOWN}.
     */
    static Map<Currency, Double> knownPrices(@Nullable Map<Currency, Double> prices) {
        if (prices == null || prices.isEmpty()) {
            return Map.of();
        }

        Map<Currency, Double> known = new EnumMap<>(Currency.class);
        known.putAll(prices);
        known.remove(UNKNOWN);
        return Collections.unmodifiableMap(known);
    }
}
