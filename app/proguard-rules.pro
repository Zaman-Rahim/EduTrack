# Optimizations for Jetpack Compose and Kotlin Coroutines
-repackageclasses
-allowaccessmodification

# Room SQLite entities & DAOs
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * extends com.example.edutrack.data.local.entity.** { *; }
-keep class com.example.edutrack.data.local.relation.** { *; }

# ViewModel reflection
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preserve Compose model classes and enums
-keepclassmembers enum * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
