package io.github.davolabsl.dao.support;

import io.github.davolabsl.advice.exception.FasteException;
import io.github.davolabsl.support.ResourceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

/**
 * The type Jpa dao support handler.
 *
 * @param <T> the type parameter
 * @author - Shehara
 * @date - 1/25/2022
 */
public class JpaDaoSupportHandler<T> {

    /**
     * The Em.
     */
    @PersistenceContext
    protected EntityManager em;
    @Autowired
    ResourceConverter resourceConverter;
    private @Nullable
    CrudMethodMetadata metadata;

    /**
     * Gets meta domain class.
     *
     * @param entityClass the entity class
     * @return the meta domain class
     */
    protected Class<?> getMetaDomainClass(T entityClass) {
        return getMetaClassFromType(entityClass);
    }

    /**
     * Gets domain class.
     *
     * @param entityClass the entity class
     * @return the domain class
     */
    protected Class<?> getDomainClass(T entityClass) {
        return getClassFromType(entityClass);
    }

    /**
     * Gets java type.
     *
     * @param aClass the a class
     * @return the java type
     */
    protected Class getJavaType(Class<?> aClass) {
        return getJpaEntityInformation(aClass).getJavaType();
    }

    /**
     * Gets meta class from type.
     *
     * @param entityClass the entity class
     * @return the meta class from type
     */
    protected Class<?> getMetaClassFromType(T entityClass) {
        try {
            Class aClass = (Class) entityClass;
            return aClass.newInstance().getClass();
        } catch (Exception e) {
            throw new FasteException("can not extract specific entity for the given class", e);
        }
    }

    private Class<?> getClassFromType(T entityClass) {
        try {
            return entityClass.getClass();
        } catch (Exception e) {
            throw new FasteException("can not extract specific entity for the given class", e);
        }
    }

    /**
     * Gets jpa entity information.
     *
     * @param aClass the a class
     * @return the jpa entity information
     */
    public JpaEntityInformation getJpaEntityInformation(Class<?> aClass) {
        return new JpaMetamodelEntityInformation(aClass, em.getMetamodel());
    }

    /**
     * Gets query.
     *
     * @param spec        the spec
     * @param domainClass the domain class
     * @param sort        the sort
     * @return the query
     */
    protected TypedQuery<T> getQuery(@Nullable Specification<T> spec, Class<T> domainClass, Sort sort) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(domainClass);

        Root<T> root = applySpecificationToCriteria(spec, domainClass, query);
        query.select(root);

        if (sort.isSorted()) {
            query.orderBy(toOrders(sort, root, builder));
        }

        return applyRepositoryMethodMetadata(em.createQuery(query));
    }

    private <S, U extends T> Root<U> applySpecificationToCriteria(@Nullable Specification<U> spec, Class<U> domainClass,
                                                                  CriteriaQuery<S> query) {

        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(query, "CriteriaQuery must not be null!");

        Root<U> root = query.from(domainClass);

        if (spec == null) {
            return root;
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }

    private <S> TypedQuery<S> applyRepositoryMethodMetadata(TypedQuery<S> query) {

        if (metadata == null) {
            return query;
        }

        LockModeType type = metadata.getLockModeType();
        TypedQuery<S> toReturn = type == null ? query : query.setLockMode(type);

        return toReturn;
    }
}
