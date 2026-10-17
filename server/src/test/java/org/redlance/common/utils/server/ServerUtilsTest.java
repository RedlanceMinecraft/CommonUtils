package org.redlance.common.utils.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;
import java.util.Map;

/** The locale of a visitor, from the Accept-Language header or else the country Cloudflare puts in CF-IPCountry. */
public class ServerUtilsTest {
    private static final Locale FALLBACK = Locale.US;

    @Test
    public void acceptLanguageBeatsTheCountry() {
        Assertions.assertEquals(Locale.of("pt", "BR"), findLocale(Map.of("Accept-Language", "pt-BR,pt;q=0.9", "CF-IPCountry", "RU")));
    }

    @ParameterizedTest
    @CsvSource({
            "RU, ru_RU", "BY, be_BY", "CH, de_CH", "BE, fr_BE", "BR, pt_BR",
            "GR, el_GR", "GT, es_GT", "PY, es_PY", "EC, es_EC", "IQ, ar_IQ", "SN, fr_SN", "CD, fr_CD",
            "EE, et_EE", "HR, hr_HR", "KG, ky_KG", "KH, km_KH", "AF, fa_AF", "AD, ca_AD", "MO, zh_MO",
            "NG, en_NG", "JM, en_JM", "gt, es_GT"
    })
    public void countryGivesItsLanguage(String country, String locale) {
        Assertions.assertEquals(Locale.forLanguageTag(locale.replace('_', '-')), findLocale(Map.of("CF-IPCountry", country)));
    }

    @ParameterizedTest
    @CsvSource({"XX", "T1"})
    public void countryCloudflareDoesNotKnowFallsBack(String country) {
        Assertions.assertEquals(FALLBACK, findLocale(Map.of("CF-IPCountry", country)));
    }

    @Test
    public void noHeadersFallBack() {
        Assertions.assertEquals(FALLBACK, findLocale(Map.of()));
    }

    private static Locale findLocale(Map<String, String> headers) {
        return ServerUtils.findLocale(headers::get, (code, fallback) -> Locale.forLanguageTag(code.replace('_', '-')), () -> FALLBACK);
    }
}
