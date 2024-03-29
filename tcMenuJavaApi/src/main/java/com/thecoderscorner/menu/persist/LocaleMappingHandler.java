package com.thecoderscorner.menu.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface LocaleMappingHandler {
    LocaleMappingHandler NOOP_IMPLEMENTATION = new NoLocaleEnabledLocalHandler();

    boolean isLocalSupportEnabled();
    String getLocalSpecificEntry(String source) throws IllegalArgumentException;
    void setLocalSpecificEntry(String source, String newValue) throws IllegalArgumentException;
    List<Locale> getEnabledLocales();
    void changeLocale(Locale locale) throws IOException;
    void saveChanges();
    Map<String, String> getUnderlyingMap();
    Locale getCurrentLocale();

    default String getFromLocaleWithDefault(String localeEntry, String defText) {
        if(!isLocalSupportEnabled()) return defText;

        if((!localeEntry.startsWith("%") || localeEntry.length() < 2)) {
            return defText.startsWith("\\%") ? defText.substring(1) : defText;
        }
        if(getCurrentLocale().getLanguage().equals("--")) return defText;
        String ret = getLocalSpecificEntry(localeEntry.substring(1));
        return (ret == null) ? defText : ret;
    }

    default String getWithLocaleInitIfNeeded(String localeName, String existing) {
        if(isLocalSupportEnabled()) {
            if(!existing.startsWith("%") && this instanceof PropertiesLocaleEnabledHandler) {
                if(existing.startsWith("\\%")) existing = existing.substring(1);
                ((PropertiesLocaleEnabledHandler)this).putIntoDefaultIfNeeded(localeName.substring(1), existing);
            }
            return getFromLocaleWithDefault(localeName, existing);
        } else {
            return existing;
        }
    }

    default String getFromLocaleOrUseSource(String name) {
        return getFromLocaleWithDefault(name, name);
    }

    /**
     * Report a change in the file system that has occurred in the directory this locale represents, ignored by no
     * locale handler.
     */
    default void notifyExternalChange(String propertiesFile) {}

    default boolean isDirty(Optional<String> fileName) { return false; }

    default void setSaveNotificationConsumer(BiConsumer<Path, String> listener) {}
}
