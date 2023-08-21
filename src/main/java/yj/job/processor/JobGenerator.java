package yj.job.processor;

import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import org.objectweb.asm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import yj.job.JobException;
import yj.job.annotation.JobDescription;
import yj.utils.UtilsJackSon;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * 任务生成器
 *
 * @author zwk
 * @version 1.0
 * @date 2023/5/31 15:27
 */

public class JobGenerator {
    /**
     * 父类名称
     */
    private static final String SUPER_JOB = "yj/job/AbstractJob";
    /**
     * 任务字段名
     */
    private static final String JOB_FIELD_NAME = "job";
    private static final Method defineClassMethod;
    private final Environment environment;
    private final AtomicInteger index = new AtomicInteger(1);
    /**
     * 父级任务和子级任务
     */
    private Map<Method, List<Method>> parentAndChildren = new HashMap<>();
    /**
     * 子级任务和父级任务
     */
    private Map<Method, Method> childAndParent = new HashMap<>();
    /**
     * 聚合父节点和子节点
     */
    private Map<Method, Method> aggregateAndChild = new HashMap<>();
    /**
     * 聚合子节点和父节点
     */
    private Map<Method, List<Method>> aggregateAndParent = new HashMap<>();

    public JobGenerator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 生成job class
     *
     * @param originClass
     * @return java.lang.Class<?>
     * @author zwk
     * @date 2023/6/8 9:56
     */
    public Class<?> generateJobClass(Class<?> originClass) {
        //找到所有的public但不是static的任务
        Set<Method> methods = MethodIntrospector.selectMethods(
                originClass,
                (ReflectionUtils.MethodFilter) m -> (
                        Modifier.isPublic(m.getModifiers())
                                && !Modifier.isStatic(m.getModifiers())));
        //原始类名
        String internalName = Type.getInternalName(originClass);
        //分析依赖关系
        analysisDependency(methods);
        generateDependencyJson(internalName);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        //生成类名
        String className = internalName + "$Job$";
        cw.visit(V1_8, ACC_PUBLIC , className, null, SUPER_JOB, null);
        visitAnnotation(cw);
        //添加字段
        visitField(cw, originClass);
        //添加构造器
        visitConstructor(cw);
        //添加方法
        visitMethods(cw, methods, className, originClass);
        byte[] bytes = cw.toByteArray();
        //生成类是否保存到文件
        String path = System.getProperty("save.job.generate.class.path");
        if (path != null) {
            File file = new File(path + className + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //加载class
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return (Class<?>) defineClassMethod.invoke(classLoader, className.replace("/", "."), bytes, 0, bytes.length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateDependencyJson(String internalName) {
        Map<String, Map<String, List<String>>> map = new HashMap<>();
        handleJson(map, parentAndChildren, "parentAndChildren");
        handleJson(map, aggregateAndParent, "aggregateAndParent");


        String path = System.getProperty("save.job.generate.class.path");
        if (path != null) {
            File file = new File(path + internalName + ".json");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                String json = UtilsJackSon.objToJson(map);
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void handleJson(Map<String, Map<String, List<String>>> map, Map<Method, List<Method>> aggregateAndParent, String key) {
        if (!aggregateAndParent.isEmpty()) {
            Map<String, List<String>> aap = new HashMap<>();
            for (Map.Entry<Method, List<Method>> entry : aggregateAndParent.entrySet()) {
                Method aggregate = entry.getKey();
                aap.put(getJobName(aggregate), entry.getValue().stream().map(this::getJobName).collect(Collectors.toList()));
            }
            map.put(key, aap);
        }
    }

    private void visitAnnotation(ClassWriter cw) {
        cw.visitAnnotation(Type.getDescriptor(Component.class), true);
    }

    /**
     * 分析依赖关系
     *
     * @param methods
     * @return void
     * @author zwk
     * @date 2023/6/8 9:59
     */
    private void analysisDependency(Set<Method> methods) {
        //任务名称和任务方法
        Map<String, Method> map = new HashMap<>();
        //父级任务名称和子级任务方法
        Map<String, List<Method>> children = new HashMap<>();
        Map<String, List<Method>> aggregate = new HashMap<>();
        for (Method method : methods) {
            //任务名称
            String jobName = getJobName(method);
            JobDescription jd = method.getAnnotation(JobDescription.class);

            map.put(jobName, method);

            if (jd != null && !"".equals(jd.parent())) {
                children.computeIfAbsent(jd.parent(), k -> new ArrayList<>()).add(method);
            }
            if (jd != null && !"".equals(jd.aggregate())) {
                aggregate.computeIfAbsent(jd.aggregate(), k -> new ArrayList<>()).add(method);
            }
        }
        for (Map.Entry<String, List<Method>> entry : children.entrySet()) {
            String parentName = entry.getKey();
            List<Method> ms = entry.getValue();
            Method method = map.get(parentName);
            //父级任务必须存在
            if (method == null) {
                throw new RuntimeException("parent job: `" + parentName + "` does not exists");
            }
            if (method.getReturnType() == Void.TYPE) {
                throw new RuntimeException("parent job: `" + parentName + "` return type must not be void");
            }
            //父子级任务
            parentAndChildren.put(method, ms);
            for (Method m : ms) {
                int parameterCount = m.getParameterCount();
                if (parameterCount != 1) {
                    throw new RuntimeException("child job: `" + getJobName(m) + "` parameter count must be 1");
                }
                if (m.getParameterTypes()[0] != String.class) {
                    throw new RuntimeException("child job: `" + getJobName(m) + "` parameter type must be String");
                }
                //子父级任务
                childAndParent.put(m, method);
            }
        }
        for (Map.Entry<String, List<Method>> entry : aggregate.entrySet()) {
            String aggregateName = entry.getKey();
            List<Method> ms = entry.getValue();
            Method method = map.get(aggregateName);
            //聚合节点任务必须存在
            if (method == null) {
                throw new RuntimeException("aggregate job: `" + aggregateName + "` does not exists ");
            }
            int parameterCount = method.getParameterCount();
            if (parameterCount != 1) {
                throw new RuntimeException("aggregate job: `" + aggregateName + "` parameter count must be 1");
            }
            if (method.getParameterTypes()[0] != String.class) {
                throw new RuntimeException("aggregate job: `" + aggregateName + "` parameter type must be String");
            }
            aggregateAndParent.put(method, ms);
            for (Method m : ms) {
                if (m.getReturnType() == Void.TYPE) {
                    throw new RuntimeException("aggregate parent job:`" + getJobName(m) + " return type must not be void");
                }
                aggregateAndChild.put(m, method);
            }
        }
    }


    private String getJobName(Method method) {
        JobDescription jd = method.getAnnotation(JobDescription.class);
        if (jd == null || "".equals(jd.name())) {
            return method.getName();
        }
        return jd.name();
    }

    /**
     * visitMethods
     *
     * @param cw
     * @param methods
     * @param className
     * @param originClass
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:17
     */
    private void visitMethods(ClassWriter cw, Set<Method> methods, String className, Class<?> originClass) {
        for (Method method : methods) {
            visitMethod(cw, method, className, originClass);
        }
    }

    /**
     * visitMethod
     *
     * @param cw
     * @param method
     * @param className
     * @param originClass
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:17
     */
    private void visitMethod(ClassWriter cw, Method method, String className, Class<?> originClass) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, method.getName() + "$Method$" + index.getAndIncrement(), "()V", null, null);
        //添加注解
        visitAnnotation(mv, method);
        //添加方法体
        visitMethodCode(mv, method, className, originClass);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void visitMethodCode(MethodVisitor mv, Method method, String className, Class<?> originClass) {
        JobDescription jd = method.getAnnotation(JobDescription.class);
        //方法参数不能多于1个
        int count = method.getParameterCount();
        if (count > 1) {
            throw new RuntimeException("job parameter count too much,maybe 0 or 1");
        }
        //方法参数必须是string
        if (count == 1) {
            Class<?> parameterType = method.getParameterTypes()[0];
            if (parameterType != String.class) {
                throw new RuntimeException("job parameter type must be String");
            }
        }

        //是否需要重试
        boolean needRetry = (jd != null && !"".equals(jd.parent()) && count == 1) || aggregateAndParent.containsKey(method);
        mv.visitCode();

        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label ret = new Label();
        //需要重试添加try-catch
        if (needRetry) {
            mv.visitTryCatchBlock(start, end, handler, Type.getInternalName(JobException.class));
        }
        String jobInKey = null;
        //方法参数个数不是0
        if (count != 0) {

            if (jd != null && !"".equals(jd.parent()) || aggregateAndParent.containsKey(method)) {
                //从队列中弹出值到栈上
                mv.visitVarInsn(ALOAD, 0);
                jobInKey = getJobKey(originClass, method);
                mv.visitLdcInsn(jobInKey);
                mv.visitMethodInsn(INVOKEVIRTUAL, SUPER_JOB, "popKey", "(Ljava/lang/String;)Ljava/lang/String;", false);
            } else {
                //添加null到栈上
                mv.visitInsn(ACONST_NULL);
            }
            //保存的局部变量表1的位置
            mv.visitVarInsn(ASTORE, 1);
        }
        if (needRetry) {
            mv.visitLabel(start);
        }
        mv.visitVarInsn(ALOAD, 0);

        //获取任务对象
        mv.visitFieldInsn(GETFIELD, className, JOB_FIELD_NAME, Type.getDescriptor(originClass));
        if (count != 0) {
            mv.visitVarInsn(ALOAD, 1);
        }
        //执行任务方法
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(originClass), method.getName(), Type.getMethodDescriptor(method), false);

        //如果是父级任务，收集子级的任务key
        if (parentAndChildren.containsKey(method)) {
            List<Method> methods = parentAndChildren.get(method);
            String keys = methods.stream().map(m -> getJobKey(originClass, m)).collect(Collectors.joining(","));
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(keys);
            mv.visitVarInsn(ALOAD, 2);
            //执行leftPush方法
            mv.visitMethodInsn(INVOKEVIRTUAL, SUPER_JOB, "leftPush", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            //如果是聚合父节点
        } else if (aggregateAndChild.containsKey(method)) {
            Method child = aggregateAndChild.get(method);
            String jobKey = getJobKey(originClass, child);

            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(jobKey + ":done:");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitIntInsn(SIPUSH, aggregateAndParent.get(child).size());
            //判断子级任务是否全部执行完
            mv.visitMethodInsn(INVOKEVIRTUAL, SUPER_JOB, "jobDone", "(Ljava/lang/String;Ljava/lang/String;I)Z", false);
            Label label = new Label();
            mv.visitJumpInsn(IFEQ, label);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(jobKey);
            mv.visitVarInsn(ALOAD, 2);
            //子级任务全部执行完，执行leftPush方法
            mv.visitMethodInsn(INVOKEVIRTUAL, SUPER_JOB, "leftPush", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitLabel(label);
            mv.visitFrame(F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);

            //其他类型
        } else if (method.getReturnType() != Void.TYPE) {
            //不需要保存，弹栈
            mv.visitInsn(POP);
        }
        if (needRetry) {
            mv.visitLabel(end);
            mv.visitJumpInsn(GOTO, ret);
            //捕获异常后把从队列中弹出的值放回到队列中
            mv.visitLabel(handler);
            if (aggregateAndChild.containsKey(method)) {
                mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"yj/job/JobException"});
            } else {
                mv.visitFrame(F_FULL, 2, new Object[]{className, "java/lang/String"}, 1, new Object[]{"yj/job/JobException"});
            }
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(jobInKey);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, SUPER_JOB, "leftPush", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitLabel(ret);
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
    }

    private String getJobKey(Class<?> originClass, Method method) {
        String name = originClass.getName();
        name = name.replace(".", ":") + ":";
        name = name + method.getName();
        if (method.getParameterCount() > 0) {
            name = name + ":" + method.getParameterTypes()[0].getSimpleName();
        }
        return name;
    }

    /**
     * 添加注解
     *
     * @param mv
     * @param method
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:35
     */
    private void visitAnnotation(MethodVisitor mv, Method method) {
        JobDescription jd = method.getAnnotation(JobDescription.class);
        XxlRegister annotation = method.getAnnotation(XxlRegister.class);
        String jobName = method.getName();
        if (jd != null && !"".equals(jd.name())) {
            jobName = jd.name();
        }
        String cron = "0/10 * * * * ?";
        String author = "以见科技";
        String jobDesc = jobName;
        String jobGroup = "";
        String jobGroupTitle = "";
        String executorRouteStrategy = "ROUND";
        int triggerStatus = 0;
        int jobGroupAddressType = 0;
        if (annotation != null) {
            cron = annotation.cron();
            author = annotation.author();
            jobDesc = annotation.jobDesc();
            jobGroup = annotation.jobGroup();
            jobGroupTitle = annotation.jobGroupTitle();
            executorRouteStrategy = annotation.executorRouteStrategy();
            triggerStatus = annotation.triggerStatus();
            jobGroupAddressType = annotation.jobGroupAddressType();
        }
        //添加 XxlRegister 注解
        AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(XxlRegister.class), true);

        //jobGroup 为空则从配置文件中取值
        if ("".equals(jobGroup)) {
            jobGroup = environment.getProperty("xxl.job.executor.appname");
        }
        //jobGroupTitle 为空则从配置文件中取值
        if ("".equals(jobGroupTitle)) {
            jobGroupTitle = environment.getProperty("xxl.job.executor.title");
        }
        av.visit("cron", cron);
        av.visit("author", author);
        av.visit("jobDesc", jobDesc);
        av.visit("jobGroup", jobGroup);
        av.visit("jobGroupTitle", jobGroupTitle);
        av.visit("executorRouteStrategy", executorRouteStrategy);
        av.visit("triggerStatus", triggerStatus);
        av.visit("jobGroupAddressType", jobGroupAddressType);
        av.visitEnd();

        //添加 XxlJob 注解
        av = mv.visitAnnotation(Type.getDescriptor(XxlJob.class), true);
        av.visit("value", jobName);
        av.visitEnd();

    }

    /**
     * 添加构造器
     *
     * @param cw
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:37
     */
    private void visitConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER_JOB, "<init>", "()V", false);
        mv.visitMaxs(1, 1);
        mv.visitInsn(RETURN);
        mv.visitEnd();
    }


    /**
     * 添加字段
     *
     * @param cw
     * @param originClass
     * @return void
     * @throws
     * @author zwk
     * @date 2023/6/8 10:38
     */
    private void visitField(ClassWriter cw, Class<?> originClass) {
        String descriptor = Type.getDescriptor(originClass);
        FieldVisitor fv = cw.visitField(ACC_PRIVATE, JOB_FIELD_NAME, descriptor, null, null);
        fv.visitAnnotation(Type.getDescriptor(Autowired.class), true);
        fv.visitEnd();
    }

    static {
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClassMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
