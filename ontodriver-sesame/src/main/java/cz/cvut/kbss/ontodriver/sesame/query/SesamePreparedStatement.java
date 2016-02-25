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
package cz.cvut.kbss.ontodriver.sesame.query;

import cz.cvut.kbss.ontodriver.PreparedStatement;
import cz.cvut.kbss.ontodriver.ResultSet;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.sesame.connector.StatementExecutor;
import cz.cvut.kbss.ontodriver.util.StatementHolder;

import java.util.Objects;

import static cz.cvut.kbss.ontodriver.util.ErrorUtils.npxMessage;

public class SesamePreparedStatement extends SesameStatement implements PreparedStatement {

    private StatementHolder statementHolder;

    public SesamePreparedStatement(StatementExecutor executor, String statement)
            throws OntoDriverException {
        super(executor);
        this.statementHolder = new StatementHolder(statement);
        if (statementHolder.getStatement().isEmpty()) {
            throw new IllegalArgumentException("The statement string cannot be empty.");
        }
        statementHolder.analyzeStatement();
    }

    @Override
    public void setObject(String binding, Object value) throws OntoDriverException {
        ensureOpen();
        Objects.requireNonNull(value, npxMessage("value"));
        statementHolder.setParameter(binding, value.toString());
    }

    @Override
    public ResultSet executeQuery() throws OntoDriverException {
        ensureOpen();
        return executeQuery(statementHolder.assembleStatement());
    }

    @Override
    public void executeUpdate() throws OntoDriverException {
        ensureOpen();
        executeUpdate(statementHolder.assembleStatement());
    }

    @Override
    public void clearParameters() throws OntoDriverException {
        statementHolder.clearParameters();
    }
}
