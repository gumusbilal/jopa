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
package cz.cvut.kbss.jopa.sessions;

import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

public interface UnitOfWork extends Session {

    /**
     * Clears this Unit of Work.
     */
    void clear();

    /**
     * Commit changes to the ontology.
     */
    void commit();

    /**
     * Rolls back changes done since last commit.
     *
     * @see #commit()
     */
    void rollback();

    /**
     * Returns true if the specified entity is managed in the current
     * persistence context. This method is used by the EntityManager's contains
     * method.
     *
     * @param entity Object
     * @return {@literal true} if entity is managed, {@literal false} otherwise
     */
    boolean contains(Object entity);

    /**
     * Is this Unit of Work active?
     *
     * @return boolean
     */
    boolean isActive();

    /**
     * Returns true if this {@code UnitOfWork} represents persistence context of
     * a currently running transaction.
     *
     * @return True if in an active transaction
     */
    boolean isInTransaction();

    /**
     * Return true if the given entity is managed. This means it is either in
     * the shared session cache or it is a new object ready for persist.
     *
     * @param entity Object
     * @return boolean
     */
    boolean isObjectManaged(Object entity);

    /**
     * Checks whether context specified by {@code context} is consistent. </p>
     * <p>
     * Can be {@code null}, indicating that consistency of the whole repository
     * should be checked.
     *
     * @param context Context URI
     * @return {@code true} if the context is consistent, {@code false}
     * otherwise
     * @throws OWLPersistenceException If an ontology access error occurs
     */
    boolean isConsistent(URI context);

    /**
     * Loads value of the specified field for the specified entity. </p>
     * <p>
     * The value is set on the entity.
     *
     * @param entity The entity to load field for
     * @param field  The field to load
     * @throws NullPointerException    If {@code entity} or {@code field} is {@code null}
     * @throws OWLPersistenceException If an error occurs, this may be e. g. that the field is not
     *                                 present on the entity, an ontology access error occurred etc.
     */
    <T> void loadEntityField(T entity, Field field);

    /**
     * Merges the state of the given entity into the current persistence
     * context. </p>
     * <p>
     * The {@code descriptor} argument specified the ontology contexts into
     * which the detached entity and its fields belong and should be merged.
     *
     * @param entity     entity instance
     * @param descriptor Entity descriptor, specifies repository context
     * @return the managed instance that the state was merged to
     * @throws NullPointerException If {@code entity} or {@code repository} is {@code null}
     */
    <T> T mergeDetached(T entity, Descriptor descriptor);

    /**
     * Retrieves object with the specified primary key. </p>
     * <p>
     * The object as well as its fields are looked for in contexts specified by
     * the descriptor. The result is then cast to the specified type.
     *
     * @param cls        The type of the returned object
     * @param primaryKey Primary key
     * @param descriptor Entity descriptor
     * @return The retrieved object or {@code null} if there is no object with
     * the specified primary key in the specified repository
     * @throws NullPointerException    If {@code cls}, {@code primaryKey} or {@code repository} is
     *                                 {@code null}
     * @throws OWLPersistenceException If {@code repository} is not valid or if an error during
     *                                 object loading occurs
     */
    <T> T readObject(Class<T> cls, Object primaryKey, Descriptor descriptor);

    /**
     * Register an existing object in this Unit of Work. The passed object comes
     * usually from the parent session cache. This method creates a working
     * clone of this object and puts the given object into this Unit of Work
     * cache.
     *
     * @param object     Object
     * @param descriptor Entity descriptor identifying repository contexts
     * @return Object Returns clone of the registered object
     */
    Object registerExistingObject(Object object, Descriptor descriptor);

    /**
     * Registers the specified new object in this Unit of Work. </p>
     * <p>
     * The object will be persisted into the context specified by
     * {@code descriptor}.
     *
     * @param object     The object to register
     * @param descriptor Entity descriptor
     * @throws NullPointerException    If {@code entity} or {@code context} is {@code null}
     * @throws OWLPersistenceException If {@code context} is not a valid context URI or if an error
     *                                 during registration occurs
     */
    void registerNewObject(Object object, Descriptor descriptor);

    /**
     * Remove the given object. Calling this method causes the entity to be
     * removed from the shared cache and a delete query is initiated on the
     * ontology.
     *
     * @param object Object
     */
    void removeObject(Object object);

    /**
     * Release the current unit of work. Calling this method disregards any
     * changes made to clones.
     */
    void release();

    /**
     * Reverts any changes to the given object.</p>
     * <p>
     * This method modifies the specified object. The object has to be managed
     * by this persistence context.
     *
     * @param object The object to revert
     */
    <T> void revertObject(T object);

    /**
     * This method returns true, if the UnitOfWork should be released after the
     * commit call. This is done for inferred attributes, which cause the whole
     * session cache to be invalidated.
     *
     * @return True if the UnitOfWork should be released after commit.
     */
    boolean shouldReleaseAfterCommit();

    /**
     * Writes any uncommitted changes into the ontology. This method may be
     * useful when flushing entity manager or closing sessions, because we don't
     * want to let the changes to get lost.
     */
    void writeUncommittedChanges();

    /**
     * Gets repository contexts available to this session.
     *
     * @return Unmodifiable list of context URIs
     */
    List<URI> getContexts();

    /**
     * Sets the transactional ontology as the one used for SPARQL query
     * processing.
     */
    void setUseTransactionalOntologyForQueryProcessing();

    /**
     * Returns true if the transactional ontology is set as the one processing
     * SPARQL queries.
     *
     * @return boolean
     */
    boolean useTransactionalOntologyForQueryProcessing();

    /**
     * Sets the backup (central) ontology as the one used for SPARQL query
     * processing.
     */
    void setUseBackupOntologyForQueryProcessing();

    /**
     * Returns true if the backup (central) ontology is set as the one
     * processing SPARQL queries.
     *
     * @return boolean
     */
    boolean useBackupOntologyForQueryProcessing();
}
