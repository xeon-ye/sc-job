package com.sc.job.core.handler;

import com.sc.job.core.biz.dto.ReturnT;

import java.lang.reflect.InvocationTargetException;

/**
 * job handler
 *
 * @author scer 2020-12-19 19:06:38
 */
public abstract class IJobHandler {


	/** success */
	public static final ReturnT<String> SUCCESS = new ReturnT<String>(200, null);
	/** fail */
	public static final ReturnT<String> FAIL = new ReturnT<String>(500, null);
	/** fail timeout */
	public static final ReturnT<String> FAIL_TIMEOUT = new ReturnT<String>(502, null);


	/**
	 * execute handler, invoked when executor receives a scheduling request
	 *
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public abstract ReturnT<String> execute(String param) throws Exception;


	/**
	 * init handler, invoked when JobThread init
	 */
	public void init() throws InvocationTargetException, IllegalAccessException {
		// do something
	}


	/**
	 * destroy handler, invoked when JobThread destroy
	 */
	public void destroy() throws InvocationTargetException, IllegalAccessException {
		// do something
	}


}
