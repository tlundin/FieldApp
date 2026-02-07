# FieldApp

A mobile field data collection application for Android with GIS capabilities, data synchronization, and dynamic workflow management.

## Overview

FieldApp is designed for field workers to collect, manage, and synchronize geospatial data while working in the field. The app supports offline operation with automatic backup and synchronization capabilities.

## Key Features

- **GIS Integration**: Geographic Information System (GIS) functionality with map support via Mapbox
- **Data Synchronization**: Bi-directional data sync between field devices and central servers
- **Offline Capability**: Full functionality without network connectivity
- **Dynamic Workflows**: Configurable data collection workflows
- **Photo Management**: Air photo metadata handling and photo collection
- **Backup & Restore**: Automatic database backup with configurable frequency
- **Multiple Connection Types**: Bluetooth, mobile network, and WLAN support

## Tech Stack

- **Language**: Java & Kotlin
- **Platform**: Android (min SDK 29, target SDK 35)
- **Architecture**: MVVM pattern with ViewModels
- **Maps**: Mapbox Maps SDK (NDK 27 for 16KB page size support)
- **Networking**: OkHttp, Volley
- **Location**: Google Play Services Location
- **Build System**: Gradle

## Project Structure

```
app/src/main/java/com/teraim/fieldapp/
├── dynamic/              # Dynamic workflow system
│   ├── blocks/          # Reusable workflow blocks
│   ├── templates/       # UI templates
│   ├── types/           # Data type definitions
│   └── workflow_*/      # Workflow implementations
├── gis/                 # GIS utilities and math
├── loadermodule/        # Configuration and data loading
├── synchronization/     # Data sync framework
├── utils/               # Utility classes (backup, persistence)
├── viewmodels/          # MVVM ViewModels
└── ui/                  # UI fragments and dialogs
```

## Building

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API level 35
- Mapbox access token

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/tlundin/FieldApp.git
   cd FieldApp
   ```

2. Create a `local.properties` file in the root directory with your configuration:
   ```properties
   sdk.dir=/path/to/android/sdk
   MAPBOX_ACCESS_TOKEN=your_mapbox_token_here
   
   # Optional: Signing configuration (for release builds)
   SIGNING_KEY_ALIAS=your_key_alias
   SIGNING_KEY_PASSWORD=your_key_password
   SIGNING_STORE_FILE=/path/to/keystore
   SIGNING_STORE_PASSWORD=your_store_password
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run on device/emulator:
   ```bash
   ./gradlew installDebug
   ```

## Configuration

The app uses a modular configuration system with:
- CSV-based configuration files
- JSON/XML configuration modules
- Bundle-based versioning for updates

## Security Note

Never commit sensitive credentials to version control. All signing keys and API tokens should be stored in `local.properties` (which is gitignored) or environment variables.

## Dependencies

Key dependencies include:
- AndroidX AppCompat, Material, ConstraintLayout
- Google Play Services (Maps, Location, Vision)
- Mapbox Maps SDK
- OkHttp for networking
- Gson for JSON parsing
- Kotlin Coroutines

## Contributing

When contributing to this project:
1. Follow Android development best practices
2. Use consistent logging (Android's Log class, not System.out)
3. Remove commented code - version control preserves history
4. Keep security credentials out of build files
5. Document significant changes

## License

[Add license information here]

## Contact

For questions or support, please contact [repository maintainer contact info].
