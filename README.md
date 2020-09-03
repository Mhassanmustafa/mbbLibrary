# Mobile Book Bank Library

A react native book reading library based on mupdf

### Installation

```bash
yarn add git+https://git@github.com/Mhassanmustafa/mbbLibrary.git
```

### Add it to your android project

```bash
react-native link mbbLibrary
```

### Manual configuration

#### react-native/android/build.gradle

- minSdkVersion >= 16 `must`
- compileSdkVersion = 28 `must`
- targetSdkVersion = 28 `is best`
- supportLibVersion = "28.0.0" `is best`

#### android/app/build.gradle

Local reference AAR/JAR

```
android{
    ...

    repositories {
        flatDir {
            dirs project(':mbbLibrary').file('libs')
        }
    }
}
```

## Usage

See the detail in [./demo/app.js](https://github.com/Mhassanmustafa/mbbLibrary/blob/master/demo/app.js)

```jsx harmony
import { startPDFActivity } from "mbbLibrary";

startPDFActivity({
  OpenMode: "",
  dark: "",
  Uri: "",
});
```

### startPDFActivity

| Options  | Description   | Example                                 |
| -------- | ------------- | --------------------------------------- |
| OpenMode | Open mode     | "daily","Master","Accused"              |  |
| Uri      | Path of Pdf   | /storage/emulated/0/Download/pdf_t1.pdf |
| Page     | Starting Page | Number 0                                |
| dark     | night mode    | true                                    |

### finishPDFActivity finish the current activity

```jsx harmony
finishPDFActivity();
```
