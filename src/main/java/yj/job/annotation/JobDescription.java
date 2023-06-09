package yj.job.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 任务描述
 *
 * @author zwk
 * @date 2023/6/8 9:31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JobDescription {
    /**
     * 任务名称
     *
     * @return java.lang.String
     * @author zwk
     */
    String name() default "";


    /**
     * 父级节点任务名
     *
     * @return java.lang.String
     * @author zwk
     */
    String parent() default "";
    /**
     * 聚合节点任务名
     *
     * @return java.lang.String
     * @author zwk
     */
    String aggregate() default "";

}
