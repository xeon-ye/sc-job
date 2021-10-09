package com.sc.job.core.biz.client;

import com.sc.job.core.biz.JobExecutorBiz;
import com.sc.job.core.biz.dto.*;
import com.sc.job.core.util.ScJobRemotingUtil;

/**
 * admin api test
 *
 * @author scer 2017-07-28 22:14:52
 */
public class JobExecutorBizClient implements JobExecutorBiz {

    public JobExecutorBizClient() {
    }
    public JobExecutorBizClient(String addressUrl, String accessToken) {
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
    public ReturnT<String> beat() {
        return ScJobRemotingUtil.postReq(addressUrl+"beat", accessToken, timeout, null, String.class);
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam){
        return ScJobRemotingUtil.postReq(addressUrl+"idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return ScJobRemotingUtil.postReq(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return ScJobRemotingUtil.postReq(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return ScJobRemotingUtil.postReq(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }

}
