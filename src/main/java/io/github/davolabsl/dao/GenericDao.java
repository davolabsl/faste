package io.github.davolabsl.dao;

import io.github.davolabsl.dao.support.FastPage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The interface Generic dao.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 * @param <K>  the type parameter
 * @param <V>  the type parameter
 */
public interface GenericDao<T, ID extends Serializable, K, V> {
    /**
     * Save t.
     *
     * @param entity the entity
     * @return the t
     */
    T save(T entity);

    /**
     * Save all iterable.
     *
     * @param entities the entities
     * @return the iterable
     */
    Iterable<T> saveAll(Iterable<T> entities);

    /**
     * Find by id optional.
     *
     * @param entityClass the entity class
     * @param id          the id
     * @return the optional
     */
    Optional<T> findById(T entityClass, ID id);


    /**
     * Find by reference id optional.
     *
     * @param mainEntity      the main entity
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    Optional<T> findByReferenceId(T mainEntity, T referenceEntity, ID id);

    /**
     * Find reference optional.
     *
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    Optional<T> findReference(T referenceEntity, ID id);

    /**
     * Find all list.
     *
     * @param entityClass the entity class
     * @return the list
     */
    List<T> findAll(T entityClass);

    /**
     * Find all by ids list.
     *
     * @param entityClass the entity class
     * @param ids         the ids
     * @return the list
     */
    List<T> findAllByIds(T entityClass, Iterable<ID> ids);

    /**
     * Paginate page.
     *
     * @param entityClass the entity class
     * @param page        the page
     * @param size        the size
     * @return the page
     */
    FastPage<T> paginate(T entityClass, ID page, ID size);

    /**
     * Update t.
     *
     * @param entity the entity
     * @return the t
     */
    T update(T entity);

    /**
     * Update all iterable.
     *
     * @param entities the entities
     * @return the iterable
     */
    Iterable<T> updateAll(Iterable<T> entities);

    /**
     * Delete.
     *
     * @param entity the entity
     */
    void delete(T entity);

    /**
     * Delete by id.
     *
     * @param entity the entity
     * @param id     the id
     */
    void deleteById(T entity, ID id);

    /**
     * Delete all.
     *
     * @param entity the entity
     */
    void deleteAll(T entity);


    /**
     * Find selected by id map.
     *
     * @param aClass the a class
     * @param id     the id
     * @param fields the fields
     * @return the map
     */
    Map findSelectedById(T aClass, ID id, String[] fields);

    /**
     * Find selected by given key value object.
     *
     * @param aClass      the a class
     * @param keys        the keys
     * @param values      the values
     * @param fields      the fields
     * @param conditions  the conditions
     * @param page        the page
     * @param size        the size
     * @param resultType
     * @param orderBy
     * @param parentheses
     * @return the object
     */
    Object findSelectedByGivenKeyValue(T aClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orderBy, List<String> parentheses);
}
