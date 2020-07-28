package uk.ac.rdg.resc.edal.ncwms.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileLocation {
    /**
     * Join parts of a file or url path into a single valid path string.
     * The delimiter {splitDelimiter} separates the parts strings into elementary parts.
     * Elementary parts are joined into a result path with {joinDelimiter}.
     * This allows for several options, including strict splitting and/or transformation
     * of delimiters (e.g., from "\" to "/").
     * Repeated delimiters, between and within parts, are normalized to single delimiters.
     * Empty parts are filtered out. Thus leading and trailing delimiters in successive
     * parts do not cause problems.
     * The leading delimiter in a path (if any) is kept. Trailing delimiters are removed.
     *
     * @param splitDelimiter a regex defining the delimiter in input path parts
     * @param joinDelimiter a string defining the delimiter in the output path
     * @param parts
     * @return
     */
    public static String pathJoin(String splitDelimiter, String joinDelimiter, String... parts) {
        if (parts.length == 0) {
            return "";
        }
        final String prefix = parts[0].matches("^" + splitDelimiter + ".*") ? joinDelimiter : "";
        return prefix + Arrays.stream(parts)
            .flatMap(s1 -> Stream.of(s1.split(splitDelimiter)))
            .filter(s1 -> !(s1 == null || s1.equals("")))
            .reduce((r, s) -> r + joinDelimiter + s)
            .orElse("");
    }

    /**
     * Join parts of a file or url path into a single valid path string, using the same
     * delimiter both to split and join path components.
     * WARNING: This will produce unwanted results for URLs (e.g., "http://example.com/some/path"),
     * because it treats the protocol delimiter incorrectly.
     *
     *
     * @param delimiter
     * @param parts
     * @return
     */
    public static String pathJoinLoose(String delimiter, String... parts) {
        return pathJoin(delimiter, delimiter, parts);
    }

    /**
     * Join parts of a file or url path into a single valid path string, using a strict
     * split delimiter regex derived from the limiter that does not split on repeats
     * of the delimiter. This will produce the desired results for typical URLs such as
     * "http://example.com/some/path".
     *
     *
     * @param delimiter
     * @param parts
     * @return
     */
    public static String pathJoinStrict(String delimiter, String... parts) {
        final String strictDelimiter = String.format(
            "(?<!%s)%s(?!%s)", delimiter, delimiter, delimiter);
        return pathJoin(strictDelimiter, delimiter, parts);
    }
}
