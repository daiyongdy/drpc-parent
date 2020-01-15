/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.drpc.register;

import java.util.Collection;
import java.util.Map;

/**
 * 注册接口
 */
public interface Register {


    /**
     * 连接注册中心
     * @param url
     */
    void connectRegisterCenter(String url);

    /**
     * 注册服务
     * @param meta
     */
    void register(RegisterMetaData meta);

    /**
     * 取消注册服务
     * @param meta
     */
    void unregister(RegisterMetaData meta);

    /**
     * 订阅服务
     */
    void subscribe(RegisterMetaData.ServiceMeta serviceMeta, NotifyListener listener);

    /**
     *  查询服务
     */
    Collection<RegisterMetaData> lookup(RegisterMetaData.ServiceMeta serviceMeta);

    /**
     * List all consumer's info.
     */
    Map<RegisterMetaData.ServiceMeta, Integer> consumers();

    /**
     *  所有提供者
     */
    Map<RegisterMetaData, RegisterState> providers();

    /**
     * 关闭
     */
    boolean isShutdown();

    /**
     * 优雅关闭.
     */
    void shutdownGracefully();

    enum RegistrerType {
        ZOOKEEPER("zookeeper");

        private final String value;

        RegistrerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RegistrerType parse(String name) {
            for (RegistrerType s : values()) {
                if (s.name().equalsIgnoreCase(name)) {
                    return s;
                }
            }
            return null;
        }
    }

    enum RegisterState {
        PREPARE,
        DONE
    }
}
