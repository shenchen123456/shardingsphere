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

package org.apache.shardingsphere.test.e2e.agent.common.container.plugin;

import org.apache.shardingsphere.test.e2e.env.container.atomic.DockerITContainer;

/**
 * Agent plugin HTTP endpoint provider.
 */
public final class AgentPluginHTTPEndpointProvider {
    
    /**
     * Get HTTP URL.
     *
     * @param container docker container
     * @param defaultExposePort default expose port
     * @return HTTP URL
     */
    public String getHURL(final DockerITContainer container, final int defaultExposePort) {
        return String.format("http://%s:%s", container.getHost(), container.getMappedPort(defaultExposePort));
    }
}
