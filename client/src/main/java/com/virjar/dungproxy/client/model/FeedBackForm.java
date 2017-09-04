package com.virjar.dungproxy.client.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by virjar on 16/10/3.
 */
@Getter
@Setter
public class FeedBackForm {
	private String domain;
	private List<AvProxyVO> avProxy;
	private List<AvProxyVO> disableProxy;
}
