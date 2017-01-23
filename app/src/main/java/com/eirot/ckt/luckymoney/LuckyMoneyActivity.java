package com.eirot.ckt.luckymoney;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

/**
 * Created by Eirot Chen on 2016/2/3.
 */

// 抢红包主界面
public class LuckyMoneyActivity extends BaseActivity {
    private final String TAG = "Eirot";
    private Dialog mTipsDialog;

    //广播监听器
    private BroadcastReceiver qhbConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isFinishing()) {
                return;
            }
            String action = intent.getAction();
            Log.d(TAG, "qhbConnectReceiver--> " + action);
            if(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT.equals(action)) {
                if (mTipsDialog != null) {
                    mTipsDialog.dismiss();
                }
            } else if(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT.equals(action)) {
                showOpenAccessibilityServiceDialog();
            }
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        getFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commitAllowingStateLoss();

        QHBApplication.activityStartMain(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        filter.addAction(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        registerReceiver(qhbConnectReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(QiangHongBaoService.isRunning()) {
            if(mTipsDialog != null) {
                mTipsDialog.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(qhbConnectReceiver);
        } catch (Exception e) {}
        mTipsDialog = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, 0, 0, R.string.open_service_button);
        item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == 0) {
            //右上角的“ ：”的监听事件
            openAccessibilityServiceSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 自定义显示未开启辅助服务的对话框*/
    private void showOpenAccessibilityServiceDialog() {
        if(mTipsDialog != null && mTipsDialog.isShowing()) {
            return;
        }
        View view = getLayoutInflater().inflate(R.layout.dialog_tips_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.open_service_title);
        builder.setView(view);
        builder.setPositiveButton(R.string.open_service_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openAccessibilityServiceSettings();
            }
        });

        builder.setNegativeButton(R.string.close_service_button , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mTipsDialog = builder.show();
    }

    /** 跳转到Setting模块，让用户自己选择打开抢红包的辅助服务*/
    private void openAccessibilityServiceSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, R.string.tips, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MainFragment extends PreferenceFragment {
        protected Preference mPrefWechatPay;
        View mWeChatPay;

        /*@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Log.d("Eirot", "---------onCreateView()");
            View view = inflater.inflate(R.layout.wechat_pay_view, container, false);
            mWehatPay = view.findViewById(R.id.wechat_pay_me);
            mWehatPay.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d("Eirot", "我要成功了！");
                    return true;
                }
            });

            return super.onCreateView(inflater, container, savedInstanceState);
        }*/

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d("Eirot", "---------MainFragment::onCreate()");
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(Config.PREFERENCE_NAME);
            addPreferencesFromResource(R.xml.main);

            //找不到Preference自定义里面的东西，监听无效
            View view = View.inflate(getActivity().getApplicationContext(), R.layout.wechat_pay_view, null);
            mWeChatPay = view.findViewById(R.id.wechat_pay_me);
            mWeChatPay.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d("Eirot", "onLongClick成功了！");
                    return true;
                }
            });
            mWeChatPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Eirot", "onClick成功了！");
                }
            });

            //抢红包开关
            final SwitchPreference mLMSwitchPreference = (SwitchPreference) findPreference(Config.KEY_ENABLE_LUCKYMONEY);
            mLMSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean isChecked = !mLMSwitchPreference.isChecked();
                    mLMSwitchPreference.setSelectable(true);
                    if ((Boolean) newValue && !QiangHongBaoService.isRunning()) {
                        ((LuckyMoneyActivity) getActivity()).showOpenAccessibilityServiceDialog();
                    }
                    if (isChecked && QiangHongBaoService.isRunning()) {
                        mLMSwitchPreference.setChecked(true);
                        mLMSwitchPreference.setSelectable(false);
                        mLMSwitchPreference.setSummary(R.string.preference_enable_luckymoney_summary);
                    }
                    return true;
                }
            });

            final EditTextPreference delayEditTextPre = (EditTextPreference) findPreference(Config.KEY_WECHAT_DELAY_TIME);
            delayEditTextPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if("0".equals(String.valueOf(newValue))) {
                        preference.setSummary("");
                    } else {
                        preference.setSummary("已延时" + newValue + "毫秒");
                    }
                    return true;
                }
            });
            String delay = delayEditTextPre.getText();
            if("0".equals(String.valueOf(delay))) {
                delayEditTextPre.setSummary("");
            } else {
                delayEditTextPre.setSummary("已延时" + delay  + "毫秒");
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
            if (preference == mPrefWechatPay) {
                Log.d("Eirot", "点击二维码吧");
                return false;
            }
            return super.onPreferenceTreeClick(screen, preference);
        }
    }
}
