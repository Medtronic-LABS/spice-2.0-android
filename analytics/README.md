# Spice Analytics Library

Spice Analytics is a Android library for logging event in local database and upload to server/s3 bucket in json format.

## Installation

### 1. Copy "analytics" folder into your project

### 2. Include in project
Add following line in "settings.gradle.kts" from project root directory.
```bash
include(":analytics")
```

Add following line in app level "build.gradle.kts" file inside "dependencies". 
```bash
implementation(project(":analytics"))
```

Thats it. You are completed your installation. Clean and Build the project and ensure that no issues arises.

## Usage

### 1. Create dependany injection for AnalyticsRepository. Add following lines in AppModule.kt
```bash
@Singleton
@Provides
fun provideAnalyticsRepo(@ApplicationContext context: Context): AnalyticsRepository {
    return AnalyticsRepository(context)
}
```

### 2. Get "AnalyticsRepository" instance in any Viewmodel class by adding as constructor arguments
```bash
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val analyticsRepository: AnalyticsRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
}
```

### 3. Setting userId and Application information 
```bash
UserDetail.userId = "43"

val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
UserDetail.appVersion = packageInfo.versionName
```

### 4. Log Event
```bash
viewModelScope.launch(dispatcherIO) {
     val eventName = "login"
     val parameter = mapOf(
         "startTime" to "2024-04-23 19:28:00",
         "endTime" to "2024-04-23 19:29:20",
         "isCompleted" to true,
         "apiId" to 12
       )
            
    analyticsRepository.logEvent(eventName, parameter)
}
```

### 5. Generate report. 
```bash
viewModelScope.launch(dispatcherIO) {
     analyticsRepository.generateAnalyticsReport()
}
```

After this step we can see a json file will be generated in app memory with following name convention "YYYY_MM_DD_userID_analytics.json"

