package gm.tieba.tabswitch.dao

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity
data class AcRule(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "matcher") val matcher: String,
    @ColumnInfo(name = "class") val clazz: String,
    @ColumnInfo(name = "method") val method: String
) {
    companion object {
        fun create(matcher: String, clazz: String, method: String) =
            AcRule(0, matcher, clazz, method)
    }
}

@Dao
interface AcRuleDao {
    @Query("SELECT * FROM AcRule")
    fun getAll(): List<AcRule>

    @Query("SELECT * FROM AcRule WHERE matcher IN (:matchers)")
    fun loadAllMatch(vararg matchers: String): List<AcRule>

    @Insert
    fun insertAll(vararg rules: AcRule)

    @Delete
    fun delete(rule: AcRule)
}

@Database(entities = [AcRule::class], version = 1, exportSchema = false)
abstract class AcRuleDatabase : RoomDatabase() {
    abstract fun acRuleDao(): AcRuleDao
}

object AcRuleMigrations {
    @JvmStatic
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("alter table rules rename column rule to matcher")
            database.execSQL("alter table rules rename to AcRule")
        }
    }
}
