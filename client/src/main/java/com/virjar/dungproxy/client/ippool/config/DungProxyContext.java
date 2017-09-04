package com.virjar.dungproxy.client.ippool.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.GroupBindRouter;
import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.Offline;
import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.ippool.strategy.Scoring;
import com.virjar.dungproxy.client.ippool.strategy.impl.AvProxyDumperWrapper;
import com.virjar.dungproxy.client.ippool.strategy.impl.BlackListProxyStrategy;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultOffliner;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultProxyChecker;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultScoring;
import com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.impl.ProxyAllStrategy;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/1/23.<br/>
 * 适用在整个项目的上下文
 *
 */
@Slf4j
public class DungProxyContext {

	private AvProxyDumper avProxyDumper;
	@Getter
	private ProxyDomainStrategy needProxyStrategy;
	@Getter
	private String clientID;
	@Getter
	private GroupBindRouter groupBindRouter = new GroupBindRouter();
	@Getter
	private long feedBackDuration;
	@Getter
	private PreHeater preHeater = new PreHeater(this);
	@Getter
	private String serverBaseUrl;
	@Getter
	private long serializeStep;
	@Getter
	private boolean poolEnabled;

	// for domain
	private Class<? extends ResourceFacade> defaultResourceFacade;
	private Class<? extends Offline> defaultOffliner;
	private Class<? extends Scoring> defaultScoring;
	private Class<? extends ProxyChecker> defaultProxyChecker;
	@Getter
	private int defaultCoreSize;
	@Getter
	private double defaultSmartProxyQueueRatio;
	@Getter
	private long defaultUseInterval;
	@Getter
	private int defaultScoreFactory;
	@Getter
	private Map<String, DomainContext> domainConfig = Maps.newConcurrentMap();

	// 这个需要考虑并发安全吗?
	private Set<AvProxyVO> cloudProxySet = Sets.newConcurrentHashSet();

	private boolean waitIfNoAvailableProxy = false;

	/**
	 * 加载全局的默认配置,统一加载后防止NPE
	 */
	public void fillDefaultStrategy() {
		avProxyDumper = new JSONFileAvProxyDumper();
		needProxyStrategy = new ProxyAllStrategy();// new WhiteListProxyStrategy();
		feedBackDuration = 1200000;// 20分钟一次 反馈
		defaultResourceFacade = DefaultResourceFacade.class;
		defaultOffliner = DefaultOffliner.class;
		defaultScoring = DefaultScoring.class;
		defaultProxyChecker = DefaultProxyChecker.class;
		defaultCoreSize = 50;
		defaultSmartProxyQueueRatio = 0.3D;
		defaultUseInterval = 15000;// 默认IP15秒内不能重复使用
		defaultScoreFactory = 15;
		serverBaseUrl = "http://proxy.scumall.com:8080";
		serializeStep = 30;
		poolEnabled = false;
		waitIfNoAvailableProxy = false;
		handleConfig();
	}

	public AvProxyDumper getAvProxyDumper() {
		return avProxyDumper;
	}

	public DungProxyContext setAvProxyDumper(AvProxyDumper avProxyDumper) {
		this.avProxyDumper = new AvProxyDumperWrapper(avProxyDumper);
		return this;
	}

	public DungProxyContext setWaitIfNoAvailableProxy(boolean waitIfNoAvailableProxy) {
		this.waitIfNoAvailableProxy = waitIfNoAvailableProxy;
		return this;
	}

	public boolean isWaitIfNoAvailableProxy() {
		return waitIfNoAvailableProxy;
	}

	public DungProxyContext setPoolEnabled(boolean poolEnabled) {
		this.poolEnabled = poolEnabled;
		return this;
	}

	public DungProxyContext setClientID(String clientID) {
		this.clientID = clientID;
		return this;
	}

	public DungProxyContext setDefaultUseInterval(long defaultUseInterval) {
		this.defaultUseInterval = defaultUseInterval;
		return this;
	}

	public DungProxyContext setDefaultCoreSize(int defaultCoreSize) {
		this.defaultCoreSize = defaultCoreSize;
		return this;
	}

	public Class<? extends Offline> getDefaultOffliner() {
		return defaultOffliner;
	}

	public DungProxyContext setDefaultOffliner(Class<? extends Offline> defaultOffliner) {
		this.defaultOffliner = defaultOffliner;
		return this;
	}

	public Class<? extends ResourceFacade> getDefaultResourceFacade() {
		return defaultResourceFacade;
	}

	public DungProxyContext setDefaultResourceFacade(Class<? extends ResourceFacade> defaultResourceFacade) {
		this.defaultResourceFacade = defaultResourceFacade;
		return this;
	}

