package ch.psi.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * File system operations.
 */
public class IO {

    //Orderer Property file entries    
    public static final String PROPERTY_FILE_COMMENTED_FLAG = "#";

    public static ArrayList<String> getOrderedPropertyKeys(String fileName) throws IOException {
        return getOrderedPropertyKeys(fileName, false);
    }

    public static ArrayList<String> getOrderedPropertyKeys(String fileName, boolean commented) throws IOException {
        ArrayList<String> ret = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(fileName))) {
            line = line.trim();
            if ((!line.trim().startsWith(PROPERTY_FILE_COMMENTED_FLAG)) || (commented)) {
                String[] tokens = line.split("=");
                if (tokens.length >= 2) {
                    String key = tokens[0];
                    if (key.startsWith(PROPERTY_FILE_COMMENTED_FLAG)) {
                        key = key.substring(PROPERTY_FILE_COMMENTED_FLAG.length());
                    }
                    key = key.replace("\\\\", "\\"); //Property files dupplicate '\' charachter
                    key = key.replace("\\:", ":");
                    key = key.replace("\\ ", " ");
                    key = key.replace("\\u0020", " ");
                    ret.add(key);
                }
            }
        }
        return ret;
    }

    //ZIP & Jarfiles        
    public static void extractZipFile(File zip, String extractionPath, String zipSubFolder) throws IOException {
        zip.mkdirs();
        try (ZipFile zipFile = new ZipFile(zip)) {
            Enumeration enumEntries = zipFile.entries();
            while (enumEntries.hasMoreElements()) {
                ZipEntry file = (ZipEntry) enumEntries.nextElement();
                if ((zipSubFolder == null) || (file.getName().startsWith(zipSubFolder))) {
                    File f = new File(extractionPath, file.getName());
                    if (file.isDirectory()) { // if its a directory, create it
                        f.mkdir();
                        continue;
                    }
                    try (InputStream in = zipFile.getInputStream(file); OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(extractionPath, file.getName())));) {
                        IO.copy(in, out);
                    }
                }
            }
        }
    }

    public static void extractZipFile(File zip, String extractionPath) throws IOException {
        extractZipFile(zip, extractionPath, null);
    }

    public static byte[] extractZipFileContent(File zip, String resName) throws IOException {
        zip.mkdirs();
        try (ZipFile zipFile = new ZipFile(zip)) {
            ZipEntry entry = zipFile.getEntry(resName);
            if (entry == null) {
                throw new FileNotFoundException(resName);
            }
            InputStream stream = zipFile.getInputStream(entry);
            if (stream == null) {
                throw new FileNotFoundException(resName);
            }
            int available = stream.available();
            byte[] ret = new byte[available];
            int count = 0;
            while (count < available) {
                count += stream.read(ret, count, available - count);
            }
            return ret;
        }
    }

    public static void extractZipFileContent(File zip, String resourceFileName, String fileName) throws IOException {
        byte[] data = extractZipFileContent(zip, resourceFileName);
        new File(fileName).getParentFile().mkdirs();
        Files.write(Paths.get(fileName), data);
    }

    public static String[] getJarContents(String fileName) throws IOException {
        try (JarFile file = new JarFile(fileName)) {
            return getJarContents(file);
        }
    }

    public static String[] getJarChildren(String fileName, String path) throws IOException {
        try (JarFile file = new JarFile(fileName)) {
            return getJarChildren(file, path);
        }
    }

    public static String[] getJarContents(JarFile file) throws IOException {
        ArrayList<String> ret = new ArrayList<>();
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements()) {
            ret.add(String.valueOf((JarEntry) e.nextElement()));
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getJarChildren(JarFile file, String path) throws IOException {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        ArrayList<String> ret = new ArrayList<>();
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements()) {
            String name = e.nextElement().getName();
            if (name.startsWith(path)) {
                String entry = name.substring(path.length());
                int index = entry.indexOf("/");
                if (index > 0) {
                    entry = entry.substring(0, index);
                }
                entry = entry.trim();
                if (entry.startsWith("/")) {
                    entry = entry.substring(1);
                }
                if (!entry.isEmpty() && !ret.contains(entry)) {
                    ret.add(entry);
                }
            }
        }
        return ret.toArray(new String[0]);
    }

    //TODO: on Linux does not work on hard links
    public static String getExecutingJar(Class cls) {
        String location = getExecutingLocation(cls);
        if ((location != null) && (location.endsWith(".jar"))) {
            return location;
        }
        return null;
    }

    public static String getExecutingLocation(Class cls) {
        try {
            return Paths.get(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (Exception ex) {
            try {
                String path = cls.getProtectionDomain().getCodeSource().getLocation().toString();
                if (path.startsWith("file:/")) {
                    path = path.substring(6);
                }
                return Paths.get(path).toString();
            } catch (Exception ex1) {
                return null;
            }
        }
    }

    //Directory operations
    /**
     * / Faster than file.listFiles
     */
    public static File[] listFiles(String pathName) {
        return listFiles(new File(pathName));
    }

    public static File[] listFiles(File f) {
        return listFiles(f, (String) null);
    }

    public static File[] listFiles(String pathName, final String[] extensions) {
        return listFiles(new File(pathName), extensions);
    }

    public static File[] listFiles(File path, final String[] extensions) {
        StringBuilder filter = new StringBuilder();
        filter.append("*.{");
        filter.append(String.join(",", extensions)).append("}");
        return listFiles(path, filter.toString());
    }

    public static File[] listFiles(String pathName, final String filter) {
        return listFiles(new File(pathName), filter);
    }
    // "*.{c,h,cpp,hpp,java}"

    public static File[] listFiles(File path, DirectoryStream.Filter<Path> filter) {
        ArrayList<File> list = new ArrayList<>();
        if (filter != null) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path.toPath(), filter)) {
                for (Path f : stream) {
                    list.add(f.toFile());
                }
            } catch (IOException ex) {
            }
        }
        return list.toArray(new File[0]);
    }

    public static File[] listFiles(File path, String filter) {
        ArrayList<File> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = (filter == null)
                ? Files.newDirectoryStream(path.toPath())
                : Files.newDirectoryStream(path.toPath(), filter)) {
            for (Path f : stream) {
                list.add(f.toFile());
            }
        } catch (IOException ex) {
        }
        return list.toArray(new File[0]);
    }

    public static File[] listSubFolders(String pathName) {
        return listSubFolders(new File(pathName));
    }

    public static File[] listSubFolders(File path) {
        ArrayList<File> ret = new ArrayList<>();
        File[] children = listFiles(path);
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    ret.add(child);
                }
            }
        }
        return ret.toArray(new File[0]);
    }

    public static void orderByName(File[] files) {
        Arrays.sort(files, (a, b) -> a.compareTo(b));
    }

    public static void orderByModified(File[] files) {
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
    }

    static public void deleteRecursive(String pathName) throws IOException {
        Path path = Paths.get(pathName);

        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                for (File child : new File(pathName).listFiles()) {
                    deleteRecursive(child.getAbsolutePath());
                }
            }
            Files.delete(path);
        }
    }

    public static long getSize(File file) {
        if (!file.isDirectory()) {
            return file.length();
        }
        long length = 0;
        for (File child : file.listFiles()) {
            if (child.isFile()) {
                length += child.length();
            } else {
                length += getSize(child);
            }
        }
        return length;
    }

    /**
     * The listener interface for receiving grep events.
     */
    public interface GrepListener {

        void onMatch(File f, int line, String text);
    }

    private static void grep(File f, Pattern pattern, GrepListener listener) throws IOException {
        int lineNumber = 0;
        Matcher pm = null;
        for (String line : Files.readAllLines(f.toPath())) {
            lineNumber++;
            if (pm == null) {
                pm = pattern.matcher(line);
            } else {
                pm.reset(line);
            }
            if (pm.find()) {
                if (listener != null) {
                    listener.onMatch(f, lineNumber, line);
                } else {
                    System.out.println(f + "|" + lineNumber + "|" + line);
                }
            }
        }
    }

    private static void grep(File file, final String filter, Pattern pattern, boolean recursive, String[] ignored, GrepListener listener) throws IOException {
        for (File f : IO.listFiles(file, filter)) {
            if (!f.isDirectory()) {
                grep(f, pattern, listener);
            }
        }
        if (recursive) {
            for (File child : IO.listSubFolders(file)) {
                boolean ignore = false;
                if (ignored != null) {
                    for (String name : ignored) {
                        if (child.equals(new File(name))) {
                            ignore = true;
                        }
                    }
                }
                if (!ignore) {
                    grep(child, filter, pattern, true, ignored, listener);
                }
            }
        }
    }

    public static void grep(String fileName, String pattern, boolean caseInsensitive, boolean wholeWords, GrepListener listener) throws IOException {
        grep(fileName, null, pattern, caseInsensitive, wholeWords, false, listener);
    }

    public static void grep(String pathName, final String filter, String pattern, boolean caseInsensitive, boolean wholeWords, boolean recursive, GrepListener listener) throws IOException {
        grep(pathName, filter, pattern, caseInsensitive, wholeWords, recursive, null, listener);
    }

    public static void grep(String pathName, final String filter, String pattern, boolean caseInsensitive, boolean wholeWords, boolean recursive, String[] ignored, GrepListener listener) throws IOException {
        if (wholeWords) {
            pattern = ".*\\b" + pattern + "\\b.*";
        }
        Pattern p = Pattern.compile(pattern, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
        File f = new File(pathName);
        if (f.isDirectory()) {
            grep(f, filter, p, recursive, ignored, listener);
        } else {
            grep(f, p, listener);
        }
    }

    //Copy
    public static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = from.read(buffer)) >= 0) {
            to.write(buffer, 0, len);
        }
    }

    public static void copy(String fromFileName, String toFileName) throws IOException {
        try (FileChannel ic = new FileInputStream(fromFileName).getChannel(); FileChannel oc = new FileOutputStream(toFileName).getChannel();) {
            // Faster but may fail for big remote files
            ic.transferTo(0, ic.size(), oc);
        } catch (IOException ex) {
            try (InputStream in = new FileInputStream(new File(fromFileName)); OutputStream out = new FileOutputStream(new File(toFileName));) {
                copy(in, out);
            }
        }
        new File(toFileName).setLastModified(new File(fromFileName).lastModified());
    }

    //File operations
    public static void insert(String fileName, long offset, byte[] data) throws IOException {
        RandomAccessFile r = new RandomAccessFile(new File(fileName), "rw");
        File temp = File.createTempFile(new File(fileName).getName(), "");
        try (RandomAccessFile rtemp = new RandomAccessFile(temp, "rw");
                FileChannel sourceChannel = r.getChannel();
                FileChannel targetChannel = rtemp.getChannel()) {
            long fileSize = r.length();
            sourceChannel.transferTo(offset, (fileSize - offset), targetChannel);
            sourceChannel.truncate(offset);
            r.seek(offset);
            r.write(data);
            long newOffset = r.getFilePointer();
            targetChannel.position(0L);
            sourceChannel.transferFrom(targetChannel, newOffset, (fileSize - offset));
        } finally {
            try {
                Files.delete(temp.toPath());
            } catch (Exception ex) {
            }
        }
    }

    public static void replace(String fileName, String from, String to) throws IOException {
        Path path = Paths.get(fileName);
        String content = new String(Files.readAllBytes(path));
        content = content.replaceAll(from, to);
        Files.write(path, content.getBytes());
    }

    //File parsing
    public static String[][] parse(String fileName, String separator, String comment) throws IOException {
        ArrayList<String[]> ret = new ArrayList<>();
        int columns = 0;
        for (String line : Files.readAllLines(Paths.get(fileName))) {
            line = line.trim();
            if ((comment == null) || !line.startsWith(comment)) {
                String tokens[] = line.split(separator);
                if (ret.size() == 0) {
                    columns = tokens.length;
                }
                if (tokens.length < columns) {
                    // Make sure size of array is respected if data is missing - returning null in this case
                    tokens = Arr.append(tokens, new String[columns - tokens.length]);
                }
                ret.add(tokens);
            }
        }
        return ret.toArray(new String[0][]);
    }

    //File names
    public static String getExtension(File file) {
        if (file == null) {
            return null;
        }
        String short_name = file.getName();
        if (!short_name.contains(".")) {
            return "";
        }
        return short_name.substring(short_name.lastIndexOf('.') + 1);
    }

    public static String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        return getExtension(new File(fileName));
    }

    public static String getPrefix(File file) {
        if (file == null) {
            return null;
        }
        String short_name = file.getName();
        if (short_name.contains(".") == false) {
            return short_name;
        }
        return short_name.substring(0, short_name.lastIndexOf('.'));
    }

    public static String getPrefix(String fileName) {
        if (fileName == null) {
            return null;
        }
        return getPrefix(new File(fileName));
    }

    public static String getFolder(String fileName) {
        if (fileName == null) {
            return null;
        }
        return (new File(fileName)).getParent();
    }

    public static boolean isSamePath(String path1, String path2) {
        try {
            return new File(path1).getCanonicalPath().equals(new File(path2).getCanonicalPath());
        } catch (Exception ex) {
        }
        return false;
    }

    public static boolean isSubPath(String path, String referencePath) {
        try {
            return new File(path).getCanonicalPath().startsWith(new File(referencePath).getCanonicalPath());
        } catch (Exception ex) {
        }
        return false;
    }

    public static String getRelativePath(String fileName, String referencePath) {
        try {
            File file = new File(fileName);
            try{
                file = file.getCanonicalFile();
            } catch (Exception ex) {
            }   
            File referenceFile = new File(referencePath);
            try{
                referenceFile = referenceFile.getCanonicalFile();
            } catch (Exception ex) {
            }        
            return referenceFile.toURI().relativize(file.toURI()).getPath();
        } catch (Exception ex) {
        }
        return fileName;
    }

    //Asserting
    public static void assertExists(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
    }

    public static void assertExistsFolder(String path) throws FileNotFoundException {
        File file = new File(path);
        if ((!file.exists()) || (!file.isDirectory())) {
            throw new FileNotFoundException(path);
        }
    }

    public static void assertExistsFile(String path) throws FileNotFoundException {
        File file = new File(path);
        if ((!file.exists()) || (file.isDirectory())) {
            throw new FileNotFoundException(path);
        }
    }
}
