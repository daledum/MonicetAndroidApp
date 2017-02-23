package net.monicet.monicet;

/**
 * Created by ubuntu on 23-02-2017.
 */

public enum AllowedFileExtension {
    JSON(".json"),
    CSV(".csv");

    private final String fileExtension;

    AllowedFileExtension(String vFileExtension) {
        fileExtension = vFileExtension;
    }

    @Override
    public String toString() {
        return fileExtension;
    }
}
