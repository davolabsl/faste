package io.github.davolabsl.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.davolabsl.advice.exception.FasteException;
import io.github.davolabsl.dao.GenericDao;
import io.github.davolabsl.dao.support.FastPage;
import io.github.davolabsl.dao.support.JpaDaoSupport;
import io.github.davolabsl.util.Parentheses;
import io.github.davolabsl.web.rest.dto.FasteQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The type Request handler.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 */
@Component
public class RequestHandler<T, ID extends Serializable, K, V> {

//    @Autowired
//    private ObjectMapper objectMapper;

    @Autowired
    ResourceConverter resourceConverter;
    @Autowired
    private GenericDao genericDao;

    @Autowired
    private ObjectMapper objectMapper;

    private static void validateKeys(String key, Class<?> classFromType) {
        List<String> fieldNames = Arrays.stream(classFromType.getDeclaredFields()).filter(field -> !field.getName().toUpperCase().contains("SERIALVERSIONUID"))  // Exclude serialVersionUID
//                .filter(field -> !Iterable.class.isAssignableFrom(field.getType()))  // Exclude collection types
                .map(Field::getName).collect(Collectors.toList());
        if (!fieldNames.contains(key) && !key.contains(".") && !key.contains(":")) {
            throw new FasteException("System cannot find search key - [" + key + "] in entity [" + classFromType.getSimpleName() + "]. Here are some possibilities " + fieldNames);
        }
    }

    /**
     * Save handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T saveHandler(Class<?> aClass, T entity) {
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object value = objectMapper.convertValue(entity, aClass);
        return (T) genericDao.save(value);
    }

    /**
     * Save all handler t.
     *
     * @param aClass   the a class
     * @param entities the entities
     * @return the t
     */
    public T saveAllHandler(Class<?> aClass, T entities) {
        List<?> objects = collectEntities(entities, aClass);
        return (T) genericDao.saveAll(objects);
    }

