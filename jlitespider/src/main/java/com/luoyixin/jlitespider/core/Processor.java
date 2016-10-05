package com.luoyixin.jlitespider.core;

import java.io.IOException;
import java.util.Map;

/**
 * author: Yixin Luo
 * 2016/3/3
 * 
 * 解析器的接口。解析器用于提取关键信息，使用jsoup或正则表达式等完成任务。
 * 
 * **/
public interface Processor{
	/**
	 * pages是传入的要进行解析的文本
	 * 
	 * **/
	public void process(Object page, Map<String, MessageQueue> mQueue) throws IOException;
}
