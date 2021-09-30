# Running the ICPC Tools inside IntelliJ IDEA

You can use [IntelliJ IDEA](https://www.jetbrains.com/idea/) to develop and debug the ICPC Tools.
Note that to work on the CDS, the Ultimate edition is required, since it uses Java EE.

## Configuring general settings

To get started, first open the whole repository in IDEA.

Next, open File -> Project structure. Select a valid SDK under `Project SDK`. If none are available,
click `Add SDK` and add one. Also select a directory under `Project compiler output` to put compilation results.

## Configuring modules

Next, go to Modules on the same window. Remove the `icpctools` module (or how it is called, depends on
your directory name).
Now perform the following steps for the top level icpctools directory, and it should find all the modules:

Alternatively, if this doesn't work, perform the steps for each of the directories listed below.

* Click `+`
* Click `Import Module`
* Select `Create module from existing sources`. Click `Next`.
* Leave the checkbox selected on the source files screen. Click `Next`.
* On the Libraries page, deselect all libraries. Click `Next`.
* On the Modules page, click `Next`.
* On the second Libraries page, deselect all libraries. Click `Next`.
* On the second Modules page, click `Next`.
* On the Frameworks page, click `Finish`. Note that for the CDS the page shows a different output.
  This is fine.

Do this for the following directories:

* `BalloonUtility`
* `CDS`
* `CoachView`
* `ContestModel`
* `ContestUtil`
* `PresAdmin`
* `PresContest`
* `PresCore`
* `ProblemSet`
* `Resolver`

Now we need to add dependencies. Some modules need other modules and some modules need external JARs.
For each of the modules listed below, add the specified modules, directories and JARs. First, select the module,
then click the `Dependencies` tab. Click the `+` button. Select `Module Dependency...` to add a module dependency
and select `JARs or Directories...` to add a directory or JAR.
Add the dependencies listed below. If you need to add the SWT JAR for your platform, select the correct JAR in
`SWTLauncher/lib` based on your operating system.

Module dependencies might be added automatically when importing all the modules at once.

* For `BalloonUtility` add:
  * Module `ContestModel`
  * Module `PresCore`
  * The SWT JAR for your platform
  * The file 'svgSalamander-1.1.2.4.jar' from the 'ContestModel/lib' directory.
* For `CoachView` add:
  * Module `ContestModel`
  * Module `PresCore`
  * The `lib` directory in the `CoachView` directory
* For `CDS` add:
  * Module `ContestModel`
  * The files `javax.servlet-api-4.0.1.jar`, `javax.servlet.jsp-api-2.3.3.jar` and `javax.websocket-api-1.1.jar`
    from the `CDS` directory.
* For `ContestModel` add:
  * The `lib` directory in the project root
  * The `testlib` directory in the project root
  * The `lib` directory in the `ContestModel` directory
* For `ContestUtil` add:
  * Module `ContestModel`
* For `PresCore` add:
  * Module `ContestModel`
  * The `lib` directory in the `PresCore` directory
* For `PresAdmin` add:
  * Module `ContestModel`
  * Module `PresCore`
  * The SWT JAR for your platform
* For `PresContest` add:
  * Module `ContestModel`
  * Module `PresCore`
  * The `lib` directory in the `PresContest` directory
  * The file 'svgSalamander-1.1.2.4.jar' from the 'ContestModel/lib' directory.
* For `ProblemSet` add:
  * The `lib` directory in the `ProblemSet` directory
  * The SWT JAR for your platform
* For `Resolver` add:b
  * Module `ContestModel`
  * Module `PresCore`
  * Module `PresContest`

## Creating the artifact for the CDS

Now select the `Facets` item on the left. At the bottom it will tell you that the Web Facet resources
are not included in an artifact. Click the `Create Artifact` button. Rename the artifact to `CDS` and
select an output directory for the artifact. We need this directory later. Keep clicking the `Fix`
button at the bottom until it goes away. Now expand `WEB-INF`, then `classes`. Click on `classes` and
click on the `+` button. Select `Module Output` and then select `CDS`. Click `OK` twice.

## Installing and setting up Openliberty

If you want to run the CDS from inside IntelliJ IDEA,  download Openliberty from
[this page](https://openliberty.io/downloads/). Make sure to pick the `Web Profile 8` package.
Unpack the ZIP file and place it somewhere. In IntelliJ IDEA go to the prefenences and under
`Build, Execution, Deployment` select `Application Servers`. Click the `+` button and select
`WebSphere Server` as the type. As `WebSphere Home` select the `wlp` directory inside the unpacked
Openliberty archive. It should show the correct version. Click `Ok` to close the window.
Copy the `CDS/config/wlp/cds` directory to `wlp/usr/servers` inside the unpacked Openliberty archive.
Open the file `server.xml` and change the following on the line that contains `<webApplication`:

* Change `webApplication` to `application`
* Change the value inside the `location` tag to the artifact directory of the CDS you picked earlier
* Change `contextRoot` to `context-root`
* Add `type="war"`

Also change `</webApplication>` to `</application>`.

If the `server.xml` or any files in this directory change in the repository, make sure to update them accordingly.

## Adding the CDS as a build and run configuration

Now that we have set up all modules, we can finally add the build configurations. Setting up the CDS is the
most cumbersome, but it is also the tool most other tools need, so it should be the first thing you set up.

Click the `Run` menu item and select `Edit configurations...`. Click `+` and select `WebSphere Server`
-> `Local`.  Name it `CDS` and uncheck `After launch` if you do not want your browser to launch after the
CDS has started up. Use `https://localhost:8443/` as URL. Select `cds` under `WebSphere Server Settings` -> 
`Server`. On the `Deployment` tab click the `+` button and select `Artifact...`. The CDS artifact will
automatically be added. Make sure to check the `Use contest context root` checkbox and leave the context as `/`.
Click the `Fix` button at the bottom if it complains about JMX administration.

Now you can finally run and debug the CDS using the IntelliJ IDEA features. The CDS configuration can be found
in the `wlp/usr/servers/cds` directory inside the Openliberty directory, so you can edit the `cdsConfig.xml` and
other files there. You can use `File` -> `New` -> `Module from existing sources` and point to this directory to
open it inside IntelliJ IDEA.

## Adding other tools as a build an run configuration

The other tools are way easier to run. Configuring each of them is similar with only small differences. First,
the general process will be explained and below that the specific settings for each tool will be shown.

In the `Run` menu, select `Edit configurations...`. Click the `+` button and select `Application`.
Give the application a logical name. Under `Build and run` click the first empty dropdown and select the module
you want to run. Click the Browse button inside the `Main class` input and select the correct class. If the tool
requires program arguments, enter them in the `Program arguments` input field.

When using macOS, click `Modify options` and select `Add VM options`. In the `VM options` field, type
`-XstartOnFirstThread`.

Click `OK` to add the tool.

### Per tool settings

* Balloon utility:
  * Module: `BalloonUtility`
  * Main class: `BalloonUtility`
* Coach view:
  * Module: `CoachView`
  * Main class: `CoachView`
  * Program arguments: `https://localhost:8443/api/contests/<id> public publ1c` where `<id>` is a configured contest ID
* Floor generators:
  * Module: `ContestUtil`
  * Main class: `FloorGenerator<X>` with `<X>` the floor generator you want to use
* Presentation admin:
  * Module: `PresAdmin`
  * Main class: `Admin`
  * Program arguments: `https://localhost:8443/ admin adm1n`
  * Note that the presentation admin requires a presentation client ZIP file inside a `present` directory in the same
    directory as the `cdsConfig.xml` file
* Presentation client:
  * Module: `PresContest`
  * Main class: `ClientLauncher`
  * Program arguments: `https://localhost:8443/api/contests/<id> presentation presentat1on --display 1d` where `<id>` is a
    configured contest ID. The `-display 1d` is to make the presentation client not be full screen
* Problem set editor:
  * Module: `ProblemSet`
  * Main class: `ProblemSetEditor`
* Resolver:
  * Module: `Resolver`
  * Main class: `Resolver`
  * Program arguments: `https://localhost:8443/api/contests/<id> presAdmin padm1n --display 1d`  where `<id>` is a
    configured contest ID. The `-display 1d` is to make the resolver not be full screen. Other useful options are
    `--fast 0.15`, `--info` and `--presenter`
* Awards utility:
  * Module: `Resolver`
  * Main class: `Awards`
