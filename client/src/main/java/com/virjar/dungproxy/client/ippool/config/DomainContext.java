package com.virjar.dungproxy.client.ippool.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.strategy.Offline;
import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.ippool.strategy.Scoring;
import com.virjar.dungproxy.client.model.AvProxyVO;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by virjar on 17/1/23.
 */
public class DomainContext {
	@Getter
	private DungProxyContext dungProxyContext;
	@Getter
	@Setter
	private ResourceFacade resourceFacade;
	@Getter
	private Scoring scoring;
	@Getter
	private Offline offline;
	@Getter
	private ProxyChecker proxyChecker;
	@Getter
	private int coreSize;
	@Getter
	private double smartProxyQueueRatio;
	@Getter
	private long useInterval;
	@Getter
	private String domain;
	@Getter
	private int scoreFactory;
	@Getter
	private List<AvProxyVO> defaultProxy;

	/**
	 * 不允许包外访问
	 */
	DomainContext(String domain) {
		this.domain = domain;
	}

	public DomainContext setCoreSize(int coreSize) {
		Preconditions.checkArgument(coreSize > 0, "domain pool core size must greater zero");
		this.coreSize = coreSize;
		return this;
	}

	public DomainContext setDungProxyContext(DungProxyContext dungProxyContext) {
		this.dungProxyContext = dungProxyContext;
		return this;
	}

	public DomainContext setSmartProxyQueueRatio(double smartProxyQueueRatio) {
		this.smartProxyQueueRatio = smartProxyQueueRatio;
		return this;
	}

	public DomainContext setUseInterval(long useInterval) {
		Preconditions.checkArgument(useInterval > 0, "useInterval must greater zero");
		this.useInterval = useInterval;
		return this;
	}

	public static DomainContext create(String domain) {
		Preconditions.checkNotNull(domain);
		return new DomainContext(domain);
	}

	public DomainContext setOffline(Offline offline) {
		this.offline = offline;
		return this;
	}

	public DomainContext setScoring(Scoring scoring) {
		this.scoring = scoring;
		return this;
	}

	public DomainContext setScoreFactory(int scoreFactory) {
		Preconditions.checkArgument(scoreFactory > 0, "scoreFactory must greater than zero");
		this.scoreFactory = scoreFactory;
		return this;
	}

	public DomainContext setProxyChecker(ProxyChecker proxyChecker) {
		this.proxyChecker = proxyChecker;
		return this;
	}

	public DomainContext setDefaultProxy(List<AvProxyVO> defaultProxy) {
		this.defaultProxy = defaultProxy;
		return this;
	}

	/**
	 * 检查缺失配置项,如果有缺失,则添加默认策略
	 * 
	 * @param dungProxyContext
	 *            全局context,获取全局的配置信息
	 * @return DomainContext
	 */
	DomainContext extendWithDungProxyContext(DungProxyContext dungProxyContext) {
		this.dungProxyContext = dungProxyContext;
		if (coreSize <= 1) {
			coreSize = dungProxyContext.getDefaultCoreSize();
		}
		if (smartProxyQueueRatio <= 0.1) {
			smartProxyQueueRatio = dungProxyContext.getDefaultSmartProxyQueueRatio();
		}
		if (useInterval <= 1L) {
			useInterval = dungProxyContext.getDefaultUseInterval();
		}
		if (resourceFacade == null) {
			resourceFacade = ObjectFactory.newInstance(dungProxyContext.getDefaultResourceFacade());
		}

		if (scoring == null) {
			scoring = ObjectFactory.newInstance(dungProxyContext.getDefaultScoring());
		}

		if (offline == null) {
			this.offline = ObjectFactory.newInstance(dungProxyContext.getDefaultOffliner());
		}

		if (this.scoreFactory <= 0) {
			this.scoreFactory = dungProxyContext.getDefaultScoreFactory();
		}
		if (proxyChecker == null) {
			this.proxyChecker = ObjectFactory.newInstance(dungProxyContext.getDefaultProxyChecker());
		}
		if (defaultProxy == null) {
			this.defaultProxy = Lists.newArrayList();
		}
		return this;
	}
}
