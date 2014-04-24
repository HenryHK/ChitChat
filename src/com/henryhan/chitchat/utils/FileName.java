package com.henryhan.chitchat.utils;

import java.io.File;
import java.io.Serializable;


public class FileName implements Comparable<FileName>,Serializable{
	private static final long serialVersionUID = 1L;
	
	public int type = 0;
	public long fileSize = -1;
	public String fileName = "";
	public boolean isDirectory = true;
	
	public FileName(){}
	
	public FileName(int type,String fileName){
		this.type = type;
		this.fileName = fileName;
	}
	public FileName(int type,String fileName,long fileSize,boolean isDirectory){
		this.type = type;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.isDirectory = isDirectory;
	}
	
	public String getFileName(){
		int index = fileName.lastIndexOf(File.separator);
		return fileName.substring(index+1);
	}
	
	public String getFullPath(){
		return fileName;
	}
	
	@Override
	public int compareTo(FileName fileN) {
		int result = -2;
		if(type < fileN.type)result = -1;
		if(type == fileN.type)result = 0;
		if(type > fileN.type)result = 1;
		return result;
	}
	
    public int hashCode() {
	   int result = 56;
	   result = 56 * result + type ;
	   result = 56 * result + fileName.hashCode();
	   return result;
	}  
    
     public boolean equals(Object o) {
    	if (!(o instanceof FileName))
    		return false ;
    	FileName fileN = (FileName) o;
    	return (type == fileN.type ) && ( fileName.equals(fileN.fileName ));
     }
}
