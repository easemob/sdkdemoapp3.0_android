package com.hyphenate.chatuidemo;

import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;

import com.huawei.android.hms.agent.HMSAgent;
import com.huawei.android.hms.agent.common.handler.ConnectHandler;
import com.huawei.android.hms.agent.push.handler.GetTokenHandler;
import com.hyphenate.chat.EMClient;
import com.hyphenate.util.EMLog;

import java.lang.reflect.Method;

/**
 * Created by lzan13 on 2018/4/10.
 */

public class HMSPushHelper {

    private static HMSPushHelper instance;

    // 是否使用华为 hms
    private boolean isUseHMSPush = false;

    private HMSPushHelper(){}

    public static HMSPushHelper getInstance() {
        if (instance == null) {
            instance = new HMSPushHelper();
        }
        return instance;
    }

    /**
     * 初始化华为 HMS 推送服务
     */
    public void initHMSAgent(Application application){
        if (EMClient.getInstance().isFCMAvailable()) {
            return;
        }
        try {
            if(Class.forName("com.huawei.hms.support.api.push.HuaweiPush") != null){
                Class<?> classType = Class.forName("android.os.SystemProperties");
                Method getMethod = classType.getDeclaredMethod("get", new Class<?>[] {String.class});
                String buildVersion = (String)getMethod.invoke(classType, new Object[]{"ro.build.version.emui"});
                //在某些手机上，invoke方法不报错
                if(!TextUtils.isEmpty(buildVersion)){
                    EMLog.d("HWHMSPush", "huawei hms push is available!");
                    isUseHMSPush = true;
                    HMSAgent.init(application);
                }else{
                    EMLog.d("HWHMSPush", "huawei hms push is unavailable!");
                }
            }else{
                EMLog.d("HWHMSPush", "no huawei hms push sdk or mobile is not a huawei phone");
            }
        } catch (Exception e) {
            EMLog.d("HWHMSPush", "no huawei hms push sdk or mobile is not a huawei phone");
        }
    }

    /**
     * 连接华为移动服务，并获取华为推送
     * 注册华为推送 token 通用错误码列表
     * http://developer.huawei.com/consumer/cn/service/hms/catalog/huaweipush_agent.html?page=hmssdk_huaweipush_api_reference_errorcode
     */
    public void getHMSToken(Activity activity){
        if (isUseHMSPush) {
            HMSAgent.connect(activity, new ConnectHandler() {
                @Override
                public void onConnect(int rst) {
                    EMLog.d("HWHMSPush", "huawei hms push connect result code:" + rst);
                    if (rst == HMSAgent.AgentResultCode.HMSAGENT_SUCCESS) {
                        HMSAgent.Push.getToken(new GetTokenHandler() {
                            @Override
                            public void onResult(int rst) {
                                EMLog.d("HWHMSPush", "get huawei hms push token result code:" + rst);
                            }
                        });
                    }
                }
            });
        }
    }

    public boolean isUseHMSPush() {
        return isUseHMSPush;
    }

}
