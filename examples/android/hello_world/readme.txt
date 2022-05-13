This is a sample app that can be built with gradle and buck.

How to build
-------------

First, download libraries:

./gradlew app:download

That provides dependencies for buck.

An alternative option:

./gradlew -q :app:dependencies --configuration debugCompileClasspath >  buck-out/deps.txt
./gradlew -q :app:dependencies --configuration debugRuntimeClasspath >> buck-out/deps.txt
./gradlew -q :app:dependencies --configuration debugAnnotationProcessorClasspath >> buck-out/deps.txt

cd ../import_deps
./importdeps.py --gdeps /Users/mdzyuba/whatsapp/android/tools/buck/hello_world/buck-out/deps.txt --libs third-party
cd ../hello_world
mv ../import_deps/third-party .

Verify the generated third-party BUCK

buck targets //third-party:
buck build //third-party:core

Start an Android emulator or device. It could be based on SDK Platform v21 - v32.

To build and install the app, run:

buck install //app:app

Then run Hello World app on the device.


Hare is a list of sample buck targets:

buck build //app:deps
buck build //app:lib
buck build //keystore:debug_keystore
buck build //app:app
adb install ./buck-out/gen/app/app.apk

