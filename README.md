# KingsDeck

## Project Structure
The project contains a android and a core folder. The android folder contains the android spesific implementation code and the assets folder holding the images, ui skin as well as the pieces, spells, tutorial and game settings jsons which will generate the content for the game on runtime. The core folder holds the game code which is launched through the core main class using the packages model, view, presenter for the MVP pattern. As well as the config and service packages for the addition of the client server pattern through service and the loading config files thorugh the config package.

## Installing and running the game on Android Device 

Download the APK file from the top folder of this repository. The filename is KingsDeck.apk and transfer it onto your android device. 
Find the file on your android device and click to install the APK file. Sometimes you need to enable developer settings to allow installation of foreign APK files. 
Launch the app! 
Once launched you can click sign in and log in with your google account. 
Sometimes the emulator will have issues and say the UI or other apps on the emulator is crashed or similar, just click close for these and continue using the app. If the Login was interrupted, you might get an error like 12501 but just try to login again.
Sometimes if it takes too long you might get an error 8 and might need to restart the application fully before attempting to login again. 
You are now logged in and ready to play! 

## Installing and running the game on Android Studio 

Download the project repo. 
Open the project in Android Studio. 
You will need to make a gradle sync to make it in line with the project. 
The SDK path needs to be updated, this sometimes happens automatically, or you will need to manually adjust it in local.properties file in the top-level folder. This is also sometimes fixed during the gradle sync as a pop-up. 
Build project by clicking the Build tab and clicking Assemble Run Configuration. 
To run you need to have an android emulator with SDK version at least 23 but for best results use an emulator with SDK version 35. 
Run project in android studio and wait for the app to launch. 
Once launched you can click sign in and log in with your google account. 
Sometimes the emulator will have issues and say the UI or other apps on the emulator is crashed or similar, just click close for these and continue using the app. If the Login was interrupted, you might get an error like 12501 but just try to login again. 
Sometimes if it takes too long you might get an error 8 and might need to restart the application fully before attempting to login again. 
You are now logged in and ready to play! 
