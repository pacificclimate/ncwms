package uk.ac.rdg.resc.edal.ncwms.config;

import java.sql.SQLException;

/**
 * Generic class for transformation of one type of object to another.
 * TODO: Should @param <W> be removed and replaced by a generic Object
 *  type in the two-argument `make`? Or Object[], even, to allow for an
 *  arbitrary number of auxiliary `with` arguments? Can Java spread an
 *  array as args to a method call? Apparently not :( Another goddamn
 *  reason we can't have nice things.
 *
 * @param <F> Type of "transform-from" object.
 * @param <T> Type of "transform-to" object.
 * @param <W> Type of "transform-with" (adjunct value) object.
 */
interface TypeTransformer<F, T, W> {
    /**
     * Make an object of type T from an object of type F.
     *
     * @param from object to make from
     * @return
     * @throws SQLException
     */
    abstract public T make(F from) throws SQLException;

    /**
     * Make an object of type T from an object of type F with an adjunct
     * object of type W.
     *
     * @param from object to make from
     * @param with object to make with
     * @return
     * @throws SQLException
     */
    abstract public T make(F from, W with) throws SQLException;
}