	public Class<? extends Scoring> getDefaultScoring() {
		return defaultScoring;
	}

	public DungProxyContext setDefaultScoring(Class<? extends Scoring> defaultScoring) {
		this.defaultScoring = defaultScoring;
		return this;
	}

	public void setDefaultSmartProxyQueueRatio(double defaultSmartProxyQueueRatio) {
		this.defaultSmartProxyQueueRatio = defaultSmartProxyQueueRatio;
	}

	public DungProxyContext addDomainConfig(DomainContext domainConfig) {
		this.domainConfig.put(domainConfig.getDomain(), domainConfig);
		domainConfig.setDungProxyContext(this);
		domainConfig.extendWithDungProxyContext(this);
		return this;
	}

	public DungProxyContext setFeedBackDuration(long feedBackDuration) {
		this.feedBackDuration = feedBackDuration;
		return this;
	}

	public DungProxyContext setGroupBindRouter(GroupBindRouter groupBindRouter) {
		this.groupBindRouter = groupBindRouter;
		return this;
	}

	public DungProxyContext setNeedProxyStrategy(ProxyDomainStrategy needProxyStrategy) {
		this.needProxyStrategy = needProxyStrategy;
		return this;
	}

	public DungProxyContext setPreHeater(PreHeater preHeater) {
		this.preHeater = preHeater;
		return this;
	}

	public DungProxyContext setSerializeStep(long serializeStep) {
		this.serializeStep = serializeStep;
		return this;
	}

	public DungProxyContext setServerBaseUrl(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
		return this;
	}

	public DungProxyContext setDefaultScoreFactory(int defaultScoreFactory) {
		this.defaultScoreFactory = defaultScoreFactory;
		return this;
	}

	public Class<? extends ProxyChecker> getDefaultProxyChecker() {
		return defaultProxyChecker;
	}

	public DungProxyContext setDefaultProxyChecker(Class<? extends ProxyChecker> defaultProxyChecker) {
		this.defaultProxyChecker = defaultProxyChecker;
		return this;
	}

	public DungProxyContext addCloudProxy(AvProxyVO cloudProxy) {
		this.cloudProxySet.add(cloudProxy);
		return this;
	}

	public Collection<AvProxyVO> getCloudProxies() {
		return Lists.newArrayList(cloudProxySet);// copy 新数据到外部
	}

	/**
	 * 根据域名产生domain的schema
	 *
	 * @return DomainContext
	 */
	public DomainContext genDomainContext(String domain) {
		DomainContext domainContext = domainConfig.get(domain);
		if (domainContext != null) {
			return domainContext;
		}

		synchronized (DungProxyContext.class) {
			domainContext = domainConfig.get(domain);
			if (domainContext != null) {
				return domainContext;
			}
			domainConfig.put(domain, DomainContext.create(domain).extendWithDungProxyContext(this));
			return domainConfig.get(domain);
		}
	}

	public static DungProxyContext create() {
		DungProxyContext context = new DungProxyContext();
		context.fillDefaultStrategy();
		context.buildDefaultConfigFile();
		context.handleConfig();
		return context;
	}

	public DungProxyContext handleConfig() {
		if (defaultResourceFacade.isAssignableFrom(DefaultResourceFacade.class)) {
			DefaultResourceFacade.setAllAvUrl(serverBaseUrl + "/proxyipcenter/allAv");
			DefaultResourceFacade.setAvUrl(serverBaseUrl + "/proxyipcenter/av");
			DefaultResourceFacade.setFeedBackUrl(serverBaseUrl + "/proxyipcenter/feedBack");
			DefaultResourceFacade.setClientID(clientID);
		}
		return this;
	}

	public DungProxyContext buildDefaultConfigFile() {
		InputStream resourceAsStream = DungProxyContext.class.getClassLoader().getResourceAsStream(
				ProxyConstant.CLIENT_CONFIG_FILE_NAME);
		if (resourceAsStream == null) {
			try {
				resourceAsStream = new FileInputStream(ProxyConstant.CLIENT_CONFIG_FILE_NAME);
			} catch (FileNotFoundException fio) {
				log.warn("can not open file {}", ProxyConstant.CLIENT_CONFIG_FILE_NAME, fio);
			}
		}
		if (resourceAsStream == null) {
			log.warn("没有找到配置文件:{},代理规则可以通过代码来控制", ProxyConstant.CLIENT_CONFIG_FILE_NAME);
			return this;
		}
		Properties properties = new Properties();
		try {
			properties.load(resourceAsStream);
			return buildWithProperties(properties);
		} catch (IOException e) {
			log.error("config file load error for file:{}", ProxyConstant.CLIENT_CONFIG_FILE_NAME, e);
		} finally {
			IOUtils.closeQuietly(resourceAsStream);
		}
		return this;
	}

