package com.sc.job.core.executorinit;

import com.sc.job.core.constants.Constants;
import com.sc.job.core.handler.IJobHandler;
import com.sc.job.core.server.CoreNettyServer;
import com.sc.job.core.start.JobLogFileCleanStartThread;
import com.sc.job.core.start.JobStartAndExecuteThread;
import com.sc.job.core.start.TriggerCallbackStartThread;
import com.sc.job.core.util.NetUtil;
import com.sc.job.core.biz.JobManageBiz;
import com.sc.job.core.biz.client.JobManageBizClient;
import com.sc.job.core.log.ScJobFileAppender;
import com.sc.job.core.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author scner
 * @date 2016/3/2 21:14
 */
public class ScJobExecutorInit {
    private static final Logger logger = LoggerFactory.getLogger(ScJobExecutorInit.class);

    // ---------------------- param ----------------------
    private String adminAddresses;
    private String accessToken;
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setAppname(String appname) {
        this.appname = appname;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }


    // ---------------------- start + stop ----------------------
    public void start() throws Exception {

        // init logpath
        ScJobFileAppender.initLogPath(logPath);

        // init invoker, admin-client
        initAdminBizList(adminAddresses, accessToken);


        // init JobLogFileCleanThread
        JobLogFileCleanStartThread.getInstance().start(logRetentionDays);

        // init TriggerCallbackThread
        TriggerCallbackStartThread.getInstance().start();

        // init executor-server
        initEmbedServer(address, ip, port, appname, accessToken);
    }
    public void destroy(){
        // destory executor-server
        stopEmbedServer();

        // destory jobThreadRepository
        if (jobThreadRepository.size() > 0) {
            for (Map.Entry<Integer, JobStartAndExecuteThread> item: jobThreadRepository.entrySet()) {
                JobStartAndExecuteThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // wait for job thread push result to callback queue
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        logger.error(">>>>>>>>>>> sc-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            jobThreadRepository.clear();
        }
        jobHandlerRepository.clear();


        // destory JobLogFileCleanThread
        JobLogFileCleanStartThread.getInstance().toStop();

        // destory TriggerCallbackThread
        TriggerCallbackStartThread.getInstance().toStop();

    }


    // ---------------------- admin-client (rpc invoker) ----------------------
    private static List<JobManageBiz> adminBizList;
    private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses!=null && adminAddresses.trim().length()>0) {
            for (String address: adminAddresses.trim().split(",")) {
                if (address!=null && address.trim().length()>0) {

                    JobManageBiz adminBiz = new JobManageBizClient(address.trim(), accessToken);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<JobManageBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }
    public static List<JobManageBiz> getAdminBizList(){
        return adminBizList;
    }

    // ---------------------- executor-server (rpc provider) ----------------------
    private CoreNettyServer embedServer = null;

    private void initEmbedServer(String address, String ip, int port, String appname, String accessToken) throws Exception {

        // fill ip port
        port = port>0?port: NetUtil.findAvailablePort(Constants.DEFAULT_PORT);
        ip = (ip!=null&&ip.trim().length()>0)?ip: IpUtil.getIp();

        // generate address
        if (address==null || address.trim().length()==0) {
            // registry-address：default use address to registry , otherwise use ip:port if address is null
            String ip_port_address = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }

        // start
        embedServer = new CoreNettyServer();
        embedServer.start(address, port, appname, accessToken);
    }

    private void stopEmbedServer() {
        // stop provider factory
        try {
            embedServer.stop();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    // ---------------------- job handler repository ----------------------
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler){
        logger.info(">>>>>>>>>>> sc-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }
    public static IJobHandler loadJobHandler(String name){
        return jobHandlerRepository.get(name);
    }


    // ---------------------- job thread repository ----------------------
    private static ConcurrentMap<Integer, JobStartAndExecuteThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobStartAndExecuteThread>();
    public static JobStartAndExecuteThread registJobThread(int jobId, IJobHandler handler, String removeOldReason){
        JobStartAndExecuteThread newJobThread = new JobStartAndExecuteThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>>> sc-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, handler});

        JobStartAndExecuteThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);	// putIfAbsent | oh my god, map's put method return the old value!!!
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }
    public static JobStartAndExecuteThread removeJobThread(int jobId, String removeOldReason){
        JobStartAndExecuteThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();

            return oldJobThread;
        }
        return null;
    }
    public static JobStartAndExecuteThread loadJobThread(int jobId){
        JobStartAndExecuteThread jobThread = jobThreadRepository.get(jobId);
        return jobThread;
    }

}
