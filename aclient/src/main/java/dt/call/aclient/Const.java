package dt.call.aclient;

import android.os.Build;
import android.util.Base64;

/**
 * Created by Daniel on 1/17/16.
 *
 * Similar to server's const.h holds various constants to force standardizing their names
 */
public class Const
{
	public static final String PACKAGE_NAME = "dt.call.aclient";

	//shared preference keys
	public static final String PREFSFILE = "call_prefs"; //file name of the shared preferences
	public static final String PREF_ADDR = "server_address";
	public static final String PREF_COMMANDPORT = "command_port";
	public static final String PREF_MEDIAPORT = "media_port";
	public static final String PREF_UNAME = "username";
	public static final String PREF_LOG = "log";
	public static final String PREF_LOGFILE = "log_file";
	public static final String PREF_LOGFILE_A = "log_a";
	public static final String PREF_LOGFILE_B = "log_b";
	public static final String INTERNAL_PRIVATEKEY_FILE = "user_privatekey_binary";
	public static final String INTERNAL_SERVER_PUBLICKEY_FILE = "server_publickey_binary";

	//file selection codes
	public static final int SELECT_SELF_PRIVATE_SODIUM = 2;
	public static final int SELECT_USER_PUBLIC_SODIUM = 3;
	public static final int SELECT_SERVER_PUBLIC_SODIUM = 4;

	//android permission request codes
	public static final int PERM_STORAGE = 1;
	public static final int PERM_MIC = 2;

	//heartbeat byte
	public static final String JBYTE = "D";
	public static final int SIZE_COMMAND = 2048;
	public static final int SIZE_MAX_UDP = 1400;

	//nobody: the default value for when not in a call
	public static final String nobody = "(nobody)";

	//log.e/d/i wrapper to avoid wasting cpu for logging
	public static final int LOGE = 1;
	public static final int LOGD = 2;
	public static final int LOGW = 3;

	//when cmd listener dies
	public static final String BROADCAST_RELOGIN = "dt.call.aclient.relogin";

	//broadcast intent shared by call main and incoming call screen
	//both need the call end signal
	// (either the person hung or changed his mind and cancelled before you answered)
	//only call main responds to call accept
	public static final String BROADCAST_CALL = "dt.call.aclient.notify_call_info";
	public static final String BROADCAST_CALL_RESP = "call_response";
	public static final String BROADCAST_CALL_TRY = "try";
	public static final String BROADCAST_CALL_START = "start";
	public static final String BROADCAST_CALL_END = "end";
	public static final String BROADCAST_CALL_MIC = "mic";

	//broadcasting login result
	public static final String BROADCAST_LOGIN = "dt.call.aclient.broadcast_login";
	public static final String BROADCAST_LOGIN_RESULT = "login_result";

	//whether or not to use the jobservice method of internet detection
	public static final String BROADCAST_HAS_INTERNET = "dt.call.aclient.HAS_INTERNET";
	public static final boolean NEEDS_MANUAL_INTERNET_DETECTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

	//persistent notification id
	public static final int STATE_NOTIFICATION_ID = 1;
	public static final String STATE_NOTIFICATION_CHANNEL = "dt.call.aclient.state";
	public static final String STATE_NOTIFICATION_NAME = "App State";

	public static final String INCOMING_NOTIFICATION_CHANNEL = "dt.call.aclient.incoming";
	public static final String INCOMING_NOTIFICATION_NAME = "Incoming Calls";

	//related to alarm receiver and alarm stuff
	public static final int ALARM_RETRY_ID = 1234;
	public static final int ALARM_HEARTBEAT_ID = 999;
	//alarm broadcast fore retry shared with broadcast for dead command listener BROADCAST_RELOGIN
	public static final String ALARM_ACTION_HEARTBEAT = "do_heartbeat";
	public static final int STD_TIMEOUT = 5*60*1000;

	//timeout (IN SECONDS) before giving up on calling someone
	public static final int CALL_TIMEOUT = 20;

	public static final int SIZEOF_INT = 4;
	public static final int STRINGIFY_EXPANSION = 3;

	public static final String EXTRA_UNAME = "user_name_extra";
	public static final int UNSIGNED_CHAR_MAX = 0xff;
}