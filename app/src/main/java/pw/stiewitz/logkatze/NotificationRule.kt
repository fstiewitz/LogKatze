/*
 * Copyright (c) 2022      Fabian Stiewitz <fabian (at) stiewitz.pw>
 * Licensed under the EUPL-1.2
 */

package pw.stiewitz.logkatze

import androidx.room.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Entity(primaryKeys = ["process", "component", "priority", "contentRegex"])
data class NotificationRule(
    val process: String,
    val component: String,
    val priority: String,
    val contentRegex: String
) {
    fun getNiceName(): String {
        val c = if(process.isNotEmpty()) process else component
        val head =
            if (c.isNotEmpty() && priority.isNotEmpty()) "$priority of $c"
            else if (c.isNotEmpty()) c
            else if (priority.isNotEmpty()) priority
            else ""

        return if (head.isNotEmpty() && contentRegex.isNotEmpty()) "$head: $contentRegex"
        else if (head.isNotEmpty()) head
        else if (contentRegex.isNotEmpty()) "Any: $contentRegex"
        else ""
    }
}

@Dao
interface NotificationRuleDAO {
    @Query("SELECT * FROM NotificationRule")
    fun getAll(): List<NotificationRule>

    @Update
    fun updateAll(vararg rule: NotificationRule)

    @Insert
    fun insertAll(vararg rule: NotificationRule)

    @Delete
    fun deleteAll(vararg rule: NotificationRule)
}

@Database(version = 1, entities = [NotificationRule::class])
abstract class LogKatzeDatabase : RoomDatabase() {
    abstract fun notificationRuleDao(): NotificationRuleDAO
}

@Module
class LogKatzeDatabaseModule {
    @Singleton
    @Provides
    fun provideLogKatzeDatabase(context: MyApplication): LogKatzeDatabase {
        return Room.databaseBuilder(context, LogKatzeDatabase::class.java, "data")
            .fallbackToDestructiveMigration().build()
    }
}