package yj.job.data;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/6/16 14:49
 */

public class XxlRegisterData {
    String cron;

    String jobDesc;

    String author;

    String executorRouteStrategy;

    int triggerStatus;

    String jobGroup;

    String jobGroupTitle;

    int jobGroupAddressType;

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobGroupTitle() {
        return jobGroupTitle;
    }

    public void setJobGroupTitle(String jobGroupTitle) {
        this.jobGroupTitle = jobGroupTitle;
    }

    public int getJobGroupAddressType() {
        return jobGroupAddressType;
    }

    public void setJobGroupAddressType(int jobGroupAddressType) {
        this.jobGroupAddressType = jobGroupAddressType;
    }
}
