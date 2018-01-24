package ch.psi.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Utilities to folder management.
 */
public class Folder {

    static final Logger logger = Logger.getLogger(Folder.class.getName());

    final String name;
    final File file;

    public Folder(String name) {
        this.name = name;
        file = new File(name);
    }

    public Folder(File folder) {
        this.name = folder.getPath();
        file = folder;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return IO.getSize(file);
    }

    public void assertExists() throws FileNotFoundException {
        if ((!file.exists()) || (!file.isDirectory())) {
            throw new FileNotFoundException(name);
        }
    }

    public File[] getContents() throws FileNotFoundException {
        assertExists();
        return file.listFiles();
    }

    public String[] getContentsNames() throws FileNotFoundException {
        File[] contents = getContents();
        String[] contentNames = new String[contents.length];
        for (int i = 0; i < contentNames.length; i++) {
            contentNames[i] = contents[i].getName();
        }
        return Arr.sort(contentNames);
    }

    public void copy(String to, boolean hidden) throws FileNotFoundException, IOException {
        logger.info("Copy folder" + name);
        assertExists();
        if ((hidden) || (!file.isHidden())) {
            File toFile = new File(to);
            toFile.mkdirs();
            if (file.getCanonicalPath().equalsIgnoreCase(toFile.getCanonicalPath())) {
                throw new IOException("Invalid destination folder");
            }
        }
        String[] fromContents = getContentsNames();
        String[] toContents = (new Folder(to)).getContentsNames();

        for (String fromContent : fromContents) {
            Path fromPath = Paths.get(name, fromContent);
            Path toPath = Paths.get(to, fromContent);
            File fromFile = fromPath.toFile();
            if (fromFile.isDirectory()) {
                new Folder(fromPath.toString()).copy(toPath.toString(), hidden);
            } else {
                if ((hidden) || (!fromFile.isHidden())) {
                    boolean copy = !Arr.contains(toContents, fromContent);
                    if (!copy) {
                        File toFile = toPath.toFile();
                        copy = ((fromFile.length() != toFile.length())
                                || (fromFile.lastModified() != toFile.lastModified()));
                    }
                    if (copy) {
                        logger.fine("Copying " + toPath.toString());
                        IO.copy(fromPath.toString(), toPath.toString());
                    }
                }
            }
        }

        for (String toContent : toContents) {
            Path toPath = Paths.get(to, toContent);
            boolean delete = !Arr.contains(fromContents, toContent);
            if ((delete) && (!hidden) && (toPath.toFile().isHidden())) {
                delete = false;
            }
            if (delete) {
                logger.fine("Removing " + toPath.toString());
                IO.deleteRecursive(toPath.toString());
            }
        }
    }

    public void delete() throws IOException {
        IO.deleteRecursive(name);
    }

    public void move(String to) throws IOException {
        copy(to, true);
        delete();
    }

    public void cleanup(long timeToLiveMillis, boolean files, boolean directories, boolean hidden) throws IOException {
        logger.info("Cleaning " + name + " - ttl:" + timeToLiveMillis + "ms files:" + files + " dirs:" + directories + " hidden:" + hidden);
        if (timeToLiveMillis > 0) {
            assertExists();
            if ((hidden) || (!file.isHidden())) {
                File[] contents = getContents();
                for (File content : contents) {
                    if ((hidden) || (!content.isHidden())) {
                        if ((content.isDirectory() && directories)
                                || (!content.isDirectory() && files)) {
                            long age = System.currentTimeMillis() - content.lastModified();
                            if (age > timeToLiveMillis) {
                                logger.fine("Removing " + content);
                                IO.deleteRecursive(content.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }
}
