package yj.job.aop;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.http.client.config.RequestConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import yj.utils.UtilsHttpClient;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/8/14 14:27
 */
@Component
@Aspect
@ConditionalOnProperty(value = "xxl.job.register.enabled", havingValue = "true")
public class JobRegistry {
    private static final Logger log = LoggerFactory.getLogger(JobRegistry.class);

    @Value("${xxl.job.executor.ip:}")
    private String ip;
    @Value("${xxl.job.executor.port}")
    private Integer port;
    @Value("${xxl.job.executor.address:}")
    private String address;
    @Value("${xxl.job.register.url}")
    private String url;

    @Pointcut("@annotation(com.xxl.job.core.handler.annotation.XxlJob) && @annotation(job)")
    public void pointcut(XxlJob job) {
    }

    @Before("pointcut(job)")
    public void before(JoinPoint joinPoint, XxlJob job) {
        try {
            UtilsHttpClient client = new UtilsHttpClient();
            RequestConfig config = client.initRequestConfig(2000, 2000, 2000);
            client.httpPost_restful_json(url, getBody(job), config);
        } catch (Exception e) {
            log.error("注册失败");
        }
    }

    private String getBody(XxlJob job) throws Exception {
        String jobExecutorIp = ip;
        Integer jobExecutorPort = port;
        String jobHandler = job.value();
        if (address != null && address.trim().length() > 0) {
            try {
                URL url = new URL(address);
                jobExecutorIp = url.getHost();
                jobExecutorPort = url.getPort();
            } catch (MalformedURLException e) {
                log.error("xxl job address 格式错误");
            }
        }
        if (jobExecutorIp == null || jobExecutorIp.trim().length() == 0) {
            jobExecutorIp = InetAddress.getLocalHost().getHostAddress();
            ip = jobExecutorIp;
        }
        String format = "{\"jobHandler\":\"%s\",\"jobExecutorIp\":\"%s\",\"jobExecutorPort\":%d}";
        String body = String.format(format, jobHandler, jobExecutorIp, jobExecutorPort);
        return body;
    }
}
