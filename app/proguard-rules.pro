# Optimizations for Jetpack Compose and Kotlin Coroutines
-repackageclasses
-allowaccessmodification

# Room SQLite entities & DAOs
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.edutrack.data.local.entity.** { *; }
-keep class com.example.edutrack.data.local.relation.** { *; }

# Keep @Keep annotated symbols
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ViewModel reflection
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
