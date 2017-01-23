package com.eirot.ckt.luckymoney.job;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.eirot.ckt.luckymoney.QiangHongBaoService;

import java.util.List;

public class WechatAccessbilityJob extends BaseAccessbilityJob {

    private static final String TAG = "WechatAccessbilityJob";

    //微信的包名
    private static final String WECHAT_PACKAGENAME = "com.tencent.mm";

    // 微信Open
    private static final String WECHAT654_OPEN_ID = "com.tencent.mm:id/bi3";

    private static final String WECHAT654_GET_ID = "com.tencent.mm:id/a5u";

    private static final String WECHAT654_LuckyMoneyReceiveUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String WECHAT654_LuckyMoneyDetailUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private static final String WECHAT654_LauncherUI = "com.tencent.mm.ui.LauncherUI";

    /** 红包消息的关键字*/
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    private boolean isFirstChecked;
    private boolean isFistBack;
    private int sDelayTime;
    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //更新安装包信息
            updatePackageInfo();
        }
    };

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        super.onCreateJob(service);

        updatePackageInfo();
        sDelayTime = getConfig().getWechatOpenDelayTime();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");

        getContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onStopJob() {
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {}
    }

    @Override
    public boolean isEnable() {

        return getConfig().isEnableWechat();
    }

    @Override
    public String getTargetPackageName() {

        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        //监听通知栏的红包通知事件
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                for(CharSequence t : texts) {
                    String text = String.valueOf(t);
                    if(text.contains(HONGBAO_TEXT_KEY)) {
                        openNotify(event);
                        break;
                    }
                }
            }
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //已经跳转到微信红包界面的处理，下一步等待拆红包和查看红包的操作
            openHongBao(event);
        }
    }

    /** 打开通知栏消息*/
    private void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        //以下是精华，将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;

        isFirstChecked = true;
        try {
            pendingIntent.send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openHongBao(AccessibilityEvent event) {
        if(WECHAT654_LuckyMoneyReceiveUI.equals(event.getClassName())) {
            //点中了红包，下一步就是去拆红包
            handleLuckyMoneyReceive();
            isFistBack = true;
        } else if(WECHAT654_LuckyMoneyDetailUI.equals(event.getClassName())) {
            if(isFistBack) {
                //拆完红包后看详细的纪录界面,查看完成后返回
                QiangHongBaoService.goBack();
                isFistBack = false;
            }
        } if(WECHAT654_LauncherUI.equals(event.getClassName())) {
            //如果用户已经在微信聊天界面,必须先手动点击中红包。但拆红包可以模拟
            handleChatListHongBao();
        }
    }

    /**点击聊天里的红包后，显示的界面(我们需要查找到拆红包的onClick事件)*/
    private void handleLuckyMoneyReceive() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        AccessibilityNodeInfo targetNode = null;
        List<AccessibilityNodeInfo> list = null;
        list = nodeInfo.findAccessibilityNodeInfosByViewId(WECHAT654_OPEN_ID);
        if (list == null || list.isEmpty()) {
            List<AccessibilityNodeInfo> l = nodeInfo.findAccessibilityNodeInfosByText("给你发了一个红包");
            if (l != null && !l.isEmpty()) {
                AccessibilityNodeInfo p = l.get(0).getParent();
                if (p != null) {
                    for (int i = 0; i < p.getChildCount(); i++) {
                        AccessibilityNodeInfo node = p.getChild(i);
                        if ("android.widget.Button".equals(node.getClassName())) {
                            targetNode = node;
                            break;
                        }
                    }
                }
            }
        }

        if(list != null && !list.isEmpty()) {
            targetNode = list.get(0);
        }

        if(targetNode != null) {
            final AccessibilityNodeInfo n = targetNode;
            if(sDelayTime >= 0) {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }, sDelayTime);
            } else {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    /**
     * 收到聊天里的红包
     */
    private void handleChatListHongBao() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        //List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(WECHAT654_GET_ID);

        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            list = nodeInfo.findAccessibilityNodeInfosByText("[微信红包]");

            if(list == null || list.isEmpty()) {
                return;
            }

            for(AccessibilityNodeInfo aNodeInfo : list) {
                if(aNodeInfo.isClickable()) {
                    aNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        } else if(list != null) {
            //最新的红包领起
            for(int i = list.size() - 1; i >= 0; i --) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                if(parent != null) {
                    if (isFirstChecked){
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        isFirstChecked = false;
                    }
                    break;
                }
            }
        }
    }

    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /** 获取微信的版本*/
    private int getWechatVersion() {
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /** 更新微信包信息*/
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
