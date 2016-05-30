package jacz.util.files;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * tests
 */
public class FileUtilExtendedTest {

    private static final Map<String, String> dirMap;

    static {
        dirMap = new HashMap<>();
        dirMap.put("a", "A");
        dirMap.put("b", "B");
        dirMap.put("c", "C");
    }

    private static Path oldPath = Paths.get("a", "x", "c", "y.txt");
    private static Path newPath = Paths.get("A", "x", "C", "y.txt");

    @Test
    public void testIsEmpty() throws IOException {
        Path tests = Paths.get("./etc/tests");
        Files.createDirectories(tests);
        FileUtilExtended.cleanDirectory(tests);

        Assert.assertTrue(FileUtilExtended.isEmpty(tests));
        Files.createFile(tests.resolve("a.txt"));
        Assert.assertFalse(FileUtilExtended.isEmpty(tests));
        Files.delete(tests.resolve("a.txt"));
        Assert.assertTrue(FileUtilExtended.isEmpty(tests));
    }

    @Test
    public void testCleanDirectory() throws IOException {
        Path tests = Paths.get("./etc/tests");
        Files.createDirectories(tests);
        FileUtilExtended.cleanDirectory(tests);

        Assert.assertTrue(FileUtilExtended.isEmpty(tests));

        Files.createFile(tests.resolve("a.txt"));
        Files.createFile(tests.resolve("b.txt"));
        Files.createFile(tests.resolve("c.txt"));
        Files.createDirectory(tests.resolve("d"));
        Files.createFile(tests.resolve("d").resolve("a.txt"));
        Files.createFile(tests.resolve("d").resolve("b.txt"));
        Files.createDirectory(tests.resolve("e"));
        Files.createFile(tests.resolve("d").resolve("c.txt"));
        Files.createFile(tests.resolve("d").resolve("d.txt"));

        Assert.assertFalse(FileUtilExtended.isEmpty(tests));

        FileUtilExtended.cleanDirectory(tests);

        Assert.assertTrue(FileUtilExtended.isEmpty(tests));
    }

    @Test
    public void testDeleteDirectoryAndContents() throws Exception {
        Path tests = Paths.get("./etc/tests");
        Files.createDirectories(tests);
        FileUtilExtended.cleanDirectory(tests);

        Files.createFile(tests.resolve("a.txt"));
        Files.createFile(tests.resolve("b.txt"));
        Files.createFile(tests.resolve("c.txt"));
        Files.createDirectory(tests.resolve("d"));
        Files.createFile(tests.resolve("d").resolve("a.txt"));
        Files.createFile(tests.resolve("d").resolve("b.txt"));
        Files.createDirectory(tests.resolve("e"));
        Files.createFile(tests.resolve("d").resolve("c.txt"));
        Files.createFile(tests.resolve("d").resolve("d.txt"));

        FileUtilExtended.deleteDirectoryAndContents("./etc/tests");
        Assert.assertTrue(Files.notExists(tests));
    }

    @Test
    public void testTransformRoute() throws Exception {
        String newRoute = FileUtilExtended.transformRoute(oldPath.toString(), dirMap);
        Assert.assertEquals(newPath.toString(), newRoute);

        newRoute = FileUtilExtended.transformRoute(oldPath.toAbsolutePath().toString(), dirMap);
        Assert.assertEquals(newPath.toAbsolutePath().toString(), newRoute);
    }

    @Test
    public void testTransformRoute1() throws Exception {
        Assert.assertEquals(newPath, FileUtilExtended.transformRoute(oldPath, dirMap));
        Assert.assertEquals(newPath.toAbsolutePath(), FileUtilExtended.transformRoute(oldPath.toAbsolutePath(), dirMap));
    }
}