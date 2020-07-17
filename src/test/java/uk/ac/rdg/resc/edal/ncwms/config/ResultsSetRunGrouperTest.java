package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ResultsSetRunGrouperTest {
    // Fields used as parameters of the test. These must be public.
    public ResultSet x;
    public ResultSet y;
    public ArrayList<A> expected;

    // Constructor for test parameterization
    public ResultsSetRunGrouperTest(
            ResultSet x, ResultSet y, ArrayList<A> expected
    ) {
        this.x = x;
        this.y = y;
        this.expected = expected;
    }

    // Objects for making test parameters
    private static ResultSet xs0 = null;
    private static ResultSet xs1 = null;
    private static ResultSet xs2 = null;
    private static ResultSet xs3 = null;
    private static ResultSet ys0 = null;
    private static ResultSet ys1 = null;
    private static ResultSet ys2 = null;
    private static ResultSet ys3 = null;

    static {
        try {
            xs0 = MockResultSet.create(
                new String[]{"id"},
                new Object[][]{
                }
            );
            xs1 = MockResultSet.create(
                new String[]{"id"},
                new Object[][]{
                    {"alpha"}
                }
            );
            xs2 = MockResultSet.create(
                new String[]{"id"},
                new Object[][]{
                    {"alpha"},
                    {"beta"},
                    {"gamma"}
                }
            );
            xs3 = MockResultSet.create(
                new String[]{"id"},
                new Object[][]{
                    {"beta"}
                }
            );

            ys0 = MockResultSet.create(
                new String[]{"id", "value"},
                new Object[][]{
                }
            );
            ys1 = MockResultSet.create(
                new String[]{"id", "value"},
                new Object[][]{
                        {"alpha", 1}
                }
            );
            ys2 = MockResultSet.create(
                new String[]{"id", "value"},
                new Object[][]{
                        {"alpha", 1},
                        {"alpha", 2},
                        {"beta", 3},
                        {"gamma", 4},
                        {"gamma", 5},
                        {"gamma", 6}
                }
            );
            ys3 = MockResultSet.create(
                new String[]{"id", "value"},
                new Object[][]{
                    {"beta", 1},
                    {"beta", 2},
                }
            );

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    // `as_i_j` is for the combination `x_i`, `y_j`

    private static ArrayList<A> as_0_0 = new ArrayList<>();
    private static ArrayList<A> as_0_1 = new ArrayList<>();
    private static ArrayList<A> as_0_2 = new ArrayList<>();

    private static ArrayList<A> as_1_0 = new ArrayList<>(Arrays.asList(
        new A("alpha", new ArrayList<>())
    ));

    private static ArrayList<A> as_1_1 = new ArrayList<>(Arrays.asList(
            new A("alpha", new ArrayList<>(Arrays.asList(new B(1))))
    ));

    private static ArrayList<A> as_1_2 = new ArrayList<>(Arrays.asList(
            new A("alpha", new ArrayList<>(Arrays.asList(
                new B(1),
                new B(2)
            )))
    ));

    private static ArrayList<A> as_2_0 = new ArrayList<>(Arrays.asList(
            new A("alpha", new ArrayList<>()),
            new A("beta", new ArrayList<>()),
            new A("gamma", new ArrayList<>())
    ));

    private static ArrayList<A> as_2_1 = new ArrayList<>(Arrays.asList(
            new A("alpha", new ArrayList<>(Arrays.asList(new B(1)))),
            new A("beta", new ArrayList<>()),
            new A("gamma", new ArrayList<>())
    ));

    private static ArrayList<A> as_2_2 = new ArrayList<>(Arrays.asList(
            new A("alpha", new ArrayList<>(Arrays.asList(
                new B(1),
                new B(2)
            ))),
            new A("beta", new ArrayList<>(Arrays.asList(
                new B(3)
            ))),
            new A("gamma", new ArrayList<>(Arrays.asList(
                new B(4),
                new B(5),
                new B(6)
            )))
    ));

    private static ArrayList<A> as_2_3 = new ArrayList<>(Arrays.asList(
        new A("alpha", new ArrayList<>()),
        new A("beta", new ArrayList<>(Arrays.asList(
            new B(1),
            new B(2)
        ))),
        new A("gamma", new ArrayList<>())
    ));

    private static ArrayList<A> as_3_2 = new ArrayList<>(Arrays.asList(
        new A("beta", new ArrayList<>(Arrays.asList(
            new B(3)
        )))
    ));

    // Specify test parameters
    @Parameterized.Parameters
    public static Collection<Object[]> input() throws SQLException {
        return Arrays.asList(
            new Object[][] {
                // Edge cases
                {xs0, ys0, as_0_0},
                {xs0, ys1, as_0_1},
                {xs0, ys2, as_0_2},
                {xs1, ys0, as_1_0},
                {xs1, ys1, as_1_1},
                {xs1, ys2, as_1_2},
                {xs2, ys0, as_2_0},
                {xs2, ys1, as_2_1},

                // Simple full case
                {xs2, ys2, as_2_2},

                // More x's than y's, both ends
                {xs2, ys3, as_2_3},

                // More y's than x's, both ends
                {xs3, ys2, as_3_2},
            }
        );
    }

    @Test
    public void test() throws SQLException {
        ResultSetToA resultSetToA = new ResultSetToA();
        ResultSetToB resultSetToB = new ResultSetToB();
        ResultSetRunGrouper<A, B> grouper =
                new ResultSetRunGrouper<>("id", resultSetToA, resultSetToB);
        x.beforeFirst();
        y.beforeFirst();
        ArrayList<A> result = grouper.group(x, y);
        // This is another fucking reason we can't have nice things. Oh for zip.
        assertEquals(expected.size(), result.size());
        for (int i = 0; i < expected.size(); i += 1) {
            A e = expected.get(i);
            A r = result.get(i);
            assertEquals(e.id, r.id);
            assertEquals(e.bs.size(), r.bs.size());
            for (int j = 0; j < e.bs.size(); j += 1) {
                B eb = e.bs.get(j);
                B rb = r.bs.get(j);
                assertEquals(eb.value, rb.value);
            }
        }
    }

    // Classes for constructing the tests
    private static class A {
        String id;
        ArrayList<B> bs;

        public A(String id, ArrayList<B> bs) {
            this.id = id;
            this.bs = bs;
        }

        @Override
        public String toString() {
            return "A{id='" + id + '\'' + ", bs=" + bs + '}';
        }
    }

    private static class B {
        int value;

        public B(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "B{value=" + value + '}';
        }
    }

    private static class ResultSetToA implements TypeTransformer<ResultSet, A, ArrayList<B>> {
        @Override
        public A make(ResultSet from) throws SQLException {
            throw new NotImplementedException();
        }

        @Override
        public A make(ResultSet from, ArrayList<B> bs) throws SQLException {
            return new A(from.getString("id"), bs);
        }
    }

    private static class ResultSetToB implements TypeTransformer<ResultSet, B, Object> {
        @Override
        public B make(ResultSet from) throws SQLException {
            return new B(from.getInt("value"));
        }

        @Override
        public B make(ResultSet from, Object with) throws SQLException {
            throw new NotImplementedException();
        }
    }
}
