package io.github.davolabsl.support;

import io.github.davolabsl.advice.exception.FasteException;
import io.github.davolabsl.web.rest.FasteResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author - Shehara
 * @date - 1/28/2022
 */

@Component
public class SwaggerResourceHandler {

    @Value("${server.port:8080}")
    private String port;


    @Value("${swagger.server-url:}")
    private String swaggerUrl;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Autowired
    private EntityManager em;

    public List<Map> getAllEntityNames() {
        return em.getMetamodel().getEntities().stream().filter(entityType -> entityType.getJavaType().isAnnotationPresent(FasteEntity.class)).map(entityType -> {
            Map<String, Object> objectMap = new HashMap<>();
            FasteEntity fasteAnnotation = entityType.getJavaType().getAnnotation(FasteEntity.class);
            if (fasteAnnotation != null) {
                String mainEntityName = entityType.getName().toLowerCase();
                objectMap.put("mainEntity", mainEntityName);
                objectMap.put("displayName", fasteAnnotation.displayName());
            }
            List<Map<String, Object>> associations = new ArrayList<>();
            for (SingularAttribute<?, ?> singularAttribute : entityType.getSingularAttributes()) {
                if (singularAttribute.isAssociation()) {
                    Map<String, Object> map = new HashMap<>();
                    String association = singularAttribute.getType().getJavaType().getSimpleName().toLowerCase();
                    FasteEntity fasteEntity = singularAttribute.getType().getJavaType().getAnnotation(FasteEntity.class);
                    if (fasteEntity != null) {
                        String displayName = fasteEntity.displayName();
                        map.put("association", association);
                        map.put("displayName", displayName);
                        associations.add(map);
                    }
                }
            }
            objectMap.put("associations", associations);
            return objectMap;
        }).collect(Collectors.toList());
//        return em.getMetamodel().getEntities().stream().map(entityType -> entityType.getName().toLowerCase()).collect(Collectors.toList());
    }

    private Map<Object, Object> getRequestBody(String resource, HttpMethod httpMethod) {
        Map<Object, Object> objectMap = new HashMap<>();
        Optional<EntityType<?>> optionalEntityType = em.getMetamodel().getEntities().stream().filter(entityType -> entityType.getName().toLowerCase().equalsIgnoreCase(resource)).findFirst();
        if (optionalEntityType.isPresent()) {
            EntityType<?> entityType = optionalEntityType.get();
            Arrays.stream(entityType.getJavaType().getDeclaredFields()).forEach(field -> {
                if (field.getType().getCanonicalName().startsWith("java.time") || field.getType().getCanonicalName().startsWith("java.lang") || field.getType().getCanonicalName().startsWith("java.math") || field.getType().getCanonicalName().startsWith("java.util")) {
                    if (!Collection.class.isAssignableFrom(field.getType())) {
                        if (httpMethod.equals(HttpMethod.POST)) {
                            if (!field.isAnnotationPresent(Id.class)) {
                                objectMap.put(field.getName(), field.getType().getSimpleName());
                            }
                        } else {
                            objectMap.put(field.getName(), field.getType().getSimpleName());
                        }

                    }
                } else {
                    int modifiers = field.getModifiers();
                    if (!field.getName().contains("serial") && !(Modifier.isPrivate(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))) {
                        objectMap.put(field.getName(), getForeign(field.getType().getSimpleName().toLowerCase()));
                    }
                }
            });
            return objectMap;
        } else {
            throw new FasteException("Error in entity process");
        }
    }

    private Map<Object, Object> getForeign(String resource) {
        Map<Object, Object> objectMap = new HashMap<>();
        Optional<EntityType<?>> optionalEntityType = em.getMetamodel().getEntities().stream().filter(entityType -> entityType.getName().toLowerCase().equalsIgnoreCase(resource)).findFirst();
        if (optionalEntityType.isPresent()) {
            EntityType<?> entityType = optionalEntityType.get();
            Arrays.stream(entityType.getJavaType().getDeclaredFields()).forEach(field -> {
                if (field.getType().getCanonicalName().startsWith("java.lang") || field.getType().getCanonicalName().startsWith("java.math") || field.getType().getCanonicalName().startsWith("java.util")) {
                    if (field.isAnnotationPresent(Id.class)) {
                        objectMap.put(field.getName(), field.getType().getSimpleName());
                    }
                }
            });
            return objectMap;
        } else {
            throw new FasteException("Error in entity process");
        }
    }

    public List<FasteResourceDetails> getHttpDetails(String resource) {
        List<FasteResourceDetails> fasteResourceDetailsList = new ArrayList<>();
        String baseUrl = "http://%s:%s/faste";
        String baseUrlForSwagger = "%s/faste";
        List<HttpMethodAndValue> resourceMeta = getResourceMeta();
        for (HttpMethodAndValue methodAndValue : resourceMeta) {
            FasteResourceDetails fasteResourceDetails = constructHttpDetail(baseUrl, baseUrlForSwagger, methodAndValue.getValue(), resource, methodAndValue.getHttpMethod(), methodAndValue.getAdditionalHttpInfo());
            fasteResourceDetailsList.add(fasteResourceDetails);
        }
        return fasteResourceDetailsList;
    }

