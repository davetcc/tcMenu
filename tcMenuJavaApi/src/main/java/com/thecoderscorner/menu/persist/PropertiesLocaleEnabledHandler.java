package com.thecoderscorner.menu.persist;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public class PropertiesLocaleEnabledHandler implements LocaleMappingHandler {
    public static final Locale DEFAULT_LOCALE = new Locale("");

    private final System.Logger logger = System.getLogger(getClass().getSimpleName());

    private final SafeBundleLoader bundleLoader;
    private final Object localeLock = new Object();
    private Locale currentLocale;
    private Map<String, String> cachedEntries;
    private Map<String, String> parentCachedEntries;
    private Map<String, String> defaultCachedEntries;
    private boolean needsSave = false;
    private boolean defaultNeedsSave = false;
    private AtomicReference<BiConsumer<Path, String>> saveListener = new AtomicReference<>();

    public PropertiesLocaleEnabledHandler(SafeBundleLoader bundleLoader) {
        this.bundleLoader = bundleLoader;
        try {
            changeLocale(DEFAULT_LOCALE);
        } catch(Exception ex) {
            throw new UnsupportedOperationException("Default Locale not available", ex);
        }
    }

    public void setSaveNotificationConsumer(BiConsumer<Path, String> listener) {
        saveListener.set(listener);
    }

    @Override
    public boolean isLocalSupportEnabled() {
        return true;
    }

    @Override
    public String getLocalSpecificEntry(String source) throws IllegalArgumentException {
        synchronized (localeLock) {
            var ret = cachedEntries.get(source);
            if(ret == null && parentCachedEntries != null) {
                ret = parentCachedEntries.get(source);
            }
            if(ret == null) {
                ret = defaultCachedEntries.get(source);
            }
            return ret;
        }
    }

    @Override
    public void setLocalSpecificEntry(String source, String newValue) throws IllegalArgumentException {
        synchronized (localeLock) {
            if(!defaultCachedEntries.containsKey(source)) {
                defaultCachedEntries.put(source, newValue);
                defaultNeedsSave = true;
            }
            cachedEntries.put(source, newValue);
            needsSave = true;
        }
    }

    @Override
    public List<Locale> getEnabledLocales() {
        try(var filesStream = Files.walk(bundleLoader.getLocation(), 2, FileVisitOption.FOLLOW_LINKS)) {
            return
                    filesStream.filter(p -> p.toString().endsWith(".properties"))
                    .map(p -> p.getFileName().toString().replace(bundleLoader.getBaseName(), ""))
                    .map(s -> s.replace(".properties", ""))
                    .map(s -> s.startsWith("_") ? s.substring(1) : s)
                    .map(this::makeLocale)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private Locale makeLocale(String s) {
        var localeStr = (s.startsWith("_")) ? s.substring(1) : s;
        if(localeStr.isEmpty() || localeStr.equals("==")) {
            return new Locale(localeStr);
        }
        if(localeStr.length() == 2) {
            return new Locale(localeStr);
        } else if(localeStr.matches("\\w\\w_\\w\\w")) {
            return new Locale(localeStr.substring(0, 2), localeStr.substring(3));
        } else throw new UnsupportedOperationException("Unsupported type of locale only language or languageCountry" + localeStr);
    }

    @Override
    public void changeLocale(Locale locale) throws IOException {
        synchronized (localeLock) {
            // if we haven't yet loaded the defaults, load them now
            if(defaultCachedEntries != null) {
                saveChanges();
            }

            // reload the main bundle every time, to be sure we have the latest.
            defaultCachedEntries = bundleLoader.loadResourceBundleAsMap(DEFAULT_LOCALE);
            currentLocale = locale;
            parentCachedEntries = null;

            if(locale.getLanguage().equals("--")) {
                cachedEntries = new HashMap<>();
            } else if(locale.getLanguage().isEmpty()) {
                cachedEntries = defaultCachedEntries;
            } else if(locale.getCountry() == null || locale.getCountry().isEmpty() ) {
                cachedEntries = bundleLoader.loadResourceBundleAsMap(currentLocale);
            } else {
                cachedEntries = bundleLoader.loadResourceBundleAsMap(currentLocale);
                parentCachedEntries = bundleLoader.loadResourceBundleAsMap(new Locale(currentLocale.getLanguage()));
            }
            needsSave = false;
            defaultNeedsSave = false;
        }
    }

    @Override
    public void notifyExternalChange(String propertiesFile) {
        synchronized (localeLock) {
            var currentPath = bundleLoader.getPathForLocale(currentLocale).getFileName();
            boolean fileBeingEdited = Paths.get(propertiesFile).equals(currentPath);
            // if there are changes, we do not load the files back
            if(defaultNeedsSave || (needsSave && fileBeingEdited)) {
                logger.log(INFO, "Locale reload was skipped as files were dirty");
                return;
            }
            try {
                changeLocale(currentLocale);
            } catch (IOException e) {
                logger.log(ERROR, "Locale reload has failed for " + propertiesFile);
            }
        }
    }

    @Override
    public boolean isDirty(Optional<String> propertiesFile) {
        var currentPath = bundleLoader.getPathForLocale(currentLocale).getFileName();
        if(propertiesFile.isPresent()) {
            // TODO what about the intermediate level
            boolean fileBeingEdited = Paths.get(propertiesFile.get()).equals(currentPath);
            return (defaultNeedsSave || (needsSave && fileBeingEdited));
        } else {
            return needsSave || defaultNeedsSave;
        }
    }

    @Override
    public void saveChanges() {
        synchronized (localeLock) {
            if(defaultNeedsSave) {
                defaultNeedsSave = false;
                bundleLoader.saveChangesKeepingFormatting(DEFAULT_LOCALE, defaultCachedEntries, saveListener.get());
            }
            if(needsSave) {
                needsSave = false;
                bundleLoader.saveChangesKeepingFormatting(currentLocale, cachedEntries, saveListener.get());
            }
        }
    }

    @Override
    public Map<String, String> getUnderlyingMap() {
        synchronized (localeLock) {
            // lowest priority is the default locale.
            var underlyingAll = new HashMap<>(defaultCachedEntries);
            // followed by the parent (IE language level)
            if(parentCachedEntries != null) {
                underlyingAll.putAll(parentCachedEntries);
            }
            // lastly followed by country.
            underlyingAll.putAll(cachedEntries);
            return underlyingAll;
        }
    }

    public SafeBundleLoader getSafeLoader() {
        return bundleLoader;
    }

    @Override
    public Locale getCurrentLocale() {
        synchronized (localeLock) {
            return currentLocale;
        }
    }

    public void putIntoDefaultIfNeeded(String localeName, String existing) {
        if(defaultCachedEntries.containsKey(localeName)) return;
        defaultCachedEntries.put(localeName, existing);
        defaultNeedsSave = true;
    }
}
