/*
 * Copyright (c)  2016-2020 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator;

import com.thecoderscorner.menu.editorui.storage.ConfigurationStorage;
import com.thecoderscorner.menu.editorui.util.IHttpClient;
import com.thecoderscorner.menu.persist.ReleaseType;
import com.thecoderscorner.menu.persist.VersionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;

public class OnlineLibraryVersionDetector implements LibraryVersionDetector {
    private static final System.Logger logger = System.getLogger(OnlineLibraryVersionDetector.class.getSimpleName());


    public final static String LIBRARY_VERSIONING_URL_APPEND = "/products/arduino-libraries/libraries/library-versions.xml";
    private static final long REFRESH_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(2);

    private final String urlBase;
    private final ConfigurationStorage configurationStore;
    private final IHttpClient client;

    private final Object cacheLock = new Object();
    private long lastAccess;
    private Map<String, VersionInfo> versionCache;
    private ReleaseType cachedReleaseType;

    public OnlineLibraryVersionDetector(String urlBase, IHttpClient client, ConfigurationStorage configurationStore) {
        this.client = client;
        this.urlBase = urlBase;
        this.configurationStore = configurationStore;
        changeReleaseType(configurationStore.getReleaseStream());
    }

    public void changeReleaseType(ReleaseType relType) {
        synchronized (cacheLock) {
            lastAccess = 0;
            versionCache = Map.of();
            cachedReleaseType = relType;
            configurationStore.setReleaseStream(relType);
        }
    }

    public ReleaseType getReleaseType() {
        synchronized (cacheLock) {
            return cachedReleaseType;
        }
    }


    public Map<String, VersionInfo> acquireVersions() {
        ReleaseType relType;
        synchronized (cacheLock) {
            if ((System.currentTimeMillis() - lastAccess) < REFRESH_TIMEOUT_MILLIS) {
                return versionCache;
            }
            relType = cachedReleaseType;
        }

        try {
            logger.log(INFO, "Starting to acquire version, cache not present or timed out");
            var libDict = new HashMap<String, VersionInfo>();

            var verData = client.postRequestForString(urlBase + LIBRARY_VERSIONING_URL_APPEND, "", IHttpClient.HttpDataType.FORM);
            var inStream = new ByteArrayInputStream(verData.getBytes());

            logger.log(DEBUG, "Data acquisition from server completed");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document doc = dBuilder.parse(inStream);
            var root = doc.getDocumentElement();

            logger.log(DEBUG, "Document created");

            addVersionsToMap(root.getElementsByTagName("Libraries"), "Library", relType, libDict);
            addVersionsToMap(root.getElementsByTagName("Plugins"), "Plugin", relType, libDict);
            addVersionsToMap(root.getElementsByTagName("Apps"), "App", relType, libDict);

            logger.log(INFO, "Version information fully loaded.");

            synchronized (cacheLock) {
                lastAccess = System.currentTimeMillis();
                versionCache = libDict;
            }
            return libDict;
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Unable to get versions from main site", e);
        }
        return versionCache;
    }

    private void addVersionsToMap(NodeList topLevelElem, String type, ReleaseType relType, HashMap<String, VersionInfo> libDict) {
        logger.log(DEBUG, "Starting to acquire version list from core site");
        for(int i=0; i< topLevelElem.getLength(); i++) {
            var item = topLevelElem.item(i);
            if (item.getAttributes().getNamedItem("stream").getNodeValue().equals(relType.toString())) {
                var children = ((Element)item).getElementsByTagName(type);
                for(int j=0; j<children.getLength(); j++) {
                    var versionData = children.item(j);
                    var ver = new VersionInfo(versionData.getAttributes().getNamedItem("version").getNodeValue());
                    libDict.put(versionData.getAttributes().getNamedItem("name").getNodeValue() + "/" + type, ver);
                }
            }
        }
        logger.log(DEBUG, "Successfully got version list from core site for " + cachedReleaseType);
    }

    @Override
    public boolean availableVersionsAreValid(boolean doRefresh) {
        synchronized (cacheLock) {
            if (versionCache.isEmpty() || (System.currentTimeMillis() - lastAccess) > (REFRESH_TIMEOUT_MILLIS - 1000) && doRefresh) {
                acquireVersions();
            }
            return (!versionCache.isEmpty()) && ((System.currentTimeMillis() - lastAccess) < REFRESH_TIMEOUT_MILLIS);
        }
    }

    public static void extractFilesFromZip(Path outDir, InputStream inStream) throws IOException {
        try(var zipStream =  new ZipInputStream(inStream)) {
            ZipEntry entry;
            while((entry = zipStream.getNextEntry())!=null) {
                Path filePath = outDir.resolve(entry.getName());
                String fileInfo = String.format("Entry: [%s] len %d to %s", entry.getName(), entry.getSize(), filePath);
                logger.log(DEBUG, fileInfo);

                if(!Files.exists(filePath.getParent())) {
                    Files.createDirectories(filePath.getParent());
                }

                if(entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.write(filePath, zipStream.readAllBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        }
    }
}
