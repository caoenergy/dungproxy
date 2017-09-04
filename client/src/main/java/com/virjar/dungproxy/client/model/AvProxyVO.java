package com.virjar.dungproxy.client.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.apache.http.Header;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.DomainPool;
import com.virjar.dungproxy.client.ippool.config.DomainContext;

/**
 * Created by virjar on 16/12/24. <br/>
 * 仅仅为了序列化和反序列化
 */
public class AvProxyVO {
	// IP地址
	@Getter
	@Setter
	private String ip;

	// 端口号
	@Getter
	@Setter
	private Integer port;

	// 引用次数,当引用次数为0的时候,由调度任务清除该
	@Getter
	@Setter
	private Integer referCount = 0;

	@Getter
	@Setter
	private Integer failedCount = 0;

	// 平均打分 需要归一化,在0-1的一个小数
	@Getter
	@Setter
	private double avgScore = 0D;

	// 属于那一个网站的代理IP
	@Getter
	@Setter
	private String domain;

	// 是否是云代理,云代理将会参与使用竞争,但是不会下线,云代理本身后面存在一个IP池,做数据转发
	@Getter
	@Setter
	private Boolean cloud;

	/**
	 * 云代理允许存在副本,这样实际上一个云代理可以持有多条通道。有些云代理供应商对客户的使用有并发限制,这个特性就是在这里控制
	 */
	@Getter
	@Setter
	private Integer partnerSize;

	// 用户名,如果本IP需要登录认证
	@Getter
	@Setter
	private String username;

	// 密码,如果本IP需要登录认证
	@Getter
	@Setter
	private String password;

	// 有些代理是通过请求头进行认证的
	@Setter
	private List<Header> authenticationHeaders = Lists.newArrayList();

	public List<Header> getAuthenticationHeaders() {
		return Lists.newArrayList(authenticationHeaders);
	}

	public AvProxy toModel(DomainContext domainContext) {
		AvProxy avProxy;
		if (cloud != null && cloud) {
			avProxy = new CloudProxy(domainContext);
		} else {
			avProxy = new AvProxy(domainContext);
		}
		avProxy.setIp(ip);
		avProxy.setPort(port);
		avProxy.setAvgScore(avgScore);
		avProxy.setReferCount(referCount);
		avProxy.setFailedCount(failedCount);
		avProxy.setUsername(username);
		avProxy.setPassword(password);
		avProxy.setAuthenticationHeaders(getAuthenticationHeaders());
		return avProxy;
	}

	public List<? extends AvProxy> toPartnerModels(DomainContext domainContext) {
		if (cloud == null || !cloud) {
			return Lists.newArrayList(toModel(domainContext));
		}
		List<CloudProxy> cloudProxies = Lists.newArrayList();
		for (int i = 0; i < partnerSize; i++) {
			CloudProxy cloudProxy = (CloudProxy) toModel(domainContext);
			cloudProxy.setPartners(cloudProxies);
			cloudProxy.setOffset(i);
			cloudProxies.add(cloudProxy);

		}
		return cloudProxies;
	}

	public AvProxy toModel(DomainPool domainPool) {
		return toModel(domainPool.getDomainContext());
	}

	public static AvProxyVO fromModel(AvProxy avProxy) {
		AvProxyVO avProxyVO = new AvProxyVO();
		avProxyVO.setIp(avProxy.getIp());
		avProxyVO.setPort(avProxy.getPort());
		avProxyVO.setFailedCount(avProxy.getFailedCount());
		avProxyVO.setReferCount(avProxy.getReferCount());
		avProxyVO.setAvgScore(avProxy.getAvgScore());
		avProxyVO.setDomain(avProxy.getDomainPool().getDomain());
		avProxyVO.setCloud(avProxy instanceof CloudProxy);

		avProxyVO.setAuthenticationHeaders(avProxy.getAuthenticationHeaders());
		avProxyVO.setUsername(avProxy.getUsername());
		avProxyVO.setPassword(avProxy.getPassword());
		return avProxyVO;
	}
}