	public DungProxyContext buildWithProperties(Properties properties) {
		if (properties == null) {
			return this;
		}

		// IP下载策略
		String resourceFace = properties.getProperty(ProxyConstant.RESOURCE_FACADE);
		if (StringUtils.isNotEmpty(resourceFace)) {
			defaultResourceFacade = ObjectFactory.classForName(resourceFace);
		}
		String defaultResourceServerAddress = properties.getProperty(ProxyConstant.DEFAULT_RESOURCE_SERVER_ADDRESS);
		if (StringUtils.isNotEmpty(defaultResourceServerAddress)) {
			serverBaseUrl = defaultResourceServerAddress;
		}

		// IP代理策略
		String proxyDomainStrategy = properties.getProperty(ProxyConstant.PROXY_DOMAIN_STRATEGY);
		if (StringUtils.isEmpty(proxyDomainStrategy)) {// 如果没有明确配置代理策略,则以黑白名单key值为主
			if (properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY) != null) {
				proxyDomainStrategy = WhiteListProxyStrategy.class.getName();
			} else if (properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY) != null) {
				proxyDomainStrategy = BlackListProxyStrategy.class.getName();
			} else {// 如果都没有,则默认代理所有请求
				proxyDomainStrategy = ProxyAllStrategy.class.getName();
			}
		}
		if ("WHITE_LIST".equalsIgnoreCase(proxyDomainStrategy)) {
			proxyDomainStrategy = WhiteListProxyStrategy.class.getName();
		} else if ("BLACK_LIST".equalsIgnoreCase(proxyDomainStrategy)) {
			proxyDomainStrategy = BlackListProxyStrategy.class.getName();
		}
		needProxyStrategy = ObjectFactory.newInstance(proxyDomainStrategy);
		if (needProxyStrategy instanceof WhiteListProxyStrategy) {
			WhiteListProxyStrategy whiteListProxyStrategy = (WhiteListProxyStrategy) needProxyStrategy;
			String whiteListProperty = properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY);
			whiteListProxyStrategy.addAllHost(whiteListProperty);
		} else if (needProxyStrategy instanceof BlackListProxyStrategy) {
			BlackListProxyStrategy blackListProxyStrategy = (BlackListProxyStrategy) needProxyStrategy;
			String proxyDomainStrategyWhiteList = properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY);
			blackListProxyStrategy.addAllHost(proxyDomainStrategyWhiteList);
		}

		// 反馈时间
		String feedBackDurationProperties = properties.getProperty(ProxyConstant.FEEDBACK_DURATION);

		if (!Strings.isNullOrEmpty(feedBackDurationProperties)) {
			feedBackDuration = NumberUtils.toLong(feedBackDurationProperties);
		}

		// 序列化接口
		String avDumper = properties.getProperty(ProxyConstant.PROXY_SERIALIZER);
		if (StringUtils.isNotEmpty(avDumper)) {
			AvProxyDumper tempDumper = ObjectFactory.newInstance(avDumper);
			setAvProxyDumper(tempDumper);// 对他做一层包装,防止空序列化

		}
		String defaultAvDumpeFileName = properties.getProperty(ProxyConstant.DEFAULT_PROXY_SERALIZER_FILE);
		if (StringUtils.isNotEmpty(defaultAvDumpeFileName)) {
			avProxyDumper.setDumpFileName(defaultAvDumpeFileName);
		}

		String preHeaterTaskList = properties.getProperty(ProxyConstant.PREHEATER_TASK_LIST);
		if (StringUtils.isNotEmpty(preHeaterTaskList)) {
			for (String url : Splitter.on(",").split(preHeaterTaskList)) {
				preHeater.addTask(url);
			}
		}

		String preheaterSerilizeStep = properties.getProperty(ProxyConstant.PREHEAT_SERIALIZE_STEP);
		if (StringUtils.isNotEmpty(preheaterSerilizeStep)) {
			serializeStep = NumberUtils.toLong(preheaterSerilizeStep, 30L);
		}

		String proxyUseInterval = properties.getProperty(ProxyConstant.PROXY_USE_INTERVAL);
		if (StringUtils.isNotEmpty(proxyUseInterval)) {
			defaultUseInterval = NumberUtils.toLong(proxyUseInterval, 15000);
		}
		clientID = properties.getProperty(ProxyConstant.CLIENT_ID);
		String ruleRouter = properties.getProperty(ProxyConstant.PROXY_DOMAIN_STRATEGY_ROUTE);
		if (StringUtils.isNotEmpty(ruleRouter)) {
			groupBindRouter.buildCombinationRule(ruleRouter);
		}
		handleConfig();
		return this;
	}
}
