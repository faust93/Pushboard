package com.faust93.pushboard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.util.Log;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 * Created by faust on 06.04.14.
 */
public class PushBoardService extends Service implements MqttCallback, Const {

    public static final String DEBUG_TAG = "PushBoardService";

    private static final String MQTT_CLIENT_ID = "PushBoard";

    private static final String	MQTT_THREAD_NAME = "MqttService[" + DEBUG_TAG + "]"; // Handler Thread ID

    private static String MQTT_BROKER = "xep.8800.org"; // Broker URL or IP Address
    private static int MQTT_PORT = 1883;	// Broker Port
    private static String BROADCAST_GROUP;

    public static final int	MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
    public static final int MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
    public static final int	MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )

    private static final int KEEP_ALIVE = 240000; // KeepAlive Interval in MS
    private static short MQTT_KEEP_ALIVE = 60 * 15;
    private static final String	MQTT_KEEP_ALIVE_TOPIC_FORAMT = "/users/%s/keepalive"; // Topic format for KeepAlives
    private static final byte[] MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
    private static final int	MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS

    private static final boolean MQTT_CLEAN_SESSION = false; // Start a clean session?

    private static final String MQTT_URL_FORMAT = "tcp://%s:%d"; // URL Format normally don't change

    private static final String ACTION_START = DEBUG_TAG + ".START"; // Action to start
    private static final String ACTION_STOP	= DEBUG_TAG + ".STOP"; // Action to stop
    private static final String ACTION_RESTART	= DEBUG_TAG + ".RESTART"; // Action to stop
    private static final String ACTION_KEEPALIVE= DEBUG_TAG + ".KEEPALIVE"; // Action to keep alive used by alarm manager
    private static final String ACTION_RECONNECT= DEBUG_TAG + ".RECONNECT"; // Action to reconnect


     // Device ID Format, add any prefix you'd like
    // Note: There is a 23 character limit you will get
    // An NPE if you go over that limit

    public static boolean mStarted = false; // Is the Client started?
    private String mDeviceId;
    private Handler mConnHandler;	// Seperate Handler thread for networking

    private MqttDefaultFilePersistence mDataStore; // Defaults to FileStore
    private MemoryPersistence mMemStore; // On Fail reverts to MemoryStore
    private MqttConnectOptions mOpts;	// Connection Options

    private MqttTopic mKeepAliveTopic;	// Instance Variable for Keepalive topic

    private MqttClient mClient;	// Mqtt Client

    private AlarmManager mAlarmManager;	// Alarm manager to perform repeating tasks
    private ConnectivityManager mConnectivityManager; // To check for connectivity changes

    private SharedPreferences   mPrefs;

    private boolean debugLog = false;

    /**
     * Start MQTT Client
     * @param /Context context to start the service with
     * @return void
     */
    public static void actionStart(Context ctx) {
        Log.i(DEBUG_TAG,"action start" );

        Intent i = new Intent(ctx, PushBoardService.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    /**
     * Stop MQTT Client
     * @param /context context to start the service with
     * @return void
     */
    public static void actionStop(Context ctx) {
        Intent i = new Intent(ctx, PushBoardService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    public static void actionRestart(Context ctx) {
        Intent i = new Intent(ctx, PushBoardService.class);
        i.setAction(ACTION_RESTART);
        ctx.startService(i);
    }
    /**
     * Send a KeepAlive Message
     * @param /Context context to start the service with
     * @return void
     */
    public static void actionKeepalive(Context ctx) {
        Intent i = new Intent(ctx, PushBoardService.class);
        i.setAction(ACTION_KEEPALIVE);
        ctx.startService(i);
    }

    /**
     * Initalizes the DeviceId and most instance variables
     * Including the Connection Handler, Datastore, Alarm Manager
     * and ConnectivityManager.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Secure.getString(getContentResolver(), Secure.ANDROID_ID));

        mPrefs =  getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
        thread.start();

        mConnHandler = new Handler(thread.getLooper());

//        try {
//            mDataStore = new MqttDefaultFilePersistence(getCacheDir().getAbsolutePath());
//        } catch(MqttPersistenceException e) {
//            e.printStackTrace();
            mDataStore = null;
            mMemStore = new MemoryPersistence();
//        }

        mOpts = new MqttConnectOptions();
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
        mOpts.setKeepAliveInterval(MQTT_KEEP_ALIVE);
// Do not set keep alive interval on mOpts we keep track of it with alarm's
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    /**
     * Service onStartCommand
     * Handles the action passed via the Intent
     *
     * @return START_REDELIVER_INTENT
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(intent != null) {

            String action = intent.getAction();

            Log.i(DEBUG_TAG, "Received action of " + action);

            if (action == null) {
                Log.i(DEBUG_TAG, "Starting service with no action\n Probably from a crash");
            } else {
                if (action.equals(ACTION_START)) {
                    Log.i(DEBUG_TAG, "Received ACTION_START");
                    start();
                } else if (action.equals(ACTION_STOP)) {
                    stop();
                } else if (action.equals(ACTION_RESTART)) {
                    stop();
                    start();
                } else if (action.equals(ACTION_KEEPALIVE)) {
                    keepAlive();
                } else if (action.equals(ACTION_RECONNECT)) {
                    if (isNetworkAvailable()) {
                        reconnectIfNecessary();
                    }
                }
            }
        }
             return START_STICKY;
          //   return START_REDELIVER_INTENT;

    }

    /**
     * Attempts connect to the Mqtt Broker
     * and listen for Connectivity changes
     * via ConnectivityManager.CONNECTVITIY_ACTION BroadcastReceiver
     */
    private synchronized void start() {

        if(mStarted) {
            Log.i(DEBUG_TAG,"Attempt to start while already started");
            return;
        }

        debugLog = mPrefs.getBoolean("enableLogging",false);

        if(hasScheduledKeepAlives()) {
            stopKeepAlives();
        }

        String brokerUrl = mPrefs.getString("brokerURL",null);
        if(brokerUrl != null)
            MQTT_BROKER = brokerUrl;

        String brokerPort = mPrefs.getString("brokerPort", "1883");
        try {
            MQTT_PORT = Integer.valueOf(brokerPort);
        } catch(Exception e){
            MQTT_PORT = 1883;
        }

        BROADCAST_GROUP = mPrefs.getString("broadcastGroup", null);

        connect();

        registerReceiver(mConnectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }
    /**
     * Attempts to stop the Mqtt client
     * as well as halting all keep alive messages queued
     * in the alarm manager
     */
    private synchronized void stop() {
        if(!mStarted && mClient == null) {
            Log.i(DEBUG_TAG,"Attemtpign to stop connection that isn't running");
            return;
        }
        if(debugLog)
            Log.d(DEBUG_TAG,"stop() mClient: " + mClient);

        if(mClient != null) {
            mConnHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mClient.disconnect();
                    } catch(MqttException ex) {
                        ex.printStackTrace();
                    }
                    mClient = null;
                    mStarted = false;

                    stopKeepAlives();
                }
            });
        }

       // unregisterReceiver(mConnectivityReceiver);
    }
    /**
     * Connects to the broker with the appropriate datastore
     */
    private synchronized void connect() {
        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
        Log.i(DEBUG_TAG,"Connecting with URL: " + url);
        try {
            if(mDataStore != null) {
                Log.i(DEBUG_TAG,"Connecting with DataStore");
                mClient = new MqttClient(url,mDeviceId,mDataStore);
            } else {
                Log.i(DEBUG_TAG,"Connecting with MemStore");
                mClient = new MqttClient(url,mDeviceId,mMemStore);
            }
        } catch(MqttException e) {
            e.printStackTrace();
        }

        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if(debugLog)
                        Log.i(DEBUG_TAG,"Attempting to connect..");

                    mClient.setCallback(PushBoardService.this);
                    mClient.connect(mOpts);

                    if(debugLog)
                        Log.i(DEBUG_TAG,"Connected. mClient: " + mClient);

                    String clientID = MQTT_CLIENT_ID + "/" + mDeviceId + "/#";
                    mClient.subscribe(clientID, MQTT_QOS_2);

                    if(BROADCAST_GROUP != null)
                        mClient.subscribe(MQTT_CLIENT_ID + "/" + BROADCAST_GROUP + "/#", MQTT_QOS_2);

                    mStarted = true; // Service is now connected

                    Log.i(DEBUG_TAG,"Successfully connected and subscribed starting keep alives");

                    startKeepAlives();
                } catch(MqttException e) {
                    if(debugLog)
                        Log.i(DEBUG_TAG,"Arrgh.. something goes wrong!");
                    mClient = null;
                    mStarted = false;
                    e.printStackTrace();
                }
            }
        });
    }
    /**
     * Schedules keep alives via a PendingIntent
     * in the Alarm Manager
     */
    private void startKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, PushBoardService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + KEEP_ALIVE,
                KEEP_ALIVE, pi);
    }
    /**
     * Cancels the Pending Intent
     * in the alarm manager
     */
    private void stopKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, PushBoardService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i , 0);
        mAlarmManager.cancel(pi);
    }
    /**
     * Publishes a KeepALive to the topic
     * in the broker
     */
    private synchronized void keepAlive() {
        if(isConnected()) {
            try {
                sendKeepAlive();
                return;
            } catch(MqttConnectivityException ex) {
                ex.printStackTrace();
                reconnectIfNecessary();
            } catch(MqttPersistenceException ex) {
                ex.printStackTrace();
                stop();
            } catch(MqttException ex) {
                ex.printStackTrace();
                stop();
            }
        }
    }
    /**
     * Checkes the current connectivity
     * and reconnects if it is required.
     */
    private synchronized void reconnectIfNecessary() {

        if(debugLog)
            Log.d(DEBUG_TAG,"mStarted: " + mStarted + " mClient: " + mClient);

        if(mClient == null ) {
            connect();
        }
    }
    /**
     * Query's the NetworkInfo via ConnectivityManager
     * to return the current connected state
     * @return boolean true if we are connected false otherwise
     */
    private boolean isNetworkAvailable() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();

        return (info == null) ? false : info.isConnected();
    }
    /**
     * Verifies the client State with our local connected state
     * @return true if its a match we are connected false if we aren't connected
     */
    private boolean isConnected() {
        if(mStarted && mClient != null && !mClient.isConnected()) {
            Log.i(DEBUG_TAG,"Mismatch between what we think is connected and what is connected");
        }

        if(mClient != null) {
            return (mStarted && mClient.isConnected()) ? true : false;
        }

        return false;
    }
    /**
     * Receiver that listens for connectivity chanes
     * via ConnectivityManager
     */
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(DEBUG_TAG, "Connectivity changed..");

            if(isNetworkAvailable()) {
                reconnectIfNecessary();
            }

     //       ConnectivityManager connectivityManager = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
     //       NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
     //       boolean hasConnectivity = (currentNetworkInfo != null && currentNetworkInfo.isConnected()) ? true : false;
     //       Log.i(DEBUG_TAG, "Connectivity changed: connected=" + hasConnectivity);
     //       if (hasConnectivity) {
     //           reconnectIfNecessary();
     //       } else if (mClient != null) {
     //           stop();
     //       }
        }
    };
    /**
     * Sends a Keep Alive message to the specified topic
     * @see /MQTT_KEEP_ALIVE_MESSAGE
     * @see /MQTT_KEEP_ALIVE_TOPIC_FORMAT
     * @return MqttDeliveryToken specified token you can choose to wait for completion
     */
    private synchronized MqttDeliveryToken sendKeepAlive()
            throws MqttConnectivityException, MqttException {
        if(!isConnected())
            throw new MqttConnectivityException();

        if(mKeepAliveTopic == null) {
            mKeepAliveTopic = mClient.getTopic(
                    String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORAMT,mDeviceId));
        }

        Log.i(DEBUG_TAG,"Sending Keepalive to " + MQTT_BROKER);

        MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(MQTT_KEEP_ALIVE_QOS);

        return mKeepAliveTopic.publish(message);
    }
    /**
     * Query's the AlarmManager to check if there is
     * a keep alive currently scheduled
     * @return true if there is currently one scheduled false otherwise
     */
    private synchronized boolean hasScheduledKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, PushBoardService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_NO_CREATE);

        return (pi != null) ? true : false;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    /**
     * Connectivity Lost from broker
     */
    @Override
    public void connectionLost(Throwable arg0) {

        Log.d(DEBUG_TAG, "Disconnected from broker", arg0);
        stop();

         if(isNetworkAvailable()) {
            reconnectIfNecessary();
        }
    }
    /**
     * Publish Message Completion
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {

    }
    /**
     * Received Message from broker
     */
    @Override
    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        //showNotification(new String(message.getPayload()));

        int msgType = 0;
        String TYPE_PATTERN = "^#type:(\\d)";

        Log.i(DEBUG_TAG," Topic:\t" + topic +
                " Message:\t" + new String(message.getPayload()) +
                " QoS:\t" + message.getQos());

        Pattern p = Pattern.compile(".*/(.*)$");
        Pattern typeMatcher = Pattern.compile(TYPE_PATTERN);

        Matcher m = p.matcher(topic);
        m.lookingAt();
        String subj = m.group(1);

        String messageBody = new String(message.getPayload(), "UTF-8");

        m = typeMatcher.matcher(messageBody);
        if(m.lookingAt()){
            String _mType = m.group(1);
            if (_mType.equals("0"))
                msgType = MSG_TYPE_OK;
            else if (_mType.equals("1"))
                msgType = MSG_TYPE_WARN;
            else if (_mType.equals("2"))
                msgType = MSG_TYPE_CRITICAL;
        }

        messageBody = messageBody.replaceFirst(TYPE_PATTERN,"");

        DBhelper db = new DBhelper(this);
        long id = db.addMessage(new pbMessage(msgType,subj,messageBody));
        if(id!=0) {
            Intent i = new Intent(NOTIFY_ACTION);
            i.putExtra("id",id);
            i.putExtra("type", msgType);
            i.putExtra("subj", subj);
            i.putExtra("msg", messageBody);
            sendOrderedBroadcast(i, null);
        }
    }
    /**
     * MqttConnectivityException Exception class
     */
    private class MqttConnectivityException extends Exception {
        private static final long serialVersionUID = -7385866796799469420L;
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(debugLog)
            Log.d(DEBUG_TAG,"onTaskRemoved: restart service");

        // TODO Auto-generated method stub

        Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setAction(ACTION_START);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +2000, restartServicePI);

    }

}
