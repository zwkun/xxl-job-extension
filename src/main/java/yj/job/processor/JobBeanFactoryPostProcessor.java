package yj.job.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * BeanFactory后置处理器
 *
 * @author zwk
 * @version 1.0
 * @date 2023/5/31 14:45
 */
@Component
@ConditionalOnProperty(name = "self.xxljob.enable", havingValue = "true")
public class JobBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private String scanPackage;
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        scanPackage = environment.getProperty("job.scan.package", "yj.job");
        this.environment = environment;
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        new ClassPathJobScanner(registry, environment).scan(scanPackage);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
