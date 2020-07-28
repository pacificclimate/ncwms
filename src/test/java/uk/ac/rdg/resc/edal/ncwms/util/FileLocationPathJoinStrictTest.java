package uk.ac.rdg.resc.edal.ncwms.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Tests a small subset sufficient to suggest that the parameters are being passed to
 * pathJoin correctly.
 */
@RunWith(Parameterized.class)
public class FileLocationPathJoinStrictTest {
    // Test parameters
    public String delimiter;
    public String[] parts;
    public String expected;

    public FileLocationPathJoinStrictTest(
        String delimiter, String[] parts, String expected
    ) {
        this.delimiter = delimiter;
        this.parts = parts;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
            // Note how repeated delimiters are preserved.
            {"/", new String[]{"/"}, "/"},
            {"/", new String[]{"//"}, "//"},
            {"/", new String[]{"///"}, "///"},
            {"/", new String[]{"////"}, "////"},
            {"/", new String[]{"/alpha/beta/", "/gamma/delta/"}, "/alpha/beta/gamma/delta"},
            {"/", new String[]{"//alpha//beta/", "/gamma/delta/"}, "//alpha//beta/gamma/delta"},
            {"/", new String[]{"/alpha//beta//", "/gamma/delta/"}, "/alpha//beta///gamma/delta"},
            {"/", new String[]{"protocol://alpha/beta/", "/gamma/delta/"}, "protocol://alpha/beta/gamma/delta"},
            {"/", new String[]{}, ""}
        });
    }

    @Test
    public void test() {
        assertEquals(expected,
            FileLocation.pathJoinStrict(delimiter, parts));
    }
}
