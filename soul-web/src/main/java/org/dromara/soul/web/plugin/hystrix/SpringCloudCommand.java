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

package org.dromara.soul.web.plugin.hystrix;

import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.convert.SpringCloudHandle;
import org.dromara.soul.common.enums.HttpMethodEnum;
import org.dromara.soul.common.enums.ResultEnum;
import org.dromara.soul.common.result.SoulResult;
import org.dromara.soul.common.utils.GSONUtils;
import org.dromara.soul.common.utils.JsonUtils;
import org.dromara.soul.common.utils.LogUtils;
import org.dromara.soul.web.plugin.SoulPluginChain;
import org.dromara.soul.web.request.RequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * the spring cloud command.
 *
 * @author xiaoyu(Myth)
 */
@SuppressWarnings("all")
public class SpringCloudCommand extends HystrixObservableCommand<Void> {

    /**
     * logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudCommand.class);

    private static final WebClient WEB_CLIENT = WebClient.create();

    private final ServerWebExchange exchange;

    private final SoulPluginChain chain;

    private final SpringCloudHandle springCloudHandle;

    private final RequestDTO requestDTO;

    private final URI remoteURI;

    /**
     * Instantiates a new Spring cloud command.
     *
     * @param setter            the setter
     * @param exchange          the exchange
     * @param chain             the chain
     * @param springCloudHandle the spring cloud handle
     * @param requestDTO        the request dto
     * @param remoteURI         the remote uri
     */
    public SpringCloudCommand(final Setter setter,
                              final ServerWebExchange exchange,
                              final SoulPluginChain chain,
                              final SpringCloudHandle springCloudHandle,
                              final RequestDTO requestDTO,
                              final URI remoteURI) {
        super(setter);
        this.exchange = exchange;
        this.chain = chain;
        this.springCloudHandle = springCloudHandle;
        this.requestDTO = requestDTO;
        this.remoteURI = remoteURI;
    }

    @Override
    protected Observable<Void> construct() {
        return RxReactiveStreams.toObservable(doRpcInvoke());
    }

    private Mono<Void> doRpcInvoke() {
        if (requestDTO.getHttpMethod().equals(HttpMethodEnum.GET.getName())) {
            String uri = buildRealURL();
            if (StringUtils.isNoneBlank(requestDTO.getExtInfo())) {
                uri = uri + "?" + GSONUtils.getInstance().toGetParam(requestDTO.getExtInfo());
            }
            return WEB_CLIENT.get().uri(uri)
                    .exchange()
                    .doOnError(e -> LogUtils.error(LOGGER, e::getMessage))
                    .timeout(Duration.ofMillis(springCloudHandle.getTimeout()))
                    .flatMap(this::doNext);
        } else if (requestDTO.getHttpMethod().equals(HttpMethodEnum.POST.getName())) {
            return WEB_CLIENT.post().uri(buildRealURL())
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
                    .exchange()
                    .doOnError(e -> LogUtils.error(LOGGER, e::getMessage))
                    .timeout(Duration.ofMillis(springCloudHandle.getTimeout()))
                    .flatMap(this::doNext);
        }
        return Mono.empty();
    }

    @Override
    protected Observable<Void> resumeWithFallback() {
        return RxReactiveStreams.toObservable(doFallback());
    }

    private Mono<Void> doNext(final ClientResponse res) {
        if (res.statusCode().is2xxSuccessful()) {
            exchange.getAttributes().put(Constants.CLIENT_RESPONSE_RESULT_TYPE, ResultEnum.SUCCESS.getName());
        } else {
            exchange.getAttributes().put(Constants.CLIENT_RESPONSE_RESULT_TYPE, ResultEnum.ERROR.getName());
        }
        exchange.getAttributes().put(Constants.CLIENT_RESPONSE_ATTR, res);
        return chain.execute(exchange);
    }

    private String buildRealURL() {
        final String rewriteURI = (String) exchange.getAttributes().get(Constants.REWRITE_URI);
        if (StringUtils.isBlank(rewriteURI)) {
            return String.join("/", remoteURI.toString(), requestDTO.getMethod());
        }
        return String.join("/", remoteURI.toString(), rewriteURI);
    }

    private Mono<Void> doFallback() {
        if (isFailedExecution()) {
            LogUtils.error(LOGGER, "spring cloud rpc have error:{}", () -> getExecutionException().getMessage());
        }

        if (getExecutionException() instanceof HystrixRuntimeException) {
            HystrixRuntimeException e = (HystrixRuntimeException) getExecutionException();
            if (e.getFailureType() == HystrixRuntimeException.FailureType.TIMEOUT) {
                exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        final SoulResult error = SoulResult.error(Constants.SPRING_CLOUD_ERROR_RESULT);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(Objects.requireNonNull(JsonUtils.toJson(error)).getBytes())));
    }
}
