package com.easemob.chatuidemo;

import com.easemob.chat.EMChat;
import com.easemob.easeui.controller.EaseUI;

import android.content.Context;

public class DemoHelper {
    private EaseUI easeUI;
    
    private static DemoHelper instance = null;
    private DemoHelper(){}
    
    public synchronized static DemoHelper getInstance(){
        if(instance == null){
            instance = new DemoHelper();
        }
        return instance;
    }
    
    /**
     * init helper
     * @param context application context
     */
    public void init(Context context){
        if(EaseUI.getInstance().init(context)){
            easeUI = EaseUI.getInstance();
        }
    }
    
    /**
     * 是否登录成功过
     * @return
     */
    public boolean isLoggedIn(){
        return EMChat.getInstance().isLoggedIn();
    }
}
