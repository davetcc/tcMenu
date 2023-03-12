package com.thecoderscorner.menu.persist;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if(!isLocalSupportEnabled() || !localeEntry.startsWith("%") && localeEntry.length() > 1) return defText;
        if(getCurrentLocale().getLanguage().equals("--")) return defText;
        String ret = getLocalSpecificEntry(localeEntry.substring(1));
        return (ret == null) ? defText : ret;
    }

    default String getWithLocaleInitIfNeeded(String localeName, String existing) {
        if(isLocalSupportEnabled()) {
            if(!existing.startsWith("%") && this instanceof PropertiesLocaleEnabledHandler) {
                ((PropertiesLocaleEnabledHandler)this).putIntoDefaultIfNeeded(localeName.substring(1), existing);
            }
            return getFromLocaleWithDefault(existing, existing);
        } else {
            return existing;
        }
    }
}
