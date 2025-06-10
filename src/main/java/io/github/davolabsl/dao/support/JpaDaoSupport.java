package io.github.davolabsl.dao.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.davolabsl.advice.exception.FasteException;
import io.github.davolabsl.advice.exception.FasteExceptionNoData;
import io.github.davolabsl.support.FasteProxy;
import io.github.davolabsl.util.JsonConverter;
import io.github.davolabsl.util.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.util.ProxyUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * The type Jpa dao support.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 * @param <K>  the type parameter
 * @param <V>  the type parameter
 */
@Slf4j
public class JpaDaoSupport<T, ID, K, V> extends JpaDaoSupportHandler<T> {

    public static final Map<String, String> conditionMap = new HashMap<>();

    static {
        conditionMap.put("equal", "=");
        conditionMap.put("nequal", "<>");
        conditionMap.put("gt", ">");
        conditionMap.put("lt", "<");
        conditionMap.put("gteq", ">=");
        conditionMap.put("lteq", "<=");
        conditionMap.put("like", "LIKE");
        conditionMap.put("nlike", "NOT LIKE");
//        conditionMap.put("bt", "BETWEEN");
        conditionMap.put("in", "IN");
    }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationContext applicationContext;

    private static String getCompleteOrderPieces(String[] orders) {
        StringBuilder finalOrder = new StringBuilder();

        for (String order : orders) {
            String[] splitOrder = order.split(":");
            String orderField = "t." + splitOrder[0] + " ";
            String orderType = splitOrder[1];
            String orderPiece = orderField + orderType;

            // Append the orderPiece to the finalOrder with a comma if it's not the first piece
            if (finalOrder.length() > 0) {
                finalOrder.append(", ");
            }
            finalOrder.append(orderPiece);
        }

        // The final order string will contain all the concatenated order pieces
        return " ORDER BY " + finalOrder;
    }

    private static void validateKeys(String key, Class<?> classFromType) {
        List<String> fieldNames = Arrays.stream(classFromType.getDeclaredFields()).filter(field -> !field.getName().toUpperCase().contains("SERIALVERSIONUID"))  // Exclude serialVersionUID
                .filter(field -> !Iterable.class.isAssignableFrom(field.getType()))  // Exclude collection types
                .map(Field::getName).collect(Collectors.toList());
        if (!fieldNames.contains(key) && !key.contains(".")) {
            throw new FasteException("System cannot find where clause key - [" + key + "] in entity [" + classFromType.getSimpleName() + "]. Here are some possibilities " + fieldNames);
        }
    }

