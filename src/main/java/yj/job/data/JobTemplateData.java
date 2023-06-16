package yj.job.data;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/6/16 14:44
 */

public class JobTemplateData {
    private String methodName;
    private String jobName;
    private String jobMethodName;
    private boolean needRetry;
    private String popKey;
    private String pushKey;
    private String doneKey;
    private int doneCount;
    private XxlRegisterData registerData;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobMethodName() {
        return jobMethodName;
    }

    public void setJobMethodName(String jobMethodName) {
        this.jobMethodName = jobMethodName;
    }

    public boolean isNeedRetry() {
        return needRetry;
    }

    public void setNeedRetry(boolean needRetry) {
        this.needRetry = needRetry;
    }

    public String getPopKey() {
        return popKey;
    }

    public void setPopKey(String popKey) {
        this.popKey = popKey;
    }

    public String getPushKey() {
        return pushKey;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }

    public String getDoneKey() {
        return doneKey;
    }

    public void setDoneKey(String doneKey) {
        this.doneKey = doneKey;
    }

    public int getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
    }

    public XxlRegisterData getRegisterData() {
        return registerData;
    }

    public void setRegisterData(XxlRegisterData registerData) {
        this.registerData = registerData;
    }
}
