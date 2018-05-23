mvn clean package

wsk action delete redhatdevelopers/camel-feed
wsk trigger delete camelTrigger
wsk action delete camelTriggerAction
wsk rule delete camelEntryRule


wsk action update -a feed true \
  --main org.jboss.fuse.openwhisk.camel.CamelFeedAction \
  redhatdevelopers/camel-feed \
  camel-feed-action/target/camel-feed-action-*.jar

wsk trigger create camelTrigger \
  --feed redhatdevelopers/camel-feed \
  -p mapping '{ date: \"header[CamelTimerFiredTime]\", counter: \"header[CamelTimerCounter]\" }' \
  -p routeUrl 'timer://foo?fixedRate=true&period=10000'

wsk action create camelTriggerAction camel-feed-action/src/handler.js

wsk rule create camelEntryRule camelTrigger camelTriggerAction
