package yj.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import yj.utils.UtilsRedis;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/5/31 16:06
 */

public abstract class AbstractJob {
    @Autowired
    private UtilsRedis utilsRedis;

    @Value("${job.redis.key.expires:1800}")
    private Long expires;

    /**
     * 任务队列中弹出值
     *
     * @param key
     * @return java.lang.String
     * @throws
     * @author zwk
     * @date 2023/6/8 10:38
     */
    public String popKey(String key) {
        return (String) utilsRedis.pop(key);
    }

    /**
     * 放入任务队列
     *
     * @param key
     * @param v
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:38
     */
    public void leftPush(String key, Object v) {
        if (v == null) {
            return;
        }
        Set<String> keys = getKeys(key);
        for (String s : keys) {
            if (v instanceof String) {
                utilsRedis.leftPushAll(s, expires, Collections.singletonList((String) v));
            } else if (v instanceof List) {
                if (((List<?>) v).isEmpty()) {
                    return;
                }
                utilsRedis.leftPushAll(s, expires, (List<String>) v);
            }
        }
    }

    /**
     * 逗号分割key
     *
     * @param key
     * @return java.util.List<java.lang.String>
     * @throws
     * @author zwk
     * @date 2023/6/8 10:42
     */
    private Set<String> getKeys(String key) {
        if (key == null) {
            return Collections.emptySet();
        }
        if (key.contains(",")) {
            String[] splits = key.split(",");
            return Stream.of(splits).map(String::trim).filter(s -> s.length() > 0).collect(Collectors.toSet());
        }
        return Collections.singleton(key);
    }

    /**
     * 任务是否完成
     *
     * @param key
     * @param finishCount
     * @return boolean
     * @throws
     * @author zwk
     * @date 2023/6/8 10:39
     */
    public boolean jobDone(String key, String v, int finishCount) {
        if (key == null) {
            return false;
        }
        finishCount = finishCount - 1;
        Long count = utilsRedis.getIncr(key + v, expires);
        return count != null && count == finishCount;
    }

}
