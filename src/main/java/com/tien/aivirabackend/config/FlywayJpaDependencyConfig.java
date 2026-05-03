package com.tien.aivirabackend.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayJpaDependencyConfig {
    private static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";
    private static final String FLYWAY_INITIALIZER = "flywayInitializer";

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!beanFactory.containsBeanDefinition(ENTITY_MANAGER_FACTORY)
                        || !beanFactory.containsBeanDefinition(FLYWAY_INITIALIZER)) {
                    return;
                }
                BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY);
                String[] existingDependsOn = entityManagerFactory.getDependsOn();
                if (existingDependsOn == null || existingDependsOn.length == 0) {
                    entityManagerFactory.setDependsOn(FLYWAY_INITIALIZER);
                    return;
                }
                for (String dependency : existingDependsOn) {
                    if (FLYWAY_INITIALIZER.equals(dependency)) {
                        return;
                    }
                }
                String[] dependsOn = new String[existingDependsOn.length + 1];
                System.arraycopy(existingDependsOn, 0, dependsOn, 0, existingDependsOn.length);
                dependsOn[dependsOn.length - 1] = FLYWAY_INITIALIZER;
                entityManagerFactory.setDependsOn(dependsOn);
            }
        };
    }
}
