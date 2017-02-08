#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>

#include "mosquitto.h"

#define HOST        "iot.cht.com.tw"
#define MQTT_PORT   1883
#define API_KEY     "PKUSY5EBG1BFRHKU5Y"

#define DEVICE_ID           "864330373"
#define OUTPUT_SENSOR_ID    "Output"
#define INPUT_SENSOR_ID     "Input"

#define BUFFER_SIZE 128

int        g_thread_running = 1;
pthread_t  g_thread_to_post = { 0 };

// fork a thread to change the rawdata of input sensor
void thread_to_post(void *arg) {
  char topic[BUFFER_SIZE];
  char json[BUFFER_SIZE];
  int i = 0;
  int r = 0;

  struct mosquitto *mosq = (struct mosquitto *) arg;

  // specify rawdata topic
  snprintf(topic, BUFFER_SIZE, "/v1/device/%s/rawdata", DEVICE_ID);
  
  while (g_thread_running) {
    // prepare the JSON data to change the rawdata
    snprintf(json, BUFFER_SIZE, "[{\"id\":\"%s\",\"value\":[\"%d\"]}]", INPUT_SENSOR_ID, i++);

    // publish the rawdata via MQTT
    r = mosquitto_publish(mosq, NULL, topic, strlen(json), json, 0, false);
    if (r == MOSQ_ERR_SUCCESS) {
      printf("Save: %s\n", json);

    } else {
      printf("Failed to publish the rawdata [%d]\n", r); // MOSQ_ERR_INVAL, MOSQ_ERR_NOMEM, MOSQ_ERR_NO_CONN, MOSQ_ERR_PROTOCOL, MOSQ_ERR_PAYLOAD_SIZE
    }
    
    sleep(5);
  }
  
  pthread_detach(g_thread_to_post);
}

// connection is ready
void mqtt_callback_connect(struct mosquitto *mosq, void *obj, int result) {
  char topic[BUFFER_SIZE];
  int r = 0;

  printf("MQTT is connected.\n");

  snprintf(topic, BUFFER_SIZE, "/v1/device/%s/sensor/%s/rawdata", DEVICE_ID, OUTPUT_SENSOR_ID);
  
  // subscribe the INPUT sensor
  r = mosquitto_subscribe(mosq, NULL, topic, 0);  
  if (r == 0) {    
    printf("%s is subscribed.\n", topic);

  } else {    
    printf("Failed to subscribe the topic - %s\n", topic);
  }

  // fork a thread to change rawdata of the OUTPUT sensor
  if (pthread_create(&g_thread_to_post, NULL, (void *) thread_to_post, mosq)) {
    printf("Failed to create a thread to change the rawdata.\n");    
  }
}

// connection is lost
void mqtt_callback_disconnect(struct mosquitto *mosq, void *obj, int result) {
  printf("MQTT is disconnected!");
 
  mosquitto_disconnect(mosq);
}

// got the MQTT message
void mqtt_callback_message(struct mosquitto *mosq, void *obj, const struct mosquitto_message *message) {
  if ((message->topic == NULL) ||
      (message->payload == NULL) ||
      (message->payloadlen <= 0)) {

    return;
  }
 
  // just print out the payload
  printf("[%s] %s\n", message->topic, (char *) message->payload);
}

// establish the MQTT connection
int mqtt_loop(char *host, int port, char *username, char *password) {
  struct mosquitto *mosq;
  int r = 0;
  
  mosquitto_lib_init();
  
  if ((mosq = mosquitto_new(NULL, true, NULL)) == NULL) {    
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
  
  if (r) {    
    r = -2;
    
    printf("Failed to setup the MQTT authentication - %d.\n", r);
    
    goto  FUNCTION_END;
  }
  
  // connect and wait  
  if (mosquitto_connect(mosq, host, port, 60) == MOSQ_ERR_SUCCESS) {    
    r = 0;
    
    mosquitto_loop_forever(mosq, -1, 1);

  } else {    
    r = -3;
    
    printf("Failed to connect to MQTT broker.\n");
  }
  
  mosquitto_destroy(mosq);
  
FUNCTION_END:
  
  mosquitto_lib_cleanup();
  
  return r;
}


/* okay, we start from here */
int main(int argc, char **argv) {
  int r = 0;
  
  r = mqtt_loop(HOST, MQTT_PORT, API_KEY, API_KEY); // blocking here
  
  if (r) {    
    printf("MQTT is broken - %d\n", r);
  }  

  // stop the rawdata saving thread  

  g_thread_running = 0;
  
  pthread_join(g_thread_to_post, NULL);
  
  return  0;
}

