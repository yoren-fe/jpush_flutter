package com.jiguang.jpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.data.JPushLocalNotification;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * JPushPlugin
 */
public class JPushPlugin implements MethodCallHandler {
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "jpush");
        channel.setMethodCallHandler(new JPushPlugin(registrar, channel));

    }

    public static JPushPlugin instance;
    static List<Map<String, Object>> openNotificationCache = new ArrayList<>();

    private boolean dartIsReady = false;
    private boolean jpushDidinit = false;

    private List<Result> getRidCache;

    private final Registrar registrar;
    public final MethodChannel channel;
    public final Map<Integer, Result> callbackMap;
    private int sequence;

    private JPushPlugin(Registrar registrar, MethodChannel channel) {

        this.registrar = registrar;
        this.channel = channel;
        this.callbackMap = new HashMap<>();
        this.sequence = 0;
        this.getRidCache = new ArrayList<>();

        instance = this;
    }


    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("setup")) {
            setup(call, result);
        } else if (call.method.equals("setTags")) {
            setTags(call, result);
        } else if (call.method.equals("cleanTags")) {
            cleanTags(call, result);
        } else if (call.method.equals("addTags")) {
            addTags(call, result);
        } else if (call.method.equals("deleteTags")) {
            deleteTags(call, result);
        } else if (call.method.equals("getAllTags")) {
            getAllTags(call, result);
        } else if (call.method.equals("setAlias")) {
            setAlias(call, result);
        } else if (call.method.equals("deleteAlias")) {
            deleteAlias(call, result);
        } else if (call.method.equals("stopPush")) {
            stopPush(call, result);
        } else if (call.method.equals("resumePush")) {
            resumePush(call, result);
        } else if (call.method.equals("clearAllNotifications")) {
            clearAllNotifications(call, result);
        } else if (call.method.equals("getLaunchAppNotification")) {
            getLaunchAppNotification(call, result);
        } else if (call.method.equals("getRegistrationID")) {
            getRegistrationID(call, result);
        } else if (call.method.equals("sendLocalNotification")) {
            sendLocalNotification(call, result);
        } else {
            result.notImplemented();
        }
    }

    public void setup(MethodCall call, Result result) {
        HashMap<String, Object> map = call.arguments();
        boolean debug = (boolean) map.get("debug");
        JPushInterface.setDebugMode(debug);

        JPushInterface.init(registrar.context());            // 初始化 JPush

        String channel = (String) map.get("channel");
        JPushInterface.setChannel(registrar.context(), channel);

        JPushPlugin.instance.dartIsReady = true;

        // try to clean getRid cache
        scheduleCache();
    }

    public void scheduleCache() {
        if (dartIsReady) {
            // try to shedule notifcation cache
            for (Map<String, Object> notification : JPushPlugin.openNotificationCache) {
                JPushPlugin.instance.channel.invokeMethod("onOpenNotification", notification);
                JPushPlugin.openNotificationCache.remove(notification);
            }
        }
        String rid = JPushInterface.getRegistrationID(registrar.context());
        boolean ridAvailable = rid != null && !rid.isEmpty();
        if (ridAvailable && dartIsReady) {
            // try to schedule get rid cache
            for (Result res : JPushPlugin.instance.getRidCache) {
                res.success(rid);
                JPushPlugin.instance.getRidCache.remove(res);
            }
        }
    }

    public void setTags(MethodCall call, Result result) {
        List<String> tagList = call.arguments();
        Set<String> tags = new HashSet<>(tagList);
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.setTags(registrar.context(), sequence, tags);
    }

    public void cleanTags(MethodCall call, Result result) {
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.cleanTags(registrar.context(), sequence);
    }

    public void addTags(MethodCall call, Result result) {
        List<String> tagList = call.arguments();
        Set<String> tags = new HashSet<>(tagList);
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.addTags(registrar.context(), sequence, tags);
    }

    public void deleteTags(MethodCall call, Result result) {
        List<String> tagList = call.arguments();
        Set<String> tags = new HashSet<>(tagList);
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.deleteTags(registrar.context(), sequence, tags);
    }

    public void getAllTags(MethodCall call, Result result) {
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.getAllTags(registrar.context(), sequence);
    }

    public void setAlias(MethodCall call, Result result) {
        String alias = call.arguments();
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.setAlias(registrar.context(), sequence, alias);
    }

    public void deleteAlias(MethodCall call, Result result) {
        String alias = call.arguments();
        sequence += 1;
        callbackMap.put(sequence, result);
        JPushInterface.deleteAlias(registrar.context(), sequence);
    }

    public void stopPush(MethodCall call, Result result) {
        JPushInterface.stopPush(registrar.context());
    }

    public void resumePush(MethodCall call, Result result) {
        JPushInterface.resumePush(registrar.context());
    }

    public void clearAllNotifications(MethodCall call, Result result) {
        JPushInterface.clearAllNotifications(registrar.context());
    }

    public void getLaunchAppNotification(MethodCall call, Result result) {

    }

    public void getRegistrationID(MethodCall call, Result result) {

        String rid = JPushInterface.getRegistrationID(registrar.context());
        if (rid == null || rid.isEmpty()) {
            getRidCache.add(result);
        } else {
            result.success(rid);
        }
    }


    public void sendLocalNotification(MethodCall call, Result result) {
        try {
            HashMap<String, Object> map = call.arguments();

            JPushLocalNotification ln = new JPushLocalNotification();
            ln.setBuilderId((Integer) map.get("buildId"));
            ln.setNotificationId((Integer) map.get("id"));
            ln.setTitle((String) map.get("title"));
            ln.setContent((String) map.get("content"));
            HashMap<String, Object> extra = (HashMap<String, Object>) map.get("extra");

            if (extra != null) {
                JSONObject json = new JSONObject(extra);
                ln.setExtras(json.toString());
            }

            long date = (long) map.get("fireTime");
            ln.setBroadcastTime(date);

            JPushInterface.addLocalNotification(registrar.context(), ln);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收自定义消息,通知,通知点击事件等事件的广播
     * 文档链接:http://docs.jiguang.cn/client/android_api/
     */
    public static class JPushReceiver extends BroadcastReceiver {

        private static final List<String> IGNORED_EXTRAS_KEYS = Arrays.asList("cn.jpush.android.TITLE",
                "cn.jpush.android.MESSAGE", "cn.jpush.android.APPKEY", "cn.jpush.android.NOTIFICATION_CONTENT_TITLE", "key_show_entity", "platform");

        public JPushReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(JPushInterface.ACTION_REGISTRATION_ID)) {
                String rId = intent.getStringExtra(JPushInterface.EXTRA_REGISTRATION_ID);
                Log.d("JPushPlugin", "on get registration");
                Log.d("JPushPlugin", JPushPlugin.instance.getRidCache.toString());
                JPushPlugin.transmitReceiveRegistrationId(rId);
            } else if (action.equals(JPushInterface.ACTION_MESSAGE_RECEIVED)) {
                handlingMessageReceive(intent);
            } else if (action.equals(JPushInterface.ACTION_NOTIFICATION_RECEIVED)) {
                handlingNotificationReceive(context, intent);
            } else if (action.equals(JPushInterface.ACTION_NOTIFICATION_OPENED)) {
                instance.dartIsReady = AppUtils.isAppForground(context);
                handlingNotificationOpen(context, intent);
            }
        }

        private void handlingMessageReceive(Intent intent) {
            String msg = intent.getStringExtra(JPushInterface.EXTRA_MESSAGE);
            Map<String, Object> extras = getNotificationExtras(intent);
            JPushPlugin.transmitMessageReceive(msg, extras);
        }

        private void handlingNotificationOpen(Context context, Intent intent) {
            String title = intent.getStringExtra(JPushInterface.EXTRA_NOTIFICATION_TITLE);
            String alert = intent.getStringExtra(JPushInterface.EXTRA_ALERT);
            Map<String, Object> extras = getNotificationExtras(intent);
            JPushPlugin.transmitNotificationOpen(title, alert, extras);

            Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launch != null) {
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(launch);
            }
        }

        private void handlingNotificationReceive(Context context, Intent intent) {
            String title = intent.getStringExtra(JPushInterface.EXTRA_NOTIFICATION_TITLE);
            String alert = intent.getStringExtra(JPushInterface.EXTRA_ALERT);
            Map<String, Object> extras = getNotificationExtras(intent);
            JPushPlugin.transmitNotificationReceive(title, alert, extras);
        }

        private Map<String, Object> getNotificationExtras(Intent intent) {
            Map<String, Object> extrasMap = new HashMap<String, Object>();
            for (String key : intent.getExtras().keySet()) {
                if (!IGNORED_EXTRAS_KEYS.contains(key)) {
                    if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                        extrasMap.put(key, intent.getIntExtra(key, 0));
                    } else {
                        extrasMap.put(key, intent.getStringExtra(key));
                    }
                }
            }
            return extrasMap;
        }

    }


    static void transmitMessageReceive(String message, Map<String, Object> extras) {
        if (instance == null) {
            return;
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("message", message);
        msg.put("extras", extras);

        JPushPlugin.instance.channel.invokeMethod("onReceiveMessage", msg);
    }

    static void transmitNotificationOpen(String title, String alert, Map<String, Object> extras) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("alert", alert);
        notification.put("extras", extras);
        Log.d("JPushPlugin/title", title);
        Log.d("JPushPlugin/alert", alert);
        Log.d("JPushPlugin/extras", extras.size() + "");

        JPushPlugin.openNotificationCache.add(notification);

        if (instance == null) {
            Log.d("JPushPlugin", "the instance is null");
            return;
        }
        if (instance.dartIsReady) {
            Log.d("JPushPlugin", "instance.dartIsReady is true");
            JPushPlugin.instance.channel.invokeMethod("onOpenNotification", notification);
            JPushPlugin.openNotificationCache.remove(notification);
        }

    }

    static void transmitNotificationReceive(String title, String alert, Map<String, Object> extras) {
        if (instance == null) {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("alert", alert);
        notification.put("extras", extras);
        JPushPlugin.instance.channel.invokeMethod("onReceiveNotification", notification);
    }

    static void transmitReceiveRegistrationId(String rId) {
        if (instance == null) {
            return;
        }

        JPushPlugin.instance.jpushDidinit = true;
        // try to clean getRid cache
        JPushPlugin.instance.scheduleCache();
    }

}
