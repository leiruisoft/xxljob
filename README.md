# xxl-job-plus

#### 介绍
通过改造xxl-job，实现分布式延迟任务的功能。调度时间精确，误差不超过1秒。

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明
1、新建表xxl_delay_info，sql在doc文件夹的sql文件中；

2、使用@XxlDelay注解标记处理延迟任务的业务逻辑方法；

3、使用XxlJobDelayHelper#pushDelayTask静态方法发布延迟任务；

4、使用XxlJobDelayHelper#cancel静态方法取消延迟任务；

