package com.sc.job.core.biz;

import com.sc.job.core.biz.dto.HandleCallbackParam;
import com.sc.job.core.biz.dto.RegistryParam;
import com.sc.job.core.biz.dto.ReturnT;

import java.util.List;

/**
 * @author scer 2017-07-27 21:52:49
 */
public interface JobManageBiz {


    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryRemove(RegistryParam registryParam);


    // ---------------------- biz (custome) ----------------------
    // group、job ... manage

}
