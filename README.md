Asset Factory Plugins
===============

This project contains various Asset Factory Plugins that can be used within Cascade CMS. The project is organized with tags that correspond to specific Cascade CMS versions; if a plugin is present under a specific tag, it is compatible with that version of Cascade CMS. For example, the tag **7.4.x** means that each plugin listed will work with Cascade CMS 7.4.1.

This project also utilizes Github Releases that correspond to specific Cascade CMS versions. Each release will contain a compiled JARs for each available plugin, a JAR containing all available plugins and a Zip file containing each individially compiled plugin.

Installing Plugins into Cascade CMS
---------------

To install compiled Asset Factory plugins:

1. Shut down Cascade CMS
2. Place the compiled JAR file(s) in `<Tomcat_Installation_Directory>/webapps/ROOT/WEB-INF/lib` within the Cascade CMS installation directory.
3. Restart Cascade CMS
4. Navigate to **Administration->Asset Factory Plugins**
5. In the **Add a Plugin** field, enter the fullet qualified Java class name of each plugin
    - E.g. `com.hannonhill.cascade.plugin.assetfactory.AssetFieldsPlugin`
6. Click the **Add Plugin** button

To remove a custom plugin:

1. Navigate to **Administration->Asset Factory Plugins**
2. Click on the red **X** to remove the custom plugin


Compiling Plugins
---------------

**Prerequisites:** Because this is an Maven-Eclipse project, both Eclipse and Maven must be installed.

1. Create your new plugin class within the `com.hannonhill.cascade.plugin.assetfactory` package
2. Add any required dependencies to the Maven pom.xml file. If you need to install local .jar files, see the section below.
3. Building:
    - Within Eclipse
        1. Right-click the Project and go to **Run As->Maven->Maven build...**
        2. Type `clean package` in the **Goals** field and click **Run**
        - Note: After building at least once, you can then use **Run As->Maven->Maven build** and choose the previous build setting from the configuration menu.
    - From command line
        1. Navigate to the project
        2. Type `mvn clean package`
4. Locate the compiled JAR file within the **target** directory

Installing Local JARs
---

Recent versions of Maven no longer allow linking to internal dependencies via scope and systemPath. 

Using the information provided in [this article](https://softwarecave.org/2014/06/14/adding-external-jars-into-maven-project/), we can avoid this issue by using the maven-installer-plugin to install the .jar into a local repository within the `lib` directory.

Repeat the following for each .jar:

1. Change into the `lib` directory
2. Install the .jar into the local repository using the maven-installer-plugin:
  
  ```
  mvn install:install-file -Dfile=PATH_TO_FILE -DgroupId=GROUP_ID -DartifactId=ARTIFACT_ID -Dversion=VERSION -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=.
  ```
3. Add the new dependency to the `pom.xml` file using the information from the previous step:

  ```
  <dependency>
    <groupId>GROUP_ID</groupId>
	<artifactId>ARTIFACT_ID</artifactId>
	<version>VERSION</version>
  </dependency>
  ```
