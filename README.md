# InstrumentFinder

This Android application allows users to identify musical instruments based on audio file. The app utilizes machine learning model to classify the instruments.

 ## Features

- Record and upload audio files
- Automatic conversion of audio files to MP3 format
- Classification of instruments including piano, violin, acoustic guitar, and cello
- Display of classification results with probabilities for each instrument
- History of recently uploaded files and server responses
- Stop button to interrupt file uploading

## Technologies Used
### Application
- Kotlin
- Android Jetpack (ViewModel, LiveData, ActivityResultLauncher)
- Retrofit for network requests
### Server
- Flask for the server-side application
- Librosa for audio preprocessing
- Gradient Boosting for machine learning model

## Usage

To use the app, follow these steps:

- Install the app on your Android device.
- Record or select an audio file.
- Click the "Send" button to upload the file.
- View the classification results.

## Screenshots
<img src="https://github.com/Iaura-w/InstrumentFinder/assets/26602440/81be471e-3d4a-4a77-ab22-89003ca422d3" height="450">
<img src="https://github.com/Iaura-w/InstrumentFinder/assets/26602440/f724d8e0-0015-4dec-a4c5-e6eb4ba51c32" height="450">
<img src="https://github.com/Iaura-w/InstrumentFinder/assets/26602440/dd6636a3-1305-4305-919b-535de943555d" height="450">
<img src="https://github.com/Iaura-w/InstrumentFinder/assets/26602440/e71c23f4-1fe5-459d-a39f-36478d3ed777" height="450">

## Credits

This app was developed as part of a research project. The machine learning models were trained using the IRMAS dataset.

