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

/**
 * @author Andres Almiray
 */
public class EbeanContributionAdapter implements EbeanContributionHandler {
    private static final String DEFAULT = "default";

    private EbeanProvider provider = DefaultEbeanProvider.getInstance();

    public void setEbeanProvider(EbeanProvider provider) {
        this.provider = provider != null ? provider : DefaultEbeanProvider.getInstance();
    }

    public EbeanProvider getEbeanProvider() {
        return provider;
    }

    public <R> R withEbean(Closure<R> closure) {
        return withEbean(DEFAULT, closure);
    }

    public <R> R withEbean(String ebeanServerName, Closure<R> closure) {
        return provider.withEbean(ebeanServerName, closure);
    }

    public <R> R withEbean(CallableWithArgs<R> callable) {
        return withEbean(DEFAULT, callable);
    }

    public <R> R withEbean(String ebeanServerName, CallableWithArgs<R> callable) {
        return provider.withEbean(ebeanServerName, callable);
    }
}