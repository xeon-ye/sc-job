package com.sc.job.core.handler.impl;

import com.sc.job.core.handler.IJobHandler;
import com.sc.job.core.biz.dto.ReturnT;
import com.sc.job.core.log.ScJobLogger;

/**
 * glue job handler
 *
 * @author scer 2016-5-19 21:05:45
 */
public class GlueJobHandler extends IJobHandler {

	private long glueUpdatetime;
	private IJobHandler jobHandler;
	public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
		this.jobHandler = jobHandler;
		this.glueUpdatetime = glueUpdatetime;
	}
	public long getGlueUpdatetime() {
		return glueUpdatetime;
	}

	@Override
	public ReturnT<String> execute(String param) throws Exception {
		ScJobLogger.log("----------- glue.version:"+ glueUpdatetime +" -----------");
		return jobHandler.execute(param);
	}

}
