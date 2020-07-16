package uk.ac.rdg.resc.edal.ncwms.config;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mock a {@link java.sql.ResultSet} using Mockito
 * This class copied from https://github.com/sharfah/java-utils/blob/master/src/test/java/com/sharfah/util/sql/MockResultSet.java
 * See also https://fahdshariff.blogspot.com/2018/09/java-mocking-resultset-using-mockito.html
 * Thanks, Fahd Shariff!
 *
 * With a little tweaking for Java 8 from Java 10 code.
 *
 * Usage:
 *     ResultSet rs = MockResultSet.create(
 *            new String[] { "name", "age" }, //columns
 *            new Object[][] { // data
 *              { "Alice", 20 },
 *              { "Bob", 35 },
 *              { "Charles", 50 }
 *            });
 */
public class MockResultSet {

    private final Map<String, Integer> columnIndices;
    private final Object[][] data;
    private int rowIndex;

    private MockResultSet(final String[] columnNames, final Object[][] data) {
        this.columnIndices = IntStream.range(0, columnNames.length)
                .boxed()
                .collect(Collectors.toMap(
                        k -> columnNames[k],
                        Function.identity(),
                        (a, b) -> { throw new RuntimeException("Duplicate column " + a); },
                        LinkedHashMap::new
                ));
        this.data = data;
        this.rowIndex = -1;
    }

    private ResultSet buildMock() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);

        // mock rs.beforeFirst()
        doAnswer(invocation -> {
            rowIndex = -1;
            return null;
        }).when(rs).beforeFirst();
        // mock rs.next()
        doAnswer(invocation -> {
            rowIndex++;
            return rowIndex < data.length;
        }).when(rs).next();

        // mock rs.getString(columnName)
        doAnswer(invocation -> {
            final String columnName = invocation.getArgumentAt(0, String.class);
            final Integer columnIndex = columnIndices.get(columnName);
            return (String) data[rowIndex][columnIndex];
        }).when(rs).getString(anyString());

        // mock rs.getString(columnIndex)
        doAnswer(invocation -> {
            final Integer index = invocation.getArgumentAt(0, Integer.class);
            return (String)data[rowIndex][index - 1];
        }).when(rs).getString(anyInt());

        // mock rs.getInt(columnName)
        doAnswer(invocation -> {
            final String columnName = invocation.getArgumentAt(0, String.class);
            final Integer columnIndex = columnIndices.get(columnName);
            return (Integer) data[rowIndex][columnIndex];
        }).when(rs).getInt(anyString());

        // mock rs.getObject(columnName)
        doAnswer(invocation -> {
            final String columnName = invocation.getArgumentAt(0, String.class);
            final Integer columnIndex = columnIndices.get(columnName);
            return data[rowIndex][columnIndex];
        }).when(rs).getObject(anyString());

        // mock rs.getObject(columnIndex)
        doAnswer(invocation -> {
            final Integer index = invocation.getArgumentAt(0, Integer.class);
            return data[rowIndex][index - 1];
        }).when(rs).getObject(anyInt());

        final ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

        // mock rsmd.getColumnCount()
        doReturn(columnIndices.size()).when(rsmd).getColumnCount();

        // mock rsmd.getColumnName(int)
        doAnswer(invocation -> {
            final Integer index = invocation.getArgumentAt(0, Integer.class);
            return columnIndices.keySet().stream().skip(index - 1).findFirst().get();
        }).when(rsmd).getColumnName(anyInt());

        // mock rs.getMetaData()
        doReturn(rsmd).when(rs).getMetaData();

        return rs;
    }

    /**
     * Creates the mock ResultSet.
     *
     * @param columnNames the names of the columns
     * @param data
     * @return a mocked ResultSet
     * @throws SQLException
     */
    public static ResultSet create(final String[] columnNames, final Object[][] data) throws SQLException {
        return new MockResultSet(columnNames, data).buildMock();
    }
}
