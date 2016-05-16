/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.ontodriver.sesame.connector;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.TupleQueryResult;

import cz.cvut.kbss.ontodriver.sesame.exceptions.SesameDriverException;

class PoolingStorageConnector extends AbstractConnector {

    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static Lock READ = LOCK.readLock();
    private static Lock WRITE = LOCK.writeLock();

    private final Connector centralConnector;
    private LocalModel localModel;

    public PoolingStorageConnector(Connector centralConnector) {
        this.centralConnector = centralConnector;
        this.open = true;
    }

    @Override
    public TupleQueryResult executeSelectQuery(String query) throws SesameDriverException {
        READ.lock();
        try {
            return centralConnector.executeSelectQuery(query);
        } finally {
            READ.unlock();
        }
    }

    @Override
    public boolean executeBooleanQuery(String query) throws SesameDriverException {
        READ.lock();
        try {
            return centralConnector.executeBooleanQuery(query);
        } finally {
            READ.unlock();
        }
    }

    @Override
    public void executeUpdate(String query) throws SesameDriverException {
        WRITE.lock();
        try {
            centralConnector.executeUpdate(query);
        } finally {
            WRITE.unlock();
        }
    }

    @Override
    public List<Resource> getContexts() throws SesameDriverException {
        READ.lock();
        try {
            return centralConnector.getContexts();
        } finally {
            READ.unlock();
        }
    }

    @Override
    public ValueFactory getValueFactory() {
        // We don't need to lock the central connector, as getting the value
        // factory does not require communication with the repository
        return centralConnector.getValueFactory();
    }

    @Override
    public void begin() throws SesameDriverException {
        super.begin();
        this.localModel = new LocalModel();
    }

    @Override
    public void commit() throws SesameDriverException {
        transaction.commit();
        WRITE.lock();
        try {
            centralConnector.begin();
            centralConnector.removeStatements(localModel.getRemovedStatements());
            centralConnector.addStatements(localModel.getAddedStatements());
            centralConnector.commit();
            transaction.afterCommit();
        } catch (SesameDriverException e) {
            transaction.rollback();
            centralConnector.rollback();
            transaction.afterRollback();
            throw e;
        } finally {
            WRITE.unlock();
            this.localModel = null;
        }
    }

    @Override
    public void rollback() {
        transaction.rollback();
        this.localModel = null;
        transaction.afterRollback();
    }

    @Override
    public void addStatements(Collection<Statement> statements) {
        verifyTransactionActive();
        assert statements != null;
        localModel.addStatements(statements);
    }

    @Override
    public void removeStatements(Collection<Statement> statements) throws SesameDriverException {
        verifyTransactionActive();
        assert statements != null;
        localModel.removeStatements(statements);
    }

    @Override
    public Collection<Statement> findStatements(Resource subject, URI property, Value value,
                                                boolean includeInferred, URI... contexts) throws SesameDriverException {
        verifyTransactionActive();
        READ.lock();
        try {
            final Collection<Statement> statements = centralConnector.findStatements(subject,
                    property, value, includeInferred, contexts);
            localModel.enhanceStatements(statements, subject, property, value, contexts);
            return statements;
        } catch (SesameDriverException e) {
            centralConnector.rollback();
            throw e;
        } finally {
            READ.unlock();
        }
    }

    @Override
    public <T> T unwrap(Class<T> cls) throws OntoDriverException {
        if (cls.isAssignableFrom(this.getClass())) {
            return cls.cast(this);
        }
        return centralConnector.unwrap(cls);
    }
}
