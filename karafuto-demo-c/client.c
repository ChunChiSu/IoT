#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <curl/curl.h>

#include "mosquitto.h"

#define HOST        "iot.cht.com.tw"
#define MQTT_PORT   1883
#define API_KEY     "H5T40KG55AWAA9U4"

#define DEVICE_ID           "169"
#define OUTPUT_SENSOR_ID    "Output"
#define INPUT_SENSOR_ID     "Input"

#define BUFFER_SIZE 128

int        g_thread_running = 1;
pthread_t  g_thread_to_post = { 0 };

/* Send a HTTP POST */

int http_post(char *url, char *ck, char *body)
{
  CURL                *pCurl = NULL;
  struct  curl_slist  *pHeaders = NULL;
  CURLcode             curlCode = { 0 };
  char                 strCK[BUFFER_SIZE] = { 0 };
  long                 sc = 0; // HTTP status code
  int                  r = -1;
  
  if ((pCurl = curl_easy_init()) == NULL)
  {
    return -1; // failed to initialize cURL
  }
  
  curl_easy_setopt(pCurl, CURLOPT_URL, url);
  
  snprintf(strCK, BUFFER_SIZE, "CK: %s", ck);  
  pHeaders = curl_slist_append(pHeaders, strCK); // API KEY
  
  pHeaders = curl_slist_append(pHeaders, "Content-Type: application/json"); // my body is JSON
  
  if (pHeaders == NULL)
  {  
    goto  FUNCTION_END;
  }
  
  curl_easy_setopt(pCurl, CURLOPT_HTTPHEADER, pHeaders);
  
  curl_easy_setopt(pCurl, CURLOPT_POSTFIELDS, body);
  
  r = -2;
  
  if (curl_easy_perform(pCurl) == CURLE_OK) // perform the request
  { 
    curlCode = curl_easy_getinfo(pCurl, CURLINFO_RESPONSE_CODE , &sc);
    if (curlCode == CURLE_OK) // check status code
    {
      r = sc;
    }
  }
  
FUNCTION_END:
  
  if (pHeaders)
  {    
    curl_slist_free_all(pHeaders);
  }
  
  curl_easy_cleanup(pCurl);
  
  return r;
}

/* Fore a thread to change rawdata of the input sensor */

void thread_to_post(void *arg)
{
  int  sc = 0;
  char url[BUFFER_SIZE];
  int  i = 0;
  char json[BUFFER_SIZE];

  // specify the URL
  snprintf(url, BUFFER_SIZE, "http://%s/iot/v1/device/%s/rawdata", HOST, DEVICE_ID);
  
  while (g_thread_running)
  {
    // prepare the JSON data to change the rawdata
    snprintf(json, BUFFER_SIZE, "[{\"id\":\"%s\",\"value\":[\"%d\"]}]", INPUT_SENSOR_ID, i++);

    sc = http_post(url, API_KEY, json);
    
    if (sc != 200)
    {      
      printf("HTTP POST is failed - %d\n", sc);
    }
    else
    {
      printf("Rawdata is saved\n");
    }
    
    sleep(10);
  }
  
  pthread_detach(g_thread_to_post);
}

/* MQTT connection is ready */

void mqtt_callback_connect(struct mosquitto *mosq, void *obj, int result)
{
  int  r = 0;
  char topic[BUFFER_SIZE];

  snprintf(topic, BUFFER_SIZE, "/v1/device/%s/sensor/%s/rawdata", DEVICE_ID, OUTPUT_SENSOR_ID);
  
  printf("MQTT is connected.\n");
  
  r = mosquitto_subscribe(mosq, NULL, topic, 0);
  
  if (r == 0)
  {    
    printf("MQTT topic is subscribed.\n");
  }
  else
  {    
    printf("Failed to subscribe the topic!\n" );
  }
}

/* MQTT connection is lost */

void mqtt_callback_disconnect(struct mosquitto *mosq, void *obj, int result)
{
  printf("MQTT is disconnected!");
 
  mosquitto_disconnect(mosq);
}

/* Received the MQTT message */

void mqtt_callback_message(struct mosquitto *mosq, void *obj, const struct mosquitto_message *message)
{
  if ((message->topic == NULL) || (message->payload == NULL) || (message->payloadlen <= 0))
  {    
    return;
  }
 
  // just print out the payload
  printf("[%s] %s\n", message->topic, (char *) message->payload);
}

/* Establish the MQTT connection to subscribe the rawdata topic */

int mqtt_loop(char *host, int port, char *username, char *password)
{
  struct mosquitto  *mosq;
  int               r = 0;
  
  mosquitto_lib_init();
  
  if ((mosq = mosquitto_new(NULL, true, NULL)) == NULL)
  {    
    r = -1;
    
    printf("Failed to build the MQTT object.\n" );
    
    goto  FUNCTION_END;
  }
  
  // declare the callback functions
  
  mosquitto_connect_callback_set(mosq, mqtt_callback_connect);
  
  mosquitto_message_callback_set(mosq, mqtt_callback_message);
  
  mosquitto_disconnect_callback_set(mosq, mqtt_callback_disconnect);
  
  // authentication
  
  r = mosquitto_username_pw_set(mosq, username, password);
  
  if (r)
  {    
    r = -2;
    
    printf("Failed to setup the MQTT authentication - %d.\n", r);
    
    goto  FUNCTION_END;
  }
  
  // connect and wait
  
  if (mosquitto_connect(mosq, host, port, 60) == MOSQ_ERR_SUCCESS)
  {    
    r = 0;
    
    mosquitto_loop_forever(mosq, -1, 1);
  }
  else
  {    
    r = -3;
    
    printf("Failed to connect to MQTT broker.\n");
  }
  
  mosquitto_destroy(mosq);
  
FUNCTION_END:
  
  mosquitto_lib_cleanup();
  
  return  r;
}


/* okay, we start from here */

int main(int argc, char **argv)
{
  int  r = 0;
  
  // fork a thread to change the rawdata of the input sensor
  
  if (pthread_create(&g_thread_to_post, NULL, (void *) thread_to_post, NULL))
  { 
    printf("Failed to create a thread to change the rawdata.\n");
    
    return 1;
  }
  
  r = mqtt_loop(HOST, MQTT_PORT, API_KEY, API_KEY); // blocking here
  
  if (r)
  {    
    printf("MQTT is broken - %d\n", r);
  }  

  // stop the rawdata saving thread  

  g_thread_running = 0;
  
  pthread_join(g_thread_to_post, NULL);
  
  return  0;
}

