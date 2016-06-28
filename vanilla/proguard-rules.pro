# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/neuro/programming/android-sdk-linux/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#-keep class cm.aptoide.pt.preferences.** {*;}
#-keep class cm.aptoide.pt.dataprovider.** {*;}
-keep class android.support.v7.appcompat.** { *; }
-keep class android.support.v7.widget.SearchView { *; }
-keepattributes Exceptions, InnerClasses
#-keep class cm.aptoide.pt.** {*;}
-keep class cm.aptoide.pt.**$* {
    *;
}

####### RetroLambda #######
-dontwarn java.lang.invoke.*
