package com.example.rfidpaymentsys.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rfidpaymentsys.data.dao.*
import com.example.rfidpaymentsys.data.entity.*

@Database(
    entities = [UserAccount::class, RfidCard::class, Transaction::class, AuditLog::class, ValidationLog::class, IncomeReport::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun validationLogDao(): ValidationLogDao
    abstract fun incomeReportDao(): IncomeReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Adds username and account status without wiping existing local data
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN username TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN accountStatus TEXT NOT NULL DEFAULT 'ACTIVE'")
            }
        }

        // Adds the passenger type column without wiping existing local data
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN passengerType TEXT NOT NULL DEFAULT 'REGULAR'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rfid_payment_database"
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}