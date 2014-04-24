package com.henryhan.chitchat.main;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.henryhan.chitchat.R;
import com.henryhan.chitchat.service.MainService;
import com.henryhan.chitchat.utils.Constant;
import com.henryhan.chitchat.utils.FileName;
import com.henryhan.chitchat.utils.FileState;
import com.henryhan.chitchat.utils.Message;
import com.henryhan.chitchat.utils.Person;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity implements OnClickListener{
	private Person person = null;
	private Person me = null;
	private EditText chartMsg = null;
	private Button chartMsgSend = null;
	private Button chartMsgFile = null;
	private LinearLayout chartMsgPanel = null;
	private MainService mService = null;
	private Intent mMainServiceIntent = null;
	private MyBroadcastRecv broadcastRecv = null;
	private IntentFilter bFilter = null;
	private ScrollView chartMsgScroll = null;
	private boolean isPaused = false;//whether you are visible 
	private boolean isRemoteUserClosed = false; //Whether another user has end the connection 
	private ArrayList<FileState> receivedFileNames = null;//received file name
	private ArrayList<FileState> beSendFileNames = null;//sending file name
	private ReceiveSendFileListAdapter receiveFileListAdapter = new ReceiveSendFileListAdapter(this);
	private ReceiveSendFileListAdapter sendFileListAdapter = new ReceiveSendFileListAdapter(this);
	
	//scroll to the last line
	private final Handler mHandler = new Handler();
    private Runnable scrollRunnable= new Runnable() {
	    @Override
	    public void run() {
            int offset = chartMsgPanel.getMeasuredHeight() - chartMsgScroll.getHeight();//height
            if (offset > 0) {
	            chartMsgScroll.scrollBy(0, 100);// scroll 100 units/time
	        }
	    }
    };
    
	
	/**
	 * bind service
	 */
	private ServiceConnection sConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((MainService.ServiceBinder)service).getService();
			showMsg(person.personId);//get user information if connection is set up
			System.out.println("Service connected to activity...");
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			System.out.println("Service disconnected to activity...");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.person_chart_layout);
		Intent intent = getIntent();
		person = (Person)intent.getExtras().getSerializable("person");
		me = (Person)intent.getExtras().getSerializable("me");
		
		((ImageView)findViewById(R.id.my_head_icon)).setImageResource(person.personHeadIconId);
		((TextView)findViewById(R.id.my_nickename)).setText(person.personNickeName);
		chartMsg = (EditText)findViewById(R.id.chart_msg);
		chartMsgSend = (Button)findViewById(R.id.chart_msg_send);
		chartMsgSend.setOnClickListener(this);
		chartMsgFile = (Button)findViewById(R.id.chart_msg_file);
		chartMsgFile.setOnClickListener(this);
		chartMsgPanel = (LinearLayout)findViewById(R.id.chart_msg_panel);
		chartMsgScroll = (ScrollView)findViewById(R.id.chart_msg_scroll);
		
		//binding MainService
        mMainServiceIntent = new Intent(this,MainService.class);
        bindService(mMainServiceIntent, sConnection, BIND_AUTO_CREATE);
        regBroadcastRecv();
        
	}

	@Override
	public void onClick(View vi) {
		switch(vi.getId()){
		case R.id.chart_msg_send:
			String msg = chartMsg.getText().toString();
			if(null==msg || msg.length()<=0){
				Toast.makeText(this, getString(R.string.content_is_empty), Toast.LENGTH_SHORT).show();
				return;
			}
			chartMsg.setText("");
			View view = getLayoutInflater().inflate(R.layout.send_msg_layout, null);
			ImageView iView = (ImageView)view.findViewById(R.id.send_head_icon);
			TextView smcView = (TextView)view.findViewById(R.id.send_msg_content);
			TextView smtView = (TextView)view.findViewById(R.id.send_msg_time);
			TextView nView = (TextView)view.findViewById(R.id.send_nickename);
			iView.setImageResource(me.personHeadIconId);
			smcView.setText(msg);
			smtView.setText(new Date().toLocaleString());
			nView.setText(me.personNickeName);
			chartMsgPanel.addView(view);
			
			mService.sendMsg(person.personId, msg); // method to send message
			mHandler.post(scrollRunnable);
			break;
		case R.id.chart_msg_file:
			Intent intent = new Intent(this,MyFileManager.class);
			intent.putExtra("selectType", Constant.SELECT_FILES);
			startActivityForResult(intent, Constant.FILE_RESULT_CODE);
			break;
		}
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.chart_menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getOrder()){
    	case 1:
    		Intent intent = new Intent(this,MyFileManager.class);
			intent.putExtra("selectType", Constant.SELECT_FILES);
			startActivityForResult(intent, Constant.FILE_RESULT_CODE);
    		break;
    	case 2:
    		AlertDialog.Builder  builder = new AlertDialog.Builder(this);
			builder.setTitle(me.personNickeName);
			String title = String.format(getString(R.string.talk_with), person.personNickeName);
			builder.setMessage(title);
			builder.setIcon(me.personHeadIconId);
			builder.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface tdialog, int which) {
					tdialog.dismiss();
				}
			});
			final AlertDialog talkDialog = builder.show();
			talkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					mService.stopTalk(person.personId);
				}
			});
			mService.startTalk(person.personId);
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
	//get message from service according to userID
	private void showMsg(int userId){
		List<Message> msgs = mService.getMessagesById(userId);
		if(null!=msgs){
			while(msgs.size()>0){
				View view = getLayoutInflater().inflate(R.layout.received_msg_layout, null);
				ImageView iView = (ImageView)view.findViewById(R.id.received_head_icon);
				TextView smcView = (TextView)view.findViewById(R.id.received_msg_content);
				TextView smtView = (TextView)view.findViewById(R.id.received_msg_time);
				TextView nView = (TextView)view.findViewById(R.id.received_nickename);
				iView.setImageResource(person.personHeadIconId);
				Message msg = msgs.remove(0);
				smcView.setText(msg.msg);
				smtView.setText(msg.receivedTime);
				nView.setText(person.personNickeName);
				chartMsgPanel.addView(view);
				mHandler.post(scrollRunnable);
			}
		}
	}
	
	boolean finishedSendFile = false;//record whether the file has got received already
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){
			if(null!=data){
				int selectType = data.getExtras().getInt("selectType");
				if(selectType == Constant.SELECT_FILE_PATH){// select file path to store received file
					String fileSavePath = data.getExtras().getString("fileSavePath");
					if(null!=fileSavePath){
						mService.receiveFiles(fileSavePath);
						finishedSendFile = true;//set status as true
					}else{
						Toast.makeText(this, getString(R.string.folder_can_not_write), Toast.LENGTH_SHORT).show();
					}
				}else if(selectType == Constant.SELECT_FILES){// sending file 
					@SuppressWarnings("unchecked")
					final ArrayList<FileName> files = (ArrayList<FileName>)data.getExtras().get("files");
					mService.sendFiles(person.personId, files);// return path
					
					//display files
					beSendFileNames = mService.getBeSendFileNames();//get file name from service
					if(beSendFileNames.size()<=0)return;
					sendFileListAdapter.setResources(beSendFileNames);
					AlertDialog.Builder  builder = new AlertDialog.Builder(this);
					builder.setTitle(me.personNickeName);
					builder.setMessage(R.string.start_to_send_file);
					builder.setIcon(me.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_file_popupwindow_layout, null);
					builder.setView(vi);
					final AlertDialog fileListDialog = builder.show();
					fileListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							beSendFileNames.clear();
				        	files.clear();
						}
					});
					ListView lv = (ListView)vi.findViewById(R.id.receive_file_list);//file list
					lv.setAdapter(sendFileListAdapter);
					Button btn_ok = (Button)vi.findViewById(R.id.receive_file_okbtn);
					btn_ok.setVisibility(View.GONE);
			        Button btn_cancle = (Button)vi.findViewById(R.id.receive_file_cancel);
			      //select path when clicking
			        btn_ok.setOnClickListener(new View.OnClickListener() {   
			            @Override  
			            public void onClick(View v) { 
			            	if(!finishedSendFile){//if received already
				            	Intent intent = new Intent(ChatActivity.this,MyFileManager.class);
				    			intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
				    			startActivityForResult(intent, 0);
			            	}
			            }   
				     });   
				    //decline
			        btn_cancle.setOnClickListener(new View.OnClickListener() {   
				        @Override  
				        public void onClick(View v) { 
				        	fileListDialog.dismiss();
				        }   
			        });
				}
			}
		}
	}
	
    //Broadcast Receiver
    private class MyBroadcastRecv extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constant.hasMsgUpdatedAction)){
				showMsg(person.personId);
			}else if(intent.getAction().equals(Constant.dataReceiveErrorAction) 
					|| intent.getAction().equals(Constant.dataSendErrorAction)){
				Toast.makeText(ChatActivity.this, intent.getExtras().getString("msg"), Toast.LENGTH_SHORT).show();
			}else if(intent.getAction().equals(Constant.fileSendStateUpdateAction)){//receiving file
				beSendFileNames = mService.getBeSendFileNames();//get status
				sendFileListAdapter.setResources(beSendFileNames);
				sendFileListAdapter.notifyDataSetChanged();//refresh
			}else if(intent.getAction().equals(Constant.fileReceiveStateUpdateAction)){
				receivedFileNames = mService.getReceivedFileNames();
				receiveFileListAdapter.setResources(receivedFileNames);
				receiveFileListAdapter.notifyDataSetChanged();
			}else if(intent.getAction().equals(Constant.receivedTalkRequestAction)){
				if(!isPaused){
					isRemoteUserClosed = false;
					final Person psn = (Person)intent.getExtras().get("person");
					String title = String.format(getString(R.string.talk_with), psn.personNickeName);
					AlertDialog.Builder  builder = new AlertDialog.Builder(ChatActivity.this);
					builder.setTitle(psn.personNickeName);
					builder.setMessage(title);
					builder.setIcon(psn.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_talk_layout, null);
					builder.setView(vi);
					final AlertDialog revTalkDialog = builder.show();
					revTalkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							mService.stopTalk(psn.personId);
						}
					});
					Button talkOkBtn = (Button)vi.findViewById(R.id.receive_talk_okbtn);
					talkOkBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View okBtn) {
							if(!isRemoteUserClosed){
								mService.acceptTalk(psn.personId);
								okBtn.setEnabled(false);
							}
						}
					});
					Button talkCancelBtn = (Button)vi.findViewById(R.id.receive_talk_cancel);
					talkCancelBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View cancelBtn) {
							revTalkDialog.dismiss();
						}
					});
				}
			}else if(intent.getAction().equals(Constant.remoteUserRefuseReceiveFileAction)){
				Toast.makeText(ChatActivity.this, getString(R.string.refuse_receive_file), Toast.LENGTH_SHORT).show();
			}else if(intent.getAction().equals(Constant.receivedSendFileRequestAction)){
				if(!isPaused){//if is visible, show dialog
					receivedFileNames = mService.getReceivedFileNames();//get file name
					receiveFileListAdapter.setResources(receivedFileNames);
					AlertDialog.Builder  builder = new AlertDialog.Builder(context);
					builder.setTitle(person.personNickeName);
					builder.setMessage(R.string.sending_file_to_you);
					builder.setIcon(person.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_file_popupwindow_layout, null);
					builder.setView(vi);
					final AlertDialog revFileDialog = builder.show();
					revFileDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							receivedFileNames.clear();
				        	if(!finishedSendFile){//once stop, then send a decline broadcast
					        	Intent intent = new Intent();
								intent.setAction(Constant.refuseReceiveFileAction);
								sendBroadcast(intent);
				        	}
				        	finishedSendFile = false;//close and finish
						}
					});
					ListView lv = (ListView)vi.findViewById(R.id.receive_file_list);//list
					lv.setAdapter(receiveFileListAdapter);
					Button btn_ok = (Button)vi.findViewById(R.id.receive_file_okbtn);
			        Button btn_cancle = (Button)vi.findViewById(R.id.receive_file_cancel);
			        
			        btn_ok.setOnClickListener(new View.OnClickListener() {   
			            @Override  
			            public void onClick(View v) { 
			            	if(!finishedSendFile){
				            	Intent intent = new Intent(ChatActivity.this,MyFileManager.class);
				    			intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
				    			startActivityForResult(intent, 0);
			            	}
			            }   
				     });   
			              
			        btn_cancle.setOnClickListener(new View.OnClickListener() {   
				        @Override  
				        public void onClick(View v) { 
				        	revFileDialog.dismiss();
				        }   
			        });
				}
			}
		}
    }
    
	//Register
	private void regBroadcastRecv(){
        broadcastRecv = new MyBroadcastRecv();
        bFilter = new IntentFilter();
        bFilter.addAction(Constant.hasMsgUpdatedAction);
        bFilter.addAction(Constant.receivedSendFileRequestAction);
        bFilter.addAction(Constant.fileReceiveStateUpdateAction);
        bFilter.addAction(Constant.fileSendStateUpdateAction);
        bFilter.addAction(Constant.receivedTalkRequestAction);
        registerReceiver(broadcastRecv, bFilter);
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		isPaused = false;
	}
	@Override
	protected void onPause() {
		super.onPause();
		isPaused = true;
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(sConnection);
		unregisterReceiver(broadcastRecv);
	}
}
