/**
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.persistence.common.store;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnull;

import org.opendaylight.persistence.DataStore;
import org.opendaylight.persistence.PersistenceException;
import org.opendaylight.persistence.common.query.PagedFindQuery;
import org.opendaylight.persistence.dao.MarkPageDao;
import org.opendaylight.persistence.store.MarkPageObjectStore;
import org.opendaylight.persistence.util.common.type.Sort;
import org.opendaylight.persistence.util.common.type.page.MarkPage;
import org.opendaylight.persistence.util.common.type.page.MarkPageRequest;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * {@link MarkPageObjectStore} implementation. Separating {@link MarkPageObjectStore} from this
 * implementation allows applications to define custom persistence functionality by extending
 * {@link MarkPageObjectStore} and adding custom methods. Then the final implementation of the
 * persistence service would extend this class and implement the custom methods.
 * 
 * @param <I> type of the identifiable object's id. This type should be immutable and it is critical
 *            it implements {@link Object#equals(Object)} and {@link Object#hashCode()} correctly.
 * @param <T> type of the identifiable object (object to store in the data store)
 * @param <F> type of the associated filter. A DAO is responsible for translating this filter to any
 *            mechanism understood by the underlying data store or database technology. For example,
 *            predicates in JPA-based implementations, or WHERE clauses in SQL-base implementations.
 * @param <S> type of the associated sort attribute or sort key used to construct sort
 *            specifications. A DAO is responsible for translating this specification to any
 *            mechanism understood by the underlying data store or database technology. For example,
 *            ORDER BY clauses in SQL-based implementations.
 * @param <C> type of the context provided to queries to enable execution
 * @author Fabiel Zuniga
 * @author Nachiket Abhyankar
 */
public class DaoBasedMarkPageObjectStore<I extends Serializable, T extends Identifiable<I>, F, S, C> extends
        DaoBasedObjectStore<I, T, F, S, C> implements MarkPageObjectStore<I, T, F, S> {

    private final MarkPageDao<I, T, F, S, C> dao;

    /**
     * Creates a persistence service.
     * 
     * @param dataStore data store service
     * @param dao the DAO to delegate persistence operations to
     */
    public DaoBasedMarkPageObjectStore(@Nonnull DataStore<C> dataStore, @Nonnull MarkPageDao<I, T, F, S, C> dao) {
        super(dataStore, dao);
        this.dao = dao;
    }

    @Override
    public MarkPage<T> find(F filter, List<Sort<S>> sort, MarkPageRequest<T> pageRequest) throws PersistenceException {
        return getDataStore().execute(PagedFindQuery.createQuery(filter, sort, pageRequest, this.dao));
    }
}
