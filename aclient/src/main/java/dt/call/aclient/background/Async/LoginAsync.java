package dt.call.aclient.background.Async;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertificateException;

import dt.call.aclient.Const;
import dt.call.aclient.Utils;
import dt.call.aclient.Vars;

/**
 * Created by Daniel on 1/21/16.
 *
 * Does the login on a separate thread because you're not allowed to do network on the main
 */
public class LoginAsync extends AsyncTask<String, String, Boolean>
{
	private String uname, passwd;
	private static final String tag = "Login Async Task";
	
	public LoginAsync(String cuname, String cpasswd)
	{
		uname = cuname;
		passwd = cpasswd;
	}
	@Override
	//params = adddress, command port, media port, certificate input stream, username, password
	protected Boolean doInBackground(String... params)
	{
		try
		{
			//TODONE: find a way to stop if the server is notavailable
			//http://stackoverflow.com/a/34228756
			//check if server is available first before committing to anything
			//	otherwise this proccess will stall. host not available trips timeout exception
			Socket diag = new Socket();
			diag.connect(new InetSocketAddress(Vars.serverAddress, Vars.commandPort), 2000);

			//send login command
			Vars.commandSocket = Utils.mkSocket(Vars.serverAddress, Vars.commandPort, Vars.expectedCertDump);
			String login = Const.JBYTE + Utils.generateServerTimestamp() + "|login|" + uname + "|" + passwd;
			Vars.commandSocket.getOutputStream().write(login.getBytes());

			//read response
			InputStream cmdin = Vars.commandSocket.getInputStream();
			BufferedReader cmdTxtIn = new BufferedReader(new InputStreamReader(cmdin));
			String loginresp = cmdTxtIn.readLine();
			Utils.logcat(Const.LOGD, tag, loginresp);

			//process login response
			String[] respContents = loginresp.split("\\|");
			if(respContents.length != 4)
			{
				Utils.logcat(Const.LOGD, tag, "Server response imporoperly formatted");
				return false; //not a legitimate server response
			}
			if(!(respContents[1].equals("resp") && respContents[2].equals("login")))
			{
				Utils.logcat(Const.LOGD, tag, "Server response CONTENTS imporperly formateed");
				return false; //server response doesn't make sense
			}
			long ts = Long.valueOf(respContents[0]);
			if(!Utils.validTS(ts))
			{
				Utils.logcat(Const.LOGD, tag, "Server had an unacceptable timestamp");
				return false;
			}
			Vars.sessionid = Long.valueOf(respContents[3]);
			Utils.logcat(Const.LOGD, tag, "Established command socket with sessionid: " + Vars.sessionid);

			//establish media socket
			publishProgress("Establishing media port");
			Vars.mediaSocket = Utils.mkSocket(Vars.serverAddress, Vars.mediaPort, Vars.expectedCertDump);
			String associateMedia = Const.JBYTE + Utils.generateServerTimestamp() + "|" + Vars.sessionid;
			Vars.mediaSocket.getOutputStream().write(associateMedia.getBytes());
			Vars.mediaSocket.getOutputStream().write("testing testing 1 2 3".getBytes()); //sometimes java socket craps out
			return true;
		}
		catch (CertificateException c)
		{
			Utils.logcat(Const.LOGD, tag, "server certificate didn't match the expected");
			return false;
		}
		catch (Exception i)
		{
			Utils.dumpException(tag, i);
			return false;
		}
	}
}
