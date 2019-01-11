/**
 * Copyright (C) 2016 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.model;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.exceptions.NoUniqueResultException;
import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.query.Parameter;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.query.QueryHolder;
import cz.cvut.kbss.jopa.sessions.ConnectionWrapper;
import cz.cvut.kbss.jopa.sessions.MetamodelProvider;
import cz.cvut.kbss.jopa.sessions.UnitOfWork;
import cz.cvut.kbss.jopa.utils.ErrorUtils;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.iteration.ResultRow;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TypedQueryImpl<X> extends AbstractQuery implements TypedQuery<X> {

    private final Class<X> resultType;
    private final MetamodelProvider metamodelProvider;

    private UnitOfWork uow;

    private Descriptor descriptor;

    public TypedQueryImpl(final QueryHolder query, final Class<X> resultType,
                          final ConnectionWrapper connection, MetamodelProvider metamodelProvider) {
        super(query, connection);
        this.resultType = Objects.requireNonNull(resultType, ErrorUtils.getNPXMessageSupplier("resultType"));
        this.metamodelProvider = Objects
                .requireNonNull(metamodelProvider, ErrorUtils.getNPXMessageSupplier("metamodelProvider"));
    }

    public void setUnitOfWork(UnitOfWork uow) {
        this.uow = uow;
    }

    @Override
    public List<X> getResultList() {
        ensureOpen();
        try {
            return getResultListImpl();
        } catch (OntoDriverException e) {
            markTransactionForRollback();
            throw queryEvaluationException(e);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
    }

    private List<X> getResultListImpl() throws OntoDriverException {

        final boolean isEntityType = metamodelProvider.isEntityType(resultType);
        final Descriptor instDescriptor = descriptor != null ? descriptor : new EntityDescriptor();
        final List<X> res = new ArrayList<>();
        executeQuery(rs -> {
            if (isEntityType) {
                loadEntityInstance(rs, instDescriptor).ifPresent(res::add);
            } else {
                res.add(loadResultValue(rs));
            }
        });
        return res;
    }

    private Optional<X> loadEntityInstance(ResultRow resultRow, Descriptor instanceDescriptor)
            throws OntoDriverException {
        if (uow == null) {
            throw new IllegalStateException("Cannot load entity instance without Unit of Work.");
        }
        assert resultRow.isBound(0);
        final URI uri = URI.create(resultRow.getString(0));
        return Optional.ofNullable(uow.readObject(resultType, uri, instanceDescriptor));
    }

    private X loadResultValue(ResultRow resultRow) {
        try {
            return resultRow.getObject(0, resultType);
        } catch (OntoDriverException e) {
            throw new OWLPersistenceException("Unable to map the query result to class " + resultType, e);
        }
    }

    @Override
    public X getSingleResult() {
        ensureOpen();
        try {
            // call it with maxResults = 2 just to see whether there are
            // multiple results
            final List<X> res = getResultListImpl();
            if (res.isEmpty()) {
                throw new NoResultException("No result found for query " + query);
            }
            if (res.size() > 1) {
                throw new NoUniqueResultException("Multiple results found for query " + query);
            }
            return res.get(0);
        } catch (OntoDriverException e) {
            markTransactionForRollback();
            throw queryEvaluationException(e);
        } catch (RuntimeException e) {
            if (exceptionCausesRollback(e)) {
                markTransactionForRollback();
            }
            throw e;
        }
    }

    @Override
    public TypedQuery<X> setMaxResults(int maxResults) {
        ensureOpen();
        if (maxResults < 0) {
            markTransactionForRollback();
            throw new IllegalArgumentException("Cannot set maximum number of results to less than 0.");
        }
        query.setMaxResults(maxResults);
        return this;
    }

    @Override
    public TypedQuery<X> setFirstResult(int startPosition) {
        ensureOpen();
        checkNumericParameter(startPosition, "first result offset");
        query.setFirstResult(startPosition);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position, Object value) {
        ensureOpen();
        try {
            query.setParameter(query.getParameter(position), value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(int position, String value, String language) {
        ensureOpen();
        try {
            query.setParameter(query.getParameter(position), value, language);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name, Object value) {
        ensureOpen();
        try {
            query.setParameter(query.getParameter(name), value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(String name, String value, String language) {
        ensureOpen();
        try {
            query.setParameter(query.getParameter(name), value, language);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public <T> TypedQuery<X> setParameter(Parameter<T> parameter, T value) {
        ensureOpen();
        try {
            query.setParameter(parameter, value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<String> parameter, String value, String language) {
        ensureOpen();
        try {
            query.setParameter(parameter, value, language);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setUntypedParameter(int position, Object value) {
        ensureOpen();
        try {
            query.setUntypedParameter(query.getParameter(position), value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setUntypedParameter(String name, Object value) {
        ensureOpen();
        try {
            query.setUntypedParameter(query.getParameter(name), value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public <T> TypedQuery<X> setUntypedParameter(Parameter<T> parameter, T value) {
        ensureOpen();
        try {
            query.setUntypedParameter(parameter, value);
        } catch (RuntimeException e) {
            markTransactionForRollback();
            throw e;
        }
        return this;
    }

    @Override
    public TypedQuery<X> setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }
}
