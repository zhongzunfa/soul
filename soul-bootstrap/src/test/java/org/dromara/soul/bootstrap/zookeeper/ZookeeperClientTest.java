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

package org.dromara.soul.bootstrap.zookeeper;

import com.google.common.collect.Maps;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.assertj.core.util.Lists;
import org.dromara.soul.bootstrap.BaseTest;
import org.dromara.soul.common.constant.ZkPathConstants;
import org.dromara.soul.common.dto.convert.DivideHandle;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.dto.convert.DubboHandle;
import org.dromara.soul.common.dto.convert.RateLimiterHandle;
import org.dromara.soul.common.dto.convert.RewriteHandle;
import org.dromara.soul.common.dto.convert.SpringCloudHandle;
import org.dromara.soul.common.dto.convert.WafHandle;
import org.dromara.soul.common.dto.zk.AppAuthZkDTO;
import org.dromara.soul.common.dto.zk.ConditionZkDTO;
import org.dromara.soul.common.dto.zk.PluginZkDTO;
import org.dromara.soul.common.dto.zk.RuleZkDTO;
import org.dromara.soul.common.dto.zk.SelectorZkDTO;
import org.dromara.soul.common.enums.LoadBalanceEnum;
import org.dromara.soul.common.enums.MatchModeEnum;
import org.dromara.soul.common.enums.OperatorEnum;
import org.dromara.soul.common.enums.ParamTypeEnum;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.SelectorTypeEnum;
import org.dromara.soul.common.enums.WafEnum;
import org.dromara.soul.common.utils.JsonUtils;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("all")
public class ZookeeperClientTest extends BaseTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperClientTest.class);

    @Autowired
    private ZkClient zkClient;

    private static final String ROOT_PATH = "/xiaoyu";

    private static final String PLUGIN = "/soul/plugin";

    private static Map<String, PluginZkDTO> pluginZkDTOMap = Maps.newConcurrentMap();

    @Test
    public void test() {

        if (!zkClient.exists(ROOT_PATH)) {
            zkClient.createPersistent(ROOT_PATH, true);
        }
        zkClient.writeData(ROOT_PATH, new PluginZkDTO("1", PluginEnum.DIVIDE.getName(), Boolean.TRUE));
        final Object o = zkClient.readData(ROOT_PATH);
        System.out.println(o.toString());
    }

    @Test
    public void testInsertAppAuth() {
        AppAuthZkDTO appAuthZkDTO = new AppAuthZkDTO();
        appAuthZkDTO.setAppKey("gateway");
        appAuthZkDTO.setAppSecret("123456");
        appAuthZkDTO.setEnabled(true);

        final String path = ZkPathConstants.buildAppAuthPath(appAuthZkDTO.getAppKey());

        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true);
        }
        zkClient.writeData(path, appAuthZkDTO);

        final Object o = zkClient.readData(path);
        System.out.println(o.toString());

    }


    @Test
    public void testWriteDivideSelector() {

        final SelectorZkDTO selectorZkDTO = buildSelectorZkDTO("eee", "eeex", PluginEnum.DIVIDE.getName());

        writeSelector(selectorZkDTO);

    }


    @Test
    public void testWriteDubboSelectorAndRule() {

        writeSelectorAndRule(PluginEnum.DUBBO.getName());
    }


    @Test
    public void testWriteRateLimiterSelector() {

        writeSelectorAndRule(PluginEnum.RATE_LIMITER.getName());
    }


    @Test
    public void testWriteWafSelector() {
        writeSelectorAndRule(PluginEnum.WAF.getName());
    }


    @Test
    public void testWriteRewriteSelector() {
        writeSelectorAndRule(PluginEnum.REWRITE.getName());
    }

    private void writeSelectorAndRule(String pluginName) {
        final SelectorZkDTO selectorZkDTO = buildSelectorZkDTO("aaa", "aaa", pluginName);

        writeSelector(selectorZkDTO);

        final RuleZkDTO ruleZkDTO = buildRuleDTO("xxx", selectorZkDTO.getId(), selectorZkDTO.getPluginName());

        final String rulePath = ZkPathConstants
                .buildRulePath(selectorZkDTO.getPluginName(), ruleZkDTO.getSelectorId(), ruleZkDTO.getId());
        writeRule(rulePath, ruleZkDTO);
    }


    @Test
    public void testInsertRule() {
        final RuleZkDTO ruleZkDTO = buildRuleDTO("aaa", "eee", PluginEnum.DIVIDE.getName());
        final String rulePath = ZkPathConstants
                .buildRulePath(PluginEnum.DIVIDE.getName(), ruleZkDTO.getSelectorId(), ruleZkDTO.getId());
        if (!zkClient.exists(rulePath)) {
            zkClient.createPersistent(rulePath, true);
        }

        zkClient.writeData(rulePath, ruleZkDTO);

        final RuleZkDTO zkDTO = zkClient.readData(rulePath);
        LOGGER.info(zkDTO.toString());

    }

    private void writeRule(String rulePath, RuleZkDTO ruleZkDTO) {
        if (!zkClient.exists(rulePath)) {
            zkClient.createPersistent(rulePath, true);
        }

        zkClient.writeData(rulePath, ruleZkDTO);
    }


    private void writeSelector(SelectorZkDTO selectorZkDTO) {
        final String selectorRealPath =
                ZkPathConstants.buildSelectorRealPath(selectorZkDTO.getPluginName(), selectorZkDTO.getId());
        if (!zkClient.exists(selectorRealPath)) {
            zkClient.createPersistent(selectorRealPath, true);
        }
        zkClient.writeData(selectorRealPath, selectorZkDTO);
    }


    private SelectorZkDTO buildSelectorZkDTO(String id, String name, String pluginName) {
        SelectorZkDTO selectorZkDTO = new SelectorZkDTO();
        selectorZkDTO.setId(id);
        selectorZkDTO.setName(name);
        selectorZkDTO.setSort(1);
        selectorZkDTO.setContinued(Boolean.TRUE);
        selectorZkDTO.setLoged(Boolean.TRUE);
        selectorZkDTO.setEnabled(Boolean.TRUE);
        selectorZkDTO.setPluginName(pluginName);
        selectorZkDTO.setType(SelectorTypeEnum.FULL_FLOW.getCode());
        selectorZkDTO.setMatchMode(MatchModeEnum.AND.getCode());
        final ConditionZkDTO conditionZkDTO = buildConditionZkDTO();
        selectorZkDTO.setConditionZkDTOList(Collections.singletonList(conditionZkDTO));
        return selectorZkDTO;
    }

    private RuleZkDTO buildRuleDTO(String id, String selectorId, String pluginName) {
        RuleZkDTO dto1 = new RuleZkDTO();
        dto1.setId(id);
        dto1.setSelectorId(selectorId);
        dto1.setName("宇测试");
        dto1.setConditionZkDTOList(Collections.singletonList(buildConditionZkDTO()));
        dto1.setEnabled(true);
        dto1.setLoged(Boolean.TRUE);
        dto1.setMatchMode(MatchModeEnum.AND.getCode());
        if (PluginEnum.DIVIDE.getName().equals(pluginName)) {
            final String jsonStr = JsonUtils.toJson(buildDivideHandle());
            dto1.setHandle(jsonStr);
        } else if (PluginEnum.RATE_LIMITER.getName().equals(pluginName)) {
            final String jsonStr = JsonUtils.toJson(buildRateLimiterHandle());
            dto1.setHandle(jsonStr);
        } else if (PluginEnum.WAF.getName().equals(pluginName)) {
            dto1.setHandle(JsonUtils.toJson(buildWafHandle()));
        } else if (PluginEnum.REWRITE.getName().equals(pluginName)) {
            dto1.setHandle(JsonUtils.toJson(buildRewriteHandle()));
        } else if (PluginEnum.DUBBO.getName().equals(pluginName)) {
            dto1.setHandle(JsonUtils.toJson(buildDubboHandle()));
        }
        dto1.setSort(120);
        return dto1;
    }

    public static void main(String[] args) {
        System.out.println(JsonUtils.toJson(buildSpringCloudHandle()));
    }

    private static DivideHandle buildDivideHandle() {
        DivideHandle divideHandle = new DivideHandle();
        divideHandle.setLoadBalance(LoadBalanceEnum.ROUND_ROBIN.getName());
        divideHandle.setCommandKey("PDM");
        divideHandle.setGroupKey("pdm");

        DivideUpstream upstream = new DivideUpstream();
        upstream.setTimeout(1000);
        divideHandle.setUpstreamList(buildUpstreamList());
        return divideHandle;
    }

    private static DubboHandle buildDubboHandle() {
        DubboHandle dubboHandle = new DubboHandle();
        dubboHandle.setAppName("local");
        dubboHandle.setRegistry("zookeeper://localhost:2181");
        dubboHandle.setTimeout(3000);
        dubboHandle.setGroupKey("xiaoyu");
        dubboHandle.setCommandKey("xiaoyu");
        return dubboHandle;
    }

    private static RateLimiterHandle buildRateLimiterHandle() {
        RateLimiterHandle rateLimiterHandle = new RateLimiterHandle();
        rateLimiterHandle.setBurstCapacity(1);
        rateLimiterHandle.setReplenishRate(1);
        return rateLimiterHandle;
    }


    private static WafHandle buildWafHandle() {
        WafHandle wafHandle = new WafHandle();
        wafHandle.setPermission(WafEnum.ALLOW.getName());
        wafHandle.setStatusCode("403");
        return wafHandle;
    }

    private static RewriteHandle buildRewriteHandle() {
        RewriteHandle rewriteHandle = new RewriteHandle();
        rewriteHandle.setRewriteURI("rewrite");
        return rewriteHandle;
    }


    private static SpringCloudHandle buildSpringCloudHandle(){
        SpringCloudHandle springCloudHandle = new SpringCloudHandle();
        springCloudHandle.setPath("/xxx");
        springCloudHandle.setServiceId("xiaoyu");
        return springCloudHandle;
    }

    private static List<DivideUpstream> buildUpstreamList() {
        List<DivideUpstream> upstreams = Lists.newArrayList();
        DivideUpstream upstream = new DivideUpstream();
        upstream.setTimeout(1000);
        upstream.setUpstreamHost("localhost");
        upstream.setUpstreamUrl("http://localhost:8081");
        upstream.setWeight(90);
        upstreams.add(upstream);
        return upstreams;
    }

    @Test
    public void testDelete() {
        final String dividePath = ZkPathConstants.buildSelectorParentPath(PluginEnum.DIVIDE.getName());
        zkClient.delete(dividePath);
    }


    private ConditionZkDTO buildConditionZkDTO() {
        ConditionZkDTO condition = new ConditionZkDTO();
        condition.setOperator(OperatorEnum.EQ.getAlias());
        condition.setParamName("module");
        condition.setParamValue("pdm");
        condition.setParamType(ParamTypeEnum.POST.getName());
        return condition;
    }


    @Test
    public void testWritePlugin() {

        Arrays.stream(PluginEnum.values()).forEach(pluginEnum -> {
            String pluginPath = ZkPathConstants.buildPluginPath(pluginEnum.getName());
            if (!zkClient.exists(pluginPath)) {
                zkClient.createPersistent(pluginPath, true);
            }
            zkClient.writeData(pluginPath, buildByName(pluginEnum.getName()));
            PluginZkDTO data = zkClient.readData(pluginPath);
            LOGGER.debug(data.toString());
        });


    }

    @Test
    public void testLoadPluginData() throws InterruptedException {
        Arrays.stream(PluginEnum.values()).forEach(pluginEnum -> {
            String pluginPath = PLUGIN + "/" + pluginEnum.getName();
            PluginZkDTO data = zkClient.readData(pluginPath);
            pluginZkDTOMap.put(pluginEnum.getName(), data);

            zkClient.subscribeDataChanges(pluginPath, new IZkDataListener() {
                @Override
                public void handleDataChange(String dataPath, Object data) {
                    LOGGER.info("node data changed!");
                    LOGGER.info("path=>" + dataPath);
                    LOGGER.info("data=>" + data);
                    final String key = dataPath
                            .substring(dataPath.lastIndexOf("/") + 1, dataPath.length());
                    pluginZkDTOMap.put(key, (PluginZkDTO) data);
                }

                @Override
                public void handleDataDeleted(String dataPath) {

                }
            });

        });

        LOGGER.info("ready!");

        //junit测试时，防止线程退出
        while (true) {
            TimeUnit.SECONDS.sleep(5);
        }

    }

    @Test
    public void testUpdatePlugin() {
        String divide = PLUGIN + "/" + PluginEnum.DIVIDE.getName();
        PluginZkDTO divideDTO = new PluginZkDTO("3", PluginEnum.DIVIDE.getName(), false);
        zkClient.writeData(divide, divideDTO);

        String global = PLUGIN + "/" + PluginEnum.GLOBAL.getName();
        PluginZkDTO globalDTO = new PluginZkDTO("4", PluginEnum.GLOBAL.getName(), false);
        zkClient.writeData(global, globalDTO);


    }


    private PluginZkDTO buildByName(String name) {
        PluginZkDTO pluginZkDTO = new PluginZkDTO();
        pluginZkDTO.setEnabled(true);
        pluginZkDTO.setId("1");
        pluginZkDTO.setName(name);
        return pluginZkDTO;
    }


    @Test
    public void testPlugin() {
        if (!zkClient.exists(PLUGIN)) {
            zkClient.createPersistent(PLUGIN, true);
        }
        zkClient.writeData(PLUGIN, buildMap());
        Map<String, PluginZkDTO> resultMap = zkClient.readData(PLUGIN);
        resultMap.forEach((k, v) -> LOGGER.debug(k + ":" + v.toString()));

    }

    @After
    public void dispose() {
        zkClient.close();
        LOGGER.info("zkclient closed!");
    }

    @Test
    public void testPluginUpdate() {
        final Map<String, PluginZkDTO> map = buildMap();
        map.put(PluginEnum.DIVIDE.getName(), new PluginZkDTO("2", PluginEnum.DIVIDE.getName(), Boolean.FALSE));
        zkClient.writeData(PLUGIN, map);
    }

    @Test
    public void testListener() throws InterruptedException {
        //监听指定节点的数据变化
        zkClient.subscribeDataChanges(PLUGIN, new IZkDataListener() {
            @Override
            public void handleDataChange(String path, Object o) {
                LOGGER.info("node data changed!");
                LOGGER.info("path=>" + path);
                LOGGER.info("data=>" + o);
                Map<String, PluginZkDTO> map;
                map = (Map<String, PluginZkDTO>) o;
                LOGGER.info(map.toString());
                LOGGER.info("--------------");
            }

            @Override
            public void handleDataDeleted(String path) {
                LOGGER.info("node data deleted!");
                LOGGER.info("path=>" + path);
                LOGGER.info("--------------");

            }
        });

        LOGGER.info("ready!");

        //junit测试时，防止线程退出
        while (true) {
            TimeUnit.SECONDS.sleep(5);
        }
    }


    @Test
    public void testUpdateConfig() {
        if (!zkClient.exists(ROOT_PATH)) {
            zkClient.createPersistent(ROOT_PATH);
        }
        zkClient.writeData(ROOT_PATH, "1");
        zkClient.writeData(ROOT_PATH, "2");
        zkClient.delete(ROOT_PATH);
        zkClient.delete(ROOT_PATH);//删除一个不存在的node，并不会报错
    }

    private Map<String, PluginZkDTO> buildMap() {
        Map<String, PluginZkDTO> pluginMap = Maps.newHashMap();
        pluginMap.put(PluginEnum.DIVIDE.getName(), new PluginZkDTO("6", PluginEnum.DIVIDE.getName(), Boolean.TRUE));
        pluginMap.put(PluginEnum.GLOBAL.getName(), new PluginZkDTO("7", PluginEnum.GLOBAL.getName(), Boolean.TRUE));
        pluginMap.put(PluginEnum.MONITOR.getName(), new PluginZkDTO("8", PluginEnum.MONITOR.getName(), Boolean.TRUE));
        return pluginMap;
    }
}
