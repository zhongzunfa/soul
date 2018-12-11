/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.dromara.soul.web.plugin.function;

import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.constant.DubboParamConstants;
import org.dromara.soul.common.dto.convert.DubboHandle;
import org.dromara.soul.common.dto.zk.RuleZkDTO;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.PluginTypeEnum;
import org.dromara.soul.common.enums.ResultEnum;
import org.dromara.soul.common.enums.RpcTypeEnum;
import org.dromara.soul.common.utils.GSONUtils;
import org.dromara.soul.common.utils.LogUtils;
import org.dromara.soul.web.cache.ZookeeperCacheManager;
import org.dromara.soul.web.plugin.AbstractSoulPlugin;
import org.dromara.soul.web.plugin.SoulPluginChain;
import org.dromara.soul.web.plugin.dubbo.DubboProxyService;
import org.dromara.soul.web.plugin.hystrix.DubboCommand;
import org.dromara.soul.web.plugin.hystrix.HystrixBuilder;
import org.dromara.soul.web.request.RequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import rx.Subscription;

import java.util.Map;
import java.util.Objects;

/**
 * dubbo proxy.
 *
 * @author xiaoyu(Myth)
 */
public class DubboPlugin extends AbstractSoulPlugin {

    /**
     * logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DubboPlugin.class);

    private final DubboProxyService dubboProxyService;

    /**
     * Instantiates a new Dubbo plugin.
     *
     * @param zookeeperCacheManager the zookeeper cache manager
     * @param dubboProxyService     the dubbo proxy service
     */
    public DubboPlugin(final ZookeeperCacheManager zookeeperCacheManager, final DubboProxyService dubboProxyService) {
        super(zookeeperCacheManager);
        this.dubboProxyService = dubboProxyService;
    }


    /**
     * this is Template Method child has Implement your own logic.
     *
     * @param exchange exchange the current server exchange {@linkplain ServerWebExchange}
     * @param chain    chain the current chain  {@linkplain ServerWebExchange}
     * @param rule     rule    {@linkplain RuleZkDTO}
     * @return {@code Mono<Void>} to indicate when request handling is complete
     */
    @Override
    protected Mono<Void> doExecute(final ServerWebExchange exchange, final SoulPluginChain chain, final RuleZkDTO rule) {


        final Map<String, Object> paramMap = exchange.getAttribute(Constants.DUBBO_PARAMS);

        assert paramMap != null;

        final String handle = rule.getHandle();

        final DubboHandle dubboHandle = GSONUtils.getInstance().fromJson(handle, DubboHandle.class);

        if (StringUtils.isBlank(dubboHandle.getGroupKey())) {
            dubboHandle.setGroupKey(String.valueOf(paramMap.get(DubboParamConstants.INTERFACE_NAME)));
        }

        if (StringUtils.isBlank(dubboHandle.getCommandKey())) {
            dubboHandle.setCommandKey(String.valueOf(paramMap.get(DubboParamConstants.METHOD)));
        }

        if (!checkData(dubboHandle)) {
            return chain.execute(exchange);
        }

        DubboCommand command =
                new DubboCommand(HystrixBuilder.build(dubboHandle), paramMap,
                        exchange, chain, dubboProxyService, dubboHandle);

        return Mono.create((MonoSink<Object> s) -> {
            Subscription sub = command.toObservable().subscribe(s::success,
                    s::error, s::success);
            s.onCancel(sub::unsubscribe);
            if (command.isCircuitBreakerOpen()) {
                LogUtils.error(LOGGER, () -> dubboHandle.getGroupKey() + ":dubbo execute circuitBreaker is Open !");
            }
        }).doOnError(throwable -> {
            throwable.printStackTrace();
            exchange.getAttributes().put(Constants.CLIENT_RESPONSE_RESULT_TYPE,
                    ResultEnum.ERROR.getName());
            chain.execute(exchange);
        }).then();
    }

    /**
     * return plugin type.
     *
     * @return {@linkplain PluginTypeEnum}
     */
    @Override
    public PluginTypeEnum pluginType() {
        return PluginTypeEnum.FUNCTION;
    }

    /**
     * acquire plugin name.
     *
     * @return plugin name.
     */
    @Override
    public String named() {
        return PluginEnum.DUBBO.getName();
    }

    /**
     * plugin is execute.
     *
     * @param exchange the current server exchange
     * @return default false.
     */
    @Override
    public Boolean skip(final ServerWebExchange exchange) {
        final RequestDTO body = exchange.getAttribute(Constants.REQUESTDTO);
        assert body != null;
        return !Objects.equals(body.getRpcType(), RpcTypeEnum.DUBBO.getName());
    }

    @Override
    public int getOrder() {
        return PluginEnum.DUBBO.getCode();
    }

    private boolean checkData(final DubboHandle dubboHandle) {
        if (StringUtils.isBlank(dubboHandle.getGroupKey())
                || StringUtils.isBlank(dubboHandle.getCommandKey())
                || StringUtils.isBlank(dubboHandle.getRegistry())
                || StringUtils.isBlank(dubboHandle.getAppName())) {
            LogUtils.error(LOGGER, () -> "dubbo handle require param not config!");
            return false;
        }
        return true;
    }

}
