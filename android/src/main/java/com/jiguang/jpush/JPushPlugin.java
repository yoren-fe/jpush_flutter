package com.jiguang.jpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

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
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * JPushPlugin
 */
public class JPushPlugin implements FlutterPlugin, MethodCallHandler {

  private int sequence;
  private Context context;
  private List<Result> getRidCache;
  public static JPushPlugin instance;
  public static MethodChannel channel;
  private boolean dartIsReady = false;
  private boolean jpushDidinit = false;
  final SparseArray<Result> callbackMap;
  private final static String CHANNEL_NAME = "jpush";
  private static List<Map<String, Object>> openNotificationCache = new ArrayList<>();

  public JPushPlugin() {
    this.callbackMap = new SparseArray<>();
    this.sequence = 0;
    this.getRidCache = new ArrayList<>();
    instance = this;
  }

  public static void registerWith(PluginRegistry.Registrar registrar) {
    JPushPlugin instance = new JPushPlugin();
    channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
    instance.context = registrar.context();
    channel.setMethodCallHandler(instance);
  }

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    this.context = binding.getApplicationContext();
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
    teardownChannel();
  }

  private void teardownChannel() {
    channel.setMethodCallHandler(null);
    channel = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;
      case "setup":
        setup(call, result);
        break;
      case "setTags":
        setTags(call, result);
        break;
      case "cleanTags":
        cleanTags(call, result);
        break;
      case "addTags":
        addTags(call, result);
        break;
      case "deleteTags":
        deleteTags(call, result);
        break;
      case "getAllTags":
        getAllTags(call, result);
        break;
      case "setAlias":
        setAlias(call, result);
        break;
      case "deleteAlias":
        deleteAlias(call, result);
        break;
      case "stopPush":
        stopPush(call, result);
        break;
      case "resumePush":
        resumePush(call, result);
        break;
      case "clearAllNotifications":
        clearAllNotifications(call, result);
        break;
      case "getLaunchAppNotification":
        getLaunchAppNotification(call, result);
        break;
      case "getRegistrationID":
        getRegistrationID(call, result);
        break;
      case "sendLocalNotification":
        sendLocalNotification(call, result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void setup(MethodCall call, Result result) {
    HashMap<String, Object> map = call.arguments();
    boolean debug = (boolean) map.get("debug");
    JPushInterface.setDebugMode(debug);

    JPushInterface.init(context);            // 初始化 JPush

    String channel = (String) map.get("channel");
    JPushInterface.setChannel(context, channel);

    JPushPlugin.instance.dartIsReady = true;

    // try to clean getRid cache
    scheduleCache();
  }

  private void scheduleCache() {
    if (dartIsReady) {
      // try to shedule notifcation cache
      for (Map<String, Object> notification : JPushPlugin.openNotificationCache) {
        JPushPlugin.instance.channel.invokeMethod("onOpenNotification", notification);
        JPushPlugin.openNotificationCache.remove(notification);
      }
    }
    String rid = JPushInterface.getRegistrationID(context);
    boolean ridAvailable = rid != null && !rid.isEmpty();
    if (ridAvailable && dartIsReady && instance != null) {
      // try to schedule get rid cache
      for (Result res : JPushPlugin.instance.getRidCache) {
        res.success(rid);
        JPushPlugin.instance.getRidCache.remove(res);
      }
    }
  }

  private void setTags(MethodCall call, Result result) {
    List<String> tagList = call.arguments();
    Set<String> tags = new HashSet<>(tagList);
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.setTags(context, sequence, tags);
  }

  private void cleanTags(MethodCall call, Result result) {
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.cleanTags(context, sequence);
  }

  private void addTags(MethodCall call, Result result) {
    List<String> tagList = call.arguments();
    Set<String> tags = new HashSet<>(tagList);
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.addTags(context, sequence, tags);
  }

  private void deleteTags(MethodCall call, Result result) {
    List<String> tagList = call.arguments();
    Set<String> tags = new HashSet<>(tagList);
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.deleteTags(context, sequence, tags);
  }

  private void getAllTags(MethodCall call, Result result) {
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.getAllTags(context, sequence);
  }

  private void setAlias(MethodCall call, Result result) {
    String alias = call.arguments();
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.setAlias(context, sequence, alias);
  }

  private void deleteAlias(MethodCall call, Result result) {
    String alias = call.arguments();
    sequence += 1;
    callbackMap.put(sequence, result);
    JPushInterface.deleteAlias(context, sequence);
  }

  private void stopPush(MethodCall call, Result result) {
    JPushInterface.stopPush(context);
  }

  private void resumePush(MethodCall call, Result result) {
    JPushInterface.resumePush(context);
  }

  private void clearAllNotifications(MethodCall call, Result result) {
    JPushInterface.clearAllNotifications(context);
  }

  private void getLaunchAppNotification(MethodCall call, Result result) {

  }

  private void getRegistrationID(MethodCall call, Result result) {

    String rid = JPushInterface.getRegistrationID(context);
    if (rid == null || rid.isEmpty()) {
      getRidCache.add(result);
    } else {
      result.success(rid);
    }
  }


  private void sendLocalNotification(@NonNull MethodCall call, @NonNull Result result) {
    try {
      HashMap<String, Object> map = call.arguments();
      if (map != null) {
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

        JPushInterface.addLocalNotification(context, ln);
      }
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
      if (action != null) {
        switch (action) {
          case JPushInterface.ACTION_REGISTRATION_ID:
            JPushPlugin.transmitReceiveRegistrationId();
            break;
          case JPushInterface.ACTION_MESSAGE_RECEIVED:
            handlingMessageReceive(intent);
            break;
          case JPushInterface.ACTION_NOTIFICATION_RECEIVED:
            handlingNotificationReceive(intent);
            break;
          case JPushInterface.ACTION_NOTIFICATION_OPENED:
            if (instance != null) {
              instance.dartIsReady = AppUtils.isAppForground(context);
            }
            handlingNotificationOpen(context, intent);
            break;
        }
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

    private void handlingNotificationReceive(Intent intent) {
      String title = intent.getStringExtra(JPushInterface.EXTRA_NOTIFICATION_TITLE);
      String alert = intent.getStringExtra(JPushInterface.EXTRA_ALERT);
      Map<String, Object> extras = getNotificationExtras(intent);
      JPushPlugin.transmitNotificationReceive(title, alert, extras);
    }

    private Map<String, Object> getNotificationExtras(Intent intent) {
      if (intent.getExtras() == null) {
        return new HashMap<>();
      }
      Map<String, Object> extrasMap = new HashMap<>();
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


  private static void transmitMessageReceive(String message, Map<String, Object> extras) {
    if (instance == null) {
      return;
    }
    Map<String, Object> msg = new HashMap<>();
    msg.put("message", message);
    msg.put("extras", extras);

    JPushPlugin.instance.channel.invokeMethod("onReceiveMessage", msg);
  }

  private static void transmitNotificationOpen(String title, String alert, Map<String, Object> extras) {
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

  private static void transmitNotificationReceive(String title, String alert, Map<String, Object> extras) {
    if (instance == null) {
      return;
    }

    Map<String, Object> notification = new HashMap<>();
    notification.put("title", title);
    notification.put("alert", alert);
    notification.put("extras", extras);
    JPushPlugin.instance.channel.invokeMethod("onReceiveNotification", notification);
  }

  private static void transmitReceiveRegistrationId() {
    if (instance == null) {
      return;
    }

    JPushPlugin.instance.jpushDidinit = true;
    // try to clean getRid cache
    JPushPlugin.instance.scheduleCache();
  }

}
