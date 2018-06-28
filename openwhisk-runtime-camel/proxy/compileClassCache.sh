#!/bin/bash
#### Construct Class Cache with HTTP Server classes by starting the server ####
java "-Xshareclasses:cacheDir=/javaSharedCache/" "-Xquickstart" "-jar" "/camelAction/build/libs/camelAction-all.jar" "test"
