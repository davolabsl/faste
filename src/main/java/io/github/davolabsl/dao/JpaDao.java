package io.github.davolabsl.dao;

import io.github.davolabsl.dao.support.FastPage;
import io.github.davolabsl.dao.support.JpaDaoSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Jpa dao.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 * @param <K>  the type parameter
 * @param <V>  the type parameter
 */
@Repository
public class JpaDao<T, ID extends Serializable, K, V> extends JpaDaoSupport<T, ID, K, V> implements GenericDao<T, ID, K, V>, RevisionRepository<T, Integer, Integer> {

    @Override
    @Transactional
    public T save(T entity) {
        return super.saveEntity(entity);
    }

    @Override
    @Transactional
    public Iterable<T> saveAll(Iterable<T> entities) {
        for (T entity : entities) {
            save(entity);
        }
        return entities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(T entityClass, ID id) {
        return super.findEntityById(entityClass, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByReferenceId(T mainEntity, T referenceEntity, ID id) {
        return super.findByReferenceId(mainEntity, referenceEntity, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findReference(T referenceEntity, ID id) {
        return super.findReference(referenceEntity, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll(T entityClass) {
        return super.findAllEntities(entityClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllByIds(T entityClass, Iterable<ID> ids) {
        return super.findAllEntitiesByIds(entityClass, ids);
    }

    @Override
    @Transactional(readOnly = true)
    public FastPage<T> paginate(T entityClass, ID page, ID size) {
        return super.paginate(entityClass, page, size);
    }

    @Override
    @Transactional
    public T update(T entity) {
        return super.saveEntity(entity);
    }

    @Override
    @Transactional
    public Iterable<T> updateAll(Iterable<T> entities) {
        for (T entity : entities) {
            update(entity);
        }
        return entities;
    }

    @Override
    @Transactional
    public void delete(T entity) {
        super.deleteEntity(entity);
    }

    @Override
    @Transactional
    public void deleteById(T entity, ID id) {
        super.deleteEntityById(entity, id);
    }

    @Override
    @Transactional
    public void deleteAll(T entity) {
        super.deleteAllEntities(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findSelectedById(T entityClass, ID id, String[] fields) {
        return super.findSelectedEntityFieldsById(entityClass, id, fields);
    }

    @Override
    @Transactional(readOnly = true)
    public Object findSelectedByGivenKeyValue(T entityClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orderBy, List<String> parentheses) {
        return super.findSelectedEntityFieldsByGivenKeyValue(entityClass, keys, values, fields, conditions, conditionSeparators, page, size, resultType, orderBy, parentheses);
    }

    @Override
    public Optional<Revision<Integer, T>> findLastChangeRevision(Integer integer) {
        return Optional.empty();
    }

    @Override
    public Revisions<Integer, T> findRevisions(Integer integer) {
        return null;
    }

    @Override
    public org.springframework.data.domain.Page<Revision<Integer, T>> findRevisions(Integer integer, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<Revision<Integer, T>> findRevision(Integer integer, Integer revisionNumber) {
        return Optional.empty();
    }
}
