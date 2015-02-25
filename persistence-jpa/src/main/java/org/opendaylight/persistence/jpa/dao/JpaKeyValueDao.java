/**
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.persistence.jpa.dao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.opendaylight.persistence.PersistenceException;
import org.opendaylight.persistence.dao.BaseDao;
import org.opendaylight.persistence.dao.KeyValueDao;
import org.opendaylight.persistence.dao.UpdateStrategy;
import org.opendaylight.persistence.jpa.JpaContext;
import org.opendaylight.persistence.util.common.Converter;
import org.opendaylight.persistence.util.common.converter.CollectionConverter;
import org.opendaylight.yangtools.concepts.Identifiable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * JPA {@link BaseDao}.
 * <p>
 * This class must remain state-less so it is thread safe.
 * <p>
 * A DAO should be used by {@link Query queries}.
 * <p>
 * This implementation follows the data transfer pattern. Data Transfer Object Pattern: Provides the ability for the
 * transport objects to carry the data between application layers (DTO). A DTO (Here, {@link Identifiable}) should be a
 * type-safe POJO with object value types for attributes when appropriated. The DAO internally use an entity (an object
 * annotated with {@link javax.persistence.Entity}) where attributes are directly translated to the database native data
 * types. In this way the internals of the entity are not exposed (For example, changes in column names should not
 * affect the DTO).
 * <p>
 * The big advantage of separating the two objects (DTO and Storable) is that restrictions of the underlying technology
 * are hidden from the consumer. Examples of JPA restrictions:
 * <ul>
 * <li>Entities must provide a default constructor. This is not good for model objects. Constructors are defines to make
 * sure an object is in a valid state after creation. This restriction defeats the purpose of constructors or factory
 * methods.</li>
 * </ul>
 * <p>
 * <b>Some notes on primary keys:</b>
 * <p>
 * A candidate key is a column or a set of columns that could be used to identify a particular row in a table. To become
 * a primary key, a candidate key must satisfy the following properties:
 * <ul>
 * <li><b>Required:</b> Its value (for any column of the candidate key) is never null.</li>
 * <li><b>Unique:</b> Each row has a unique value.</li>
 * <li><b>Constant:</b> The value of a particular row never changes.</li>
 * </ul>
 * If a table has only one identifying attribute, it's, by definition, the primary key. However, several columns or
 * combinations of columns may satisfy the properties for a particular table; you choose between candidate keys to
 * decide the best primary key for the table. Candidate keys not chosen as the primary key should be declared as unique
 * keys in the table.
 * <p>
 * A natural key is a key with business meaning: an attribute or combination of attributes that is unique by value of
 * its business semantic (U.S. Social Security Number for example). If a candidate key attribute has meaning outside the
 * database context, it is a natural key, whether or not it is automatically generated.
 * <p>
 * Experience has shown that natural keys almost always cause problems in the long run. A good primary key must be
 * unique, constant and required. Few entity attributes satisfy the requirements, and some that do can't be efficiently
 * indexed by SQL databases (although this is an implementation detail and shouldn't be the primary motivation for or
 * against a particular key). Natural keys can only be found only by combining several columns in a composite natural
 * key. These composite keys, although certainly appropriate for some relations (like a link table in a many-to-many
 * relationship), usually make maintenance, ad-hoc queries, and schema evolution much more difficult.
 * <p>
 * For these reasons it is recommended to use synthetic identifiers, also called surrogate keys. Surrogate keys have not
 * business meaning - they are unique values generated by the database or application. Application users ideally don't
 * see or refer to these key values; they're part of the system internals. Introducing surrogate key column is also
 * appropriate in a common situation: If there are no candidate keys, a table is by definition not a relation as defined
 * by the relational model -it permits duplicated rows- and so you have to add a surrogate key column.
 * <p>
 * <b>Some notes on composite keys:</b>
 * <p>
 * If an entity defines a composite key, it is more elegant to define a separate composite identifier class that
 * declares just the key properties, and this is actually forced in this class by the type-parameter {@literal I} (The
 * entity's id is typed with the generic type).
 * <p>
 * Strategies for handling composite keys:
 * <p>
 * <ol>
 * <li>Encapsulate the identifier properties in a separate class and mark it {@code @Embeddable}, like a regular
 * component. Include a property of this component in your entity class, and map it with {@code @Id} for an application
 * assigned strategy.</li>
 * <li>Encapsulate the identifier properties in a separate class without any annotations on it. Include a property of
 * this type in your entity class, and map it with {@code @EmbeddedId}.</li>
 * <li>Encapsulate the identifier properties in a separate class. Now duplicate all the identifier properties in the
 * entity class. Then, annotate the entity class with {@code @IdClass} and specify the name of your encapsulated
 * identifier class.</li>
 * </ol>
 * <p>
 * For example, suppose you have an {@code User} entity where the primary key consists of a USERNAME and
 * DEPARTMENT_NUMBER. Then an {@code UserId} class could be created to keep the primary key attributes:
 * 
 * <pre>
 * public class UserId implements Serializable {
 *     private String username;
 *     private int departmentNumber;
 *     ...
 *     // Default constructor required by JPA.
 *     // Constructor expects primary key attributes and validates they are not null.
 *     // Just setters are exposed.
 *     // override equals and hasCode properly.
 * }
 * </pre>
 * 
 * As for all component mappings, you can define extra mapping attributes on the fields of the entity's class. To map
 * the composite key for {@code User}, set the generation strategy to application assigned by omitting the
 * {@code @GeneratedValue} annotation.
 * <p>
 * Strategy 1 (<b>This requires to annotate {@code UserId}</b> and it is not necessary to override properties, they are
 * override in case we need to change the column name for example).
 * <p>
 * 
 * <pre>
 * {@literal @}Id
 * {@literal @}AttributeOverrides
 * (
 *     {
 *         {@literal @}AttributeOverride(name="username", column = {@literal @}Column(name="USERNAME")),
 *         {@literal @}AttributeOverride(name="departmentNumber", column = {@literal @}Column(name="DEPARTMENT_NUMBER"))
 *     }
 * )
 * private UserId userId;
 * </pre>
 * 
 * Strategy 2 (<b>This does not require {@code UserId} to be annotated</b>).
 * <p>
 * 
 * <pre>
 * {@literal @}EmbeddedId
 * {@literal @}AttributeOverrides
 * (
 *     {
 *         {@literal @}AttributeOverride(name="username", column = {@literal @}Column(name="USERNAME")),
 *         {@literal @}AttributeOverride(name="departmentNumber", column = {@literal @}Column(name="DEPARTMENT_NUMBER")),
 *     }
 * )
 * private UserId userId;
 * </pre>
 * 
 * Strategy 3 (<b>This does not require {@code UserId} to be annotated</b>).
 * <p>
 * 
 * <pre>
 * {@literal @}Entity
 * {@literal @}Table(name="Users")
 * {@literal @}IdClass(UserId.class)
 * public class User {
 *     {@literal @}Id
 *     private String username;
 *     {@literal @}Id
 *     private int departmentNumber;
 *     ...
 * }
 * </pre>
 * 
 * @param <I>
 *            type of the identifiable object's id. This type should be immutable and it is critical it implements
 *            {@link Object#equals(Object)} and {@link Object#hashCode()} correctly.
 * @param <T>
 *            type of the identifiable object (object to store in the data store)
 * @param <P>
 *            type of the entity (an object annotated with {@link javax.persistence.Entity})
 * @author Fabiel Zuniga
 * @author Nachiket Abhyankar
 */
