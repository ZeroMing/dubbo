/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.router.condition;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.router.AbstractRouter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConditionRouter
 * 条件路由核心类
 */
public class ConditionRouter extends AbstractRouter {

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);
    private static final int DEFAULT_PRIORITY = 2;
    /**
     * 第一个括号匹配特殊的字符:&!=,。
     * 第二个括号匹配排除特殊字符的其他符号。
     */
    private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    private final boolean force;
    //
    private final Map<String, MatchPair> whenCondition;
    private final Map<String, MatchPair> thenCondition;

    public static void main(String[] args) throws ParseException {
        Matcher matcher = ROUTE_PATTERN.matcher("host = 2.2.2.2 & host != 1.1.1.1 & method = hello");
        while (matcher.find()) {
            String group = matcher.group(1);
            String group1 = matcher.group(2);
            System.out.println(String.format("匹配:[%s] \t\t [%s]",group,group1));
        }
        // {host:{matches:1,mismatches:2},method:{matches:1,mismatches:2},...}
        Map<String, MatchPair> stringMatchPairMap = parseRule("host = 2.2.2.2 & host != 1.1.1.1 & method = hello");
        System.out.println(stringMatchPairMap.toString());

    }


    /**
     * 解析条件表达式
     * @param url
     */
    public ConditionRouter(URL url) {
        this.url = url;
        // key: priority  。默认是 2
        this.priority = url.getParameter(Constants.PRIORITY_KEY, DEFAULT_PRIORITY);
        // key: force。默认为非强制。
        this.force = url.getParameter(Constants.FORCE_KEY, false);
        try {
            // key:rule。获取参数并解码.获取路由规则
            String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            // host = 10.20.153.10 => host = 10.20.153.11
            rule = rule.replace("consumer.", "").replace("provider.", "");
            // 定位 => 分隔符
            int i = rule.indexOf("=>");
            // 分别获取服务消费者和提供者匹配规则
            // 当 => 不存在，意味着服务消费者条件路由为空。只存在提供者匹配条件。否则，截取 => 之前的内容为消费者匹配条件。
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            // 当 => 不存在时,整个规则只存在服务提供者匹配条件。否则，=> 之后的内容为提供者匹配条件。
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            Map<String, MatchPair> when =
                    StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            Map<String, MatchPair> then =
                    StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);

            // NOTE: It should be determined on the business level whether the `When condition` can be empty or not.
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 解析规则
     * @param rule
     * @return
     * @throws ParseException
     */
    private static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {

        /**
         * 例子: rule => host = 2.2.2.2 & host != 1.1.1.1 & method = hello
         * 解析结果:
         * {
         *     "host": {
         *         "matches": ["2.2.2.2"],
         *         "mismatches": ["1.1.1.1"]
         *     },
         *     "method": {
         *         "matches": ["hello"],
         *         "mismatches": []
         *     }
         * }
         */
        Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        // Key-Value pair, stores both match and mismatch conditions
        MatchPair pair = null;
        // Multiple values
        Set<String> values = null;
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        // 循环匹配。示例: host = 2.2.2.2 & host != 1.1.1.1 & method = hello
        while (matcher.find()) {
            // Try to match one by one
            String separator = matcher.group(1);
            // 内容
            String content = matcher.group(2);
            // Start part of the condition expression.
            if (separator == null || separator.length() == 0) {
                pair = new MatchPair();
                condition.put(content, pair);
            }
            // The KV part of the condition expression
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    pair = condition.get(content);
                }
            }
            // The Value in the KV part.
            else if ("=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }
                values = pair.matches;
                values.add(content);
            }
            // The Value in the KV part.
            else if ("!=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }
                values = pair.mismatches;
                values.add(content);
            }
            // The Value in the KV part, if Value have more than one items.
            // Should be seperateed by ','
            else if (",".equals(separator)) {
                if (values == null || values.isEmpty())
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    /**
     * 路由使用入口
     * @param invokers
     * @param url        refer url
     * @param invocation
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {

        if (invokers == null || invokers.isEmpty()) {
            return invokers;
        }
        try {

            // 先对服务消费者条件进行匹配，如果匹配失败，表明服务消费者 url 不符合匹配规则，
            // 无需进行后续匹配，直接返回 Invoker 列表即可。比如下面的规则：
            //     host = 10.20.153.10 => host = 10.0.0.10
            // 这条路由规则希望 IP 为 10.20.153.10 的服务消费者调用 IP 为 10.0.0.10 机器上的服务。
            // 当消费者 ip 为 10.20.153.11 时，matchWhen 返回 false，
            // 表明当前这条路由规则不适用于当前的服务消费者，此时无需再进行后续匹配，直接返回即可。
            if (!matchWhen(url, invocation)) {
                return invokers;
            }

            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            // 服务提供者匹配条件未配置，表明对匹配到指定的服务消费者禁用服务，也就是服务消费者在黑名单中
            if (thenCondition == null) {
                // 黑名单
                logger.warn("The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
                return result;
            }

            // 这里可以简单的把 Invoker 理解为服务提供者，现在使用服务提供者匹配规则对
            // 根据Invoker获取服务提供方的URL, 进行匹配
            for (Invoker<T> invoker : invokers) {
                // 如果匹配到指定的URL
                if (matchThen(invoker.getUrl(), url)) {
                    result.add(invoker);
                }
            }
            // 返回匹配结果，如果 result 为空列表，且 force = true，表示强制返回空列表，
            // 否则路由结果为空的路由规则将自动失效
            if (!result.isEmpty()) {
                return result;
            } else if (force) {
                // 强制执行
                logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }
        // 原样返回，此时 force = false，表示该条路由规则失效
        return invokers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int compareTo(Router o) {
        if (o == null || o.getClass() != ConditionRouter.class) {
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }

    /**
     * 匹配服务消费者
     * @param url
     * @param invocation
     * @return
     */
    boolean matchWhen(URL url, Invocation invocation) {
        return whenCondition == null || whenCondition.isEmpty() || matchCondition(whenCondition, url, null, invocation);
    }

    /**
     * 匹配服务提供者
     * @param url 提供者URL
     * @param param 消费者URL
     * @return
     */
    private boolean matchThen(URL url, URL param) {
        return !(thenCondition == null || thenCondition.isEmpty()) && matchCondition(thenCondition, url, param, null);
    }

    /**
     * 匹配条件
     * @param condition
     * @param url 服务提供者或者消费者URL
     * @param param
     * @param invocation
     * @return
     */
    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param, Invocation invocation) {
        Map<String, String> sample = url.toMap();
        boolean result = false;
        for (Map.Entry<String, MatchPair> matchPair : condition.entrySet()) {
            String key = matchPair.getKey();
            String sampleValue;
            //get real invoked method name from invocation
            if (invocation != null && (Constants.METHOD_KEY.equals(key) || Constants.METHODS_KEY.equals(key))) {
                sampleValue = invocation.getMethodName();
            } else {
                // 从服务提供者或消费者 url 中获取指定字段值，比如 host、application 等
                sampleValue = sample.get(key);
                if (sampleValue == null) {
                    // 尝试通过 default.xxx 获取相应的值
                    sampleValue = sample.get(Constants.DEFAULT_KEY_PREFIX + key);
                }
            }

            if (sampleValue != null) {
                // 调用 MatchPair 的 isMatch 方法进行匹配
                if (!matchPair.getValue().isMatch(sampleValue, param)) {
                    return false;
                } else {
                    result = true;
                }
            } else {
                //not pass the condition
                // sampleValue 为空，表明服务提供者或消费者 url 中不包含相关字段。此时如果
                // MatchPair 的 matches 不为空，表示匹配失败，返回 false。比如我们有这样
                // 一条匹配条件 loadbalance = random，假设 url 中并不包含 loadbalance 参数，
                // 此时 sampleValue = null。既然路由规则里限制了 loadbalance 必须为 random，
                // 但 sampleValue = null，明显不符合规则，因此返回 false
                if (!matchPair.getValue().matches.isEmpty()) {
                    return false;
                } else {
                    result = true;
                }



            }
        }
        return result;
    }

    /**
     * 匹配对
     */
    private static final class MatchPair {
        // 匹配集合
        final Set<String> matches = new HashSet<String>();
        // 不匹配集合
        final Set<String> mismatches = new HashSet<String>();

        private boolean isMatch(String value, URL param) {
            // 情况一：matches 非空，mismatches 为空
            if (!matches.isEmpty() && mismatches.isEmpty()) {
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }

            // 情况一：mismatches 非空，matches 为空
            if (!mismatches.isEmpty() && matches.isEmpty()) {
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                return true;
            }

            // 情况一：matches 非空，mismatches 非空
            if (!matches.isEmpty() && !mismatches.isEmpty()) {
                //when both mismatches and matches contain the same value, then using mismatches first
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }
}
