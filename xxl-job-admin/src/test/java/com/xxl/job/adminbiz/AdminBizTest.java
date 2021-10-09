package com.xxl.job.adminbiz;

import com.sc.job.core.biz.JobManageBiz;
import com.sc.job.core.biz.client.JobManageBizClient;
import com.sc.job.core.biz.dto.HandleCallbackParam;
import com.sc.job.core.biz.dto.RegistryParam;
import com.sc.job.core.biz.dto.ReturnT;
import com.sc.job.core.enums.RegistryConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * admin api test
 *
 * @author scer 2017-07-28 22:14:52
 */
public class AdminBizTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:8080/xxl-job-admin/";
    private static String accessToken = null;


    @Test
    public void callback() throws Exception {
        JobManageBiz adminBiz = new JobManageBizClient(addressUrl, accessToken);

        HandleCallbackParam param = new HandleCallbackParam();
        param.setLogId(1);
        param.setExecuteResult(ReturnT.SUCCESS);

        List<HandleCallbackParam> callbackParamList = Arrays.asList(param);

        ReturnT<String> returnT = adminBiz.callback(callbackParamList);

        Assert.assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor
     *
     * @throws Exception
     */
    @Test
    public void registry() throws Exception {
        JobManageBiz adminBiz = new JobManageBizClient(addressUrl, accessToken);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registry(registryParam);

        Assert.assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor remove
     *
     * @throws Exception
     */
    @Test
    public void registryRemove() throws Exception {
        JobManageBiz adminBiz = new JobManageBizClient(addressUrl, accessToken);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registryRemove(registryParam);

        Assert.assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);

    }

}
