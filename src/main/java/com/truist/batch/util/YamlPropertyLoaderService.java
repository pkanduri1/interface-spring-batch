package com.truist.batch.util;

import java.util.Properties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

public class YamlPropertyLoaderService {

    /**
     * Loads properties from a YAML file based on the provided jobType and
     * sourceSystem.
     *
     * @param jobType the job type (e.g., "p327", "atoctran")
     * @param sourceSystem the source system name (e.g., "shaw", "hr")
     * @return a Map representing the properties loaded from the YAML file
     * @throws RuntimeException if the file cannot be found or loaded
     */
    public static Properties loadProperties(String jobType, String sourceSystem) {
        String filePath = String.format("%s/%s/%s-%s.yml", jobType.toLowerCase(), sourceSystem.toLowerCase(), jobType.toLowerCase(), sourceSystem.toLowerCase());
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(filePath));
        Properties properties = yaml.getObject();
        if (null == properties || properties.isEmpty()) {
            throw new RuntimeException("Failed to laod Properties from: " + filePath);
        }
        return properties;
    }
}
