[![](https://jitpack.io/v/VolmitSoftware/NMSTools.svg)](https://jitpack.io/#VolmitSoftware/NMSTools)
## Example Usage:
build.gradle
```groovy
buildscript() {
    repositories {
        maven { url 'https://jitpack.io'}
    }
    dependencies {
        classpath 'com.github.VolmitSoftware:NMSTools:1.0.0'
    }
}

apply plugin: 'com.volmit.nmstools'

nmsTools {
    jvm = 21
    version = "1.20.6-R0.1-SNAPSHOT"
}
```

gradle.properties
```properties
nmsTools.useBuildTools=false
nmsTools.repo-url=https://repo.codemc.org/repository/nms/
nmsTools.specialSourceVersion=1.11.4
```