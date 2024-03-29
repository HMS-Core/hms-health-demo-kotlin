# HMS Health Kit Demo for kotlin
English | [中文](https://github.com/HMS-Core/hms-health-demo-kotlin/blob/master/README_ZH.md)
## Table of Contents

 * [Introduction](#introduction)
 * [Getting Started](#getting-started)
 * [Supported Environments](#supported-environments)
 * [Result](#result)
 * [Question or Issues](#question-or-issues)
 * [License](#license)

## Introduction
HUAWEI Health is an open service for sports & health capabilities provided by Huawei. Developers can access the Huawei Health Platform and obtain sports & health data by integrating HUAWEI Health.   
Health demo code encapsulates APIs of the HUAWEI Health Kit. It provides demo programs for your reference or usage.   

The following describes the functions of Health Kit:  
1)  Login and Authorization  
This function applies to login the account and enable the authorization.  
Android APIs code location:  \app\src\main\java\com\huawei\demo\health\auth\HealthKitAuthActivity.kt   

2)  Cancel Scope  
This function applies to cancel account authorization.  
Android APIs code location:  \app\src\main\java\com\huawei\demo\health\HealthKitMainActivity.kt
    
3)  Operate health and sport data  
The function has ability to add/delete/update/query the health and sport data.  
Code location: \app\src\main\java\com\huawei\demo\health\HealthKitDataControllerActivity.kt   
    
4) Auto-record the step count  
This function can enable and disable the automatically record method for the step count.  
Code location: \app\src\main\java\com\huawei\demo\health\HealthKitAutoRecorderControllerActivity.kt   
    
5) Operate Activity Record   
This function can create and manage a single sport or activity.  
Code location:  \app\src\main\java\com\huawei\demo\health\HealthKitActivityRecordControllerActivity.kt   
    
6) Operate DataType and Records  
This function can add/read DataType and cancel All Records.   
Code location:  \app\src\main\java\com\huawei\demo\health\HealthKitSettingControllerActivity.kt 
    
7) Operate Health Record  
This function can add/read/update a health record. Currently, the data types of the tachycardia bradycardia are supported.   
Code location:  \app\src\main\java\com\huawei\demo\health\HealthKitHealthRecordControllerActivity.kt   

## Getting Started
Before using Health demo code, check whether the IDE environment has been installed. 
1. Decompress the demo code package.    
2. Copy the Health package into the IDE directory and import it into the IDE Tool.
3. You should also generate a signing certificate fingerprint  and add the certificate file to the project, and add configuration to build.gradle. See (https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides-V5/signing-fingerprint-0000001059050363-V5)
4. Press Sync Project with Gradle Files to ensure the development environment builds completed.

To use functions provided by packages in examples, you need to set parameters as following:
1. HuaweiAccount: Health need verify the user's permission to operate user's health data
2. HMS SDK maven Address:
    * Configure the maven address of the HMS SDK in allprojects->repositories of project's build.gradle : maven {url 'https://developer.huawei.com/repo/'}
    * Add maven configuration of HMS SDK in buildscript->repositories : maven {url 'https://developer.huawei.com/repo/'}
3. Compile dependencies: Add compile dependencies in model's build.gradle : implementation 'com.huawei.hms:health:{version}'
4. AppId info: In the application AndroidManifest.xml, add the appId information generated by the Developer Alliance when creating the application.

## Supported Environments
* Android Studio 3.6.1 or later.
* Java SDK 1.8 or later.
* Mobile Android Version 7.0-13 (API level 24-33) or HarmonyOS. HMS Core (APK) 4.0.2.300 or later has been installed on the phone. To read data from the Health app, HMS Core (APK) 5.0.4.300 or later should be installed. HUAWEI Health app of version 11.0.0.512 or later has been installed.

## Result
   <img src="images/result_1.png" width = 30% height = 30%> <img src="images/result_5.png" width = 30% height = 30%>
   <img src="images/result_2.png" width = 30% height = 30%> <img src="images/result_6.png" width = 30% height = 30%>
   <img src="images/result_3.png" width = 30% height = 30%> <img src="images/result_7.png" width = 30% height = 30%>
   <img src="images/result_4.png" width = 30% height = 30%> 
   
   
## Question or Issues
If you want to evaluate more about HMS Core, [r/HMSCore on Reddit](https://www.reddit.com/r/HuaweiDevelopers/) is for you to keep up with latest news about HMS Core, and to exchange insights with other developers.

If you have questions about how to use HMS samples, try the following options:
- [Stack Overflow](https://stackoverflow.com/questions/tagged/huawei-mobile-services) is the best place for any programming questions. Be sure to tag your question with 
`huawei-mobile-services`.
- [Huawei Developer Forum](https://forums.developer.huawei.com/forumPortal/en/home?fid=0101187876626530001) HMS Core Module is great for general questions, or seeking recommendations and opinions.

If you run into a bug in our samples, please submit an [issue](https://github.com/HMS-Core/hms-health-demo-kotlin/issues) to the Repository. Even better you can submit a [Pull Request](https://github.com/HMS-Core/hms-health-demo/pulls) with a fix.
	
##  License
   Health kit demo is licensed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