public abstract class JpaKeyValueDao<I extends Serializable, T extends Identifiable<I>, P>
        implements KeyValueDao<I, T, JpaContext>, Converter<P, T> {

    private final Class<P> entityClass;

    // UpdateStrategy is state-less, so this class remains thread safe.
    private final UpdateStrategy<P, T> updateStrategy;

    /**
     * Creates a DAO.
     * 
     * @param entityClass
     *            class of the object annotated with {@link javax.persistence.Entity}
     */
    protected JpaKeyValueDao(Class<P> entityClass) {
        this(entityClass, null);
    }

    /**
     * Creates a DAO.
     * 
     * @param entityClass
     *            class of the object annotated with {@link javax.persistence.Entity}
     * @param updateStrategy
     *            update strategy
     */
    protected JpaKeyValueDao(Class<P> entityClass,
            UpdateStrategy<P, T> updateStrategy) {
        this.entityClass = Preconditions.checkNotNull(entityClass,
                "entityClass");
        this.updateStrategy = updateStrategy;
    }

    @Override
    public T add(T identifiable, JpaContext context)
            throws PersistenceException {
        Preconditions.checkNotNull(identifiable, "identifiable");
        P entity = create(identifiable);
        JpaUtil.persist(entity, context);
        return convert(entity);
    }

    @Override
    public T update(T identifiable, JpaContext context)
            throws PersistenceException {
        Preconditions.checkNotNull(identifiable, "identifiable");
        Preconditions.checkNotNull(identifiable.getIdentifier(), "Id");

        P entity = getEntity(identifiable.getIdentifier(), context);

        if (entity == null) {
            throw new PersistenceException("entity with id "
                    + identifiable.getIdentifier() + " not found");
        }

        if (this.updateStrategy != null) {
            this.updateStrategy.validateWrite(entity, identifiable);
        }
        conform(entity, identifiable);
        return identifiable;
    }

    @Override
    public void delete(I id, JpaContext context) throws PersistenceException {
        final P entity = getEntity(id, context);
        JpaUtil.delete(entity, context);
    }

    @Override
    public T get(I id, JpaContext context) throws PersistenceException {
        P entity = getEntity(id, context);
        return convert(entity);
    }

    @Override
    public boolean exist(I id, JpaContext context) throws PersistenceException {
        return JpaUtil.exist(this.entityClass, id, context);
    }

    @Override
    public Collection<T> getAll(JpaContext context) throws PersistenceException {
        return convert(JpaUtil.loadAll(getEntityClass(), context));
    }

    @Override
    public long size(JpaContext context) throws PersistenceException {
        return JpaUtil.size(getEntityClass(), context);
    }

    @Override
    public void clear(JpaContext context) throws PersistenceException {
        JpaUtil.delete(getEntityClass(), null, context);
    }

    @Override
    public T convert(P source) {
        T target = doConvert(source);
        if (!Objects.equal(getId(source), target.getIdentifier())) {
            throw new IllegalStateException("Invalid id: source="
                    + getId(source) + ", target=" + target.getIdentifier());
        }

        if (this.updateStrategy != null) {
            this.updateStrategy.validateRead(source, target);
        }
        return target;
    }

    /**
     * Gets the entity class.
     * 
     * @return the entity class
     */
    protected Class<P> getEntityClass() {
        return this.entityClass;
    }

    /**
     * Gets the update strategy.
     * 
     * @return the update strategy.
     */
    protected UpdateStrategy<P, T> getUpdateStrategy() {
        return this.updateStrategy;
    }

    /**
     * Loads an entity from the data store
     * 
     * @param id
     *            entiry's id
     * @param context
     *            context
     * @return the entity if found, {@code null} otherwise
     * @throws PersistenceException
     *             if persistence errors occur while executing the operation
     */
    protected P getEntity(I id, JpaContext context) throws PersistenceException {
        Preconditions.checkNotNull(id, "id");
        return JpaUtil.get(this.entityClass, id, context);
    }

    /**
     * Deletes the entity from the data store. Subclasses could override this method to do any pre-processing /
     * post-processing work.
     * 
     * @param entity
     *            entity to delete
     * @param context
     *            context
     * @throws PersistenceException
     *             if persistence errors occur while executing the operation
     */
    protected void deleteEntity(P entity, JpaContext context)
            throws PersistenceException {
        JpaUtil.delete(entity, context);
    }

    /**
     * Convert a List of entities to their corresponding identifiable object.
     * 
     * @param entities
     *            entities to convert
     * @return a list of identifiable objects
     */
    protected List<T> convert(Collection<P> entities) {
        return CollectionConverter.convert(entities, this,
                CollectionConverter.<T> getArrayListFactory());
    }

    /**
     * Gets the Id of the entity.
     * 
     * @param entity
     *            entity to get the id for
     * @return the id
     */
    protected abstract I getId(P entity);

    /**
     * Creates the instance of the entity.
     * 
     * @param identifiable
     *            object to get the data from
     * @return a new instance of the entity
     */
    protected abstract P create(T identifiable);

    /**
     * Synchronizes the identifiable object and the entity state. This method is called from
     * {@link #update(Identifiable, JpaContext)}, thus if {@link #update(Identifiable, JpaContext)} is overwritten this
     * method could have an empty implementation.
     * 
     * @param target
     *            object to update
     * @param source
     *            object to take the data from
     */
    protected abstract void conform(P target, T source);

    /**
     * Converts the entity to its corresponding identifiable object.
     * 
     * @param source
     *            object to convert
     * @return an identifiable object with the same data than {@code source}
     */
    protected abstract T doConvert(P source);
}
