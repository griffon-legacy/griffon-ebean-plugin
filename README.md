
Ebean support
-------------

Plugin page: [http://artifacts.griffon-framework.org/plugin/ebean](http://artifacts.griffon-framework.org/plugin/ebean)


The Ebean plugin enables lightweight access to datasources using [Ebean][1].
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in
`$appdir/griffon-app/conf`:

 * DataSource.groovy - contains the datasource and pool definitions. Its format
   is equal to GORM's requirements.
   Additional configuration for this artifact is explained in the [datasource][2] plugin.
 * EbeanConfig.groovy - contains the eban server definitions.
 * BootstrapEbean.groovy - defines init/destroy hooks for data to be manipulated
   during app startup/shutdown.

A new dynamic method named `withEbean` will be injected into all controllers,
giving you access to a `com.avaje.ebean.EbeanServer` object, with which you'll
be able to make calls to the database. Remember to make all database calls off
the UI thread otherwise your application may appear unresponsive when doing
long computations inside the UI thread.

This method is aware of multiple databases. If no databaseName is specified
when calling it then the default database will be selected. Here are two example
usages, the first queries against the default database while the second queries
a database whose name has been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withEbean { ebeanServerName, ebeanServer -> ... }
            withEbean('internal') { ebeanServerName, ebeanServer -> ... }
        }
    }

The following list enumerates all the variants of the injected method

 * `<R> R withEbean(Closure<R> stmts)`
 * `<R> R withEbean(CallableWithArgs<R> stmts)`
 * `<R> R withEbean(String ebeanServerName, Closure<R> stmts)`
 * `<R> R withEbean(String ebeanServerName, CallableWithArgs<R> stmts)`

These methods are also accessible to any component through the singleton
`griffon.plugins.ebean.EbeanEnhancer`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `EbeanEnhancer.enhance(metaClassInstance)`.

This plugin relies on the facilities exposed by the [datasource][2] plugin.

Configuration
-------------
### EbeanAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.ebean.EbeanAware`. This transformation injects the
`griffon.plugins.ebean.EbeanContributionHandler` interface and default
behavior that fulfills the contract.

### Dynamic Method Injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.ebean.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.ebean.EbeanContributionHandler`.

### Events

The following events will be triggered by this addon

 * EbeanConnectStart[config, databaseName] - triggered before connecting to
   the database
 * EbeanConnectEnd[databaseName, database] - triggered after connecting to
   the database
 * EbeanDisconnectStart[config, databaseName, database] - triggered before
   disconnecting from the database
 * EbeanDisconnectEnd[config, databaseName] - triggered after disconnecting
   from the database

### Configuration Storage

The plugin will load and store the contents of `EbeanConfig.groovy` inside the
application's configuration, under the `pluginConfig` namespace. You may retrieve
and/or update values using

    app.config.pluginConfig.ebean

### Connect at Startup

The plugin will attempt a connection to the default database at startup. If this
behavior is not desired then specify the following configuration flag in
`Config.groovy`

    griffon.ebean.connect.onstartup = false

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/ebean][3]

Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`EbeanEnhancer.enhance(metaClassInstance, ebeanProviderInstance)` where
`ebeanProviderInstance` is of type `griffon.plugins.ebean.EbeanProvider`.
The contract for this interface looks like this

    public interface EbeanProvider {
        <R> R withEbean(Closure<R> closure);
        <R> R withEbean(CallableWithArgs<R> callable);
        <R> R withEbean(String ebeanServerName, Closure<R> closure);
        <R> R withEbean(String ebeanServerName, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyEbeanProvider implements EbeanProvider {
        public <R> R withEbean(Closure<R> closure) { null }
        public <R> R withEbean(CallableWithArgs<R> callable) { null }
        public <R> R withEbean(String ebeanServerName, Closure<R> closure) { null }
        public <R> R withEbean(String ebeanServerName, CallableWithArgs<R> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            EbeanEnhancer.enhance(service.metaClass, new MyEbeanProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@EbeanAware` then usage
of `EbeanEnhancer` should be avoided at all costs. Simply set
`ebeanProviderInstance` on the service instance directly, like so, first the
service definition

    @griffon.plugins.ebean.EbeanAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.ebeanProvider = new MyEbeanProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-ebean-compile-x.y.z.jar`, with locations

 * dsdl/ebean.dsld
 * gdsl/ebean.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][4] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][4] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/ebean-<version>/dist/griffon-ebean-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:        griffon-lombok-compile-<version>.jar:griffon-ebean-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@EbeanAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][5]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@EbeanAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][4] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-ebean-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/ebean-<version>/dist/griffon-ebean-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@EbeanAware`.


[1]: http://www.avaje.org
[2]: /plugin/datasource
[3]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/ebean
[4]: /plugin/lombok
[5]: http://netbeans.org/kb/docs/java/annotations-lombok.html

### Building

This project requires all of its dependencies be available from maven compatible repositories.
Some of these dependencies have not been pushed to the Maven Central Repository, however you
can obtain them from [lombok-dev-deps][lombok-dev-deps].

Follow the instructions found there to install the required dependencies into your local Maven
repository before attempting to build this plugin.

[lombok-dev-deps]: https://github.com/aalmiray/lombok-dev-deps