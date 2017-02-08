Example in CentOS 6

This example show you how to save the rawdata and listen for the value changed from CHT IoT platform by MQTT.

1. login to 'https://iot.cht.com.tw'

   a) create a project and read the API KEY from detail.
   b) create a device and get it's numeric ID from URL.
   c) create 2 sensors - names 'Input' and 'Output'.

2. modify the API_KEY, DEVICE_ID, INPUT_SENSOR_ID, OUTPUT_SENSOR_ID in client.c

3. make

4. ./client