    private List<HttpMethodAndValue> getResourceMeta() {
        List<HttpMethodAndValue> httpMethodAndValues = new ArrayList<>();
        Class<FasteResource> fasteResourceClass = FasteResource.class;
        Method[] declaredMethods = fasteResourceClass.getDeclaredMethods();
        Arrays.stream(declaredMethods).forEach(method -> {
            HttpMethodAndValue httpMethodAndValue = filterAnnotation(method);
            httpMethodAndValues.add(httpMethodAndValue);
        });
        return httpMethodAndValues;
    }

    private HttpMethodAndValue filterAnnotation(Method method) {
        Annotation[] annotations = method.getAnnotations();
        HttpMethodAndValue httpMethodAndValue = new HttpMethodAndValue();
        AdditionalHttpInfo additionalHttpInfo = getAdditionalHttpInfo(method);
        httpMethodAndValue.setAdditionalHttpInfo(additionalHttpInfo);
        for (Annotation annotation : annotations) {
            String prefix = "/%s";
            if (annotation.annotationType().equals(GetMapping.class)) {
                httpMethodAndValue.setValue(prefix + ((GetMapping) annotation).value()[0]);
                httpMethodAndValue.setHttpMethod(HttpMethod.GET);
                break;
            }
            if (annotation.annotationType().equals(PostMapping.class)) {
                httpMethodAndValue.setValue(prefix + ((PostMapping) annotation).value()[0]);
                httpMethodAndValue.setHttpMethod(HttpMethod.POST);
                break;
            }
            if (annotation.annotationType().equals(PutMapping.class)) {
                httpMethodAndValue.setValue(prefix + ((PutMapping) annotation).value()[0]);
                httpMethodAndValue.setHttpMethod(HttpMethod.PUT);
                break;
            }
            if (annotation.annotationType().equals(DeleteMapping.class)) {
                httpMethodAndValue.setValue(prefix + ((DeleteMapping) annotation).value()[0]);
                httpMethodAndValue.setHttpMethod(HttpMethod.DELETE);
                break;
            }
        }
        return httpMethodAndValue;
    }

    private AdditionalHttpInfo getAdditionalHttpInfo(Method method) {
        AdditionalHttpInfo additionalHttpInfo = new AdditionalHttpInfo();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<String> pathVariables = new ArrayList<>();
        List<String> requestParams = new ArrayList<>();
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            if (parameterAnnotation[0].annotationType().equals(PathVariable.class)) {
                if (!((PathVariable) parameterAnnotation[0]).value().equalsIgnoreCase("resource")) {
                    additionalHttpInfo.setApplyPathVariable(true);
                    pathVariables.add(((PathVariable) parameterAnnotation[0]).value());
                }
            }
            if (parameterAnnotation[0].annotationType().equals(RequestBody.class)) {
                additionalHttpInfo.setApplyRequestBody(true);
            }
            if (parameterAnnotation[0].annotationType().equals(RequestParam.class)) {
                additionalHttpInfo.setApplyRequestParam(true);
                requestParams.add(((RequestParam) parameterAnnotation[0]).value());
            }
        }
        additionalHttpInfo.setPathVariables(pathVariables);
        additionalHttpInfo.setRequestParams(requestParams);
        return additionalHttpInfo;
    }


    public FasteResourceDetails constructHttpDetail(String baseUrl, String baseUrlForSwagger, String uri, String resource, HttpMethod httpMethod, AdditionalHttpInfo additionalHttpInfo) {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            FasteResourceDetails frd = new FasteResourceDetails();
            if (!contextPath.isEmpty()) {
                if (!swaggerUrl.isEmpty()) {
                    frd.setAccessUrl(String.format(baseUrlForSwagger + uri, swaggerUrl + contextPath, resource));
                } else {
                    frd.setAccessUrl(String.format(baseUrl + uri, hostAddress, port + contextPath, resource));
                }
            } else {
                if (!swaggerUrl.isEmpty()) {
                    frd.setAccessUrl(String.format(baseUrlForSwagger + uri, swaggerUrl, resource));
                } else {
                    frd.setAccessUrl(String.format(baseUrl + uri, hostAddress, port, resource));
                }
            }
            frd.setHttpMethod(httpMethod);
            Map<String, Object> requestDetail = new HashMap<>();
            if (additionalHttpInfo.isApplyRequestBody()) {
                requestDetail.put("requestBody", getRequestBody(resource, httpMethod));
            }
            if (additionalHttpInfo.isApplyPathVariable()) {
                Map<String, String> pathVariables = new HashMap<>();
                List<String> variables = additionalHttpInfo.getPathVariables();
                for (int i = 0; i < variables.size(); i++) {
                    String pathVariable = variables.get(i);
                    pathVariables.put("pathVariable-" + i, pathVariable);
                }
                requestDetail.put("pathVariables", pathVariables);
            }
            if (additionalHttpInfo.isApplyRequestParam()) {
                Map<String, String> requestParams = new HashMap<>();
                List<String> params = additionalHttpInfo.getRequestParams();
                for (int i = 0; i < params.size(); i++) {
                    String requestParam = params.get(i);
                    requestParams.put("requestParam-" + i, requestParam);
                }
                requestDetail.put("requestParams", requestParams);
            }
            frd.setRequestDetails(requestDetail);
            return frd;
        } catch (Exception e) {
            throw new FasteException("", e);
        }
    }
}
