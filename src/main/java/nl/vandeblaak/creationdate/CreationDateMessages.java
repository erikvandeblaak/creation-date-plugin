package nl.vandeblaak.creationdate;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Texts and date patterns of the balloon, in English or Dutch.
 */
final class CreationDateMessages {

    static final Locale DUTCH = Locale.forLanguageTag("nl-NL");

    private static final String BUNDLE = "messages.CreationDateBundle";

    private CreationDateMessages() {
    }

    /**
     * @return Dutch when the system language is Dutch, otherwise English
     */
    static Locale locale() {
        return "nl".equals(Locale.getDefault().getLanguage()) ? DUTCH : Locale.ENGLISH;
    }

    static String get(String key, Locale locale) {
        // No fallback to the default locale, otherwise English would pick up Dutch texts on a Dutch system
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return bundle.getString(key);
    }
}
