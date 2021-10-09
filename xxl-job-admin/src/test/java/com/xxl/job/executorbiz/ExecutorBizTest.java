package com.xxl.job.executorbiz;

import com.sc.job.core.biz.JobExecutorBiz;
import com.sc.job.core.biz.client.JobExecutorBizClient;
import com.sc.job.core.biz.dto.*;
import com.sc.job.core.enums.ExecutorBlockStrategyEnum;
import com.sc.job.core.glue.GlueTypeEnum;
import org.junit.Assert;
import org.junit.Test;

/**
 * executor api test
 *
 * Created by scner on 17/5/12.
 */
public class ExecutorBizTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:9999/";
    private static String accessToken = null;

    @Test
    public void beat() throws Exception {
        JobExecutorBiz executorBiz = new JobExecutorBizClient(addressUrl, accessToken);
        // Act
        final ReturnT<String> retval = executorBiz.beat();

        // Assert result
        Assert.assertNotNull(retval);
        Assert.assertNull(((ReturnT<String>) retval).getContent());
        Assert.assertEquals(200, retval.getCode());
        Assert.assertNull(retval.getMsg());
    }

    @Test
    public void idleBeat(){
        JobExecutorBiz executorBiz = new JobExecutorBizClient(addressUrl, accessToken);

        final int jobId = 0;

        // Act
        final ReturnT<String> retval = executorBiz.idleBeat(new IdleBeatParam(jobId));

        // Assert result
        Assert.assertNotNull(retval);
        Assert.assertNull(((ReturnT<String>) retval).getContent());
        Assert.assertEquals(500, retval.getCode());
        Assert.assertEquals("job thread is running or has trigger queue.", retval.getMsg());
    }

    @Test
    public void run(){
        JobExecutorBiz executorBiz = new JobExecutorBizClient(addressUrl, accessToken);

        // trigger data
        final TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);
        triggerParam.setExecutorHandler("demoJobHandler");
        triggerParam.setExecutorParams(null);
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.COVER_EARLY.name());
        triggerParam.setGlueType(GlueTypeEnum.BEAN.name());
        triggerParam.setGlueSource(null);
        triggerParam.setGlueUpdatetime(System.currentTimeMillis());
        triggerParam.setLogId(1);
        triggerParam.setLogDateTime(System.currentTimeMillis());

        // Act
        final ReturnT<String> retval = executorBiz.run(triggerParam);

        // Assert result
        Assert.assertNotNull(retval);
    }

    @Test
    public void kill(){
        JobExecutorBiz executorBiz = new JobExecutorBizClient(addressUrl, accessToken);

        final int jobId = 0;

        // Act
        final ReturnT<String> retval = executorBiz.kill(new KillParam(jobId));

        // Assert result
        Assert.assertNotNull(retval);
        Assert.assertNull(((ReturnT<String>) retval).getContent());
        Assert.assertEquals(200, retval.getCode());
        Assert.assertNull(retval.getMsg());
    }

    @Test
    public void log(){
        JobExecutorBiz executorBiz = new JobExecutorBizClient(addressUrl, accessToken);

        final long logDateTim = 0L;
        final long logId = 0;
        final int fromLineNum = 0;

        // Act
        final ReturnT<LogResult> retval = executorBiz.log(new LogParam(logDateTim, logId, fromLineNum));

        // Assert result
        Assert.assertNotNull(retval);
    }

}
