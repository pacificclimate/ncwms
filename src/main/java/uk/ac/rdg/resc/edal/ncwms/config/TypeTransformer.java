package uk.ac.rdg.resc.edal.ncwms.config;

import java.sql.SQLException;

/**
 * Generic class for transformation of one type of object to another.
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
