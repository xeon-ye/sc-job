package com.sc.job.core.handler.impl;

import com.sc.job.core.handler.IJobHandler;
import com.sc.job.core.util.ScriptUtil;
import com.sc.job.core.biz.dto.ReturnT;
import com.sc.job.core.glue.GlueTypeEnum;
import com.sc.job.core.log.ScJobFileAppender;
import com.sc.job.core.log.ScJobLogger;
import com.sc.job.core.util.ShardingUtil;

import java.io.File;

/**
 * Created by scner on 17/4/27.
 */
public class ScriptJobHandler extends IJobHandler {

    private int jobId;
    private long glueUpdatetime;
    private String gluesource;
    private GlueTypeEnum glueType;

    public ScriptJobHandler(int jobId, long glueUpdatetime, String gluesource, GlueTypeEnum glueType){
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.glueType = glueType;

        // clean old script file
        File glueSrcPath = new File(ScJobFileAppender.getGlueSrcPath());
        if (glueSrcPath.exists()) {
            File[] glueSrcFileList = glueSrcPath.listFiles();
            if (glueSrcFileList!=null && glueSrcFileList.length>0) {
                for (File glueSrcFileItem : glueSrcFileList) {
                    if (glueSrcFileItem.getName().startsWith(String.valueOf(jobId)+"_")) {
                        glueSrcFileItem.delete();
                    }
                }
            }
        }

    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public ReturnT<String> execute(String param) throws Exception {

        if (!glueType.isScript()) {
            return new ReturnT<String>(IJobHandler.FAIL.getCode(), "glueType["+ glueType +"] invalid.");
        }

        // cmd
        String cmd = glueType.getCmd();

        // make script file
        String scriptFileName = ScJobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(String.valueOf(jobId))
                .concat("_")
                .concat(String.valueOf(glueUpdatetime))
                .concat(glueType.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, gluesource);
        }

        // log file
        String logFileName = ScJobFileAppender.contextHolder.get();

        // script params：0=param、1=分片序号、2=分片总数
        ShardingUtil.ShardingVO shardingVO = ShardingUtil.getShardingVo();
        String[] scriptParams = new String[3];
        scriptParams[0] = param;
        scriptParams[1] = String.valueOf(shardingVO.getIndex());
        scriptParams[2] = String.valueOf(shardingVO.getTotal());

        // invoke
        ScJobLogger.log("----------- script file:"+ scriptFileName +" -----------");
        int exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, scriptParams);

        if (exitValue == 0) {
            return IJobHandler.SUCCESS;
        } else {
            return new ReturnT<String>(IJobHandler.FAIL.getCode(), "script exit value("+exitValue+") is failed");
        }

    }

}
