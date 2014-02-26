package com.kiven.main;

import com.kiven.test.Public;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * 发送消息类
 * @author Kiven
 *
 */
public class P{
	private final static String host = Public.host;
    private final static String QUEUE_NAME = Public.QUEUE_NAME;
    private final static String EXCHANGE_NAME = Public.EXCHANGE_NAME;
    private final static String ROUTINGKEY = Public.ROUTINGKEY;
    
    private static ConnectionFactory factory;
    private static Connection connection;
    /**
     * 线程设置
     */
    static int threads = Public.threads;		// 运行的测试线程数   
    static int runs = Public.runs;  			// 每个线程运行的次数
    static int size = Public.size;				//写入数据的大小,单位：字节
    
    static long sendTime = 0;
    static long recvTime = 0;
    
    static Integer myLock;   		// 锁定一下计数器
    static byte[] testdata;
    
    public static void main(String[] args) throws Exception {
    	//设置数据大小
    	testdata = new byte[size];
		for(int i=0;i<size;i++){
			testdata[i] = 'A';
		}
        myLock = new Integer(threads);
        System.out.println("正在测试...");   
        
		// 创建链接工程
		factory = new ConnectionFactory();
        factory.setHost(host);
        // 创建一个新的消息队列服务器实体的连接
        connection = factory.newConnection();
        
        new P().dipatcher();
    }
    
    public void dipatcher(){
    	SendThread send = new SendThread();
		for(int i =0;i<threads;i++){
			new Thread(send).start();
		}
	}

    class SendThread implements Runnable{
		SendThread(){
			
    	}
		
		public void run(){
			publish();
		}
    	
    	public void publish(){
	        try{       
	        	// 创建一个新的消息读写的通道
	            Channel channel = connection.createChannel();
		        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		        // declare a queue (声明一个队列)  
		        // 参数分别为
		        // 1,队列的名字  
		        // 2,是否为一个持久的队列，持久的队列在服务重新启动的后依然存在  
		        // 3,如果为true，那么就在此次连接中建立一个独占的队列  
		        // 4,是否为自动删除  
		        // 5,队列的一些其他构造参数 
		        // channel.queueDeclare(QUEUE_NAME, true, false, false, null);
		        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
		        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTINGKEY);
				
				//==========================发送消息开始=================================
	    		long startTime = System.currentTimeMillis();
	    		for(int i=0;i<runs;i++){
	        		try {
	        			//发送消息  MessageProperties.PERSISTENT_TEXT_PLAIN:将消息设为持久化
	        			channel.basicPublish(EXCHANGE_NAME, ROUTINGKEY, MessageProperties.PERSISTENT_TEXT_PLAIN, testdata);
	        			///System.out.println(" [x] Sent '" + new String(testdata,"utf-8") + "'");
	    			} catch (Exception e) {
	    				e.printStackTrace();
	    			}
	    		}
	    		long endTime = System.currentTimeMillis() - startTime;
	    		synchronized (myLock) {
	    			//发送消息总时间
	            	sendTime += endTime;
	            	myLock--;
	    			if(myLock.equals(0)){
	    				try {
	    					channel.close();
	    					connection.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
		    			System.out.println("测试完成!\n启动线程数:【" + threads + "】\t每个线程发送消息数:【" + runs + "】\t发送消息包大小:【"+size+" byte】");   
		                System.out.println("发送消息处理时间:【" + sendTime + " ms】\t处理发送消息速度(QPS):每秒【" + runs * threads * 1000 / sendTime + " 次】\t发送消息的平均时间:【"+sendTime/(runs*threads)+" ms】");
	    			}
	            }
	    		//==========================发送消息结束=================================
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
    	}
		
    }
}