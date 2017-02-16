package net.monicet.monicet;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by ubuntu on 16-02-2017.
 */

public class MyFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        if (pathname.getName().toLowerCase().endsWith(Utils.JSON_FILE_EXTENSION) ||
                pathname.getName().toLowerCase().endsWith(Utils.CSV_FILE_EXTENSION)) {
            return true;
        }
        return false;
    }
}
