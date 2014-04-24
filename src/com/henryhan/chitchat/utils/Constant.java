package com.henryhan.chitchat.utils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


import com.henryhan.chitchat.R;

public class Constant {
	
	public static Map<String,Integer> exts = new HashMap<String,Integer>();
	  static{
		  exts.put("doc", R.drawable.doc);exts.put("docx", R.drawable.doc);exts.put("xls", R.drawable.xls);exts.put("xlsx", R.drawable.xls);exts.put("ppt", R.drawable.ppt);exts.put("pptx", R.drawable.ppt);
		  exts.put("jpg", R.drawable.image);exts.put("jpeg", R.drawable.image);exts.put("gif", R.drawable.image);exts.put("png", R.drawable.image);exts.put("ico", R.drawable.image);
		  exts.put("apk", R.drawable.apk);exts.put("jar", R.drawable.jar);exts.put("rar", R.drawable.rar);exts.put("zip", R.drawable.rar);
		  exts.put("mp3", R.drawable.music);exts.put("wma", R.drawable.music);exts.put("aac", R.drawable.music);exts.put("ac3", R.drawable.music);exts.put("ogg", R.drawable.music);exts.put("flac", R.drawable.music);exts.put("midi", R.drawable.music);
		  exts.put("pcm", R.drawable.music);exts.put("wav", R.drawable.music);exts.put("amr", R.drawable.music);exts.put("m4a", R.drawable.music);exts.put("ape", R.drawable.music);exts.put("mid", R.drawable.music);exts.put("mka", R.drawable.music);
		  exts.put("svx", R.drawable.music);exts.put("snd", R.drawable.music);exts.put("vqf", R.drawable.music);exts.put("aif", R.drawable.music);exts.put("voc", R.drawable.music);exts.put("cda", R.drawable.music);exts.put("mpc", R.drawable.music);
		  exts.put("mpeg", R.drawable.video);exts.put("mpg", R.drawable.video);exts.put("dat", R.drawable.video);exts.put("ra", R.drawable.video);exts.put("rm", R.drawable.video);exts.put("rmvb", R.drawable.video);exts.put("mp4", R.drawable.video);
		  exts.put("flv", R.drawable.video);exts.put("mov", R.drawable.video);exts.put("qt", R.drawable.video);exts.put("asf", R.drawable.video);exts.put("wmv", R.drawable.video);exts.put("avi", R.drawable.video);
		  exts.put("3gp", R.drawable.video);exts.put("mkv", R.drawable.video);exts.put("f4v", R.drawable.video);exts.put("m4v", R.drawable.video);exts.put("m4p", R.drawable.video);exts.put("m2v", R.drawable.video);exts.put("dat", R.drawable.video);
		  exts.put("xvid", R.drawable.video);exts.put("divx", R.drawable.video);exts.put("vob", R.drawable.video);exts.put("mpv", R.drawable.video);exts.put("mpeg4", R.drawable.video);exts.put("mpe", R.drawable.video);exts.put("mlv", R.drawable.video);
		  exts.put("ogm", R.drawable.video);exts.put("m2ts", R.drawable.video);exts.put("mts", R.drawable.video);exts.put("ask", R.drawable.video);exts.put("trp", R.drawable.video);exts.put("tp", R.drawable.video);exts.put("ts", R.drawable.video);
	  }
	
	//user defined actions
	public static final String updateMyInformationAction = "com.henryhan.chitchat.updateMyInformation";
	public static final String personHasChangedAction = "com.henryhan.chitchat.personHasChanged";
	public static final String hasMsgUpdatedAction = "com.henryhan.chitchat.hasMsgUpdated";
	public static final String receivedSendFileRequestAction = "com.henryhan.chitchat.receivedSendFileRequest";
	public static final String refuseReceiveFileAction = "com.henryhan.chitchat.refuseReceiveFile";
	public static final String remoteUserRefuseReceiveFileAction = "com.henryhan.chitchat.remoteUserRefuseReceiveFile";
	public static final String dataReceiveErrorAction = "com.henryhan.chitchat.dataReceiveError";
	public static final String dataSendErrorAction = "com.henryhan.chitchat.dataSendError";
	public static final String whoIsAliveAction = "com.henryhan.chitchat.whoIsAlive";
	public static final String imAliveNow = "com.henryhan.chitchat.imAliveNow";
	public static final String remoteUserUnAliveAction = "com.henryhan.chitchat.remoteUserUnAlive";
	public static final String fileSendStateUpdateAction = "com.henryhan.chitchat.fileSendStateUpdate";
	public static final String fileReceiveStateUpdateAction = "com.henryhan.chitchat.fileReceiveStateUpdate";
	public static final String receivedTalkRequestAction = "com.henryhan.chitchat.receivedTalkRequest";
	public static final String acceptTalkRequestAction = "com.henryhan.chitchat.acceptTalkRequest";
	public static final String remoteUserClosedTalkAction = "com.henryhan.chitchat.remoteUserClosedTalk";
	
	//System Action declare
	public static final String bootCompleted = "android.intent.action.BOOT_COMPLETED";
	public static final String WIFIACTION="android.net.conn.CONNECTIVITY_CHANGE";
	public static final String ETHACTION = "android.intent.action.ETH_STATE";
	
	//generate only user ID
	public static int getMyId(){
		int id = (int)(Math.random()*1000000);
		return id;
	}
	
	
	public static final int bufferSize = 256;
	public static final int msgLength = 180;
	public static final int fileNameLength = 90;
	public static final int readBufferSize = 4096;
	public static final byte[] pkgHead = "AND".getBytes();
	public static final int CMD80 = 80;
	public static final int CMD81 = 81;
	public static final int CMD82 = 82;
	public static final int CMD83 = 83;
	public static final int CMD_TYPE1 = 1;
	public static final int CMD_TYPE2 = 2;
	public static final int CMD_TYPE3 = 3;
	public static final int OPR_CMD1 = 1;
	public static final int OPR_CMD2 = 2;
	public static final int OPR_CMD3 = 3;
	public static final int OPR_CMD4 = 4;
	public static final int OPR_CMD5 = 5;
	public static final int OPR_CMD6 = 6;
	public static final int OPR_CMD10 = 10;
	public static final String MULTICAST_IP = "239.9.9.1";
	public static final int PORT = 5760;
	public static final int AUDIO_PORT = 5761;
	
	//int to ip
	public static String intToIp(int i) {   
		String ip = ( (i >> 24) & 0xFF) +"."+((i >> 16 ) & 0xFF)+"."+((i >> 8 ) & 0xFF)+"."+(i & 0xFF );
		
		return ip;
	}
	
	//others
	public static final int FILE_RESULT_CODE = 1;
	public static final int SELECT_FILES = 1;
	public static final int SELECT_FILE_PATH = 2;
	
	public static TreeMap<Integer,Boolean> fileSelectedState = new TreeMap<Integer,Boolean>();
	
	
	//file size  
 	  public static String formatFileSize(long fileS) {
	      DecimalFormat df = new DecimalFormat("#.00");
	      String fileSizeString = "";
	      if (fileS < 1024) {
	    	  fileSizeString = fileS+"B";
	      
	      } else if (fileS < 1048576) {
	          fileSizeString = df.format((double) fileS / 1024) + "K";
	      } else if (fileS < 1073741824) {
	          fileSizeString = df.format((double) fileS / 1048576) + "M";
	      } else {
	          fileSizeString = df.format((double) fileS / 1073741824) + "G";
	      }
	      return fileSizeString;
	  }
 	  
}
