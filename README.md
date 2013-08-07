Asset Factory Plugins
===============

This project contains various Asset Factory Plugins that can be used within Cascade Server. The project is organized with tags that correspond to specific Cascade Server versions; if a plugin is present under a specific tag, it is compatible with that version of Cascade Server. For example, the tag **7.4.x** means that each plugin listed will work with Cascade Server 7.4.1.

This project also utilizes Github Releases that correspond to specific Cascade Server versions. Each release will contain a compiled JARs for each available plugin, a JAR containing all available plugins and a Zip file containing each individially compiled plugin.

Installing Plugins
---------------

To install compiled Asset Factory plugins:

1. Shut down Cascade Server
2. Place the compiled JAR file(s) in `<Tomcat_Installation_Directory>/webapps/ROOT/WEB-INF/lib` within the Cascade Server installation directory.
3. Restart Cascade Server
4. Navigate to **Global->Administration->Asset Factories->Manage Plugins**
5. In the **Add a Plugin** field, enter the fullet qualified Java class name of each plugin
    - E.g. `com.hannonhill.cascade.plugin.AssetFieldsPlugin`
6. Click the Submit button

Compiling Plugins
---------------

**Prerequisites:** Because this is an Eclipse project that uses Maven, both Eclipse and Maven must be installed.

1. Create your new plugin class within the `com.hannonhill.cascade.plugin` package
2. Add any required dependencies to the Maven pom.xml file
3. Right-click the Project and go to **Run As->Maven->Build**
4. Locate the compiled JAR file within the **target** directory