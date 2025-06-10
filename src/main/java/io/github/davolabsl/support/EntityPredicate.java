package io.github.davolabsl.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author - Shehara
 * @date - 1/25/2022
 */

@Component
public class EntityPredicate {

    @Autowired
    private EntityManager em;

    public Optional<? extends Class<?>> predicateClass(String resource) {
        Set<EntityType<?>> entities = em.getMetamodel().getEntities();
        Optional<EntityType<?>> entity = entities.stream().filter(entityType -> entityType.getName().equalsIgnoreCase(resource)).findFirst();
        Predicate<Class> contentEquals = resourceName -> resourceName.getName().contentEquals(entity.get().getJavaType().getName());
        Predicate<Class> equalsIgnoreCase = resourceName -> resourceName.getName().equalsIgnoreCase(entity.get().getJavaType().getName());
        Optional<? extends Class<?>> optionalClass = entities.stream().map(entityType -> entityType.getJavaType()).filter(contentEquals.or(equalsIgnoreCase)).findFirst();
        return optionalClass;
    }
}
