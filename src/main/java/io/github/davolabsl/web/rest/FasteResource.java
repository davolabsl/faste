package io.github.davolabsl.web.rest;

import io.github.davolabsl.dao.support.FastPage;
import io.github.davolabsl.support.RequestHandler;
import io.github.davolabsl.support.ResourceConverter;
import io.github.davolabsl.web.rest.dto.FasteQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Generic resource.
 *
 * @param <T>  the type parameter
 * @param <ID> the type parameter
 */
@RestController
@RequestMapping("/faste")
@ConditionalOnProperty(value = "faste.rest.enabled")
public class FasteResource<T, ID extends Serializable, K, V> {

    @Autowired
    private RequestHandler requestHandler;

    @Autowired
    private ResourceConverter resourceConverter;

    /**
     * Save response entity.
     *
     * @param resource the resource
     * @param entity   the entity
     * @return the response entity
     */
    @PostMapping("/{resource}/save")
    public ResponseEntity<T> save(@PathVariable("resource") String resource, @RequestBody T entity) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.saveHandler(aClass, entity);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Save all response entity.
     *
     * @param resource the resource
     * @param entities the entities
     * @return the response entity
     */
    @PostMapping("/{resource}/save/all")
    public ResponseEntity<T> saveAll(@PathVariable("resource") String resource, @RequestBody T entities) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.saveAllHandler(aClass, entities);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Find by id response entity.
     *
     * @param resource the resource
     * @param id       the id
     * @return the response entity
     */
    @GetMapping("/{resource}/find/{id}")
    public ResponseEntity<T> findById(@PathVariable("resource") String resource, @PathVariable("id") ID id) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.findByIdHandler(aClass, id);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Find by reference response entity.
     *
     * @param resource    the resource
     * @param association the association
     * @param id          the id
     * @return the response entity
     */
    @GetMapping("/{resource}/find/by/association/{association}/{id}")
    public ResponseEntity<T> findByAssociation(@PathVariable("resource") String resource, @PathVariable("association") String association, @PathVariable("id") ID id) {
        Class<?> mainEntity = resourceConverter.convertResourceToActualEntity(resource);
        Class<?> referenceEntity = resourceConverter.convertResourceToActualEntity(association);
        T response = (T) requestHandler.findByReferenceIdHandler(mainEntity, referenceEntity, id);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Find all response entity.
     *
     * @param resource the resource
     * @return the response entity
     */
    @GetMapping("/{resource}/find/all")
    public ResponseEntity<T> findAll(@PathVariable("resource") String resource) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.findAllHandler(aClass);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Paginate response entity.
     *
     * @param resource the resource
     * @param page     the page
     * @param size     the size
     * @return the response entity
     */
    @GetMapping("/{resource}/paginate")
    public ResponseEntity<FastPage> paginate(@PathVariable("resource") String resource, @RequestParam("page") ID page, @RequestParam("size") ID size) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        FastPage fastPageResponse = requestHandler.paginateHandler(aClass, page, size);
//        Map<String, Object> data = new HashMap<>();
//        data.put("data", response);
        return new ResponseEntity(fastPageResponse, HttpStatus.OK);
    }

    /**
     * Find all by ids response entity.
     *
     * @param resource the resource
     * @param ids      the ids
     * @return the response entity
     */
    @GetMapping("/{resource}/find/all/in")
    public ResponseEntity<T> findAllByIds(@PathVariable("resource") String resource, @RequestParam("ids") ID[] ids) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.findAllByIdsHandler(aClass, ids);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Update response entity.
     *
     * @param resource the resource
     * @param entity   the entity
     * @return the response entity
     */
    @PutMapping("/{resource}/update")
    public ResponseEntity<T> update(@PathVariable("resource") String resource, @RequestBody T entity) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.updateHandler(aClass, entity);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Update response entity.
     *
     * @param resource the resource
     * @param entity   the entity
     * @return the response entity
     */
    @PutMapping("/{resource}/dynamic/update/{id}")
    public ResponseEntity<T> updateSelectedFields(@PathVariable("resource") String resource, @PathVariable("id") ID id, @RequestBody T entity) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.dynamicUpdateHandler(aClass, entity, id);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Update all response entity.
     *
     * @param resource the resource
     * @param entities the entities
     * @return the response entity
     */
    @PutMapping("/{resource}/update/all")
    public ResponseEntity<T> updateAll(@PathVariable("resource") String resource, @RequestBody T entities) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        T response = (T) requestHandler.updateAllHandler(aClass, entities);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    /**
     * Delete response entity.
     *
     * @param resource the resource
     * @param entity   the entity
     * @return the response entity
     */
    @DeleteMapping("/{resource}/delete")
    public ResponseEntity<Void> delete(@PathVariable("resource") String resource, @RequestBody T entity) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        requestHandler.deleteHandler(aClass, entity);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Delete by id response entity.
     *
     * @param resource the resource
     * @param id       the id
     * @return the response entity
     */
    @DeleteMapping("/{resource}/delete/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("resource") String resource, @PathVariable("id") ID id) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        requestHandler.deleteByIdHandler(aClass, id);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Delete all response entity.
     *
     * @param resource the resource
     * @return the response entity
     */
    @DeleteMapping("/{resource}/delete/all")
    public ResponseEntity<Void> deleteAll(@PathVariable("resource") String resource) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        requestHandler.deleteAllHandler(aClass);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Find selected fields by id response entity.
     *
     * @param resource the resource
     * @param id       the id
     * @param fields   the fields
     * @return the response entity
     */
    @GetMapping("/{resource}/selected/find/{id}")
    public ResponseEntity<Map> findSelectedFieldsById(@PathVariable("resource") String resource, @PathVariable("id") ID id, @RequestParam("fields") String[] fields) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        Map response = requestHandler.findSelectedByIdHandler(aClass, id, fields);
        Map<String, Object> data = new HashMap<>();
        data.put("result", response);
        return new ResponseEntity(data, HttpStatus.OK);
    }

    @GetMapping("/{resource}/selected/find/by")
    public ResponseEntity<Object> findSelectedByGivenKeyValue(@PathVariable("resource") String resource, @RequestParam("keys") String[] keys, @RequestParam("values") String[] values, @RequestParam(value = "fields", required = false) String[] fields, @RequestParam(value = "conditions", required = false) String[] conditions, @RequestParam(value = "conditionSeparators", required = false) String[] conditionSeparators, @RequestParam(value = "orderBy", required = false) String[] orderBy, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "resultType", required = false) String resultType) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(resource);
        Object response = requestHandler.findSelectedByGivenKeyValueHandler(aClass, keys, values, fields, conditions, conditionSeparators, page, size, resultType, orderBy);
//        Map<String, Object> data = new HashMap<>();
//        data.put("data", response);
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @PostMapping("/advanced-search/{resource}")
    public ResponseEntity<Object> advancedSearch(@RequestBody FasteQuery fasteQuery, @PathVariable("resource") String resource) {
        Class<?> aClass = resourceConverter.convertResourceToActualEntity(fasteQuery.getFrom());
        Object response = requestHandler.advancedSearch(aClass, fasteQuery);
//        Map<String, Object> data = new HashMap<>();
//        data.put("data", response);
        return new ResponseEntity(response, HttpStatus.OK);
    }
}
