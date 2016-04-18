package dt.call.aclient.background;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import dt.call.aclient.Const;
import dt.call.aclient.Utils;
import dt.call.aclient.Vars;
import dt.call.aclient.background.async.KillSocketsAsync;
import dt.call.aclient.sqlite.DB;
import dt.call.aclient.sqlite.DBLog;

/**
 * Created by Daniel on 1/22/16.
 *
 * Once logged in. Manages setting up the CmdListener whenever wifi/lte drops, switches, reconnects
 * Manages the heartbeat service which preiodically checks to see if the connections are really good or not.
 */
public class BackgroundManager extends BroadcastReceiver
{
	private static final String tag = "BackgroundManager";
	private DB db;

	public BackgroundManager()
	{
		db = new DB(Vars.applicationContext);
	}

	@Override
	public void onReceive(final Context context, Intent intent)
	{
		Utils.logcat(Const.LOGD, tag, "received broadcast intent");
		Utils.initAlarmVars(); //double check to make sure these things are setup
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if(Vars.uname == null || Vars.passwd == null)
		{
			//if the person hasn't logged in then there's no way to start the command listener
			//	since you won't have a command socket to listen on
			Utils.logcat(Const.LOGD, tag, "can't login when there is no username/password information");
			return;
		}


		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			Utils.logcat(Const.LOGD, tag, "Got a connectivity event from android");
			if(intent.getExtras() != null && intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false))
			{//internet lost case
				db.insertLog(new DBLog(tag, "android OS says there is no internet"));
				Utils.logcat(Const.LOGD, tag, "Internet was lost");

				//Apparently you can't close a socket from here because it's on the UI thread???
				Vars.dontRestart = true; //why bother when there's no internet
				new KillSocketsAsync().execute();

				Vars.hasInternet = false;
				manager.cancel(Vars.pendingHeartbeat);
				manager.cancel(Vars.pendingRetries);
			}
			else
			{
				//internet reconnected case
				// don't immediately try to reconnect on fail in case the person has to do a wifi sign in
				//	or other things
				db.insertLog(new DBLog(tag, "android OS says there is internet"));
				Utils.logcat(Const.LOGD, tag, "Internet was reconnected");
				Vars.hasInternet = true;

				manager.cancel(Vars.pendingRetries);
				manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Const.ONE_MIN, Vars.pendingRetries);
			}
		}
		else if (intent.getAction().equals(Const.BROADCAST_BK_CMDDEAD))
		{
			db.insertLog(new DBLog(tag, "dead command listener intent received"));
			Utils.logcat(Const.LOGD, tag, "command listener dead received");
			if(!Vars.hasInternet)
			{
				db.insertLog(new DBLog(tag, "dead command listener intent received but no internet to restart"));
				Utils.logcat(Const.LOGW, tag, "no internet connection to restart command listener");
				return;
			}
			manager.cancel(Vars.pendingRetries);
			manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Const.ONE_MIN, Vars.pendingRetries);
		}
	}
}
