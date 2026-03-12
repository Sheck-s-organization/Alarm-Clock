# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entity classes
-keep class com.smartalarm.app.data.entities.** { *; }

# Keep Gson model classes (used for JSON serialization in smart alarm config)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep manager models serialized by Gson
-keep class com.smartalarm.app.manager.MonthPeriod { *; }
-keep class com.smartalarm.app.data.entities.ShiftCycle { *; }
-keep class com.smartalarm.app.data.entities.DayTimeOverride { *; }
-keep class com.smartalarm.app.data.entities.MonthlyTimeOverride { *; }
-keep class com.smartalarm.app.data.entities.AlarmLocationRule { *; }

# Keep BroadcastReceivers and Services
-keep class com.smartalarm.app.receiver.** { *; }
-keep class com.smartalarm.app.service.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Navigation safe args
-keep class com.smartalarm.app.ui.alarm.EditAlarmFragmentArgs { *; }
-keep class com.smartalarm.app.ui.schedule.EditScheduleFragmentArgs { *; }
