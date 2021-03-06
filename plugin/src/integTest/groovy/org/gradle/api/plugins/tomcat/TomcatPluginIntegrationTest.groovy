package org.gradle.api.plugins.tomcat

import org.gradle.api.plugins.tomcat.embedded.TomcatVersion
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.Task
import spock.lang.Ignore
import spock.lang.Unroll

class TomcatPluginIntegrationTest extends AbstractIntegrationTest {
    def "Adds default Tomcat tasks for Java project"() {
        when:
            GradleProject project = runTasks(integTestDir, 'tasks')
        then:
            Task tomcatRunTask = findTask(project, TomcatPlugin.TOMCAT_RUN_TASK_NAME)
            tomcatRunTask
            tomcatRunTask.description == 'Uses your files as and where they are and deploys them to Tomcat.'
            Task tomcatRunWarTask = findTask(project, TomcatPlugin.TOMCAT_RUN_WAR_TASK_NAME)
            tomcatRunWarTask
            tomcatRunWarTask.description == 'Assembles the webapp into a war and deploys it to Tomcat.'
            Task tomcatStopTask = findTask(project, TomcatPlugin.TOMCAT_STOP_TASK_NAME)
            tomcatStopTask
            tomcatStopTask.description == 'Stops Tomcat.'
            Task tomcatJasperTask = findTask(project, TomcatPlugin.TOMCAT_JASPER_TASK_NAME)
            tomcatJasperTask
            tomcatJasperTask.description == 'Runs the JSP compiler and turns JSP pages into Java source.'
    }

    @Unroll
    def "Start and stop #tomcatVersion with #taskName supporting default web app directory"() {
        setup:
            setupWebAppDirectory()

        expect:
            buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
            buildFile << getTomcatContainerLifecycleManagementBuildFileContent(taskName)
            runTasks(integTestDir, 'startAndStopTomcat')

        where:
            tomcatVersion             | taskName
            TomcatVersion.VERSION_6X  | 'tomcatRun'
            TomcatVersion.VERSION_6X  | 'tomcatRunWar'
            TomcatVersion.VERSION_7X  | 'tomcatRun'
            TomcatVersion.VERSION_7X  | 'tomcatRunWar'
    }

    @Unroll
    def "Start and stop #tomcatVersion with #taskName without supporting web app directory"() {
        expect:
            buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
            buildFile << getTomcatContainerLifecycleManagementBuildFileContent(taskName)
            runTasks(integTestDir, 'startAndStopTomcat')

        where:
            tomcatVersion             | taskName
            TomcatVersion.VERSION_6X  | 'tomcatRun'
            TomcatVersion.VERSION_6X  | 'tomcatRunWar'
            TomcatVersion.VERSION_7X  | 'tomcatRun'
            TomcatVersion.VERSION_7X  | 'tomcatRunWar'
    }

    @Unroll
    def "Running #taskName task redirecting logs to file cleans up resources after stopping Tomcat in daemon mode"() {
        expect:
        buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
        buildFile << getTomcatContainerLifecycleManagementBuildFileContent(taskName)
        buildFile << """
[tomcatRun, tomcatRunWar]*.outputFile = file('logs/tomcat.log')
"""
        runTasks(integTestDir, 'startAndStopTomcat')
        new File(integTestDir, 'logs/tomcat.log').exists()
        !new File(integTestDir, 'logs/tomcat.log.lck').exists()

        where:
        tomcatVersion             | taskName
        TomcatVersion.VERSION_6X  | 'tomcatRun'
        TomcatVersion.VERSION_6X  | 'tomcatRunWar'
        TomcatVersion.VERSION_7X  | 'tomcatRun'
        TomcatVersion.VERSION_7X  | 'tomcatRunWar'
    }

    @Ignore
    def "Start and stop Tomcat 8x with tomcatRun task supporting default web app directory"() {
        setup:
            setupWebAppDirectory()

        expect:
            buildFile << getBasicTomcatBuildFileContent(TomcatVersion.VERSION_8X)
            buildFile << getTomcatContainerLifecycleManagementBuildFileContent('tomcatRun')
            runTasks(integTestDir, 'startAndStopTomcat')
    }

    private String getBasicTomcatBuildFileContent(TomcatVersion tomcatVersion) {
        switch(tomcatVersion) {
            case TomcatVersion.VERSION_6X: return getBasicTomcat6xBuildFileContent()
            case TomcatVersion.VERSION_7X: return getBasicTomcat7xBuildFileContent()
            case TomcatVersion.VERSION_8X: return getBasicTomcat8xBuildFileContent()
            default: throw new IllegalArgumentException("Unknown Tomcat version $tomcatVersion")
        }
    }

    private String getBasicTomcat6xBuildFileContent() {
        """
dependencies {
    def tomcatVersion = '6.0.29'
    tomcat "org.apache.tomcat:catalina:\${tomcatVersion}",
           "org.apache.tomcat:coyote:\${tomcatVersion}",
           "org.apache.tomcat:jasper:\${tomcatVersion}"
}
"""
    }

    private String getBasicTomcat7xBuildFileContent() {
        """
dependencies {
    def tomcatVersion = '7.0.11'
    tomcat "org.apache.tomcat.embed:tomcat-embed-core:\${tomcatVersion}",
           "org.apache.tomcat.embed:tomcat-embed-logging-juli:\${tomcatVersion}"
    tomcat("org.apache.tomcat.embed:tomcat-embed-jasper:\${tomcatVersion}") {
        exclude group: 'org.eclipse.jdt.core.compiler', module: 'ecj'
    }
}
"""
    }

    private String getBasicTomcat8xBuildFileContent() {
        """
dependencies {
    def tomcatVersion = '8.0.0-RC5'
    tomcat "org.apache.tomcat.embed:tomcat-embed-core:\${tomcatVersion}",
           "org.apache.tomcat.embed:tomcat-embed-logging-juli:\${tomcatVersion}"
    tomcat("org.apache.tomcat.embed:tomcat-embed-jasper:\${tomcatVersion}") {
        exclude group: 'org.eclipse.jdt.core.compiler', module: 'ecj'
    }
}
"""
    }

    private String getTomcatContainerLifecycleManagementBuildFileContent(String tomcatStartTask) {
        """
task startAndStopTomcat {
    dependsOn $tomcatStartTask
    finalizedBy tomcatStop
}
"""
    }
}
