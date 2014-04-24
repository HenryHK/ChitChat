package com.henryhan.chitchat.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.henryhan.chitchat.R;
import com.henryhan.chitchat.main.MainActivity;
import com.henryhan.chitchat.utils.ByteAndInt;
import com.henryhan.chitchat.utils.Constant;
import com.henryhan.chitchat.utils.FileName;
import com.henryhan.chitchat.utils.FileState;
import com.henryhan.chitchat.utils.Message;
import com.henryhan.chitchat.utils.Person;

public class MainService extends Service {
	private ServiceBinder sBinder = new ServiceBinder();//Service binder
	private static ArrayList<Map<Integer,Person>> children = new ArrayList<Map<Integer,Person>>();//all users
	private static Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();//online users
	private static ArrayList<Integer> personKeys = new ArrayList<Integer>();//all id
	private static Map<Integer,List<Message>> msgContainer = new HashMap<Integer,List<Message>>();//all messages
	private SharedPreferences pre = null;
	private SharedPreferences.Editor editor = null;
	private WifiManager wifiManager = null;
	private ServiceBroadcastReceiver receiver = null;
	public InetAddress localInetAddress = null;
	private String localIp = null;
	private byte[] localIpBytes = null; 
	private byte[] regBuffer = new byte[Constant.bufferSize];//network reg buffer
	private byte[] msgSendBuffer = new byte[Constant.bufferSize];//message buffer
	private byte[] fileSendBuffer = new byte[Constant.bufferSize];//file buffer
	private byte[] talkCmdBuffer = new byte[Constant.bufferSize];//voice call buffer
	private static Person me = null;//myself
	private CommunicationBridge comBridge = null;//communication module
	//private Notification notification = null;
	//private NotificationManager notificationManager = null;
	//private PendingIntent pd = null;
	//final static int NOTIFICATION_ID = 123;
	
