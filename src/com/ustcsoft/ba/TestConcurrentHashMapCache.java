package com.ustcsoft.ba;

import java.util.concurrent.ConcurrentHashMap;

public class TestConcurrentHashMapCache<K,V> {
	private final ConcurrentHashMap<K, V>  cacheMap=new ConcurrentHashMap<K,V> (); 
	
	public  Object getCache(K keyValue,String ThreadName){
		System.out.println("ThreadName getCache=============="+ThreadName);
		Object value=null;
		//从缓存获取数据
		value=cacheMap.get(keyValue);
		//如果没有的话，把数据放到缓存
		if(value==null){
			return putCache(keyValue,ThreadName);
		}
		return value;
	}
	
	public Object putCache(K keyValue,String ThreadName){
		System.out.println("ThreadName 执行业务数据并返回处理结果的数据（访问数据库等）=============="+ThreadName);
		//可以根据业务从数据库获取等取得数据,这边就模拟已经获取数据了
		@SuppressWarnings("unchecked")
		V value=(V) "dataValue";
		//把数据放到缓存
		 cacheMap.put(keyValue, value);
		return value;
	}

	
	
	public static void main(String[] args) {
		final TestConcurrentHashMapCache<String,String> TestGuaVA=new TestConcurrentHashMapCache<String,String>();
		
		Thread t1=new Thread(new Runnable() {
			@Override
			public void run() {
				
				System.out.println("T1======start========");
			    Object value=TestGuaVA.getCache("key","T1");
			    System.out.println("T1 value=============="+value);
				System.out.println("T1======end========");
				
			}
		});
		
		Thread t2=new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("T2======start========");
				Object value=TestGuaVA.getCache("key","T2");
			    System.out.println("T2 value=============="+value);
				System.out.println("T2======end========");
				
			}
		});
		
		Thread t3=new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("T3======start========");
				Object value=TestGuaVA.getCache("key","T3");
			    System.out.println("T3 value=============="+value);
				System.out.println("T3======end========");
				
			}
		});
		
		t1.start();
		t2.start();
		t3.start();

	}

}