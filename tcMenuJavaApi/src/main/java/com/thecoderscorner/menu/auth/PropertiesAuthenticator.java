package com.thecoderscorner.menu.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

/**
 * Stores authentication to a properties file and then validates against the stored values. By default, there are no
 * authentication pairs stored, and the secure passcode is "1234"
 *
 * NOTE:
 * This is only suited to very simple use cases where the level of security required is not particularly high and
 * the file system of the device is completely secured. The authentication UUIDs are stored PLAIN TEXT.
 */
public class PropertiesAuthenticator implements MenuAuthenticator {
    private final System.Logger logger = System.getLogger(getClass().getSimpleName());
    private final Properties properties = new Properties();
    private final String location;

    public PropertiesAuthenticator(String location) {
        this.location = location;
        try {
            this.properties.load(Files.newBufferedReader(Path.of(location)));
        } catch (IOException e) {
            logger.log(ERROR, "Unable to read authentication properties");
        }
    }

    @Override
    public boolean authenticate(String user, UUID uuid) {
        String val;
        synchronized (this.properties) {
            val = this.properties.getProperty(user);
        }
        if(val == null) return false;
        return UUID.fromString(val).equals(uuid);
    }

    /**
     * Adds an authentication token to the store, it assumes that all appropriate permission from the user has
     * been sought.
     * @param user the user to add
     * @param uuid the uuid associated with the user
     * @return
     */
    @Override
    public boolean addAuthentication(String user, UUID uuid) {
        try {
            synchronized (properties) {
                properties.setProperty(user, uuid.toString());
                properties.store(Files.newBufferedWriter(Path.of(location)), "TcMenu Auth properties");
            }
            logger.log(INFO, "Wrote auth properties to ", location);
            return true;
        } catch (Exception e) {
            logger.log(ERROR, "Failed to write auth properties to ", location);
            return false;
        }
    }

    @Override
    public boolean doesPasscodeMatch(String passcode) {
        return properties.getProperty("securityPasscode", "1234").equals(passcode);
    }
}
