package catena.server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;


public class RemoteServer {
	
	private static String USERNAME ="paramita";
	private static String PASSWORD ="Cat&Bear15";
	private static String host = "cte.comp.nus.edu.sg";
	private static int port=22;
	private Session session;
	
	public RemoteServer() throws JSchException {
		JSch jsch = new JSch();
		session = jsch.getSession(USERNAME, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(PASSWORD);
		session.connect();
	}
	
	public void copyFiles(File[] files, String dirRemote) throws JSchException, SftpException, FileNotFoundException {
		ChannelSftp channel = null;
		channel = (ChannelSftp)session.openChannel("sftp");
		channel.connect();
		channel.cd(dirRemote);
		for (File f : files) {
			File localFile = new File(f.getAbsolutePath());
			channel.put(new FileInputStream(localFile),localFile.getName());
		}
		channel.disconnect();
	}
	
	public void copyFile(File file, String dirRemote) throws JSchException, SftpException, FileNotFoundException {
		ChannelSftp channel = null;
		channel = (ChannelSftp)session.openChannel("sftp");
		channel.connect();
		channel.cd(dirRemote);
		File localFile = new File(file.getAbsolutePath());
		channel.put(new FileInputStream(localFile),localFile.getName());
		channel.disconnect();
	}
	
	public List<String> executeCommand(String cmd) throws JSchException, IOException {
		ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
		InputStream in = channelExec.getInputStream();
		channelExec.setCommand(cmd);
		channelExec.connect();
		
		List<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			result.add(line);
		}
		int exitStatus = channelExec.getExitStatus();
		channelExec.disconnect();
		if (exitStatus < 0) {
			//System.out.println("Done, but exit status not set!");
		} else if (exitStatus > 0) {
			//System.out.println("Done, but with error!");
		} else {
			//System.out.println("Done!");
		}
		return result;
	}
	
	public void disconnect() {
		session.disconnect();
	}

}
