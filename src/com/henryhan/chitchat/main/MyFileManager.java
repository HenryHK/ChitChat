package com.henryhan.chitchat.main;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.henryhan.chitchat.R;
import com.henryhan.chitchat.utils.Constant;
import com.henryhan.chitchat.utils.FileName;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
public class MyFileManager extends ListActivity{
	private List<FileName> filePaths = new ArrayList<FileName>();//file path
	private String rootPath = "/";//root path
	private String parentPath = "/";//upper path
	private Button returnRootBtn = null;
	private Button returnParentBtn = null;
	private ArrayList<FileName> selectedFilePath = new ArrayList<FileName>();//selected file path
	private TextView mPath;
	private String currentPath = null;//current path
	private int selectType = 0;
	private MyFileAdapter adapter = null;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fileselect_layout);

		Intent intent = getIntent();
		selectType = intent.getExtras().getInt("selectType");

		Button buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		buttonConfirm.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				if(selectType == Constant.SELECT_FILES){
					intent.putExtra("selectType", Constant.SELECT_FILES);
					intent.putExtra("files", selectedFilePath);
				}else if(selectType == Constant.SELECT_FILE_PATH){
					File file = new File(currentPath);
					intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
					if(file.canWrite()){
						intent.putExtra("fileSavePath", currentPath);
					}
				}
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		Button buttonCancle = (Button) findViewById(R.id.buttonCancle);
		buttonCancle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			
				finish();
			}
		});
		
		returnRootBtn = (Button)findViewById(R.id.return_root_path);
		returnRootBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View returnRootBtn) {
				getFileDir(rootPath);
			}
		});
		returnParentBtn = (Button)findViewById(R.id.return_parent_path);
		returnParentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View returnParentBtn) {
				getFileDir(parentPath);
			}
		});
		
		mPath = (TextView) findViewById(R.id.mPath);
		TextView title = (TextView)findViewById(R.id.file_select_title);
		if(selectType == Constant.SELECT_FILE_PATH){
			title.setText(getString(R.string.select_path_for_save));
		}else{
			title.setText(getString(R.string.select_file_for_send));
		}
		
		getFileDir(rootPath);
	}
	
	/**
	 * @param filePath path to be opened
	 * 
	 */
	private void getFileDir(String filePath) {
		if(null==filePath)return;//beyond root path
		File dirFile = new File(filePath);
		parentPath = dirFile.getParent();//get upper path
		File[] files = dirFile.listFiles();//get all files
		if(null!=files){
			filePaths.clear();
			selectedFilePath.clear();
			currentPath = filePath;
			Constant.fileSelectedState.clear();
			mPath.setText(getString(R.string.current_path_label)+filePath);
			for (File file : files) {
				if(selectType == Constant.SELECT_FILE_PATH){//directory
					if(file.isDirectory()){
						FileName fPath = new FileName(1,file.getPath());
						filePaths.add(fPath);
					}
				}else{
					if(file.isDirectory()){
						FileName fPath = new FileName(1,file.getPath());
						filePaths.add(fPath);
					}else{
						FileName fPath = new FileName(2,file.getPath(),file.length(),false);
						filePaths.add(fPath);
					}
				}
			}
			Collections.sort(filePaths);//sort
			if(null==adapter){
				adapter = new MyFileAdapter(this,filePaths);
			}else{
				adapter.setDatasource(filePaths);
			}
			setListAdapter(adapter);//update through adapter
		}
	}
	
	/**
	 * Click listener
	 */
	@Override
	protected void onListItemClick(ListView listView, View itemView, int position, long id) {
		File file = new File(filePaths.get(position).fileName);//get corresponding file
		if (file.isDirectory()) {//open directory
			getFileDir(filePaths.get(position).fileName);
		} else {//change status
			CheckBox cb = (CheckBox)itemView.findViewById(R.id.file_selected);
			cb.setChecked(!cb.isChecked());
			onCheck(cb);//pass to onCheck();
		}
	}
	
	//check status
	public void onCheck(View fileSelectedCheckBox){
		CheckBox cb = (CheckBox)fileSelectedCheckBox;
		int fileIndex = (Integer)cb.getTag();//get tag
		Constant.fileSelectedState.put(fileIndex, cb.isChecked());
		if(cb.isChecked()){//store if checked
			FileName fName = filePaths.get(fileIndex);
			if(!selectedFilePath.contains(fName))selectedFilePath.add(filePaths.get(fileIndex));
		}else{//cancel
			selectedFilePath.remove(filePaths.get(fileIndex));
		}
	}

}