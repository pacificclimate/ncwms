package uk.ac.rdg.resc.edal.ncwms.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * This class provides a method that groups a pair of {@link ResultSet}s
 * x and y that share a common id column, which is a String. The basic idea
 * is that each x has a list of zero to many y's associated with it, and
 * the result should create objects representing x's and y's, where the
 * x-representing objects contain a list of associated y-representing objects.
 *
 * Why the separate types for representing x's and y's? Because a
 * {@link ResultSet} represents relational database rows fairly directly,
 * and furthermore is a cursor object, so row cannot be stored
 * except by extracting its contents  to another object. Therefore
 * {@link ResultSet}s x and y are converted to objects of type A and B,
 * respectively. Type A includes a list of objects of type B. In some
 * sense this is a tiny ORM.
 *
 * We expect:
 *
 * - In {@link ResultSet} x, `id` must be unique
 * - In {@link ResultSet} y, `id` may occur any number of times
 *
 * To simplify and make this algorithm much more efficient, we require the
 * following:
 *
 * - {@link ResultSet}s x and y must both be sorted in ascending order
 *   of id.
 *
 * Why define this fairly abstract class when it is likely to be instantiated in only
 * one way?
 * 1. Clarity. The core algorithm is tight, but concrete details can obscure that.
 * 2. Testability.
 *
 * Why not define this class with ResultSet being a generic type too?
 * - Because it is focused in particular on processing {@link ResultSet}s.
 *
 * @param <A>
 * @param <B>
 */
public class ResultSetRunGrouper<A, B> {
    private String idColumnLabel;
    private TypeTransformer<ResultSet, A, ArrayList<B>> aMaker;
    private TypeTransformer<ResultSet, B, Object> bMaker;

    public ResultSetRunGrouper(
            String idColumnLabel,
            TypeTransformer<ResultSet, A, ArrayList<B>> aMaker,
            TypeTransformer<ResultSet, B, Object> bMaker
    ) {
        this.idColumnLabel = idColumnLabel;
        this.aMaker = aMaker;
        this.bMaker = bMaker;
    }

    public ArrayList<A> group(ResultSet x, ResultSet y) throws SQLException {
        ArrayList<A> as = new ArrayList<>();
        ArrayList<B> bs = new ArrayList<>();

        // Discard any `y`s whose id comes before the first `x`'s id.
        // These by definition cannot be part of the result set.
        // Probably never occurs.
        String xId = nextId(x);
        String yId = nextId(y);
        while (xId != null && yId != null && yId.compareTo(xId) < 0) {
            yId = nextId(y);;
        }

        // Advance through both result sets, accumulating runs of `y`s
        // with the same id as the current `x`. When a run is complete
        // (empty runs, i.e., `x`s with no matching `y`s, are allowed),
        // a new object of type A, made from the current `x` and the run of
        // `y`s, is added to the list `as`.
        while (xId != null || yId != null) {
            if (xId != null && yId != null && xId.equals(yId)) {
                // Accumulating a run of `y`s matching current `x` id.
                bs.add(bMaker.make(y));
                yId = nextId(y);
            } else if (xId != null) {
                // Run of `y`s matching current `x` id is complete.
                as.add(aMaker.make(x, bs));
                xId = nextId(x);
                // Create a new empty `b`s list, ready to accumulate the next run of `y`s.
                bs = new ArrayList<>();
            } else {
                // If we got here, x's have been exhausted.
                break;
            }
        }

        return as;
    }

    /**
     * Advance a ResultSet and return the id of the result advanced to.
     * Return null if no more items remain in s.
     *
     * BEWARE: This both returns a value and performs a side effect. Icky.
     *
     * @param s The ResultSet
     * @return Next id or null if s is done
     * @throws SQLException
     */
    private String nextId(ResultSet s) throws SQLException {
        if (s.next()) {
            return s.getString(idColumnLabel);
        }
        return null;
    }
}
