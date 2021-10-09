package com.sc.job.core.biz.client;

import com.sc.job.core.biz.JobManageBiz;
import com.sc.job.core.biz.dto.HandleCallbackParam;
import com.sc.job.core.biz.dto.RegistryParam;
import com.sc.job.core.biz.dto.ReturnT;
import com.sc.job.core.util.ScJobRemotingUtil;

import java.util.List;

/**
 * admin api
 *
 * @author scer 2017-07-28 22:14:52
 */
public class JobManageBizClient implements JobManageBiz {

    public JobManageBizClient() {
    }
    public JobManageBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl ;
    private String accessToken;
    private int timeout = 3;


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return ScJobRemotingUtil.postReq(addressUrl+"api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return ScJobRemotingUtil.postReq(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return ScJobRemotingUtil.postReq(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

}
