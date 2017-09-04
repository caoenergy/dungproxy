package com.virjar.dungproxy.client.ippool.config;

/**
 * Created by virjar on 16/9/30. 全局配置项
 */
public interface ProxyConstant {
	String USED_PROXY_KEY = "USED_PROXY_KEY";
	/**
	 * 在httpclient里面,如果存在这个key值为true,则取消代理
	 */
	String DISABLE_DUNGPROXY_KEY = "DISABLE_DUNGPROXY_KEY";

	/**
	 * 用户的key值,在httpclientContext里面,同这个key值确定是那个用户在访问,以实现用户隔离
	 */
	String DUNGPROXY_USER_KEY = "DUNGPROXY_USER_KEY";

	// config 文件默认配置key值
	String RESOURCE_FACADE = "proxyclient.resouce.resourceFacade";
	String PROXY_DOMAIN_STRATEGY = "proxyclient.proxyDomainStrategy";
	String DEFAULT_DOMAIN_STRATEGY = "WHITE_LIST";
	String WHITE_LIST_STRATEGY = "proxyclient.proxyDomainStrategy.whiteList";
	String FEEDBACK_DURATION = "proxyclient.feedback.duration";
	String DEFAULT_RESOURCE_SERVER_ADDRESS = "proxyclient.resource.defaultResourceServerAddress";

	String PROXY_USE_INTERVAL = "proxyclient.proxyUseIntervalMillis";
	String CLIENT_ID = "proxyclient.clientID";
	String PROXY_DOMAIN_STRATEGY_ROUTE = "proxyclient.proxyDomainStrategy.group";

	String PREHEATER_TASK_LIST = "proxyclient.preHeater.testList";
	String PREHEAT_SERIALIZE_STEP = "proxyclient.preHeater.serialize.step";
	String CLIENT_CONFIG_FILE_NAME = "proxyclient.properties";
	String PROXY_SERIALIZER = "proxyclient.serialize.serializer";
	String DEFAULT_PROXY_SERALIZER_FILE = "proxyclient.DefaultAvProxyDumper.dumpFileName";

	String DEFAULT_PROXY_SERALIZER_FILE_VALUE = "availableProxy.json";

	// 以下几个超时时间允许被调整
	// socket超时时间
	int SOCKET_TIMEOUT = 30000;
	// 连接超时
	int CONNECT_TIMEOUT = 30000;
	// 连接池分配连接超时时间,一般用处不大
	int REQUEST_TIMEOUT = 30000;
	int SOCKETSO_TIMEOUT = 15000;
}
