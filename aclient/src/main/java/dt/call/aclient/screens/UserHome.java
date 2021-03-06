package dt.call.aclient.screens;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import dt.call.aclient.Const;
import dt.call.aclient.R;
import dt.call.aclient.Utils;
import dt.call.aclient.Vars;
import dt.call.aclient.background.async.LoginAsync;
import dt.call.aclient.background.async.OperatorCommand;
import dt.call.aclient.sqlite.SQLiteDb;

public class UserHome extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
	private static final String tag = "UserHome";
	private static final int CHRENAME = 1;
	private static final int CHRM = 2;

	private EditText actionbox;
	private FloatingActionButton call, add;
	private LinearLayout contactList;
	private boolean inEdit = false;
	private String contactInEdit; //whenever renaming a contact just change its nickname here and pass around this object
	private SQLiteDb sqliteDb;
	private BroadcastReceiver myReceiver;
	private ProgressDialog loginProgress = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_home);
		actionbox = findViewById(R.id.user_home_actionbox);
		call = findViewById(R.id.user_home_call);
		call.setOnClickListener(this);
		add = findViewById(R.id.user_home_add);
		add.setOnClickListener(this);
		contactList = findViewById(R.id.user_home_contact_list);

		/*
		sometimes the app gets randomly killed even after saying "ignore power savings optimization".
		since the ongoing notification goes to this screen, make sure that when it comes back from the dead,
		the annoying android context is available. be a good zombie coming back from the dead and not crash
		*/
		Vars.applicationContext = getApplicationContext();
		sqliteDb = SQLiteDb.getInstance(getApplicationContext());

		//build the contacts list if it doesn't already exist
		if(Vars.contactTable == null)
		{
			sqliteDb.populateContacts();
			sqliteDb.populatePublicKeys();
			for (String userName : Vars.contactTable.keySet())
			{
				addToContactList(userName, Vars.contactTable.get(userName));
			}
		}

		//receives the server response from clicking the 2 FAB buttons
		//	Click +: whether new contact to add is valid
		myReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				//Result of clicking the phone button. If the user you're trying to call doesn't exist
				//	the server treats it as not available. It IS the server's job to verify all input given to it.
				if(intent.getAction().equals(Const.BROADCAST_CALL))
				{
					String response = intent.getStringExtra(Const.BROADCAST_CALL_RESP);
					if(response.equals(Const.BROADCAST_CALL_TRY))
					{
						Intent startCall = new Intent(UserHome.this, CallMain.class);
						startCall.putExtra(CallMain.DIALING_MODE, true);
						startActivity(startCall);
					}
					else
					{
						//sendBroadcast will send call end to both callmain and userhome when a call ends.
						//don't need a random "user cannot be reached" message after ending a call.
						if(Vars.callEndIntentForCallMain)
						{
							Vars.callEndIntentForCallMain = false;
						}
						else
						{
							Utils.showOk(UserHome.this, getString(R.string.alert_user_home_cant_dial));
						}
					}
				}
				else if (intent.getAction().equals(Const.BROADCAST_LOGIN))
				{
					boolean ok = intent.getBooleanExtra(Const.BROADCAST_LOGIN_RESULT, false);
					if(loginProgress != null)
					{
						loginProgress.dismiss();
						loginProgress = null;
					}

					if(!ok)
					{
						if(!Utils.hasInternet())
						{//if the reason is no internet, say it
							Utils.showOk(UserHome.this, getString(R.string.alert_user_home_no_internet));
						}
						else
						{
							Utils.showOk(UserHome.this, getString(R.string.alert_login_failed));
						}
					}
				}
			}
		};

		//setup the command listener if it isn't already there
		//	it will already be there if you're coming back to the UserHome screen from doing something else

		//for some types of crashes it goes back to the UserHome screen but with no save data (and missing connections)
		if(Vars.commandSocket == null)
		{
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			boolean screenOn = false; //https://stackoverflow.com/questions/30718783/andorid-is-screen-on-or-off-deprecated-isscreenon
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
			{
				screenOn = pm.isInteractive();

			}
			else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH)
			{
				screenOn = pm.isScreenOn();
			}
			loginProgress = null;

			//for self restarts or call end --> home while screen is off, this will never go away if the screen is off when the progressdialog is launched
			if(screenOn)
			{
				loginProgress = ProgressDialog.show(UserHome.this, null, getString(R.string.progress_login));
				loginProgress.setOnKeyListener(new DialogInterface.OnKeyListener()
				{//https://stackoverflow.com/questions/10346011/how-to-handle-back-button-with-in-the-dialog
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
					{
						if (keyCode == KeyEvent.KEYCODE_BACK)
						{
							loginProgress.dismiss();
						}
						return false;
					}
				});
			}

			Utils.loadPrefs();
			new LoginAsync().execute();
		}

		Utils.releaseA9CallWakelock();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		//receiver must be reregistered when loading this screen from the back button
		IntentFilter homeFilters = new IntentFilter();
		homeFilters.addAction(Const.BROADCAST_CALL);
		homeFilters.addAction(Const.BROADCAST_LOGIN);
		registerReceiver(myReceiver, homeFilters);

		//check to make sure mic permission is set... can't call without a mic
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
		{
			AlertDialog.Builder mkdialog = new AlertDialog.Builder(this);
			mkdialog.setMessage(getString(R.string.alert_user_home_mic_perm))
					.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String[] perms = new String[]{Manifest.permission.RECORD_AUDIO};
							ActivityCompat.requestPermissions(UserHome.this, perms, Const.PERM_MIC);
							dialog.cancel();
						}
					});
			AlertDialog showOkAlert = mkdialog.create();
			showOkAlert.show();
		}

		//double check storage permissions. not fatal at this point because to get here you've logged on sucessfuly in the past.
		//	that means at some point, the credentials stored here were good.
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			AlertDialog.Builder mkdialog = new AlertDialog.Builder(this);
			mkdialog.setMessage(getString(R.string.alert_storage_perm))
					.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
							ActivityCompat.requestPermissions(UserHome.this, perms, Const.PERM_STORAGE);
							dialog.cancel();
						}
					});
			AlertDialog showOkAlert = mkdialog.create();
			showOkAlert.show();
		}

		//double check the contacts buttons are there when loading the screen in case it got killed for memory saving
		if(contactList.getChildCount() == 0)
		{
			Utils.logcat(Const.LOGD, tag, "contacts list linear layout is empty. repopulating contacts");
			for(String key : Vars.contactTable.keySet())
			{
				addToContactList(key, Vars.contactTable.get(key));
			}
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		//don't leak the receiver when leaving this screen
		unregisterReceiver(myReceiver);
	}

	@Override
	public void onClick(View v)
	{
		if(v instanceof Button) //any of the contact buttons. The FABs don't count as regular buttons
		{
			String userName = (String)v.getTag();
			actionbox.setText(userName);
		}
		else if (v == add)
		{
			String actionBoxName = actionbox.getText().toString();

			//check to see if anything was entered in the action box to begin with
			if(actionbox.getText().toString().equals(""))
			{
				return;
			}

			//check to see if the new contact to add already exists
			if(sqliteDb.contactExists(actionBoxName))
			{
				Utils.showOk(this, getString(R.string.alert_user_home_duplicate));
				actionbox.setText("");
			}
			else
			{
				//default nickname is the actual user name
				sqliteDb.insertContact(actionBoxName, actionBoxName);
				addToContactList(actionBoxName, actionBoxName);
				actionbox.setText("");
				Vars.contactTable.put(actionBoxName, actionBoxName);
			}
		}
		else if (v == call)
		{
			String who = actionbox.getText().toString();
			if(who.equals("")) //in case you pressed call while the action box was empty
			{
				return;
			}

			Vars.callWith = who;
			new OperatorCommand().execute(OperatorCommand.CALL);
		}
	}

	@Override
	public boolean onLongClick(View v)
	{
		if(v instanceof Button)
		{
			inEdit = true;
			contactInEdit = (String)v.getTag(); //for prefilling in the edit popup with the current nickname
			invalidateOptionsMenu();
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(inEdit)
		{
			getMenuInflater().inflate(R.menu.menu_main_edit, menu);
			try
			{
				getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorAccent)));
			}
			catch (NullPointerException n)
			{
				Utils.logcat(Const.LOGE, tag, "null pointer changing action bar to highlight color: ");
				Utils.dumpException(tag, n);
			}
		}
		else
		{
			getMenuInflater().inflate(R.menu.menu_main, menu);
			try
			{
				getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)));
			}
			catch (NullPointerException n)
			{
				Utils.logcat(Const.LOGE, tag, "null pointer changing action bar to normal color: ");
				Utils.dumpException(tag, n);
			}
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.menu_main_dblogs:
				startActivity(new Intent(this, LogViewer2.class));
				return true;
			case R.id.menu_main_exit:
				AlertDialog.Builder mkDialog = new AlertDialog.Builder(UserHome.this);
				mkDialog.setMessage(getString(R.string.alert_user_home_really_quit))
						.setPositiveButton(getString(R.string.alert_yes), new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								Utils.quit(UserHome.this);
							}
						})
						.setNegativeButton(getString(R.string.alert_no), new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								dialog.cancel();
							}
						});
				AlertDialog reallyQuit = mkDialog.create();
				reallyQuit.show();
				return true;
			case R.id.menu_main_settings:
				startActivity(new Intent(this, DTSettings.class));
				return true;
			case R.id.menu_main_about:
				startActivity(new Intent(this, About.class));
				return true;
			case R.id.menu_main_keymgmt:
				startActivity(new Intent(this, PublicKeyManagement.class));
				return true;
			case R.id.menu_edit_done:
				inEdit = false;
				invalidateOptionsMenu();
				return true;
			case R.id.menu_edit_edit:
				//This popup is more than just your average Show OK popup

				//setup the change nickname popup strings
				String currentNick = Vars.contactTable.get(contactInEdit);
				String instructions = getString(R.string.alert_user_home_rename_instructions).replace("CONTACT", contactInEdit);

				//setup the nickname popup
				View alertCustom = View.inflate(this, R.layout.alert_rename_contact, null);
				TextView instructionsView = alertCustom.findViewById(R.id.alert_rename_instructions);
				instructionsView.setText(instructions);
				final EditText chNick = alertCustom.findViewById(R.id.alert_rename_rename);
				chNick.setText(currentNick);

				//build the alert dialog now that everything is prefilled
				AlertDialog.Builder mkdialog = new AlertDialog.Builder(UserHome.this);
				mkdialog.setView(alertCustom)
						.setPositiveButton(R.string.alert_user_home_rename_button_ch, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								String newNick = chNick.getText().toString();
								if(newNick.equals(""))
								{
									newNick = contactInEdit; //no nickname, display the user name
								}
								sqliteDb.changeNickname(contactInEdit, newNick);
								refreshContacts(CHRENAME, contactInEdit, newNick);
								inEdit = false;
								invalidateOptionsMenu();

								//don't forget to update the in memory contact list
								Vars.contactTable.put(contactInEdit, newNick);
							}
						})
						.setNegativeButton(R.string.alert_user_home_rename_button_nvm, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								dialog.cancel();
								inEdit = false;
								invalidateOptionsMenu();
							}
						});
				AlertDialog chnick = mkdialog.create();
				chnick.show();
				return true;
			case R.id.menu_edit_rm:
				sqliteDb.deleteContact(contactInEdit);
				refreshContacts(CHRM, contactInEdit, contactInEdit); //doesn't matter what the nickname is. this contact is going away
				inEdit = false;
				invalidateOptionsMenu();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	//adds a new row to the contacts table view
	private void addToContactList(String userName, String nickName)
	{
		Button contactView = new Button(this);
		contactView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		contactView.setText(nickName);
		contactView.setAllCaps(false);
		contactView.setOnClickListener(UserHome.this);
		contactView.setOnLongClickListener(UserHome.this);
		contactView.setTag(userName);
		contactList.addView(contactView);
	}

	//updates the contact list after an edit or removal
	//avoid just redoing the entire list. edit the one you already have
	private void refreshContacts(int mode, String userName, String nickName)
	{
		for(int i=0; i < contactList.getChildCount(); i++)
		{
			Button childView = (Button)contactList.getChildAt(i);
			String tag = (String)childView.getTag();
			if(tag.equals(userName))
			{
				if (mode == CHRENAME)
				{
					childView.setText(nickName);
					return;
				}
				else if (mode == CHRM)
				{
					contactList.removeViewAt(i);
					actionbox.setText(""); //blank out action box which probably has this guy in it
					return;
				}
			}
		}
	}

	@Override
	public void onBackPressed()
	{

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch(requestCode)
		{
			case Const.PERM_MIC:
			{
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED)
				{
					//with mic denied, this app can't do anything useful
					Utils.quit(this);
				}
			}
			case Const.PERM_STORAGE:
			{
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED)
				{
					//storage denied isn't fatal because to get to the home screen you've logged in sucessfuly in the past.
					//	credentials might be good but might not be. be sure to warn.
					Utils.showOk(this, getString(R.string.alert_user_home_storage_denied_warning));
				}
			}
		}

	}
}
