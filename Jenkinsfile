pipeline
 {
  agent 
   {
    label 'windows'
   }

  tools
   {
    maven 'Maven3'
    jdk 'JDK11'
   }

  options
   {
    buildDiscarder(logRotator(numToKeepStr: '4'))
    skipStagesAfterUnstable()
    disableConcurrentBuilds()
   }


  triggers
   {
    // MINUTE HOUR DOM MONTH DOW
    pollSCM('H 6-18/4 * * 1-5')
   }


  stages
   {
    stage('Clean')
     {
      steps
       {
        bat 'mvn --batch-mode clean'
       }
     }

    stage('Build')
     {
      steps
       {
        bat 'mvn --batch-mode compile'
       }
     }

    stage('UnitTests')
     {
      steps
       {
        bat 'mvn --batch-mode resources:testResources compiler:testCompile surefire:test'
        //  -Dmaven.test.failure.ignore=true
       }
      post
       {
        always
         {
          junit testResults: 'target/surefire-reports/*.xml'
         }
       }
     }

/*
    stage('MutationTests')
     {
      steps
       {
        bat 'mvn --batch-mode org.pitest:pitest-maven:mutationCoverage'
       }
     }
*/

    stage('Sanity check')
     {
      steps
       {
        bat 'mvn --batch-mode checkstyle:checkstyle pmd:pmd pmd:cpd com.github.spotbugs:spotbugs-maven-plugin:spotbugs'
        // scanForIssues tools: [java(), javaDoc(), eclipse(), cssLint(), groovyScript(), jsLint()]
        // sonarQube
        // yuiCompressor
        // dependencyCheckAnalyzer
        // dependencyCheckPublisher
        // dependencyTrackPublisher
        // Arachni Scanner Plugin 
        // Black Duck Detect 
        /*
        codesonar
        withSonarQubeEnv('My SonarQube Server')
         {
          bat 'mvn --batch-mode sonar:sonar'
         }
        waitForQualityGate
        */
       }
      /*
      post
       {
        always
         {
          // recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc(), checkStyle(), spotBugs(), cpd(pattern: '** /target/cpd.xml'), pmdParser(pattern: '** /target/pmd.xml')]
         }
       }
      */
     }

    stage('Packaging')
     {
      steps
       {
        bat 'mvn --batch-mode jar:jar'
       }
     }

    stage('Install local')
     {
      steps
       {
        bat 'mvn --batch-mode jar:jar install:install' // maven-jar-plugin falseCreation default is false, so no doubled jar construction here, but required for maven-install-plugin internal data
       }
     }

    stage('Integration tests')
     {
      steps
       {
        bat 'mvn --batch-mode failsafe:integration-test failsafe:verify'
       }
     }

    stage('Documentation')
     {
      steps
       {
        bat 'mvn --batch-mode -Dweb.server=www.powerstat.de site'
        // Change history
       }
      post
       {
        always
         {
          publishHTML(target: [reportName: 'Site', reportDir: 'target/site', reportFiles: 'index.html', keepAll: false])
          // publishHTML(target: [reportName: 'Manual', reportDir: 'target/generated-docs', reportFiles: 'TemplateEngine.html', keepAll: false])
         }
       }
     }

   }

 }
