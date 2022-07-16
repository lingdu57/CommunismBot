package com.aye10032.config;

import com.aye10032.mapper.SubTaskMapper;
import com.aye10032.utils.timeutil.SubscribableBase;
import lombok.SneakyThrows;
import org.apache.http.client.utils.DateUtils;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.PostConstruct;
import java.util.*;

@Configuration
@DependsOn("springUtils")
public class ScheduleConfig implements ApplicationContextAware, InitializingBean {

    @Autowired
    private SubTaskMapper mapper;
    public static ApplicationContext applicationContext;
    @Autowired
    private Scheduler scheduler;
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;


    @SneakyThrows
    @PostConstruct
    public void init() {
        Map<String, SubscribableBase> beansOfType = applicationContext.getBeansOfType(SubscribableBase.class);
        for (Map.Entry<String, SubscribableBase> entry : beansOfType.entrySet()) {
            JobDetail jobDetail = JobBuilder.newJob(entry.getValue().getClass())
                .withIdentity(entry.getKey(), "default")
                .storeDurably()
                .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(entry.getKey(), "default")
                .startNow()
                // 每天0点执行
                .withSchedule(CronScheduleBuilder.cronSchedule(entry.getValue().getCron()))
                .build();
            if (!scheduler.checkExists(jobDetail.getKey())) {
                scheduler.scheduleJob(jobDetail, trigger);
            }
        }

    }

    public List<Map<String, String>> getJobList() {
        List<Map<String, String>> jobList = new ArrayList<>();
        try {
            //获取Scheduler
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            //再获取Scheduler下的所有group
            List<String> triggerGroupNames = scheduler.getTriggerGroupNames();
            for (String groupName : triggerGroupNames) {
                //组装group的匹配，为了模糊获取所有的triggerKey或者jobKey
                GroupMatcher groupMatcher = GroupMatcher.groupEquals(groupName);
                //获取所有的triggerKey
                Set<TriggerKey> triggerKeySet = scheduler.getTriggerKeys(groupMatcher);
                for (TriggerKey triggerKey : triggerKeySet) {
                    //通过triggerKey在scheduler中获取trigger对象
                    CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                    //获取trigger拥有的Job
                    JobKey jobKey = trigger.getJobKey();
                    JobDetailImpl jobDetail = (JobDetailImpl) scheduler.getJobDetail(jobKey);
                    //组装需要显示的数据
                    Map<String, String> jobMap = new HashMap<>();
                    //分组名称
                    jobMap.put("groupName", groupName);
                    //定时任务名称
                    jobMap.put("jobDetailName", jobDetail.getName());
                    //cron表达式
                    String cronExpression = trigger.getCronExpression();
                    jobMap.put("jobCronExpression", cronExpression);
                    //时区
                    jobMap.put("timeZone", trigger.getTimeZone().getID());
                    //下次运行时间
                    CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
                    cronTriggerImpl.setCronExpression(cronExpression);
                    List<Date> dates = TriggerUtils.computeFireTimes(cronTriggerImpl, null, 20);
                    jobMap.put("nextRunDateTime", DateUtils.formatDate(dates.get(0), "yyyy-MM-dd HH:mm:ss"));
                    jobList.add(jobMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jobList;
    }


    public Object registerSingletonBean(String beanName, Object singletonObject) {

        //将applicationContext转换为ConfigurableApplicationContext
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;

        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();

        //动态注册bean.
        defaultListableBeanFactory.registerSingleton(beanName, singletonObject);

        //获取动态注册的bean.
        return configurableApplicationContext.getBean(beanName);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ScheduleConfig.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
