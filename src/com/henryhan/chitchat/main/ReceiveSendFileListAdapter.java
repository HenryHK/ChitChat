package com.henryhan.chitchat.main;

import java.util.ArrayList;

import com.henryhan.chitchat.R;
import com.henryhan.chitchat.utils.Constant;
import com.henryhan.chitchat.utils.FileState;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

//=============================Received file list adapter========================================================
public class ReceiveSendFileListAdapter extends BaseAdapter{
	private ArrayList<FileState> receivedFileNames = null;//file name
	private Context context = null;
	
	public ReceiveSendFileListAdapter(Context context){
		this.context = context;
	}

	@Override
	public int getCount() {
		return receivedFileNames.size();
	}

	@Override
	public Object getItem(int position) {
		return receivedFileNames.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View vi = inflater.inflate(R.layout.send_receive_file_layout, null);
		ImageView fileIcon = (ImageView)vi.findViewById(R.id.file_icon);
		TextView fileName = (TextView)vi.findViewById(R.id.file_name);
		TextView fileSize = (TextView)vi.findViewById(R.id.file_size);
		ProgressBar bar = (ProgressBar)vi.findViewById(R.id.file_progress);
		TextView filePercent = (TextView)vi.findViewById(R.id.file_percent);
		
		FileState fs = receivedFileNames.get(position);//get status
		String ext = fs.fileName.substring(fs.fileName.lastIndexOf(".")+1);
		Integer srcId = Constant.exts.get(ext);
		if(null == srcId)srcId = R.drawable.gdoc;
		fileIcon.setImageResource(srcId);
		fileName.setText(fs.fileName);
		fileSize.setText(Constant.formatFileSize(fs.fileSize));
		bar.setMax(100);
		bar.setProgress(fs.percent);
		filePercent.setText(Constant.formatFileSize(fs.currentSize)+"/"+Constant.formatFileSize(fs.fileSize));
		if(fs.percent==100)filePercent.setTextColor(Color.GREEN);
		return vi;
	}
	
	public void setResources(ArrayList<FileState> receivedFileNames){
		this.receivedFileNames = receivedFileNames;
	}
}
//================================End of List Adapter==========================================
