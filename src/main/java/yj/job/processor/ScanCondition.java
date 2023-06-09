package yj.job.processor;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/6/9 14:07
 */

public class ScanCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        boolean jobEnable = environment.getProperty("self.xxljob.enable", boolean.class, false);
        boolean scanEnable = environment.getProperty("self.xxljob.scan.enable", boolean.class, false);
        return jobEnable && scanEnable;
    }
}
