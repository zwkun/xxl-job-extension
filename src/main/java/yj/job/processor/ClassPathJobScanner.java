
package yj.job.processor;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import yj.job.annotation.JobComponent;

/**
 * 类路径任务扫描
 *
 * @author zwk
 * @date 2023/6/8 9:34
 */
public class ClassPathJobScanner {

    private final BeanDefinitionRegistry registry;

    private final Environment environment;

    public ClassPathJobScanner(BeanDefinitionRegistry registry, Environment environment) {
        this.registry = registry;
        this.environment = environment;

    }

    /**
     * 扫描包路径下的任务
     *
     * @param pkg
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 9:36
     */
    public void scan(String pkg) {
        try {
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    resolveBasePackage(pkg) + '/' + "**/*.class";
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
            CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
            for (Resource resource : resources) {
                MetadataReader metadataReader = readerFactory.getMetadataReader(resource);
                ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                sbd.setSource(resource);
                //待用JobComponent注解
                if (isCandidateComponent(sbd)) {
                    //注册job
                    registerJob(sbd.getBeanClassName());
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("扫描job失败", ex);
        }
    }

    /**
     * 注册任务
     *
     * @param originClassName
     * @author zwk
     * @date 2023/6/8 9:51
     */
    private void registerJob(String originClassName) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(originClassName);
        String property = environment.getProperty("self.xxljob.scan.generate.source", "false");
        if (Boolean.parseBoolean(property)) {
            new JobGenerator(environment).generateJobSource(clazz);
        } else {
            Class<?> jobClass = new JobGenerator(environment).generateJobClass(clazz);
            registry.registerBeanDefinition(jobClass.getName(), new RootBeanDefinition(jobClass));
        }
    }

    /**
     * 是否是任务候选组件
     *
     * @param beanDefinition
     * @return boolean
     * @throws
     * @author zwk
     * @date 2023/6/8 9:52
     */
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isAnnotated(JobComponent.class.getName());
    }

    /**
     * 例如：yj.job -> yj/job
     *
     * @param basePackage
     * @return java.lang.String
     * @throws
     * @author zwk
     * @date 2023/6/8 9:52
     */
    protected String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(basePackage);
    }

}
