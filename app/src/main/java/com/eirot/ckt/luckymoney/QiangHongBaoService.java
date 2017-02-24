package com.eirot.ckt.luckymoney;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.eirot.ckt.luckymoney.job.AccessbilityJob;
import com.eirot.ckt.luckymoney.job.WechatAccessbilityJob;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 抢红包外挂服务
 */
public class QiangHongBaoService extends AccessibilityService {

    private static final String TAG = "QiangHongBao";

    private static final Class[] ACCESSBILITY_JOBS= {
            WechatAccessbilityJob.class,
    };

    private static QiangHongBaoService service;

    private Config mConfig;
    private List<AccessbilityJob> mAccessbilityJobs;

    @Override
    public void onCreate() {
        super.onCreate();

        mAccessbilityJobs = new ArrayList<>();
        mConfig = new Config(this);

        //初始化辅助插件工作
        for(Class clazz : ACCESSBILITY_JOBS) {
            try {
                Object object = clazz.newInstance();
                if(object instanceof AccessbilityJob) {
                    AccessbilityJob job = (AccessbilityJob) object;
                    job.onCreateJob(this);
                    mAccessbilityJobs.add(job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "qianghongbao service destory");
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                job.onStopJob();
            }
            mAccessbilityJobs.clear();
        }
        service = null;
        mAccessbilityJobs = null;
        //发送广播，已经断开辅助服务
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "qianghongbao service interrupt");
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        sendBroadcast(intent);
        Toast.makeText(this, "已连接抢红包服务", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkn = String.valueOf(event.getPackageName());
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {

            for (AccessbilityJob job : mAccessbilityJobs) {
                if(pkn.equals(job.getTargetPackageName()) && job.isEnable()) {
                    job.onReceiveJob(event);
                }
            }
        }
    }

    public Config getConfig() {
        return mConfig;
    }

    /**
     * 5. go back when traped lucky money
     */
    public static void goBack() {
        if(service != null) {
            Log.d("Eirot","Now I'm go back");
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    /**
     * 判断当前服务是否正在运行
     */
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if(info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if(i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if(!isConnect) {
            return false;
        }
        return true;
    }
}
