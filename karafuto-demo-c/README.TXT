Example in CentOS 6

1. prepare the 'libcurl' first

	yum -y install libcurl-devel

2. login to 'https://iot.cht.com.tw'

   a) create a project and read the API KEY from detail.
   b) create a device and get it's numeric ID from URL.
   c) create 2 sensors - names 'Input' and 'Output'.

3. modify the API_KEY, DEVICE_ID, INPUT_SENSOR_ID, OUTPUT_SENSOR_ID in client.c

4. make

5. ./client

