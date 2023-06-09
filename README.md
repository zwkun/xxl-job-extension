# Xxl Job Extension

xxl-job扩展
###原理
使用asm字节码技术，动态生成xxl-job的任务处理器
###使用
1. 配置扫描的包`job.scan.package`默认值为`yj.job`,在任务类上添加注解@JobComponent
2. 任务类中的public但非static方法会被用来作为xxl-job的处理器
3. 方法上没有@JobDescription注解，则任务名为方法名，也不存在任何父级任务
4. 方法上有@JobDescription注解，可以使用`name`属性指定任务名称，
   1. 配置`parent`属性，则会查找`parent`属性指定的父级任务，父级任务的返回值必须是`String`或`List<String>`类型，当前任务的参数个数必须是一个并且类型是`String`
   2. 父级任务会收集所有的子级任务，把返回值分发到每个子级任务的队列中
   3. 配置`aggregate`属性，则会查找`aggregate`属性指定的聚合任务，聚合任务的参数个数必须是一个并且类型是`String`，当前任务的返回值类型必须是`String`
   4. 所有配置相同`aggregate`属性的的任务，会判断，是否任务已全部完成，如果完成会把返回值分派到`aggregate`指定的聚合任务的队列中
###限制
1. 任务依赖及任务聚合只能在同一个类中
2. 所有配置相同`aggregate`属性的的任务同一批次的返回值必须相同
###支持的任务类型
![img.png](img.png)
###### example
原始类
```java
package yj.job;

import yj.job.annotation.JobComponent;
import yj.job.annotation.JobDescription;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/5/31 13:48
 */
@JobComponent
public class TestJob {


   //---------------------------------单节点-start----------------------------------------------------------------------
   public void single() {
      System.out.println("single");
   }

   //---------------------------------单节点-end------------------------------------------------------------------------
   //---------------------------------串行-start------------------------------------------------------------------------
   //top
   public String top() {
      System.out.println("top");
      return "top";
   }

   //second
   @JobDescription(parent = "top")
   public String second(String v) {
      System.out.println("second");
      System.out.println("v = " + v);
      return "second";
   }

   //third
   @JobDescription(parent = "second")
   public String third(String v) {
      System.out.println("third");
      System.out.println("v = " + v);
      return "third";
   }

   //last
   @JobDescription(parent = "third")
   public void last(String v) {
      System.out.println("last");
      System.out.println("v = " + v);
   }
   //---------------------------------串行-end--------------------------------------------------------------------------
   //---------------------------------循环-start------------------------------------------------------------------------

   //last
   @JobDescription(parent = "circulate2")
   public String circulate1(String v) {
      System.out.println("circulate1");
      System.out.println("v = " + v);
      return "circulate1";
   }

   //last
   @JobDescription(parent = "circulate1")
   public String circulate2(String v) {
      System.out.println("circulate2");
      System.out.println("v = " + v);
      return "circulate2";
   }

   //---------------------------------循环-end--------------------------------------------------------------------------
   //---------------------------------广播-start------------------------------------------------------------------------
   public String broadcast() {
      System.out.println("broadcast");
      return "123";
   }

   @JobDescription(parent = "broadcast")
   public void broadcastChild1(String v) {
      System.out.println("broadcastChild1");
      System.out.println("v = " + v);

   }

   @JobDescription(parent = "broadcast")
   public void broadcastChild2(String v) {
      System.out.println("broadcastChild2");
      System.out.println("v = " + v);

   }

   @JobDescription(parent = "broadcast")
   public void broadcastChild3(String v) {
      System.out.println("broadcastChild3");
      System.out.println("v = " + v);

   }

   //---------------------------------广播-end--------------------------------------------------------------------------
   //---------------------------------广播+聚合-start--------------------------------------------------------------------
   public String broadcastAndAggregate() {
      System.out.println("broadcastAndAggregate");
      return "123";
   }

   @JobDescription(parent = "broadcastAndAggregate", aggregate = "aggregate")
   public String broadcastAndAggregateChild1(String v) {
      System.out.println("broadcastAndAggregateChild1");
      System.out.println("v = " + v);
      return "done";
   }

   @JobDescription(parent = "broadcastAndAggregate", aggregate = "aggregate")
   public String broadcastAndAggregateChild2(String v) {
      System.out.println("broadcastAndAggregateChild2");
      System.out.println("v = " + v);
      return "done";
   }

   @JobDescription(parent = "broadcastAndAggregate", aggregate = "aggregate")
   public String broadcastAndAggregateChild3(String v) {
      System.out.println("broadcastAndAggregateChild3");
      System.out.println("v = " + v);
      return "done";
   }

   public void aggregate(String v) {
      System.out.println("aggregate");
      System.out.println("v = " + v);
   }
   //---------------------------------广播+聚合-end----------------------------------------------------------------------

}

```
生成类
```java

package yj.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class TestJob$Job$ extends AbstractJob {
   @Autowired
   private TestJob job;

   public TestJob$Job$() {
   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "last",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("last")
   public void last$Method$1() {
      String var1 = this.popKey("yj:job:TestJob:last:String");

      try {
         this.job.last(var1);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:last:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "second",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("second")
   public void second$Method$2() {
      String var1 = this.popKey("yj:job:TestJob:second:String");

      try {
         String var2 = this.job.second(var1);
         this.leftPush("yj:job:TestJob:third:String", var2);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:second:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcast",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcast")
   public void broadcast$Method$3() {
      String var2 = this.job.broadcast();
      this.leftPush("yj:job:TestJob:broadcastChild1:String,yj:job:TestJob:broadcastChild2:String,yj:job:TestJob:broadcastChild3:String", var2);
   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "top",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("top")
   public void top$Method$4() {
      String var2 = this.job.top();
      this.leftPush("yj:job:TestJob:second:String", var2);
   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "aggregate",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("aggregate")
   public void aggregate$Method$5() {
      String var1 = this.popKey("yj:job:TestJob:aggregate:String");

      try {
         this.job.aggregate(var1);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:aggregate:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "single",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("single")
   public void single$Method$6() {
      this.job.single();
   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "circulate1",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("circulate1")
   public void circulate1$Method$7() {
      String var1 = this.popKey("yj:job:TestJob:circulate1:String");

      try {
         String var2 = this.job.circulate1(var1);
         this.leftPush("yj:job:TestJob:circulate2:String", var2);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:circulate1:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastChild1",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastChild1")
   public void broadcastChild1$Method$8() {
      String var1 = this.popKey("yj:job:TestJob:broadcastChild1:String");

      try {
         this.job.broadcastChild1(var1);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastChild1:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastChild2",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastChild2")
   public void broadcastChild2$Method$9() {
      String var1 = this.popKey("yj:job:TestJob:broadcastChild2:String");

      try {
         this.job.broadcastChild2(var1);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastChild2:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastChild3",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastChild3")
   public void broadcastChild3$Method$10() {
      String var1 = this.popKey("yj:job:TestJob:broadcastChild3:String");

      try {
         this.job.broadcastChild3(var1);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastChild3:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "third",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("third")
   public void third$Method$11() {
      String var1 = this.popKey("yj:job:TestJob:third:String");

      try {
         String var2 = this.job.third(var1);
         this.leftPush("yj:job:TestJob:last:String", var2);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:third:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "circulate2",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("circulate2")
   public void circulate2$Method$12() {
      String var1 = this.popKey("yj:job:TestJob:circulate2:String");

      try {
         String var2 = this.job.circulate2(var1);
         this.leftPush("yj:job:TestJob:circulate1:String", var2);
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:circulate2:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastAndAggregateChild3",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastAndAggregateChild3")
   public void broadcastAndAggregateChild3$Method$13() {
      String var1 = this.popKey("yj:job:TestJob:broadcastAndAggregateChild3:String");

      try {
         String var2 = this.job.broadcastAndAggregateChild3(var1);
         if (this.jobDone("yj:job:TestJob:aggregate:String:done:", var2, 3)) {
            this.leftPush("yj:job:TestJob:aggregate:String", var2);
         }
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastAndAggregateChild3:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastAndAggregate",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastAndAggregate")
   public void broadcastAndAggregate$Method$14() {
      String var2 = this.job.broadcastAndAggregate();
      this.leftPush("yj:job:TestJob:broadcastAndAggregateChild3:String,yj:job:TestJob:broadcastAndAggregateChild2:String,yj:job:TestJob:broadcastAndAggregateChild1:String", var2);
   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastAndAggregateChild2",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastAndAggregateChild2")
   public void broadcastAndAggregateChild2$Method$15() {
      String var1 = this.popKey("yj:job:TestJob:broadcastAndAggregateChild2:String");

      try {
         String var2 = this.job.broadcastAndAggregateChild2(var1);
         if (this.jobDone("yj:job:TestJob:aggregate:String:done:", var2, 3)) {
            this.leftPush("yj:job:TestJob:aggregate:String", var2);
         }
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastAndAggregateChild2:String", var1);
      }

   }

   @XxlRegister(
           cron = "0/10 * * * * ?",
           author = "以见科技",
           jobDesc = "broadcastAndAggregateChild1",
           jobGroup = "test-job",
           jobGroupTitle = "asdf",
           executorRouteStrategy = "ROUND",
           triggerStatus = 0,
           jobGroupAddressType = 0
   )
   @XxlJob("broadcastAndAggregateChild1")
   public void broadcastAndAggregateChild1$Method$16() {
      String var1 = this.popKey("yj:job:TestJob:broadcastAndAggregateChild1:String");

      try {
         String var2 = this.job.broadcastAndAggregateChild1(var1);
         if (this.jobDone("yj:job:TestJob:aggregate:String:done:", var2, 3)) {
            this.leftPush("yj:job:TestJob:aggregate:String", var2);
         }
      } catch (JobException var3) {
         this.leftPush("yj:job:TestJob:broadcastAndAggregateChild1:String", var1);
      }

   }
}
```