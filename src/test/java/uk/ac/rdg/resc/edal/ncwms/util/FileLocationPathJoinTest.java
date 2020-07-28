package uk.ac.rdg.resc.edal.ncwms.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FileLocationPathJoinTest {
    // Test parameters
    public String splitDelimiter;
    public String joinDelimiter;
    public String[] parts;
    public String expected;

    public FileLocationPathJoinTest(
        String splitDelimiter, String joinDelimiter, String[] parts, String expected
    ) {
        this.splitDelimiter = splitDelimiter;
        this.joinDelimiter = joinDelimiter;
        this.parts = parts;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
            {"/", "/", new String[]{}, ""},
            {"/", "/", new String[]{""}, ""},
            {"/", "/", new String[]{"/"}, "/"},
            {"/", "/", new String[]{"////"}, "/"},
            {"/", "/", new String[]{"alpha"}, "alpha"},
            {"/", "/", new String[]{"alpha/beta"}, "alpha/beta"},
            {"/", "/", new String[]{"alpha", "beta"}, "alpha/beta"},
            {"/", "/", new String[]{"alpha", "/beta"}, "alpha/beta"},
            {"/", "/", new String[]{"/alpha", "beta"}, "/alpha/beta"},
            {"/", "/", new String[]{"/alpha", "/beta"}, "/alpha/beta"},
            {"/", "/", new String[]{"/alpha/", "/beta/"}, "/alpha/beta"},
            {"/", "/", new String[]{"alpha", "", "beta"}, "alpha/beta"},
            {"/", "/", new String[]{"/alpha/beta/", "/gamma/delta/"}, "/alpha/beta/gamma/delta"},

            {"/", "|", new String[]{"/alpha/beta", "gamma/delta"}, "|alpha|beta|gamma|delta"},

            {"/", "/", new String[]{}, ""}
        });
    }

    @Test
    public void test() {
        assertEquals(expected,
            FileLocation.pathJoin(splitDelimiter, joinDelimiter, parts));
    }
}