	private WakeLock wakeLock = null;
	private PowerManager powerManager = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return sBinder;
	}
	@Override
	public boolean onUnbind(Intent intent) {
		return false;
	}
	@Override
	public void onRebind(Intent intent) {
		
	}
	@Override
	public void onCreate() {
		
	}
	@Override
	public void onStart(Intent intent, int startId) {
		initCmdBuffer();
		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		new CheckNetConnectivity().start();
		
		comBridge = new CommunicationBridge();
		comBridge.start();
		
		pre = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pre.edit();
		
		regBroadcastReceiver();
		getMyInfomation();
		new UpdateMe().start();//register in network
		new CheckUserOnline().start();//check user
		sendPersonHasChangedBroadcast();//send broadcast that new person enter
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
	    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	                "MyWakelockTag");
	    wakeLock.acquire();// keep service alive
		System.out.println("Service started...");
	}
	
	//bind service
	public class ServiceBinder extends Binder{
		public MainService getService(){
			return MainService.this;
		}
	}
	
    //get my information
    private void getMyInfomation(){
    	SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
    	int iconId = pre.getInt("headIconId", R.drawable.profile);
    	String nickeName = pre.getString("nickeName", "No name");
    	int myId = pre.getInt("myId", Constant.getMyId());
		editor.putInt("myId", myId);
		editor.commit();
		
    	if(null==me)me = new Person();
    	me.personHeadIconId = iconId;
    	me.personNickeName = nickeName;
    	me.personId = myId;
    	me.ipAddress = localIp;
    	
    	//register user
    	System.arraycopy(ByteAndInt.int2ByteArray(myId), 0, regBuffer, 6, 4);
    	System.arraycopy(ByteAndInt.int2ByteArray(iconId), 0, regBuffer, 10, 4);
    	for(int i=14;i<44;i++)regBuffer[i] = 0;
    	byte[] nickeNameBytes = nickeName.getBytes();
    	System.arraycopy(nickeNameBytes, 0, regBuffer, 14, nickeNameBytes.length);
    	
    	//buffer
    	System.arraycopy(ByteAndInt.int2ByteArray(myId), 0, talkCmdBuffer, 6, 4);
    	System.arraycopy(ByteAndInt.int2ByteArray(iconId), 0, talkCmdBuffer, 10, 4);
    	for(int i=14;i<44;i++)talkCmdBuffer[i] = 0;//把原来的昵称内容清空
    	System.arraycopy(nickeNameBytes, 0, talkCmdBuffer, 14, nickeNameBytes.length);
    }
	
	private String getCurrentTime(){
		Date date = new Date();
		return date.toLocaleString();
	}

    //get IP address
	private class CheckNetConnectivity extends Thread {
		public void run() {
			try {
				if (!wifiManager.isWifiEnabled()) {
					wifiManager.setWifiEnabled(true);
				}
				
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							if(inetAddress.isReachable(1000)){
								localInetAddress = inetAddress;
								localIp = inetAddress.getHostAddress().toString();
								localIpBytes = inetAddress.getAddress();
								System.arraycopy(localIpBytes,0,regBuffer,44,4);
							}
						}
					}
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		};
	};
	
	//initialize buffer
	private void initCmdBuffer(){
		
		for(int i=0;i<Constant.bufferSize;i++)regBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, regBuffer, 0, 3);
		regBuffer[3] = Constant.CMD80;
		regBuffer[4] = Constant.CMD_TYPE1;
		regBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)msgSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, msgSendBuffer, 0, 3);
		msgSendBuffer[3] = Constant.CMD81;
		msgSendBuffer[4] = Constant.CMD_TYPE1;
		msgSendBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)fileSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, fileSendBuffer, 0, 3);
		fileSendBuffer[3] = Constant.CMD82;
		fileSendBuffer[4] = Constant.CMD_TYPE1;
		fileSendBuffer[5] = Constant.OPR_CMD1;
		
		for(int i=0;i<Constant.bufferSize;i++)talkCmdBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, talkCmdBuffer, 0, 3);
		talkCmdBuffer[3] = Constant.CMD83;
		talkCmdBuffer[4] = Constant.CMD_TYPE1;
		talkCmdBuffer[5] = Constant.OPR_CMD1;
	}

	public ArrayList<Map<Integer,Person>> getChildren(){
		return children;
	}

	public ArrayList<Integer> getPersonKeys(){
		return personKeys;
	}

	public List<Message> getMessagesById(int personId){
		//notificationManager.notify(NOTIFICATION_ID, notification);
		return msgContainer.get(personId);
	}

	public int getMessagesCountById(int personId){
		List<Message> msgs = msgContainer.get(personId);
		if(null!=msgs){
			return msgs.size();
		}else {
			return 0;
		}
	}
	
	//heartbeat once per 10 seconds
	boolean isStopUpdateMe = false;
	private class UpdateMe extends Thread{
		@Override
		public void run() {
			while(!isStopUpdateMe){
				try{
					comBridge.joinOrganization();
					sleep(1000);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	//check user
	private class CheckUserOnline extends Thread{
		@Override
		public void run() {
			super.run();
			boolean hasChanged = false;
			while(!isStopUpdateMe){
				if(childrenMap.size()>0){
					Set<Integer> keys = childrenMap.keySet();
					for (Integer key : keys) {
						if(System.currentTimeMillis()-childrenMap.get(key).timeStamp>15000){
							childrenMap.remove(key);
							personKeys.remove(Integer.valueOf(key));
							hasChanged = true;
						}
					}
				}
				if(hasChanged)sendPersonHasChangedBroadcast();
				try {sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
	}
	
	//user updated broadcast
	private void sendPersonHasChangedBroadcast(){
		Intent intent = new Intent();
		intent.setAction(Constant.personHasChangedAction);
		sendBroadcast(intent);
	}
	
	//register receiver
	private void regBroadcastReceiver(){
		receiver = new ServiceBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constant.WIFIACTION);
		filter.addAction(Constant.ETHACTION);
		filter.addAction(Constant.updateMyInformationAction);
		filter.addAction(Constant.refuseReceiveFileAction);
		filter.addAction(Constant.imAliveNow);
		registerReceiver(receiver, filter);
	}
	
	//broadcast receiver
	private class ServiceBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constant.WIFIACTION) || intent.getAction().equals(Constant.ETHACTION)){
				new CheckNetConnectivity().start();
			}else if(intent.getAction().equals(Constant.updateMyInformationAction)){
				getMyInfomation();
				comBridge.joinOrganization();
			}else if(intent.getAction().equals(Constant.refuseReceiveFileAction)){
				comBridge.refuseReceiveFile();
			}else if(intent.getAction().equals(Constant.imAliveNow)){
				
			}
		}
	}
	
	public void sendMsg(int personId,String msg){
		comBridge.sendMsg(personId, msg);
	}
	
	public void sendFiles(int personId,ArrayList<FileName> files){
		comBridge.sendFiles(personId, files);
	}
	
	public void receiveFiles(String fileSavePath){
		comBridge.receiveFiles(fileSavePath);
	}
	
	public ArrayList<FileState> getReceivedFileNames(){
		return comBridge.getReceivedFileNames();
	}
	
	public ArrayList<FileState> getBeSendFileNames(){
		return comBridge.getBeSendFileNames();
	}
	
	public void startTalk(int personId){
		comBridge.startTalk(personId);
	}
	
	public void stopTalk(int personId){
		comBridge.stopTalk(personId);
	}
	
	public void acceptTalk(int personId){
		comBridge.acceptTalk(personId);
	}
	
	@Override
	public void onDestroy() {
		comBridge.release();
		unregisterReceiver(receiver);
		isStopUpdateMe = true;
		wakeLock.release();
		System.out.println("Service on destory...");
	}
	
	//========================communication module=======================================================
	private class CommunicationBridge extends Thread{
		private MulticastSocket multicastSocket = null;
		private byte[] recvBuffer = new byte[Constant.bufferSize];
		private int fileSenderUid = 0;//store id
		private boolean isBusyNow = false;//whether is busy receiving file now
		private String fileSavePath = null;//received file path
		private boolean isStopTalk = false;//stop
		private ArrayList<FileName> tempFiles = null;//file name
		private int tempUid = 0;//user id who receive file
		private ArrayList<FileState> receivedFileNames = new ArrayList<FileState>();
		private ArrayList<FileState> beSendFileNames = new ArrayList<FileState>();
		
		private FileHandler fileHandler = null;//file handler
		private AudioHandler audioHandler = null;//audio handler
		
		public CommunicationBridge(){
			fileHandler = new FileHandler();
			fileHandler.start();
			
			audioHandler = new AudioHandler();
			audioHandler.start();
		}

		//socket
		@Override
		public void run() {
			super.run();
			try {
				multicastSocket = new MulticastSocket(Constant.PORT);
				multicastSocket.joinGroup(InetAddress.getByName(Constant.MULTICAST_IP));
				System.out.println("Socket started...");
				while (!multicastSocket.isClosed() && null!=multicastSocket) {
					for (int i=0;i<Constant.bufferSize;i++){recvBuffer[i]=0;}
		        	DatagramPacket rdp = new DatagramPacket(recvBuffer, recvBuffer.length);
		        	multicastSocket.receive(rdp);
		        	parsePackage(recvBuffer);
		        }
			} catch (Exception e) {
				try {
					if(null!=multicastSocket && !multicastSocket.isClosed()){
						multicastSocket.leaveGroup(InetAddress.getByName(Constant.MULTICAST_IP));
						multicastSocket.close();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			} 
		}

		//parse package
		private void parsePackage(byte[] pkg) {
			int CMD = pkg[3];//cmd
			int cmdType = pkg[4];//typ
			int oprCmd = pkg[5];//opr

			//获得用户ID号
			byte[] uId = new byte[4];
			System.arraycopy(pkg, 6, uId, 0, 4);
			int userId = ByteAndInt.byteArray2Int(uId);
			
			switch (CMD) {
			case Constant.CMD80:
				switch (cmdType) {
				case Constant.CMD_TYPE1:
					
					if(userId != me.personId){
						updatePerson(userId,pkg);
						
						byte[] ipBytes = new byte[4];
						System.arraycopy(pkg, 44, ipBytes, 0, 4);
						try {
							InetAddress targetIp = InetAddress.getByAddress(ipBytes);
							regBuffer[4] = Constant.CMD_TYPE2;
							DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,targetIp,Constant.PORT);
							multicastSocket.send(dp);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					break;
				case Constant.CMD_TYPE2:
					updatePerson(userId,pkg);
					break;
				case Constant.CMD_TYPE3:
					childrenMap.remove(userId);
					personKeys.remove(Integer.valueOf(userId));
					sendPersonHasChangedBroadcast();
					break;
				}
				break;
			case Constant.CMD81:// receive message
				switch (cmdType) {
				case Constant.CMD_TYPE1:
					List<Message> messages = null;
					if(msgContainer.containsKey(userId)){
						messages = msgContainer.get(userId);
					}else{
						messages = new ArrayList<Message>();
					}
					byte[] msgBytes = new byte[Constant.msgLength];
					System.arraycopy(pkg, 10, msgBytes, 0, Constant.msgLength);
					String msgStr = new String(msgBytes).trim();
					Message msg = new Message();
					msg.msg = msgStr;
					msg.receivedTime = getCurrentTime();
					messages.add(msg);
					msgContainer.put(userId, messages);
					//Toast.makeText(getApplication(), "New Message", Toast.LENGTH_SHORT);
					Intent intent = new Intent();
					intent.setAction(Constant.hasMsgUpdatedAction);
					intent.putExtra("userId", userId);
					intent.putExtra("msgCount", messages.size());
					sendBroadcast(intent);
					break;
				case Constant.CMD_TYPE2:
					break;
				}
				break;
			case Constant.CMD82:
				switch (cmdType) {
				case Constant.CMD_TYPE1://file transfer request
					switch(oprCmd){
					case Constant.OPR_CMD1:
						
						if(!isBusyNow){
						//	isBusyNow = true;
							fileSenderUid = userId;//store person id 
							Person person = childrenMap.get(Integer.valueOf(userId));
							Intent intent = new Intent();
							intent.putExtra("person", person);
							intent.setAction(Constant.receivedSendFileRequestAction);
							sendBroadcast(intent);
						}else{//is busy
							Person person = childrenMap.get(Integer.valueOf(userId));
							fileSendBuffer[4]=Constant.CMD_TYPE2;
							fileSendBuffer[5]=Constant.OPR_CMD4;
							byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
							System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
							try{
								DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
								multicastSocket.send(dp);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						break;
					case Constant.OPR_CMD5://file name
						byte[] fileNameBytes = new byte[Constant.fileNameLength];
						byte[] fileSizeByte = new byte[8];
						System.arraycopy(pkg, 10, fileNameBytes, 0, Constant.fileNameLength);
						System.arraycopy(pkg, 100, fileSizeByte, 0, 8);
						FileState fs = new FileState();
						fs.fileName = new String(fileNameBytes).trim();
						fs.fileSize = Long.valueOf(ByteAndInt.byteArrayToLong(fileSizeByte));
						receivedFileNames.add(fs);
						break;
					}
					break;
				case Constant.CMD_TYPE2:
					switch(oprCmd){
					case Constant.OPR_CMD2://accept
						fileHandler.startSendFile();
						System.out.println("Start send file to remote user ...");
						break;
					case Constant.OPR_CMD3://decline
						Intent intent = new Intent();
						intent.setAction(Constant.remoteUserRefuseReceiveFileAction);
						sendBroadcast(intent);
						System.out.println("Remote user refuse to receive file ...");
						break;
					case Constant.OPR_CMD4://busy
						System.out.println("Remote user is busy now ...");
						break;
					}
					break;
				}
				break;
			case Constant.CMD83://83
				switch(cmdType){
				case Constant.CMD_TYPE1:
					switch(oprCmd){
					case Constant.OPR_CMD1://voice call
						System.out.println("Received a talk request ... ");
						isStopTalk = false;
						Person person = childrenMap.get(Integer.valueOf(userId));
						Intent intent = new Intent();
						intent.putExtra("person", person);
						intent.setAction(Constant.receivedTalkRequestAction);
						sendBroadcast(intent);
						break;
					case Constant.OPR_CMD2:
						//close
						System.out.println("Received remote user stop talk cmd ... ");
						isStopTalk = true;
						Intent i = new Intent();
						i.setAction(Constant.remoteUserClosedTalkAction);
						sendBroadcast(i);
						break;
					}
					break;
				case Constant.CMD_TYPE2:
					switch(oprCmd){
					case Constant.OPR_CMD1:
						//hang up
						if(!isStopTalk){
							System.out.println("Begin to talk with remote user ... ");
							Person person = childrenMap.get(Integer.valueOf(userId));
							audioHandler.audioSend(person);
						}
						break;
					}
					break;
				}
				break;
			}
		}
		
		//update
		private void updatePerson(int userId,byte[] pkg){
			Person person = new Person();
			getPerson(pkg,person);
			childrenMap.put(userId, person);
			if(!personKeys.contains(Integer.valueOf(userId)))personKeys.add(Integer.valueOf(userId));
			if(!children.contains(childrenMap))children.add(childrenMap);
			sendPersonHasChangedBroadcast();
		}
		
		//close socket
		private void release(){
			try {
				regBuffer[4] = Constant.CMD_TYPE3;//leave
				DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,InetAddress.getByName(Constant.MULTICAST_IP),Constant.PORT);
				multicastSocket.send(dp);
				System.out.println("Send logout cmd ...");
				
				multicastSocket.leaveGroup(InetAddress.getByName(Constant.MULTICAST_IP));
				multicastSocket.close();
				
				System.out.println("Socket has closed ...");
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				fileHandler.release();
				audioHandler.release();
			}
		}
		
		//get person
		private void getPerson(byte[] pkg,Person person){
			
			byte[] personIdBytes = new byte[4];
			byte[] iconIdBytes = new byte[4];
			byte[] nickeNameBytes = new byte[30];
			byte[] personIpBytes = new byte[4];
			
			System.arraycopy(pkg, 6, personIdBytes, 0, 4);
			System.arraycopy(pkg, 10, iconIdBytes, 0, 4);
			System.arraycopy(pkg, 14, nickeNameBytes, 0, 30);
			System.arraycopy(pkg, 44, personIpBytes, 0, 4);
			
			person.personId = ByteAndInt.byteArray2Int(personIdBytes);
			person.personHeadIconId = ByteAndInt.byteArray2Int(iconIdBytes);
			person.personNickeName = (new String(nickeNameBytes)).trim();
			person.ipAddress = Constant.intToIp(ByteAndInt.byteArray2Int(personIpBytes));
			person.timeStamp = System.currentTimeMillis();
		}
		
		//register oneself
		public void joinOrganization(){
			try {
				if(null!=multicastSocket && !multicastSocket.isClosed()){
					regBuffer[4] = Constant.CMD_TYPE1;//恢复成注册请求标志，向网络中注册自己
					DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,InetAddress.getByName(Constant.MULTICAST_IP),Constant.PORT);
					multicastSocket.send(dp);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//send message
		public void sendMsg(int personId,String msg){
			try {
				Person psn = childrenMap.get(personId);
				if(null!=psn){
					System.arraycopy(ByteAndInt.int2ByteArray(me.personId), 0, msgSendBuffer, 6, 4);
					int msgLength = Constant.msgLength+10;
					for(int i=10;i<msgLength;i++){msgSendBuffer[i]=0;}
					byte[] msgBytes = msg.getBytes();
					System.arraycopy(msgBytes, 0, msgSendBuffer, 10, msgBytes.length);
					DatagramPacket dp = new DatagramPacket(msgSendBuffer,Constant.bufferSize,InetAddress.getByName(psn.ipAddress),Constant.PORT);
					multicastSocket.send(dp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//file request
		public void sendFiles(int personId,ArrayList<FileName> files){
			if(personId>0 && null!=files && files.size()>0){
				try{
					tempUid = personId;
					tempFiles = files;
					Person person = childrenMap.get(Integer.valueOf(tempUid));
					fileSendBuffer[4]=Constant.CMD_TYPE1;
					fileSendBuffer[5]=Constant.OPR_CMD5;
					byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
					System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
					int fileNameLength = Constant.fileNameLength+10;
					//send file name
					for (final FileName file : tempFiles) {
						
						FileState fs = new FileState(file.fileSize,0,file.getFileName());
						beSendFileNames.add(fs);
						
						byte[] fileNameBytes = file.getFileName().getBytes();
						for(int i=10;i<fileNameLength;i++)fileSendBuffer[i]=0;
						System.arraycopy(fileNameBytes, 0, fileSendBuffer, 10, fileNameBytes.length);//把文件名放入头数据包
						System.arraycopy(ByteAndInt.longToByteArray(file.fileSize), 0, fileSendBuffer, 100, 8);
						DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
						multicastSocket.send(dp);
					}
					
					fileSendBuffer[5]=Constant.OPR_CMD1;
					DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
					multicastSocket.send(dp);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		public void receiveFiles(String fileSavePath) {
			this.fileSavePath = fileSavePath;
			Person person = childrenMap.get(Integer.valueOf(fileSenderUid));
			fileSendBuffer[4]=Constant.CMD_TYPE2;
			fileSendBuffer[5]=Constant.OPR_CMD2;
			byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
			System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
			try{
				DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		
		public void refuseReceiveFile(){
		
			Person person = childrenMap.get(Integer.valueOf(fileSenderUid));
			fileSendBuffer[4]=Constant.CMD_TYPE2;
			fileSendBuffer[5]=Constant.OPR_CMD3;
			byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
			System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
			try{
				DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//file name
	    public ArrayList<FileState> getReceivedFileNames() {
			return receivedFileNames;
		}
		//file name
	    public ArrayList<FileState> getBeSendFileNames(){
	    	return beSendFileNames;
	    }
	    //voice call
	    public void startTalk(int personId){
			try {
				isStopTalk = false;
				talkCmdBuffer[4] = Constant.CMD_TYPE1;
		    	talkCmdBuffer[5] = Constant.OPR_CMD1;
				System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
				Person person = childrenMap.get(Integer.valueOf(personId));
				DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    //end call
	    public void stopTalk(int personId){
	    	isStopTalk = true;
	    	talkCmdBuffer[4] = Constant.CMD_TYPE1;
	    	talkCmdBuffer[5] = Constant.OPR_CMD2;
	    	Person person = childrenMap.get(Integer.valueOf(personId));
	    	try {
	    		System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
	    		DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
	    //accept
	    public void acceptTalk(int personId){
			talkCmdBuffer[3] = Constant.CMD83;
			talkCmdBuffer[4] = Constant.CMD_TYPE2;
			talkCmdBuffer[5] = Constant.OPR_CMD1;
			try {
				//send
				System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
				Person person = childrenMap.get(Integer.valueOf(personId));
				DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
				audioHandler.audioSend(person);//audio data
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    
	    //=========================TCP file module==================================================================    
		//TCP
		private class FileHandler extends Thread{
			private ServerSocket sSocket = null;
			
			public FileHandler(){}
			@Override
			public void run() {
				super.run();
				try {
					sSocket = new ServerSocket(Constant.PORT);
					System.out.println("File Handler socket started ...");
					while(!sSocket.isClosed() && null!=sSocket){
						Socket socket = sSocket.accept();
						socket.setSoTimeout(5000);
						new SaveFileToDisk(socket).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//store received data
			private class SaveFileToDisk extends Thread{
				private Socket socket = null;
				public SaveFileToDisk(Socket socket){
					this.socket = socket;
				}
				@Override
				public void run() {
					super.run();
					OutputStream output = null;
					InputStream input = null;
					try {
						byte[] recvFileCmd = new byte[Constant.bufferSize];//receive first package
						input = socket.getInputStream();
						input.read(recvFileCmd);//read data
						int cmdType = recvFileCmd[4];//type
						int oprCmd = recvFileCmd[5];//opr
						if(cmdType==Constant.CMD_TYPE1 && oprCmd ==Constant.OPR_CMD6){
							byte[] fileNameBytes = new byte[Constant.fileNameLength];//get file name
							System.arraycopy(recvFileCmd, 10, fileNameBytes, 0, Constant.fileNameLength);
							StringBuffer sb = new StringBuffer();
							String fName = new String(fileNameBytes).trim(); 
							sb.append(fileSavePath).append(File.separator).append(fName);
							String fileName = sb.toString();
							File file = new File(fileName);//create file
							//data buffer
							byte[] readBuffer = new byte[Constant.readBufferSize];
							output = new FileOutputStream(file);//get stream
							int readSize = 0;
							int length = 0;
							long count = 0;
							FileState fs = getFileStateByName(fName,receivedFileNames);
							
							while(-1 != (readSize = input.read(readBuffer))){
								output.write(readBuffer,0,readSize);//get data in
								output.flush();
								length+=readSize;
								count++;
								if(count%10==0){
									fs.currentSize = length;
									fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
									Intent intent = new Intent();
									intent.setAction(Constant.fileReceiveStateUpdateAction);
									sendBroadcast(intent);
								}
							}
							fs.currentSize = length;
							fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
							Intent intent = new Intent();
							intent.setAction(Constant.fileReceiveStateUpdateAction);
							sendBroadcast(intent);
						}else{
							Intent intent = new Intent();
							intent.putExtra("msg", getString(R.string.data_receive_error));
							intent.setAction(Constant.dataReceiveErrorAction);
							sendBroadcast(intent);
						}
					} catch (Exception e) {
						Intent intent = new Intent();
						intent.putExtra("msg", e.getMessage());
						intent.setAction(Constant.dataReceiveErrorAction);
						sendBroadcast(intent);
						e.printStackTrace();
					}finally{
						try {
							if(null!=input)input.close();
							if(null!=output)output.close();
							if(!socket.isClosed())socket.close();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			
			//send file
			public void startSendFile() {
				//get receiving person
				Person person = childrenMap.get(Integer.valueOf(tempUid));
				final String userIp = person.ipAddress;
				
				final byte[] sendFileCmd = new byte[Constant.bufferSize];
				for(int i=0;i<Constant.bufferSize;i++)sendFileCmd[i]=0;
				System.arraycopy(Constant.pkgHead, 0, sendFileCmd, 0, 3);
				sendFileCmd[3] = Constant.CMD82;
				sendFileCmd[4] = Constant.CMD_TYPE1;
				sendFileCmd[5] = Constant.OPR_CMD6;
				System.arraycopy(ByteAndInt.int2ByteArray(me.personId), 0, sendFileCmd, 6, 4);
				for (final FileName file : tempFiles) {//multi-thread
					new Thread() {
						@Override
						public void run() {
							Socket socket = null;
							OutputStream output = null;
							InputStream input = null;
							try {
								socket = new Socket(userIp,Constant.PORT);
								byte[] fileNameBytes = file.getFileName().getBytes();
								int fileNameLength = Constant.fileNameLength+10;
								for(int i=10;i<fileNameLength;i++)sendFileCmd[i]=0;
								System.arraycopy(fileNameBytes, 0, sendFileCmd, 10, fileNameBytes.length);
								System.arraycopy(ByteAndInt.longToByteArray(file.fileSize), 0, sendFileCmd, 100, 8);
								output = socket.getOutputStream();//set up a stream
								output.write(sendFileCmd);
								output.flush();
								sleep(1000);//sleep 1 second
								//data buffer
								byte[] readBuffer = new byte[Constant.readBufferSize];
								input = new FileInputStream(new File(file.fileName));
								int readSize = 0;
								int length = 0;
								long count = 0;
								FileState fs = getFileStateByName(file.getFileName(), beSendFileNames);
								while(-1 != (readSize = input.read(readBuffer))){
									output.write(readBuffer,0,readSize);//get data in
									output.flush();
									length+=readSize;
									
									count++;
									if(count%10==0){
										fs.currentSize = length;
										fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
										Intent intent = new Intent();
										intent.setAction(Constant.fileSendStateUpdateAction);
										sendBroadcast(intent);
									}
								}
								fs.currentSize = length;
								fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
								Intent intent = new Intent();
								intent.setAction(Constant.fileSendStateUpdateAction);
								sendBroadcast(intent);
							} catch (Exception e) {
								//error
								Intent intent = new Intent();
								intent.putExtra("msg", e.getMessage());
								intent.setAction(Constant.dataSendErrorAction);
								sendBroadcast(intent);
								e.printStackTrace();
							}finally{
								try {
									if(null!=output)output.close();
									if(null!=input)input.close();
									if(!socket.isClosed())socket.close();
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							} 
						}
					}.start();
				}
			}
			
			private FileState getFileStateByName(String fileName,ArrayList<FileState> fileStates){
				for (FileState fileState : fileStates) {
					if(fileState.fileName.equals(fileName)){
						return fileState;
					}
				}
				return null;
			}
			
			public void release() {
				try {
					System.out.println("File handler socket closed ...");
					if(null!=sSocket)sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//=========================End of TCP File Module============================================================== 
		
	    //=========================TCP Voice Call Module==================================================================    
		
		private class AudioHandler extends Thread{
			private ServerSocket sSocket = null;
			public AudioHandler(){}
			@Override
			public void run() {
				super.run();
				try {
					sSocket = new ServerSocket(Constant.AUDIO_PORT);//audio port
					System.out.println("Audio Handler socket started ...");
					while(!sSocket.isClosed() && null!=sSocket){
						Socket socket = sSocket.accept();
						socket.setSoTimeout(5000);
						audioPlay(socket);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//play thread
			public void audioPlay(Socket socket){
				new AudioPlay(socket).start();
			}
			//send thread
			public void audioSend(Person person){
				new AudioSend(person).start();
			}
			
			//play thread
			public class AudioPlay extends Thread{
				Socket socket = null;
				public AudioPlay(Socket socket){
					this.socket = socket;
				//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
				}
				
				@Override
				public void run() {
					super.run();
					try {
						InputStream is = socket.getInputStream();
						//buffer size
						int bufferSize = android.media.AudioTrack.getMinBufferSize(8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT);

						//audio track
						AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, 
								8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSize,
								AudioTrack.MODE_STREAM);

						//volume
						player.setStereoVolume(1.0f, 1.0f);
						
						player.play();
						byte[] audio = new byte[160];//read buffer
						int length = 0;
						
						while(!isStopTalk){
							length = is.read(audio);//read data
							if(length>0 && length%2==0){
							
								player.write(audio, 0, length);//play
							}
						}
						player.stop();
						is.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			//send thread
			public class AudioSend extends Thread{
				Person person = null;
				
				public AudioSend(Person person){
					this.person = person;
				//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
				}
				@Override
				public void run() {
					super.run();
					Socket socket = null;
					OutputStream os = null;
					AudioRecord recorder = null;
					try {
						socket = new Socket(person.ipAddress, Constant.AUDIO_PORT);
						socket.setSoTimeout(5000);
						os = socket.getOutputStream();
						//buffer size
						int bufferSize = AudioRecord.getMinBufferSize(8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT);
						
						//recorder
						recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
								8000,AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSize*10);
						
						recorder.startRecording();//begin
						byte[] readBuffer = new byte[640];//record buffer
						
						int length = 0;
						
						while(!isStopTalk){
							length = recorder.read(readBuffer,0,640);//get from mic
							if(length>0 && length%2==0){
								os.write(readBuffer,0,length);//stream to 
							}
						}
						recorder.stop();
						os.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			public void release() {
				try {
					System.out.println("Audio handler socket closed ...");
					if(null!=sSocket)sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//=========================End of TCP voice call module================================================================== 
	}
	//========================End of Communication Module=======================================================
}
