package jacz.util.io.serialization.localstorage;

import java.io.IOException;

/**
 * A local storage with a built-in function for storing a user-given version of the stored data. An interface is
 * provided for facilitating updates to the stored data
 */
public class VersionedLocalStorage extends LocalStorage {

    private static final String VERSION = "@@@version@@@";

    public VersionedLocalStorage(String path) throws IOException {
        super(path);
    }

    public VersionedLocalStorage(String path, Updater updater, String currentVersion) throws IOException {
        super(path);
        String storedVersion = getVersion();
        while (!currentVersion.equals(storedVersion)) {
            storedVersion = updater.update(this, storedVersion);
        }
        updateVersion(currentVersion);
    }

    public static VersionedLocalStorage createNew(String path, String version) throws IOException {
        VersionedLocalStorage vls = (VersionedLocalStorage) LocalStorage.createNew(path);
        vls.updateVersion(version);
        return vls;
    }

    public String getVersion() {
        return getString(VERSION);
    }

    public void updateVersion(String version) {
        setString(VERSION, version);
    }
}
