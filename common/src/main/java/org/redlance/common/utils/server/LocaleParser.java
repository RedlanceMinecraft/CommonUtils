package org.redlance.common.utils.server;

import java.util.Locale;
import java.util.function.Supplier;

@FunctionalInterface
public interface LocaleParser {
    Locale parseLocale(String localeCode, Supplier<Locale> fallback);
}
