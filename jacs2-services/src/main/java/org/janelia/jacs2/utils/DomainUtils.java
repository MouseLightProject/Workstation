package org.janelia.jacs2.utils;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.jacs2.model.BaseEntity;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.MongoMapping;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceEvent;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public class DomainUtils {
    /**
     * @param subjectKey
     * @return the subject name part of a given subject key. For example, for "group:flylight", this returns "flylight".
     */
    public static String getNameFromSubjectKey(String subjectKey) {
        if (StringUtils.isBlank(subjectKey)) {
            return null;
        }
        List<String> subjectKeyComponents = getSubjectKeyComponents(subjectKey);
        return subjectKeyComponents.get(1);
    }

    /**
     * @param subjectKey
     * @return the type part of the given subject key. For example, for "group:flylight", this returns "group".
     */
    public static String getTypeFromSubjectKey(String subjectKey) {
        if (StringUtils.isBlank(subjectKey)) {
            return null;
        }
        List<String> subjectKeyComponents = getSubjectKeyComponents(subjectKey);
        return subjectKeyComponents.get(0);
    }

    private static List<String> getSubjectKeyComponents(String subjectKey) {
        List<String> subjectKeyComponents = Splitter.on(':').trimResults().splitToList(subjectKey);
        if (subjectKeyComponents.size() != 2) {
            throw new IllegalArgumentException("Invalid subject key '" + subjectKey + "' - expected format <type>:<name>");
        }
        return subjectKeyComponents;
    }

    public static boolean isAdminOrUndefined(Subject subject) {
        return subject == null || subject.isAdmin();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericParameterType(Class<?> parameterizedClass, int paramIndex) {
        return (Class<T>)((ParameterizedType)parameterizedClass.getGenericSuperclass()).getActualTypeArguments()[paramIndex];
    }

    public static Class<? extends BaseEntity> getEntityClass(String entityType) {
        Reflections entityReflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.janelia"))
                .filterInputsBy(new FilterBuilder().includePackage(BaseEntity.class).includePackage(DomainObject.class)));
        for (Class<? extends BaseEntity> clazz : entityReflections.getSubTypesOf(BaseEntity.class)) {
            if (clazz.getSimpleName().equals(entityType) || clazz.getName().equals(entityType)) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("Unsupported or unknown entityType: " + entityType);
    }

    public static Class<?> getBasePersistedEntityClass(String entityType) {
        Class<?> entityClass = getEntityClass(entityType);
        MongoMapping mongoMapping = null;
        for(Class<?> clazz = entityClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz.isAnnotationPresent(MongoMapping.class)) {
                return clazz; // first class encountered going up the hierarchy that has a MongoMapping annotation
            }
        }
        // if no annotated class was found assume the current class is the one being persisted.
        return entityClass;
    }

    public static MongoMapping getMapping(Class<?> objectClass) {
        MongoMapping mongoMapping = null;
        for(Class<?> clazz = objectClass; clazz != null; clazz = clazz.getSuperclass()) {
            if (clazz.isAnnotationPresent(MongoMapping.class)) {
                mongoMapping = clazz.getAnnotation(MongoMapping.class);
                break;
            }
        }
        return mongoMapping;
    }

    public static void setFileType(HasFiles objWithFiles, FileType fileType, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            objWithFiles.getFiles().remove(fileType);
        } else {
            objWithFiles.getFiles().put(fileType, fileName);
        }
    }

}