    /**
     * Find by id handler t.
     *
     * @param aClass the a class
     * @param id     the id
     * @return the t
     */
    public T findByIdHandler(Class<?> aClass, ID id) {
        Optional<T> optionalData = genericDao.findById(aClass, id);
        if (optionalData.isPresent()) {
//            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return (T) objectMapper.convertValue(optionalData.get(), optionalData.get().getClass());
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    /**
     * Find by reference id handler t.
     *
     * @param mainEntity      the main entity
     * @param referenceEntity the reference entity
     * @param id              the id
     * @return the t
     */
    public T findByReferenceIdHandler(Class<?> mainEntity, Class<?> referenceEntity, ID id) {
        Optional<T> data = genericDao.findByReferenceId(mainEntity, referenceEntity, id);
        if (data.isPresent()) {
            return data.get();
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    /**
     * Find all handler t.
     *
     * @param aClass the a class
     * @return the t
     */
    public T findAllHandler(Class<?> aClass) {
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List all = genericDao.findAll(aClass);
        if (!all.isEmpty()) {
            List list = objectMapper.convertValue(all, List.class);
            return (T) list;
        } else {
            throw new FasteException("unable to find resources");
        }
    }

    /**
     * Find all by ids handler t.
     *
     * @param aClass the a class
     * @param ids    the ids
     * @return the t
     */
    public T findAllByIdsHandler(Class<?> aClass, ID[] ids) {
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List<ID> idList = Arrays.stream(ids).collect(Collectors.toList());
        List all = genericDao.findAllByIds(aClass, idList);
        if (!all.isEmpty()) {
            List list = objectMapper.convertValue(all, List.class);
            return (T) list;
        } else {
            throw new FasteException("unable to find resources with given ids - " + ids);
        }
    }

    /**
     * Paginate handler page.
     *
     * @param entityClass the entity class
     * @param page        the page
     * @param size        the size
     * @return the page
     */
    public FastPage<T> paginateHandler(T entityClass, ID page, ID size) {
        FastPage paginate = genericDao.paginate(entityClass, page, size);
        return paginate;
    }

    /**
     * Update handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T updateHandler(Class<?> aClass, T entity) {
        try {
//            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            Object value = objectMapper.convertValue(entity, aClass);
            T update = (T) genericDao.update(value);
            return (T) objectMapper.convertValue(update, update.getClass());
        } catch (Exception e) {
            throw new FasteException("exception occurred while performing updateHandler", e);
        }
    }

    /**
     * Dynamic Update handler t.
     *
     * @param aClass the a class
     * @param entity the entity
     * @return the t
     */
    public T dynamicUpdateHandler(Class<?> aClass, T entity, ID id) {
        try {
            // Load the existing entity from the database
            Optional existingEntity = genericDao.findById(aClass, id);// Assuming you have a method to fetch the entity by its ID

            if (!existingEntity.isPresent()) {
                throw new EntityNotFoundException("Entity not found"); // Handle this appropriately
            }

            // Use ObjectMapper to map only the non-null properties from entity to existingEntity
//            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).registerModule(new JavaTimeModule());
            Object updatedValue = objectMapper.updateValue(existingEntity.get(), entity);

            // Update the existingEntity in the database

            return (T) genericDao.update(updatedValue);
        } catch (Exception e) {
            throw new FasteException("Exception occurred while performing updateHandler", e);
        }
    }

    /**
     * Update all handler t.
     *
     * @param aClass   the a class
     * @param entities the entities
     * @return the t
     */
    public T updateAllHandler(Class<?> aClass, T entities) {
        try {
            List<?> objects = collectEntities(entities, aClass);
            T updateAll = (T) genericDao.updateAll(objects);
//            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return (T) objectMapper.convertValue(updateAll, List.class);
        } catch (Exception e) {
            throw new FasteException("exception occurred while performing updateAllHandler", e);
        }
    }

    /**
     * Delete handler.
     *
     * @param aClass the a class
     * @param entity the entity
     */
    public void deleteHandler(Class<?> aClass, T entity) {
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object value = objectMapper.convertValue(entity, aClass);
        genericDao.delete(value);
    }

    /**
     * Delete by id handler.
     *
     * @param aClass the a class
     * @param id     the id
     */
    public void deleteByIdHandler(Class<?> aClass, ID id) {
        genericDao.deleteById(aClass, id);
    }

    /**
     * Delete all handler.
     *
     * @param aClass the a class
     */
    public void deleteAllHandler(Class<?> aClass) {
        genericDao.deleteAll(aClass);
    }

    private List<?> collectEntities(T entities, Class<?> aClass) {
//        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(((ArrayList) entities).iterator(), Spliterator.ORDERED), true);
        return (List<?>) stream.map(o -> objectMapper.convertValue(o, aClass)).collect(Collectors.toList());
    }

    public Map findSelectedByIdHandler(Class<?> aClass, ID id, String[] fields) {
        Map data = genericDao.findSelectedById(aClass, id, fields);
        if (!data.isEmpty()) {
            return data;
        } else {
            throw new FasteException("unable to find resource for the given id - " + id);
        }
    }

    public Object findSelectedByGivenKeyValueHandler(Class<?> aClass, String[] keys, String[] values, String[] fields, String[] conditions, String[] conditionSeparators, Integer page, Integer size, String resultType, String[] orderBy) {
        List<String> parentheses = new ArrayList<>();
        for (String key : keys) {
            parentheses.add(Parentheses.DEFAULT.getValue());
        }
        Object data = genericDao.findSelectedByGivenKeyValue(aClass, keys, values, fields, conditions, conditionSeparators, page, size, resultType, orderBy, parentheses);
        if (data != null) {
            return data;
        } else {
            throw new FasteException("unable to find resource for the given " + keys + " - " + values);
        }
    }

    public Object advancedSearch(Class<?> aClass, FasteQuery fasteQuery) {
        String[] fields = null;
        if (!fasteQuery.getSelect()[0].equals("*")) {
            fields = fasteQuery.getSelect();
        }
        Integer page = fasteQuery.getPage();
        Integer size = fasteQuery.getSize();
        String resultType = fasteQuery.getResultType();
        String[] orderBy = fasteQuery.getOrderBy();

        List<String> keys = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> conditionSeparators = new ArrayList<>();
        List<String> parentheses = new ArrayList<>();

        AtomicInteger index = new AtomicInteger(0);
        fasteQuery.getLogic().forEach(fasteCondition -> {
            int i = index.getAndIncrement();
            String key = fasteCondition.getWhere();
            keys.add(key);
            String condition = fasteCondition.getCondition();
            checkSupportConditions(condition);
            conditions.add(condition);
            String value = fasteCondition.getValue();
            values.add(value);
            String logicalOperator = fasteCondition.getLogicalOperator();
            if (logicalOperator != null) {
                conditionSeparators.add(logicalOperator);
            }
            if (i == 0 && fasteCondition.getParentheses().equals(Parentheses.CLOSE)) {
                throw new FasteException("Invalid Parentheses usage at first condition. Please use OPEN in first condition");
            }
            parentheses.add(fasteCondition.getParentheses().getValue());
        });

        if (hasCloseWithoutLeadingOpen(parentheses)) {
            throw new FasteException("Invalid Parentheses usage. Please use OPEN before use CLOSE");
        }

        String[] keysArray = keys.stream().allMatch(Objects::isNull) ? null : keys.toArray(new String[0]);
        String[] conditionsArray = conditions.stream().allMatch(Objects::isNull) ? null : conditions.toArray(new String[0]);
        String[] valuesArray = values.stream().allMatch(Objects::isNull) ? null : values.toArray(new String[0]);
        String[] separatorsArray = conditionSeparators.stream().allMatch(Objects::isNull) ? null : conditionSeparators.toArray(new String[0]);

        validateQuery(aClass, fields, keysArray, conditionsArray, separatorsArray);

        Object data = genericDao.findSelectedByGivenKeyValue(aClass, keysArray, valuesArray, fields, conditionsArray, separatorsArray, page, size, resultType, orderBy, parentheses);
        if (data != null) {
            return data;
        } else {
            throw new FasteException("unable to find resource for the given " + keys + " - " + values);
        }
    }

    public static boolean hasCloseWithoutLeadingOpen(List<String> list) {
        boolean seenX = false;

        for (String item : list) {
            if (item.equals("(")) {
                seenX = true; // Mark that we've seen an "X"
            } else if (item.equals(")") && !seenX) {
                return true; // "Y" found before any "X"
            }
        }
        return false; // No "Y" found before an "X"
    }

    private void validateQuery(Class<?> aClass, String[] fields, String[] keysArray, String[] conditionsArray, String[] separatorsArray) {
        checkQueryFields(fields, aClass);
        checkQueryConditionsWithSeparators(conditionsArray, separatorsArray);
    }

    private void checkSupportConditions(String condition) {
        Set<String> systemSupportedConditions = JpaDaoSupport.conditionMap.keySet();
        boolean isAvailable = systemSupportedConditions.contains(condition);
        if (!isAvailable) {
            throw new FasteException("Unsupported condition [" + condition + "] found. System support for " + systemSupportedConditions);
        }
    }

    private void checkQueryConditionsWithSeparators(String[] conditionsArray, String[] separatorsArray) {
        if (separatorsArray != null) {
            if (conditionsArray.length == 1 && separatorsArray.length == 1) {
                throw new FasteException("Please remove logicalOperator");
            }

            if (separatorsArray.length != conditionsArray.length - 1) {
                throw new FasteException("If you have N \"where\" clauses, you would use (N-1) logical operators to connect them.");
            }
        }
    }

    private void checkQueryFields(String[] fields, Class<?> classType) {
        if (fields != null) {
            for (String field : fields) {
                validateSearchFields(classType, field);
            }
        }
    }

    private void validateSearchFields(Class<?> classType, String field) {
        validateKeys(field, classType);
        for (Field declaredField : classType.getDeclaredFields()) {
            String k = String.valueOf(field);
            if (k.contains(".")) {
                String[] parts = k.split("\\.");
                validateKeys(parts[0], classType);
                if (declaredField.getName().equals(parts[0])) {
                    if (!declaredField.getType().getPackage().getName().startsWith("java.")) {
                        String replace = k.replace(parts[0] + ".", "");
                        Class<?> aClass = resourceConverter.convertResourceToActualEntity(declaredField.getType().getSimpleName().toLowerCase());
                        validateSearchFields(aClass, replace);
                    }
                }
            } else {
                if (declaredField.getName().equals(field)) {
                    break;
                }
            }
        }
    }


}
