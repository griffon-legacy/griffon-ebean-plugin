/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.ebean;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import com.avaje.ebean.EbeanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractEbeanProvider implements EbeanProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractEbeanProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withEbean(Closure<R> closure) {
        return withEbean(DEFAULT, closure);
    }

    public <R> R withEbean(String ebeanServerName, Closure<R> closure) {
        if (isBlank(ebeanServerName)) ebeanServerName = DEFAULT;
        if (closure != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on ebeanServer '" + ebeanServerName + "'");
            }
            return closure.call(ebeanServerName, getEbeanServer(ebeanServerName));
        }
        return null;
    }

    public <R> R withEbean(CallableWithArgs<R> callable) {
        return withEbean(DEFAULT, callable);
    }

    public <R> R withEbean(String ebeanServerName, CallableWithArgs<R> callable) {
        if (isBlank(ebeanServerName)) ebeanServerName = DEFAULT;
        if (callable != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on ebeanServer '" + ebeanServerName + "'");
            }
            callable.setArgs(new Object[]{ebeanServerName, getEbeanServer(ebeanServerName)});
            return callable.call();
        }
        return null;
    }

    protected abstract EbeanServer getEbeanServer(String ebeanServerName);
}