package app.plantdiary.myplantdiary23SS002

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class MyPlantDiaryApplication : Application() {

    /**
     * Do any special startup stuff here.
     */
    override fun onCreate() {
        super.onCreate()

        GlobalContext.startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MyPlantDiaryApplication)
            modules(appModule)
        }
    }
}