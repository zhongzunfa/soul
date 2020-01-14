/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * Contributor license agreements.See the NOTICE file distributed with
 * This work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * he License.You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dromara.soul.web.result;

import org.dromara.soul.web.spring.SpringBeanUtils;

/**
 * The type Soul result warp.
 *
 * @author xiaoyu
 */
public final class SoulResultWarp {

    /**
     * Success object.
     *
     * @param code    the code
     * @param message the message
     * @param object  the object
     * @return the object
     */
    public static Object success(final int code, final String message, final Object object) {
        SoulResult soulResult = SpringBeanUtils.getInstance().getBean(SoulResult.class);
        return soulResult.success(code, message, object);
    }

    /**
     * Error object.
     *
     * @param code    the code
     * @param message the message
     * @param object  the object
     * @return the object
     */
    public static Object error(final int code, final String message, final Object object) {
        SoulResult soulResult = SpringBeanUtils.getInstance().getBean(SoulResult.class);
        return soulResult.error(code, message, object);
    }
}
