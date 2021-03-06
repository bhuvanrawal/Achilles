/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.query.dsl.update;


import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;

import info.archinnov.achilles.internals.metamodel.AbstractEntityProperty;
import info.archinnov.achilles.internals.options.Options;
import info.archinnov.achilles.internals.query.LWTHelper;
import info.archinnov.achilles.internals.query.StatementProvider;
import info.archinnov.achilles.internals.query.action.MutationAction;
import info.archinnov.achilles.internals.query.options.AbstractOptionsForUpdateOrDelete;
import info.archinnov.achilles.internals.runtime.RuntimeEngine;
import info.archinnov.achilles.internals.statements.BoundStatementWrapper;
import info.archinnov.achilles.internals.statements.OperationType;
import info.archinnov.achilles.internals.statements.StatementWrapper;


public abstract class AbstractUpdateEnd<T extends AbstractUpdateEnd<T, ENTITY>, ENTITY>
        extends AbstractOptionsForUpdateOrDelete<T>
        implements MutationAction, StatementProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUpdateEnd.class);

    protected final Update.Where where;

    protected AbstractUpdateEnd(Update.Where where) {
        this.where = where;
    }

    protected abstract List<Object> getBoundValuesInternal();

    protected abstract List<Object> getEncodedValuesInternal();

    protected abstract AbstractEntityProperty<ENTITY> getMetaInternal();

    protected abstract Class<ENTITY> getEntityClass();

    protected abstract RuntimeEngine getRte();

    /**
     *  UPDATE ... IF EXISTS
     */
    public T ifExists(boolean ifExists) {
        if (ifExists) {
            where.ifExists();
        }
        return getThis();
    }

    /**
     *  UPDATE ... IF EXISTS
     */
    public T ifExists() {
        where.ifExists();
        return getThis();
    }

    public T usingTimeToLive(int timeToLive) {
        where.using(QueryBuilder.timestamp(QueryBuilder.bindMarker("ttl")));
        getBoundValuesInternal().add(timeToLive);
        getEncodedValuesInternal().add(timeToLive);
        return getThis();
    }

    public CompletableFuture<ExecutionInfo> executeAsyncWithStats() {

        final RuntimeEngine rte = getRte();
        final Options options = getOptions();

        final StatementWrapper statementWrapper = getInternalBoundStatementWrapper();
        final String queryString = statementWrapper.getBoundStatement().preparedStatement().getQueryString();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Execute update async with execution info : %s", queryString));
        }

        CompletableFuture<ResultSet> futureRS = rte.execute(statementWrapper);

        return futureRS
                .thenApply(options::resultSetAsyncListener)
                .thenApply(statementWrapper::logReturnResults)
                .thenApply(statementWrapper::logTrace)
                .thenApply(x -> LWTHelper.triggerLWTListeners(lwtResultListeners, x, queryString))
                .thenApply(x -> x.getExecutionInfo());
    }

    @Override
    public BoundStatement generateAndGetBoundStatement() {
        return getInternalBoundStatementWrapper().getBoundStatement();
    }


    @Override
    public String getStatementAsString() {
        return where.getQueryString();
    }

    @Override
    public List<Object> getBoundValues() {
        return getBoundValuesInternal();
    }

    @Override
    public List<Object> getEncodedBoundValues() {
        return getEncodedValuesInternal();
    }

    private StatementWrapper getInternalBoundStatementWrapper() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Get bound statement wrapper"));
        }

        final RuntimeEngine rte = getRte();
        final AbstractEntityProperty<ENTITY> meta = getMetaInternal();
        final Options options = getOptions();
        final PreparedStatement ps = rte.prepareDynamicQuery(where);

        final StatementWrapper statementWrapper = new BoundStatementWrapper(OperationType.UPDATE,
                meta, ps,
                getBoundValuesInternal().toArray(),
                getEncodedValuesInternal().toArray());

        statementWrapper.applyOptions(options);
        return statementWrapper;
    }
}