    /**
     * Save entity t.
     *
     * @param entity the entity
     * @return the t
     */
    public T saveEntity(T entity) {
        try {
            Assert.notNull(entity, "Entity must not be null.");
            if (getJpaEntityInformation(getJavaType(getDomainClass(entity))).isNew(entity)) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Delete.
     *
     * @param entity the entity
     */
    public void deleteEntity(T entity) {
        try {
            Assert.notNull(entity, "Entity must not be null!");
            if (!getJpaEntityInformation(getDomainClass(entity)).isNew(entity)) {
                Class<?> type = ProxyUtils.getUserClass(entity);
                T existing = (T) em.find(type, getJpaEntityInformation(getDomainClass(entity)).getId(entity));
                if (existing != null) {
                    this.em.remove(this.em.contains(entity) ? entity : this.em.merge(entity));
                } else {
                    throw new EmptyResultDataAccessException(String.format("No %s entity exists!", getJavaType(getDomainClass(entity)).getSimpleName()), 1);
                }
            }
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Delete by id.
     *
     * @param entity the entity
     * @param id     the id
     */
    public void deleteEntityById(T entity, ID id) {
        try {
            Assert.notNull(id, "The given id must not be null!");
            deleteEntity(findEntityById(entity, id).orElseThrow(() -> new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", getJavaType(getMetaDomainClass(entity)).getSimpleName(), id), 1)));
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Delete all entities.
     *
     * @param entity the entity
     */
    public void deleteAllEntities(T entity) {
        try {
            Iterator<T> iterator = this.findAllEntities(entity).iterator();
            while (iterator.hasNext()) {
                T element = iterator.next();
                this.deleteEntity(element);
            }
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Gets reference.
     *
     * @param entity     the entity
     * @param identifier the identifier
     * @return the reference
     */
    public T getReference(T entity, ID identifier) {
        try {
            Class<?> domainClass = getJavaType(getDomainClass(entity));
            return (T) em.getReference(domainClass, identifier);
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find all list.
     *
     * @param entityClass the entity class
     * @return the list
     */
    protected List<T> findAllEntities(T entityClass) {
        try {
            Class<?> aClass = getJavaType(getMetaDomainClass(entityClass));
            Class domainClass = getJavaType(aClass);
            TypedQuery<?> query = getQuery(null, (Class<T>) domainClass, Sort.unsorted());
            return (List<T>) query.getResultList();
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find all by ids list.
     *
     * @param entityClass the entity class
     * @param ids         the ids
     * @return the list
     */
    protected List<T> findAllEntitiesByIds(T entityClass, Iterable<ID> ids) {
        try {
            Assert.notNull(ids, "Ids must not be null!");

            if (!ids.iterator().hasNext()) {
                return Collections.emptyList();
            }

            Class<?> classFromType = getMetaClassFromType(entityClass);
            if (getJpaEntityInformation(classFromType).hasCompositeId()) {

                List<T> results = new ArrayList<T>();

                for (ID id : ids) {
                    findEntityById(entityClass, id).ifPresent(results::add);
                }

                return results;
            }

            Class idType = getJpaEntityInformation(classFromType).getIdType();
            if (idType.equals(Long.class)) {
                List<Long> collect = Streamable.of(ids).stream().map(id -> Long.parseLong(String.valueOf(id))).collect(Collectors.toList());

                ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
                TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

                return query.setParameter(specification.parameter, collect).getResultList();
            } else if (idType.equals(Integer.class)) {
                List<Integer> collect = Streamable.of(ids).stream().map(id -> Integer.parseInt(String.valueOf(id))).collect(Collectors.toList());

                ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
                TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

                return query.setParameter(specification.parameter, collect).getResultList();
            } else {
                Collection<ID> idCollection = Streamable.of(ids).toList();

                ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
                TypedQuery<T> query = getQuery(specification, (Class<T>) getJavaType(getMetaDomainClass(entityClass)), Sort.unsorted());

                return query.setParameter(specification.parameter, idCollection).getResultList();
            }

        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find by id optional.
     *
     * @param entityClass the entity class
     * @param id          the id
     * @return the optional
     */
    protected Optional<T> findEntityById(T entityClass, ID id) {
        try {
            Assert.notNull(entityClass, "Entity(T) cannot be null");
            Assert.notNull(id, "Identifier(ID) cannot be null");
            Class<?> domainClass = getJavaType(getMetaDomainClass(entityClass));
            Class idType = getJpaEntityInformation(domainClass).getIdType();
            if (idType.equals(Long.class)) {
                T t = (T) em.find(domainClass, Long.parseLong(String.valueOf(id)));
                return Optional.ofNullable(t);
            } else if (idType.equals(Integer.class)) {
                T t = (T) em.find(domainClass, Integer.parseInt(String.valueOf(id)));
                return Optional.ofNullable(t);
            } else {
                T t = (T) em.find(domainClass, id);
                return Optional.ofNullable(t);
            }
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find reference optional.
     *
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    protected Optional<T> findReference(T referenceEntity, ID id) {
        try {
            Assert.notNull(referenceEntity, "Entity(T) cannot be null");
            Assert.notNull(id, "Identifier(ID) cannot be null");
            Class<?> domainClass = getJavaType(getMetaDomainClass(referenceEntity));
            Class idType = getJpaEntityInformation(domainClass).getIdType();

            if (idType.equals(Long.class)) {
                T t = (T) em.getReference(domainClass, Long.parseLong(String.valueOf(id)));
                return Optional.ofNullable(t);
            } else if (idType.equals(Integer.class)) {
                T t = (T) em.getReference(domainClass, Integer.parseInt(String.valueOf(id)));
                return Optional.ofNullable(t);
            } else {
                T t = (T) em.getReference(domainClass, id);
                return Optional.ofNullable(t);
            }
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find by reference id optional.
     *
     * @param mainEntity      the main entity
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the optional
     */
    protected Optional<T> findByReferenceId(T mainEntity, T referenceEntity, ID id) {
        try {
            Assert.notNull(mainEntity, "table name cannot be null");
            Assert.notNull(referenceEntity, "reference cannot be null");
            Assert.notNull(id, "id cannot be null");
            String declaredReferenceFieldName = ReflectionUtil.getDeclaredReferenceFieldName((Class) mainEntity, (Class) referenceEntity);
            Optional<T> reference = findReference(referenceEntity, id);
            TypedQuery<?> query = null;
            try {
                query = em.createQuery("SELECT t FROM " + ((Class) mainEntity).getSimpleName() + " t WHERE t." + declaredReferenceFieldName + " = :ref", Class.forName(((Class) mainEntity).getName())).setParameter("ref", reference.get());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            List<?> resultList = query.getResultList();
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            T results = (T) objectMapper.convertValue(resultList, List.class);
            return Optional.ofNullable(results);
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Paginate page.
     *
     * @param entityClass the entity class
     * @param page        the page
     * @param size        the size
     * @return the page
     */
    protected FastPage<T> paginate(T entityClass, ID page, ID size) {
        try {
            Assert.notNull(entityClass, "table name cannot be null");
            Assert.notNull(page, "page number cannot be null");
            Assert.notNull(size, "page size cannot be null");
            TypedQuery<?> query = null;
            try {
                query = em.createQuery("SELECT t FROM " + ((Class) entityClass).getSimpleName() + " t", Class.forName(((Class) entityClass).getName()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            int s = Integer.parseInt(String.valueOf(size));
            int p = Integer.parseInt(String.valueOf(page));
            query.setFirstResult(p * s);
            query.setMaxResults(s);
            FastPage paginate = new FastPage();
            paginate.setPage(p);
            paginate.setPerPage(s);
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            List<?> resultList = query.getResultList();
            paginate.setResults(objectMapper.convertValue(resultList, List.class));
            TypedQuery<Long> totalQuery = em.createQuery("SELECT count(t) FROM " + ((Class) entityClass).getSimpleName() + " t", Long.class);
            Long count = totalQuery.getSingleResult();
            paginate.setTotal(count);
            Long l = count / s;
            Integer totalPages = l != null ? l.intValue() : 0;
            paginate.setTotalPages(totalPages);
            return paginate;
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    /**
     * Find selected entity fields by id map.
     *
     * @param entityClass the entity class
     * @param id          the id
     * @param fields      the fields
     * @return the map
     */
    protected Map<String, Object> findSelectedEntityFieldsById(T entityClass, ID id, String[] fields) {
        Assert.notNull(entityClass, "Entity(T) cannot be null");
        Assert.notNull(id, "Identifier(ID) cannot be null");
        Assert.notEmpty(fields, "Identifier(ID) cannot be empty");
        TypedQuery<?> query = null;
        try {
            List<String> representation = new ArrayList<>();
            List<String> selection = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            for (String field : fields) {
                representation.add(field);
                selection.add("t." + field);
            }
            Class<?> classFromType = getMetaClassFromType(entityClass);
            ByIdsSpecification<T> specification = new ByIdsSpecification<T>(getJpaEntityInformation(classFromType));
            String pkAttribute = ((JpaMetamodelEntityInformation) specification.entityInformation).getIdAttribute().getName();
            Class<?> domainClass = getJavaType(getMetaDomainClass(entityClass));
            Class idType = getJpaEntityInformation(domainClass).getIdType();
            query = em.createQuery("SELECT " + String.join(",", selection) + " FROM " + ((Class) entityClass).getSimpleName() + " t WHERE t." + pkAttribute + "=:id", Object[].class);
            Object castedValue = resolveType(String.valueOf(id), idType);
            query.setParameter("id", castedValue);
            Object singleResult = query.getSingleResult();
//            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Object convertValue = objectMapper.convertValue(singleResult, singleResult.getClass());
            if (convertValue instanceof Object[]) {
                int i = 0;
                for (Object o : ((Object[]) convertValue)) {
                    map.put(representation.get(i), o);
                    i++;
                }
            } else {
                map.put(fields[0], convertValue);
            }
            return map;
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (Exception e) {
            log.error("System Error", e);
            throw new FasteException("System Error");
        }
    }

    protected Object findSelectedEntityFieldsByGivenKeyValue(T entityClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orders, List<String> parentheses) {
        Assert.notNull(entityClass, "Entity(T) cannot be null");
        Assert.notEmpty(keys, "Keys cannot be empty");
        Assert.notEmpty(values, "Values cannot be empty");
        try {
            // Initialize a StringBuilder to build the final order string
            String completeOrderPieces = "";
            if (orders != null) {
                completeOrderPieces = getCompleteOrderPieces(orders);
            }
            List<String> representation = new ArrayList<>();
            List<String> selection = new ArrayList<>();
            boolean isSelectAll = false;
            boolean isDistinct = false;
            if (fields == null || fields.length == 0) {
                isSelectAll = true;
            } else {
                for (String field : fields) {
                    if (field.toUpperCase().contains("DISTINCT")) {
                        isDistinct = true;
                        if (fields.length > 1) {
                            throw new FasteException("You can't use more than one fields with distinct. If you want to add more fields, please use like [DISTINCT:field01:field02]");
                        } else {
                            String[] distAndFields = field.split(":");
                            for (int i = 1; i < distAndFields.length; i++) {
                                String distAndField = distAndFields[i];
                                representation.add(distAndField);
                                selection.add("t." + distAndField);
                            }
                            break;
                        }
                    } else {
                        representation.add(field);
                        selection.add("t." + field);
                    }
                }
            }

            List<Object> castedValues = new ArrayList<>();
            List<Object> sqlConditions = new ArrayList<>();
            Class<?> classFromType = getMetaClassFromType(entityClass);
            for (int i = 0; i < keys.length; i++) {
                Object castedValue = extractFieldValueAsItsType(keys[i], values[i], classFromType, conditions[i]);
                String sqlCondition = resolveCondition(conditions[i]);
                if (conditions[i].equals("like") || conditions[i].equals("nlike")) {
                    castedValues.add("%" + castedValue + "%");
                } else {
                    castedValues.add(castedValue);
                }
                sqlConditions.add(sqlCondition);
            }
            if (isSelectAll) {
                Map valueParams = new HashMap();
                StringBuilder countQuery = new StringBuilder();
                countQuery.append("SELECT count(t) FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                StringBuilder sqlQuery = new StringBuilder();
                sqlQuery.append("SELECT t FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                for (int i = 0; i < keys.length; i++) {
                    String valueParam = ":value" + i;
                    valueParams.put(valueParam, castedValues.get(i));
                    String conditionSeparator = "";
                    if (conditionSeparators != null && conditionSeparators.length > 0) {
                        if (conditionSeparators.length != i) {
                            conditionSeparator = " " + conditionSeparators[i] + " ";
                        }
                    }
//                    if (sqlConditions.get(i).equals("BETWEEN")) {
//                        sqlQuery.append(keys[i] + " " + sqlConditions.get(i) + " " + valueParam + " AND " + ":value" + i + 1 + conditionSeparator);
//                    } else {
                    if (parentheses.get(i).equals("(")) {
                        sqlQuery.append(parentheses.get(i)).append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                        countQuery.append(parentheses.get(i)).append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    } else if (parentheses.get(i).equals(")")) {
                        sqlQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(parentheses.get(i)).append(conditionSeparator);
                        countQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(parentheses.get(i)).append(conditionSeparator);
                    } else {
                        sqlQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                        countQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    }

//                    }
                }
                if (orders != null) {
                    sqlQuery.append(" ").append(completeOrderPieces);
                }
                TypedQuery<?> query;
                query = em.createQuery(sqlQuery.toString(), Object[].class);
                valueParams.forEach((k, v) -> {
                    if (v instanceof String) {
                        String stringValue = v.toString().toLowerCase();
                        query.setParameter(String.valueOf(k).split(":")[1], stringValue);
                    } else {
                        query.setParameter(String.valueOf(k).split(":")[1], v);
                    }
                });
                if (page != null && size != null) {
                    query.setFirstResult(page * size);
                    query.setMaxResults(size);
                    FastPage paginate = new FastPage();
                    paginate.setPage(page);
                    paginate.setPerPage(size);
                    List<?> resultList = query.getResultList();
//                    ObjectMapper objectMapper = new ObjectMapper().setTimeZone(TimeZone.getTimeZone("GMT+05:30")).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> collect = list.stream().map(map -> JsonConverter.removeHibernateLazyInitializer(map)).collect(Collectors.toList());
                    // TODO: Add Proxy Call Here
                    for (Map map : collect) {
                        callProxyData(map, entityClass, fields);
                    }
                    paginate.setResults(collect);
                    TypedQuery<Long> totalQuery = em.createQuery(countQuery.toString(), Long.class);
                    valueParams.forEach((k, v) -> {
                        if (v instanceof String) {
                            String stringValue = v.toString().toLowerCase();
                            totalQuery.setParameter(String.valueOf(k).split(":")[1], stringValue);
                        } else {
                            totalQuery.setParameter(String.valueOf(k).split(":")[1], v);
                        }
                    });
                    Long count = totalQuery.getSingleResult();
                    paginate.setTotal(count);
                    long l = count / size;
                    int totalPages = (int) l;
                    paginate.setTotalPages((count > 0L && totalPages == 0) ? 1 : totalPages);
                    return paginate;
                } else {
                    List<?> resultList = query.getResultList();
//                    ObjectMapper objectMapper = new ObjectMapper().setTimeZone(TimeZone.getTimeZone("GMT+05:30")).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> collect = list.stream().map(map -> JsonConverter.removeHibernateLazyInitializer(map)).collect(Collectors.toList());
                    // TODO: Add Proxy Call Here
                    for (Map map : collect) {
                        callProxyData(map, entityClass, fields);
                    }
                    Map returnMap = new HashMap<>();
                    if (resultType == null || resultType.isEmpty()) {
                        returnMap.put("result", collect);
                    } else if (resultType.equalsIgnoreCase("S")) {
                        if (!collect.isEmpty()) {
                            Map map = collect.get(0);
                            returnMap.put("result", map);
                        } else {
                            returnMap.put("result", new ArrayList<>());
                        }
                    } else if (resultType.equalsIgnoreCase("M")) {
                        returnMap.put("result", collect);
                    }
                    return returnMap;
                }
            } else {
                Map valueParams = new HashMap();
                StringBuilder countQuery = new StringBuilder();
                StringBuilder sqlQuery = new StringBuilder();
                if (isDistinct) {
                    countQuery.append("SELECT count(DISTINCT t.").append(representation.get(0)).append(") FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                    sqlQuery.append("SELECT DISTINCT ").append(String.join(",", selection)).append(" FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                } else {
                    countQuery.append("SELECT count(t) FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                    sqlQuery.append("SELECT ").append(String.join(",", selection)).append(" FROM ").append(((Class) entityClass).getSimpleName()).append(" t WHERE ");
                }
                for (int i = 0; i < keys.length; i++) {
                    String valueParam = ":value" + i;
                    valueParams.put(valueParam, castedValues.get(i));
                    String conditionSeparator = "";
                    if (conditionSeparators != null && conditionSeparators.length > 0) {
                        if (conditionSeparators.length != i) {
                            conditionSeparator = " " + conditionSeparators[i] + " ";
                        }
                    }
//                    if (sqlConditions.get(i).equals("BETWEEN")) {
//                        sqlQuery.append(keys[i] + " " + sqlConditions.get(i) + " " + valueParam + " AND " + ":value" + i + 1 + conditionSeparator);
//                    } else {
                    if (parentheses.get(i).equals("(")) {
                        sqlQuery.append(parentheses.get(i)).append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                        countQuery.append(parentheses.get(i)).append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    } else if (parentheses.get(i).equals(")")) {
                        sqlQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(parentheses.get(i)).append(conditionSeparator);
                        countQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(parentheses.get(i)).append(conditionSeparator);
                    } else {
                        sqlQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                        countQuery.append("LOWER(t.").append(keys[i]).append(") ").append(sqlConditions.get(i)).append(" ").append(valueParam).append(conditionSeparator);
                    }

//                    }
                }
                if (orders != null) {
                    sqlQuery.append(" ").append(completeOrderPieces);
                }
                TypedQuery<?> query;
                query = em.createQuery(sqlQuery.toString(), Object[].class);
                valueParams.forEach((k, v) -> {
                    if (v instanceof String) {
                        String stringValue = v.toString().toLowerCase();
                        query.setParameter(String.valueOf(k).split(":")[1], stringValue);
                    } else {
                        query.setParameter(String.valueOf(k).split(":")[1], v);
                    }
                });
                if (page != null && size != null) {
                    query.setFirstResult(page * size);
                    query.setMaxResults(size);
                    FastPage paginate = new FastPage();
                    paginate.setPage(page);
                    paginate.setPerPage(size);
                    List<?> resultList = query.getResultList();
//                    ObjectMapper objectMapper = new ObjectMapper().setTimeZone(TimeZone.getTimeZone("GMT+05:30")).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> mapList = new ArrayList<>();
                    for (Object convertValue : list) {
                        Map<String, Object> map = new HashMap<>();
                        int i = 0;
                        if (convertValue instanceof ArrayList) {
                            for (Object o : (ArrayList) convertValue) {
                                map.put(representation.get(i), o);
                                i++;
                            }
                        } else {
                            map.put(representation.get(i), convertValue);
                        }
                        Map convert = JsonConverter.convert(map);
                        // TODO: Add Proxy Call Here
                        callProxyData(convert, entityClass, fields);
                        mapList.add(convert);
                    }
                    paginate.setResults(mapList);

                    TypedQuery<Long> totalQuery = em.createQuery(countQuery.toString(), Long.class);
                    valueParams.forEach((k, v) -> {
                        if (v instanceof String) {
                            String stringValue = v.toString().toLowerCase();
                            totalQuery.setParameter(String.valueOf(k).split(":")[1], stringValue);
                        } else {
                            totalQuery.setParameter(String.valueOf(k).split(":")[1], v);
                        }
                    });
                    Long count = totalQuery.getSingleResult();
                    paginate.setTotal(count);
                    long l = count / size;
                    int totalPages = (int) l;
                    paginate.setTotalPages((count > 0L && totalPages == 0) ? 1 : totalPages);
                    return paginate;
                } else {
                    List<?> resultList = query.getResultList();
//                    ObjectMapper objectMapper = new ObjectMapper().setTimeZone(TimeZone.getTimeZone("GMT+05:30")).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    List<Map> list = objectMapper.convertValue(resultList, List.class);
                    List<Map> mapList = new ArrayList<>();
                    for (Object convertValue : list) {
                        Map<String, Object> map = new HashMap<>();
                        int i = 0;
                        if (convertValue instanceof ArrayList) {
                            for (Object o : (ArrayList) convertValue) {
                                map.put(representation.get(i), o);
                                i++;
                            }
                        } else {
                            map.put(representation.get(i), convertValue);
                        }
                        Map convert = JsonConverter.convert(map);
                        // TODO: Add Proxy Call Here
                        if (isDistinct) {
                            List<String> parts = new ArrayList<>();
                            String field = fields[0];
                            String[] split = field.split(":");
                            for (int j = 0, splitLength = split.length; j < splitLength; j++) {
                                if (j > 0) {
                                    String part = split[j];
                                    parts.add(part);
                                }
                            }
                            callProxyData(convert, entityClass, parts.toArray(new String[0]));
                        } else {
                            callProxyData(convert, entityClass, fields);
                        }
                        mapList.add(convert);
                    }
                    Map returnMap = new HashMap<>();
                    if (resultType == null || resultType.isEmpty()) {
                        returnMap.put("result", mapList);
                    } else if (resultType.equalsIgnoreCase("S")) {
                        if (!mapList.isEmpty()) {
                            Map map = mapList.get(0);
                            returnMap.put("result", map);
                        } else {
                            returnMap.put("result", new ArrayList<>());
                        }
                    } else if (resultType.equalsIgnoreCase("M")) {
                        returnMap.put("result", mapList);
                    }
                    return returnMap;
                }
            }
        } catch (NoResultException nre) {
            log.warn("Data not found for the given criteria", nre);
            throw new FasteExceptionNoData("Data not found");
        } catch (FasteException e) {
            log.error("System Error", e);
            throw new FasteException(e.getMessage());
        } catch (Exception e) {
            log.error("Runtime Error", e);
            throw new RuntimeException(e);
        }
    }

    private void callProxyData(Map map, T entityClass, String[] fields) throws ClassNotFoundException {
        Object actualDataEntity = objectMapper.convertValue(map, Class.forName(((Class<?>) entityClass).getName()));
        Map proxyDataMap = initializeProxy(actualDataEntity, fields);
        map.putAll(proxyDataMap);
    }

    private Map initializeProxy(Object actualDataEntity, String[] fields) {
        Map selectedFields = new HashMap();
        if (fields != null) {
            for (String field : fields) {
                selectedFields.put(field, field);
            }
        }
        Map returnData = new HashMap();
        for (Field declaredField : actualDataEntity.getClass().getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(FasteProxy.class) && fields == null) {
                processProxy(actualDataEntity, declaredField, returnData);
            } else if (declaredField.isAnnotationPresent(FasteProxy.class) && selectedFields.containsKey(declaredField.getName())) {
                processProxy(actualDataEntity, declaredField, returnData);
            }
        }
        return returnData;
    }

    private void processProxy(Object actualDataEntity, Field declaredField, Map returnData) {
        try {
            FasteProxy fasteProxy = declaredField.getAnnotation(FasteProxy.class);
            Class<?> aClass = fasteProxy.proxyClass();
            String method = fasteProxy.method();
            String target = fasteProxy.target().toLowerCase();
            String responseSplitter = fasteProxy.responseSplitter();
            String representation = fasteProxy.representation();
            String methodName = method.split("\\(")[0];
            String params = method.split("\\(")[1].split("\\)")[0];
            List<Object> parameterValues = new ArrayList<>();
            for (String param : params.split(",")) {
                for (Field field : actualDataEntity.getClass().getDeclaredFields()) {
                    if (field.getName().equals(param.trim())) {
                        field.setAccessible(true);
                        Object parameterValue = field.get(actualDataEntity);
                        parameterValues.add(parameterValue);
                    }
                }
                if (param.trim().equals("target")) {
                    parameterValues.add(target);
                }
            }
            Object proxyData = callMethod(actualDataEntity, aClass, methodName, parameterValues, fasteProxy);
            if (representation.equals("")) {
                returnData.putAll(objectMapper.convertValue(proxyData, Map.class));
            } else {
                returnData.put(representation, proxyData);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object callMethod(Object actualDataEntity, Class<?> aClass, String callMethod, List<Object> parameterValues, FasteProxy fasteProxy) {
        try {
            boolean isNullNotDitected = true;
            Class<?>[] parameterTypes = new Class<?>[parameterValues.size()];
            for (int i = 0; i < parameterValues.size(); i++) {
                if (parameterValues.get(i) != null) {
                    parameterTypes[i] = parameterValues.get(i).getClass();
                } else {
                    isNullNotDitected = false;
                }
            }
            if (isNullNotDitected) {
                Object contextBean = applicationContext.getBean(aClass);

                Method method = aClass.getDeclaredMethod(callMethod, parameterTypes);

                // If the method is static, pass null as the instance, otherwise pass an instance on which to invoke the method
                Object result;
                if (Modifier.isStatic(method.getModifiers())) {
                    result = method.invoke(null, parameterValues.toArray());
                } else {
                    result = method.invoke(contextBean, parameterValues.toArray());
                }
                Map map = objectMapper.convertValue(result, Map.class);
                if (fasteProxy.responseSplitter().length() > 0) {
                    return map.get(fasteProxy.responseSplitter());
                } else {
                    return result;
                }
            } else {
                return null;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveCondition(String condition) {
        try {
            return conditionMap.getOrDefault(condition, "=");
        } catch (Exception e) {
            return "=";
        }
    }

    private Object extractFieldValueAsItsType(String key, String value, Class<?> classFromType, String condition) {
        Object castedValue = null;
        validateKeys(key, classFromType);
        for (Field declaredField : classFromType.getDeclaredFields()) {
            String k = String.valueOf(key);
            if (k.contains(".")) {
                String[] parts = k.split("\\.");
                validateKeys(parts[0], classFromType);
                if (declaredField.getName().equals(parts[0])) {
                    if (!declaredField.getType().getPackage().getName().startsWith("java.")) {
                        String replace = k.replace(parts[0] + ".", "");
                        Class<?> aClass = resourceConverter.convertResourceToActualEntity(declaredField.getType().getSimpleName().toLowerCase());
                        return extractFieldValueAsItsType(replace, value, aClass, condition);
                    }
                }
            } else {
                if (declaredField.getName().equals(key)) {
                    if (conditionMap.get(condition.toLowerCase()).equals("IN")) {
                        if (value.contains(" ,") || value.contains(", ") || value.contains(" , ")) {
                            throw new FasteException("There are some unwanted characters or spaces near , ");
                        }
                        String[] split = value.split(",");
                        List<Object> var = new ArrayList<>();
                        for (String val : split) {
                            Object resolvedType = resolveType(val, declaredField.getType());
                            if (resolvedType instanceof String) {
                                String lowerCasedData = ((String) resolvedType).toLowerCase();
                                var.add(lowerCasedData);
                            } else {
                                var.add(resolvedType);
                            }
                        }
                        return var;
                    }
                    Class<?> fieldType = declaredField.getType();
                    castedValue = resolveType(value, fieldType);
                    break;
                }
            }
        }
        return castedValue;
    }

    private Object resolveType(String value, Class<?> fieldType) {
        try {
            Object castedValue = null;
            if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                castedValue = Long.parseLong(String.valueOf(value));
            } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                castedValue = Integer.parseInt(String.valueOf(value));
            } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                castedValue = Boolean.parseBoolean(String.valueOf(value));
            } else if (fieldType.equals(Date.class)) {
                castedValue = objectMapper.convertValue(String.valueOf(value), Date.class);
            } else if (fieldType.equals(java.sql.Date.class)) {
                castedValue = java.sql.Date.valueOf(String.valueOf(value));
            } else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
                castedValue = String.valueOf(value).charAt(0);
            } else if (fieldType.isEnum()) {
                castedValue = Enum.valueOf((Class<Enum>) fieldType, value);
            } else if (fieldType.equals(LocalDate.class)) {
                castedValue = LocalDate.parse(value);
            } else if (fieldType.equals(LocalTime.class)) {
                castedValue = LocalTime.parse(value);
            } else if (fieldType.equals(LocalDateTime.class)) {
                castedValue = LocalDateTime.parse(value);
            } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                castedValue = Double.parseDouble(String.valueOf(value));
            } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                castedValue = Float.parseFloat(String.valueOf(value));
            } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                castedValue = Short.parseShort(String.valueOf(value));
            } else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                castedValue = Byte.parseByte(String.valueOf(value));
            } else if (fieldType.equals(String.class)) {
                castedValue = String.valueOf(value);
            } else if (fieldType.equals(BigDecimal.class)) {
                castedValue = new BigDecimal(String.valueOf(value));
            } else if (fieldType.equals(BigInteger.class)) {
                castedValue = new BigInteger(String.valueOf(value));
            } else if (fieldType.equals(UUID.class)) {
                castedValue = UUID.fromString(String.valueOf(value));
            } else if (fieldType.equals(AtomicInteger.class)) {
                castedValue = new AtomicInteger(Integer.parseInt(String.valueOf(value)));
            } else if (fieldType.equals(AtomicLong.class)) {
                castedValue = new AtomicLong(Long.parseLong(String.valueOf(value)));
            } else {
                castedValue = fieldType.cast(value);
            }
            return castedValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static final class ByIdsSpecification<T> implements Specification<T> {

        private static final long serialVersionUID = 1L;

        private final JpaEntityInformation<T, ?> entityInformation;

        /**
         * The Parameter.
         */
        @Nullable
        ParameterExpression<Collection<?>> parameter;

        /**
         * Instantiates a new By ids specification.
         *
         * @param entityInformation the entity information
         */
        ByIdsSpecification(JpaEntityInformation<T, ?> entityInformation) {
            this.entityInformation = entityInformation;
        }

        @Override
        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

            Path<?> path = root.get(entityInformation.getIdAttribute());
            parameter = (ParameterExpression<Collection<?>>) (ParameterExpression) cb.parameter(Collection.class);
            return path.in(parameter);
        }
    }

}
