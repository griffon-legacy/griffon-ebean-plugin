/*
 * Copyright 2011-2013 the original author or authors.
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

package griffon.plugins.ebean

import javax.sql.DataSource

import com.avaje.ebean.EbeanServer
import com.avaje.ebean.EbeanServerFactory
import com.avaje.ebean.config.ServerConfig

import griffon.core.GriffonApplication
import griffon.util.ConfigUtils
import griffon.plugins.datasource.DataSourceHolder
import griffon.plugins.datasource.DataSourceConnector

/**
 * @author Andres Almiray
 */
@Singleton
final class EbeanConnector {
    private static final String DEFAULT = 'default'
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        ConfigUtils.loadConfigWithI18n('EbeanConfig')
    }

    private ConfigObject narrowConfig(ConfigObject config, String ebeanServerName) {
        return ebeanServerName == DEFAULT ? config.ebeanServer : config.ebeanServers[ebeanServerName]
    }

    EbeanServer connect(GriffonApplication app, String ebeanServerName = DEFAULT) {
        if(EbeanServerHolder.instance.isEbeanServerAvailable(ebeanServerName)) {
            return EbeanServerHolder.instance.getEbeanServer(ebeanServerName)
        }

        ConfigObject dsConfig = DataSourceConnector.instance.createConfig(app)
        if(ebeanServerName == DEFAULT) {
            dsConfig.dataSource.schema.skip = true
        } else {
            dsConfig.dataSources."$ebeanServerName".schema.skip = true
        }
        DataSource dataSource = DataSourceConnector.instance.connect(app, dsConfig, ebeanServerName)

        ConfigObject config = narrowConfig(createConfig(app), ebeanServerName)
        app.event('EbeanConnectStart', [config, ebeanServerName])
        EbeanServer ebeanServer = createEbeanServer(config, dsConfig, ebeanServerName)
        EbeanServerHolder.instance.setEbeanServer(ebeanServerName, ebeanServer)
        bootstrap = app.class.classLoader.loadClass('BootstrapEbean').newInstance()
        bootstrap.metaClass.app = app
        resolveEbeanProvider(app).withEbean(ebeanServerName) { ebsName, ebs -> bootstrap.init(ebsName, ebs) }
        app.event('EbeanConnectEnd', [ebeanServerName, ebeanServer])
        ebeanServer
    }

    void disconnect(GriffonApplication app, String ebeanServerName = DEFAULT) {
        if(!EbeanServerHolder.instance.isEbeanServerAvailable(ebeanServerName)) return

        EbeanServer ebeanServer = EbeanServerHolder.instance.getEbeanServer(ebeanServerName)
        app.event('EbeanDisconnectStart', [ebeanServerName, ebeanServer])
        resolveEbeanProvider(app).withEbean(ebeanServerName) { ebsName, ebs -> bootstrap.destroy(ebsName, ebs) }
        EbeanServerHolder.instance.disconnectEbeanServer(ebeanServerName)
        app.event('EbeanDisconnectEnd', [ebeanServerName])
        ConfigObject config = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.disconnect(app, config, ebeanServerName)
    }

    EbeanProvider resolveEbeanProvider(GriffonApplication app) {
        def ebeanProvider = app.config.ebeanProvider
        if (ebeanProvider instanceof Class) {
            ebeanProvider = ebeanProvider.newInstance()
            app.config.ebeanProvider = ebeanProvider
        } else if (!ebeanProvider) {
            ebeanProvider = DefaultEbeanProvider.instance
            app.config.ebeanProvider = ebeanProvider
        }
        ebeanProvider
    }

    private EbeanServer createEbeanServer(ConfigObject config, ConfigObject dsConfig, String ebeanServerName) {
        DataSource dataSource = DataSourceHolder.instance.getDataSource(ebeanServerName)
        boolean dbCreate = false
        if(ebeanServerName == DEFAULT) {
            dbCreate = dsConfig.dataSource.dbCreate.toString() == 'create'
        } else {
            dbCreate = dsConfig.dataSources."$ebeanServerName".dbCreate.toString() == 'create'
        }

        ServerConfig serverConfig = new ServerConfig(
            name: ebeanServerName,
            register: true,
            defaultServer: ebeanServerName == DEFAULT,
            ddlGenerate: dbCreate,
            ddlRun: dbCreate,
            dataSource: dataSource
        )
        config.each { propName, propValue ->
            serverConfig[propName] = propValue
        }

        EbeanServerFactory.create(serverConfig)
    }
}