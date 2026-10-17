package org.redlance.common.adventure;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.TranslationStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Which file a locale reads each key from, with the files named the Minecraft way ({@code ru_ru}) or by language alone
 * ({@code ru}). Every file holds its own name as the translation of each of its keys.
 */
public class TranslatorUtilsTest {
    private static final String KEY = "file";

    @TempDir
    public Path messages;

    @Test
    public void relatedLanguagesWithFilesOfTheirOwnLoadSideBySide() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "ru_ru", "tt_ru", "id_id", "ms_my");
        assertReads(store, Locale.of("tt", "RU"), "tt_ru");
        assertReads(store, Locale.of("ms", "MY"), "ms_my");
        assertReads(store, Locale.of("ru", "RU"), "ru_ru");
        assertReads(store, Locale.of("id", "ID"), "id_id");
    }

    @Test
    public void localeWithoutAFileOfItsLanguageReadsARelatedOne() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "ru_ru");
        assertReads(store, Locale.of("uk", "UA"), "ru_ru");
        assertReads(store, Locale.of("kk", "KZ"), "ru_ru");
        assertReads(store, Locale.of("ky", "KG"), "ru_ru");
    }

    @Test
    public void regionWithoutAFileReadsItsLanguage() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "ru_ru", "zh_tw");
        assertReads(store, Locale.of("ru", "UA"), "ru_ru");
        assertReads(store, Locale.of("zh", "CN"), "zh_tw");
        assertReads(store, Locale.of("zh", "HK"), "zh_tw");
    }

    @Test
    public void filesNamedByLanguageServeEveryRule() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en", "ru", "tt");
        assertReads(store, Locale.of("tt", "RU"), "tt");
        assertReads(store, Locale.of("uk", "UA"), "ru");
        assertReads(store, Locale.of("es", "MX"), "en");
    }

    @Test
    public void regionWithoutAFileReadsTheMainFileOfItsLanguage() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "en_pt", "pt_br", "pt_pt", "fr_ca", "fr_fr", "zh_cn", "zh_tw");
        assertReads(store, Locale.UK, "en_us");
        assertReads(store, Locale.of("pt", "AO"), "pt_pt");
        assertReads(store, Locale.of("fr", "LU"), "fr_fr");
        assertReads(store, Locale.of("zh", "HK"), "zh_tw");
        assertReads(store, Locale.of("zh", "SG"), "zh_cn");
    }

    @Test
    public void regionReadsItsOwnLanguageRatherThanANeighbours() throws IOException {
        write("fr_fr", "both");
        write("it_it", "both", "more");
        write("de_de", "both");
        write("nl_nl", "both", "more");
        write("en_us", "both", "more");
        TranslationStore.StringBased<MessageFormat> store = register();

        assertReads(store, Locale.of("fr", "CH"), "more", "en_us");
        assertReads(store, Locale.of("de", "AT"), "more", "en_us");
        assertReads(store, Locale.of("it", "CH"), "more", "it_it");
        assertReads(store, Locale.of("nl", "BE"), "more", "nl_nl");
    }

    @Test
    public void regionListedUnderALanguageReadsThatFile() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "de_ch", "de_de");
        assertReads(store, Locale.of("de", "AT"), "de_de");
        assertReads(store, Locale.of("de", "CH"), "de_ch");
    }

    @Test
    public void secondFileForTheSameLocaleIsSkipped() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("en_us", "main");
        assertReads(store, Locale.US, "en_us");
    }

    @Test
    public void mainIsTheDefaultLocale() throws IOException {
        TranslationStore.StringBased<MessageFormat> store = load("main", "ru");
        assertReads(store, Locale.of("es", "MX"), "main");
        assertReads(store, Locale.of("ru", "RU"), "ru");
    }

    @Test
    public void keyMissingFromALanguageComesFromTheRelatedOneThenTheDefault() throws IOException {
        write("tt_ru", "own");
        write("ru_ru", "own", "related");
        write("en_us", "own", "related", "default");
        TranslationStore.StringBased<MessageFormat> store = register();

        Locale tatar = Locale.of("tt", "RU");
        assertReads(store, tatar, "own", "tt_ru");
        assertReads(store, tatar, "related", "ru_ru");
        assertReads(store, tatar, "default", "en_us");
    }

    @Test
    public void keyMissingFromALanguageComesFromTheRelatedOneWithFilesNamedByLanguage() throws IOException {
        write("tt", "own");
        write("ru", "own", "related");
        write("en", "own", "related", "default");
        TranslationStore.StringBased<MessageFormat> store = register();

        Locale tatar = Locale.of("tt", "RU");
        assertReads(store, tatar, "own", "tt");
        assertReads(store, tatar, "related", "ru");
        assertReads(store, tatar, "default", "en");
    }

    @Test
    public void keyMissingFromARegionComesFromTheRegionListingIt() throws IOException {
        write("pt_br", "own");
        write("pt_pt", "own", "region");
        write("en_us", "own", "region", "default");
        TranslationStore.StringBased<MessageFormat> store = register();

        Locale brazil = Locale.of("pt", "BR");
        assertReads(store, brazil, "own", "pt_br");
        assertReads(store, brazil, "region", "pt_pt");
        assertReads(store, brazil, "default", "en_us");
    }

    /** Files that hold only {@link #KEY}. */
    private TranslationStore.StringBased<MessageFormat> load(String... files) throws IOException {
        for (String file : files) {
            write(file, KEY);
        }
        return register();
    }

    /** A file that holds its own name as the translation of every key. */
    private void write(String file, String... keys) throws IOException {
        StringBuilder translations = new StringBuilder();
        for (String key : keys) {
            translations.append(key).append('=').append(file).append('\n');
        }
        Files.writeString(this.messages.resolve(file + ".properties"), translations);
    }

    private TranslationStore.StringBased<MessageFormat> register() {
        TranslationStore.StringBased<MessageFormat> store = TranslationStore.messageFormat(Key.key("test", "translations"));
        store.defaultLocale(TranslatorUtils.DEFAULT_LOCALE);
        TranslatorUtils.registerLocalizations(store, TranslatorUtils.DEFAULT_LOCALE, this.messages);
        return store;
    }

    private static void assertReads(TranslationStore.StringBased<MessageFormat> store, Locale locale, String file) {
        assertReads(store, locale, KEY, file);
    }

    private static void assertReads(TranslationStore.StringBased<MessageFormat> store, Locale locale, String key, String file) {
        MessageFormat format = store.translate(key, locale);
        Assertions.assertEquals(file, format == null ? null : format.toPattern(), () -> locale + " reads " + key + " from the wrong file");
    }
}
