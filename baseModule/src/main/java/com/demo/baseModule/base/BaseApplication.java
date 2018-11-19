package com.demo.baseModule.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.demo.baseModule.BuildConfig;
import com.demo.baseModule.application.BaseAbstractApplication;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.squareup.leakcanary.LeakCanary;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseApplication extends BaseAbstractApplication implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BaseApplication";
    private ArrayList<Activity> activities;
    public static BaseApplication application;

    public static boolean isLoadingMail;//是否正在加载邮件
    private int foregroundActivityCount = 0;//统计前台Activity数量

    @Override
    public void onCreate() {
        super.onCreate();
        //检查是否主进程
        if (this.getPackageName().equals(getProcessName(this))) {
            activities = new ArrayList<>();
            // 加载系统默认设置，字体不随用户设置变化
            Resources res = super.getResources();
            Configuration config = new Configuration();
            config.setToDefaults();
            res.updateConfiguration(config, res.getDisplayMetrics());
            application = this;
            registerActivityLifecycleCallbacks(this);
            //开启 LeakCanary
            initLeakCanary();
            //初始化打印工具
            Logger.addLogAdapter(new AndroidLogAdapter() {
                @Override
                public boolean isLoggable(int priority, String tag) {
                    return BuildConfig.DEBUG;
                }
            });


            //初始化ARouter
            if (BuildConfig.DEBUG) {    // These two lines must be written before init, otherwise these configurations will be invalid in the init process
                ARouter.openLog();     // Print log
                ARouter.openDebug();   // Turn on debugging mode (If you are running in InstantRun mode, you must turn on debug mode! Online version needs to be closed, otherwise there is a security risk)
            }
            Log.d(TAG, "onCreate: --------------------");
            ARouter.init(this);
        }
    }


    private void initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }


    /**
     * 得到BaseApplication
     */
    public static BaseApplication getApplication() {
        return application;
    }

    /**
     * 获取顶部activity
     */
    public Activity getTopActivity() {
        int size = activities.size();
        if (activities.size() > 0) {
            return activities.get(size - 1);
        }
        return null;
    }

    /**
     * 得到装activity的集合
     */
    public ArrayList<Activity> getActivities() {
        return activities;
    }


    /**
     * 添加activity
     */
    private void addActivity(Activity activity) {
        activities.add(activity);
    }

    private void removeActivity(Activity activity) {
        activities.remove(activity);
    }


    /**
     * 删除activity
     */
    public void deleteActivity(Activity activity) {
        if (activities.contains(activity)) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
            activities.remove(activity);
        }
    }

    /**
     * 删除activity
     *
     * @param position 在栈中的位置
     * @param cls      类名（避免误删）
     */
    public void deleteActivity(int position, Class<?> cls) {
        Activity activity = activities.get(position);
        if (activity.getClass().equals(cls) && !activity.isFinishing()) {
            activity.finish();
            activities.remove(activity);
        }
    }

    /**
     * 删除activity
     *
     * @param position 位置（从栈顶开始算起）
     * @param cls      类名（避免误删）
     */
    public void deleteActivityFromTop(int position, Class<?> cls) {
        deleteActivity(activities.size() - 1 - position, cls);
    }

    /**
     * 清空activities
     */
    public void clearAllActivity() {
        for (int i = 0; i < activities.size(); i++) {
            if (!activities.get(i).isFinishing()) {
                activities.get(i).finish();
            }
        }
        activities.clear();
    }

    /**
     * @param ignoreActivity 需要忽略的Activity实例
     */
    public void clearAllActivity(Activity... ignoreActivity) {

        for (Activity a : ignoreActivity) {
            if (activities.contains(a)) {
                activities.remove(a);
            }
        }

        for (int i = 0; i < activities.size(); i++) {
            if (!activities.get(i).isFinishing()) {
                activities.get(i).finish();
            }
        }
        activities.clear();
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterActivityLifecycleCallbacks(this);
    }

    /**
     * 判断当前应用是否在后台运行
     *
     * @return
     */
    public boolean isRunInBackground() {
        return foregroundActivityCount == 0;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        addActivity(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        foregroundActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        foregroundActivityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        removeActivity(activity);
    }


    private String getProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return null;
    }


}
