
/*!
	@file	ring.c
	@author	Bryson Sherman, Hung Mach, Jimmy Lanh
	@date	3/5/2018
	@version 1.0
	Creators: Bryson Sherman
			  Hung Mach
			  Jimmy Lanh
		  
	Due Date: 3/1/2018
	
    C++ program using kilobot to elect a leader
	Once a new kilobot join the group, it will run an election trying to elect a new
	leader.
*/
#define SIMULATOR


#ifndef SIMULATOR
    #include <kilolib.h>
    #include <avr/io.h>  // for microcontroller register defs
    #include "ring.h"
    USERDATA myData;
    USERDATA *mydata = &myData;
#else
    #include <math.h>
    #include <kilombo.h>
    #include <stdio.h> // for printf
    #include "ring.h"
    REGISTER_USERDATA(USERDATA)
#endif

void recv_sharing(uint8_t *payload, uint8_t distance);
void recv_joining(uint8_t *payload);
void recv_election(uint8_t *payload);

/********************************/
/*       Utility Function       */
/********************************/

/* Function that will test to see if the queue of mydata is full
 */
char isQueueFull()
{
    return (mydata->tail +1) % QUEUE == mydata->head;
}

/* Helper function for setting motor speed smoothly
 */
void smooth_set_motors(uint8_t ccw, uint8_t cw)
{
    // OCR2A = ccw;  OCR2B = cw;
#ifdef KILOBOT
    uint8_t l = 0, r = 0;
    if (ccw && !OCR2A) // we want left motor on, and it's off
        l = 0xff;
    if (cw && !OCR2B)  // we want right motor on, and it's off
        r = 0xff;
    if (l || r)        // at least one motor needs spin-up
    {
        set_motors(l, r);
        delay(15);
    }
#endif
    // spin-up is done, now we set the real value
    set_motors(ccw, cw);
}

/* Function that will controls the movement of the kilobots
 */
void set_motion(motion_t new_motion)
{
    switch(new_motion) {
        case STOP:
            smooth_set_motors(0,0);
            break;
        case FORWARD:
            smooth_set_motors(kilo_straight_left, kilo_straight_right);
            break;
        case LEFT:
            smooth_set_motors(kilo_turn_left, 0);
            break;
        case RIGHT:
            smooth_set_motors(0, kilo_turn_right); 
            break;
    }
}

/* Test method to check if the distance is not farther than 90 units
 */
char in_interval(uint8_t distance)
{
    //if (distance >= 40 && distance <= 60)
    if (distance <= 90)
        return 1;
    return 0;
}

/* No clue, probably to test to make sure it's not acting crazy?
 */
char is_stabilized()
{
    uint8_t i=0,j=0;
    for (i=0; i<mydata->num_neighbors; i++)
    {
        
        if ((mydata->nearest_neighbors[i].state == AUTONOMOUS && mydata->nearest_neighbors[i].num > 2) ||
            (mydata->nearest_neighbors[i].state == COOPERATIVE && mydata->nearest_neighbors[i].num_cooperative > 2))
            j++;
    }

    return j == mydata->num_neighbors;
}

/* Search for id in the neighboring nodes
 */
uint8_t exists_nearest_neighbor(uint8_t id)
{
    uint8_t i;
    for (i=0; i<mydata->num_neighbors; i++)
    {
        if (mydata->nearest_neighbors[i].id == id)
            return i;
    }
    return i;
}


/* Search for id in the neighboring nodes
 */
uint8_t are_all_cooperative()
{
    uint8_t i;
    for (i=0; i<mydata->num_neighbors; i++)
    {
        if (mydata->nearest_neighbors[i].state == COOPERATIVE)
            return 0;
    }
    return 1;
}

/* Find the two nearest node to the node that is being focused on
 */
uint8_t get_nearest_two_neighbors()
{
    uint8_t i, l, k;
    uint16_t min_sum = 0xFFFF;
    
    k = i = mydata->num_neighbors;
    if (are_all_cooperative())
    {
        for (i=0; i<mydata->num_neighbors; i++)
        {
            // shortest
            if (mydata->nearest_neighbors[i].distance < min_sum)
            {
                k = i;
            }
        }
        if (k < mydata->num_neighbors)
        {
            i = k;
        }
    }
    else
    {
        for (i=0; i<mydata->num_neighbors; i++)
        {
            // Is it cooperative and at distance in [4cm,6cm]?
            if (mydata->nearest_neighbors[i].state == COOPERATIVE)
            {
                l = exists_nearest_neighbor(mydata->nearest_neighbors[i].right_id);
                // Does the right exits in my table?
                if (l < mydata->num_neighbors)
                {
                    if (mydata->nearest_neighbors[i].distance +
                        mydata->nearest_neighbors[l].distance < min_sum)
                    {
                        min_sum = mydata->nearest_neighbors[i].distance + mydata->nearest_neighbors[l].distance;
                        k = i;
                    }
                }
            }
        }
        if (k < mydata->num_neighbors)
        {
            i = k;
        }
    }
    return i;
}


/*This functions works to reset the bots when they 
 have broken the ring
  */ 
void reset_self()
{

    //printf("%d RESET\n", mydata->my_id);
    
    mydata->state = AUTONOMOUS;
    mydata->my_left = mydata->my_right = mydata->my_id;
    mydata->num_neighbors = 0;
    
    mydata->red = 0;
    mydata->green = 0;
    mydata->blue = 0;
    
    mydata->master = mydata->my_id;
}

/*This function is called when message_recv_delay is > than X time
Specific to ring
 */
void remove_neighbor(nearest_neighbor_t lost)
{
    uint8_t lost_bot_index;
    lost_bot_index = exists_nearest_neighbor(lost.id);
    
    if (lost.id == mydata->my_right)
    {
        if (exists_nearest_neighbor(lost.right_id) < mydata->num_neighbors)
        {
            mydata->my_right = lost.right_id;
            if (lost.is_master == 1)
            {
                mydata->master = 1;
            }
        }
        else
        {
            reset_self();
            return;
        }
    }
    if (lost.id == mydata->my_left)
    {
        if (exists_nearest_neighbor(lost.left_id) < mydata->num_neighbors)
        {
            mydata->my_left = lost.left_id;
        }
        else
        {
            reset_self();
            return;
        }
    }
    mydata->nearest_neighbors[lost_bot_index] = mydata->nearest_neighbors[mydata->num_neighbors-1];    
    mydata->num_neighbors--;
}


/* Passes the message onto the next node, which is the tail of the current node
 */
char enqueue_message(uint8_t m)
{
#ifdef SIMULATOR
    //printf("%d, Prepare %d\n", mydata->my_id, m);
#endif

	
    if (!isQueueFull())
    {
        mydata->message[mydata->tail].data[MSG] = m;
        mydata->message[mydata->tail].data[ID] = mydata->my_id;
        mydata->message[mydata->tail].data[RIGHT_ID] = mydata->my_right;
        mydata->message[mydata->tail].data[LEFT_ID] = mydata->my_left;
        mydata->message[mydata->tail].data[RECEIVER] = mydata->my_right;
        mydata->message[mydata->tail].data[SENDER] = mydata->my_id;
        mydata->message[mydata->tail].data[STATE] = mydata->state;
        //Sending Color Data 
		mydata->message[mydata->tail].data[COLOR] = RGB(mydata->red,mydata->green,mydata->blue);
        //Sending Master Statues 
        mydata->message[mydata->tail].data[MASTER] = mydata->master;
    
        mydata->message[mydata->tail].type = NORMAL;
        mydata->message[mydata->tail].crc = message_crc(&mydata->message[mydata->tail]);
		mydata->message_sent = 0;
        mydata->tail++;
        mydata->tail = mydata->tail % QUEUE;
        return 1;
    }
    return 0;
}

/********************************/
/*       Recieve Function       */
/********************************/

/* When a node recieves a message from another node
 */
void message_rx(message_t *m, distance_measurement_t *d)
{
    uint8_t dist = estimate_distance(d);
    
    if (m->type == NORMAL && m->data[MSG] !=NULL_MSG)
    {
        
#ifdef SIMULATOR
        //printf("%d Receives %d %d\n", mydata->my_id,  m->data[MSG], m->data[RECEIVER]);
#endif
   
        recv_sharing(m->data, dist);
        switch (m->data[MSG])
        {
            case JOIN:
                recv_joining(m->data);
                break;
            case MOVE:
                //recv_move(m->data);
                break;
            case ELECTION:
		if(m->data[ID] == mydata->my_left){
                recv_election(m->data);
                break;
       		} 
        }
    }
}

/* not sure what sharing would do in particular
 */
void recv_sharing(uint8_t *payload, uint8_t distance)
{
    if (payload[ID] == mydata->my_id  || payload[ID] == 0 || !in_interval(distance) ) return;
    
    mydata->loneliness = 0;

    uint8_t i = exists_nearest_neighbor(payload[ID]);
    if (i >= mydata->num_neighbors) // The id has never received
    {
        if (mydata->num_neighbors < MAX_NUM_NEIGHBORS)
        {
            i = mydata->num_neighbors;
            mydata->num_neighbors++;
            mydata->nearest_neighbors[i].num = 0;
            
        }
    }

    
    mydata->nearest_neighbors[i].id = payload[ID];
    mydata->nearest_neighbors[i].right_id = payload[RIGHT_ID];
    mydata->nearest_neighbors[i].left_id = payload[LEFT_ID];
    mydata->nearest_neighbors[i].state = payload[STATE];
    mydata->nearest_neighbors[i].distance = distance;
    
    mydata->nearest_neighbors[i].message_recv_delay = 0;
    mydata->nearest_neighbors[i].is_master = payload[MASTER];
    
    if (payload[STATE] == AUTONOMOUS)
    {
        mydata->nearest_neighbors[i].num++;
        mydata->nearest_neighbors[i].num_cooperative = 0;
    }
    else
    {
        mydata->nearest_neighbors[i].num_cooperative++;
        mydata->nearest_neighbors[i].num = 0;
    }

	//printf("%d color code %d\n", payload[SENDER], RGB(0,3,0)); 
	//update_color(payload);

}

/* Acknowledges when a bot has joined the group
 */
void recv_joining(uint8_t *payload)
{
    //ignoring irrelevant messages
    if (payload[ID] == mydata->my_id  || payload[ID] == 0 ) return;


    //if sender set me as left, set sender as my right
    if (payload[LEFT_ID] == mydata->my_id)
    {
    	mydata->my_right = payload[SENDER];
		mydata->state = COOPERATIVE;
    }
    //if sender set me as right, set sender as my left
    if (payload[RIGHT_ID] == mydata->my_id)
    {
	    mydata->my_left = payload[SENDER];
		mydata->state = COOPERATIVE;
    }


	
    // Creates a "master" bot upon ring creation. 
    // Also boolean switch for master
    if (mydata->my_left == mydata->my_right)
    {
		if (mydata->my_id < payload[SENDER]){
			mydata->master = mydata->my_id;
			mydata->state = COOPERATIVE;
		} else {
			mydata->master = payload[SENDER];
			mydata->state = COOPERATIVE;
		}
    }
	if(mydata->state == COOPERATIVE){
		mydata->red = 255;
	} else {
		mydata->red = 0;
	}
#ifdef SIMULATOR
    printf("%d Left: %d Right: %d\n", mydata->my_id, mydata->my_left, mydata->my_right);
#endif
}

/* When the bot acknowledges it's position within the group
 */
void recv_election(uint8_t *payload){
	//printf("my id = %d\n", mydata->my_id);
	//printf("master id = %d\n", payload[MASTER]);
	
	
	if (payload[MASTER] < mydata->master){ //If the leader id is less than the ID
		//printf("test1\n");
		mydata->master = payload[MASTER];
		mydata->red = 255;
		mydata->blue = 0;
		mydata->green = 0;
	} else if (payload[MASTER] > mydata->my_id){ //If the leader id is greater than the ID
		//printf("test2\n");
		mydata->red = 255;
		mydata->blue = 0;
		mydata->green = 0;
		mydata->master = mydata->my_id;
	} else if (mydata->my_id == payload[MASTER]){ //Will stop the message passing
		//printf("test3\n");
		mydata->pass_election = 0;
		mydata->red = 255;
		mydata->blue = 255;
		mydata->green = 255;
	}
}


/**********************************/
/*         SEND FUNCTIONS         */
/**********************************/

/* Sends a message to the next node
 */
message_t *message_tx()
{
    
    if (mydata->tail != mydata->head)   // Queue is not empty
    {
#ifdef SIMULATOR
    //printf("%d, Sending  %d  %d\n", mydata->my_id, mydata->message[mydata->head].data[MSG] , mydata->message[mydata->head].data[RECEIVER]);
#endif
	    
	    mydata->message_sent = 1;
        return &mydata->message[mydata->head];
    }
    return &mydata->nullmessage;
}
 
/* Ackknowledges when a message has been sent and recieved
 */
void message_tx_success() {
    if (mydata->tail != mydata->head) {  // Queue is not empty
#ifdef SIMULATOR
        //printf("%d Sent  %d,  %d\n", mydata->my_id, mydata->message[mydata->head].data[MSG], mydata->message[mydata->head].data[RECEIVER]);
#endif
        if (mydata->copies == 2)
        {
            mydata->head++;
            mydata->copies = 0;
            mydata->head = mydata->head % QUEUE;
			mydata->message_sent = 1;
        }
        else
        {
            mydata->copies++;
			mydata->message_sent = 1;
        }
    }
}

/* Send a message to the neighbor that it's joining the group
 */
void send_joining()
{
    uint8_t i;
    /* precondition  */
        
    if (mydata->state == AUTONOMOUS && is_stabilized()  && !isQueueFull())

    {
	
        i = get_nearest_two_neighbors();
	
        if (i < mydata->num_neighbors && mydata->message_sent == 1)
        {
            // effect:
            mydata->state = COOPERATIVE;
            mydata->my_right = mydata->nearest_neighbors[i].right_id;
            mydata->my_left = mydata->nearest_neighbors[i].id;
            enqueue_message(JOIN);
			//mydata->pass_election = 1;
#ifdef SIMULATOR
            printf("Sending Joining %d right=%d left=%d\n", mydata->my_id, mydata->my_right, mydata->my_left);
#endif
        }
    }
}

/* Not entirely sure what sharing does
 */
void send_sharing()
{
    // Precondition
    if (mydata->now >= mydata->nextShareSending  && !isQueueFull())
    {
        // Sending
        enqueue_message(SHARE);
        // effect:
        mydata->nextShareSending = mydata->now + SHARING_TIME;
    }
}

/* Initialize the message sending
 */
void send_election()
{
	if (mydata->state == COOPERATIVE)
//if(!isQueueFull() && mydata->state == COOPERATIVE)
    {
		enqueue_message(ELECTION);
        mydata->pass_election = 0;
    }
}


/**********************************/
/*           Core function        */
/**********************************/

/* Constantly have the bots communicate in order to stay updated
 */
void loop()
{
    delay(5);
    send_joining();
    send_election();
    send_sharing();
//	send_election();
	

    
    uint8_t i;
    for (i = 0; i < mydata->num_neighbors; i++)
    {
        mydata->nearest_neighbors[i].message_recv_delay++;

        if (mydata->nearest_neighbors[i].message_recv_delay > 100)
        {
            remove_neighbor(mydata->nearest_neighbors[i]);
            break;
        }
    } 
    
    set_color(RGB(mydata->red, mydata->green, mydata->blue));

    mydata->loneliness++;
    
    if (mydata->loneliness > 100)
    {
        reset_self();
    }
    mydata->now++;
}

/* Creates the bot "configuration"
 */
void setup() {
    rand_seed(rand_hard());

    mydata->my_id = rand_soft();
    
    mydata->state = AUTONOMOUS;
    mydata->my_left = mydata->my_right = mydata->my_id;
    mydata->num_neighbors = 0;
    mydata->message_sent = 0,
    mydata->now = 0,
    mydata->nextShareSending = SHARING_TIME,
    mydata->cur_motion = STOP;
    mydata->motion_state = STOP;
    mydata->time_active = 0;
    mydata->move_state = 0;
    mydata->master = mydata->my_id;  //Set Master to 0
    mydata->move_motion[0].motion = LEFT;
    mydata->move_motion[0].motion = 3;
    mydata->move_motion[1].motion = RIGHT;
    mydata->move_motion[1].motion = 5;
    mydata->move_motion[0].motion = LEFT;
    mydata->move_motion[0].motion = 2;
    mydata->red = 0,
    mydata->green = 0,
    mydata->blue = 0,
    mydata->send_token = 0;
	mydata->pass_election = 0;
	mydata->master = mydata->my_id;

    mydata->nullmessage.data[MSG] = NULL_MSG;
    mydata->nullmessage.crc = message_crc(&mydata->nullmessage);
    
    mydata->token = rand_soft() < 128  ? 1 : 0;
    //mydata->blue = mydata->token;
    mydata->head = 0;
    mydata->tail = 0;
    mydata->copies = 0;
    

   
#ifdef SIMULATOR
    printf("Initializing %d %d\n", mydata->my_id, kilo_uid);
#endif

    mydata->message_sent = 1;
}

#ifdef SIMULATOR
/* provide a text string for the simulator status bar about this bot */
static char botinfo_buffer[10000];
char *cb_botinfo(void)
{
    char *p = botinfo_buffer;
    p += sprintf (p, "ID: %d \n", kilo_uid);
    if (mydata->state == COOPERATIVE)
        p += sprintf (p, "State: COOPERATIVE\n");
    if (mydata->state == AUTONOMOUS)
        p += sprintf (p, "State: AUTONOMOUS\n");
    
    return botinfo_buffer;
}
#endif

int main() {
    kilo_init();
    kilo_message_tx = message_tx;
    kilo_message_tx_success = message_tx_success;
    kilo_message_rx = message_rx;
    kilo_start(setup, loop);
    
    return 0;
}

/*==========================*/
/*       UNUSED METHOD      */
/*==========================*/


/*
void send_move()
{
    // Precondition:
    if (mydata->state == COOPERATIVE  && mydata->token )
    {
        mydata->send_token = mydata->now + TOKEN_TIME;
    }
    if (mydata->state == COOPERATIVE && !isQueueFull() && mydata->token && mydata->send_token <= mydata->now)
    {
            // Sending
        enqueue_message(MOVE);
        mydata->token = 0;
        mydata->blue = 0;
        // effect:
    }

}

void move(uint8_t tick)
{
    // Precondition:
    if (mydata->motion_state == ACTIVE && mydata->state == COOPERATIVE)
    {
        
      /  if (mydata->time_active == mydata->move_motion[mydata->move_state].time)
        {
            // Effect:
            mydata->green = 1;
            mydata->move_state++;
            if (mydata->move_state == 3)
            {
                mydata->send_token = 1;
                send_move();
#ifdef SIMULATOR
                printf("Sending Move %d\n", mydata->my_id);
#endif
                mydata->motion_state = STOP;
                return;
            }
            mydata->time_active = 0;
        
        }
        set_motion(mydata->move_motion[mydata->move_state].motion);
        mydata->time_active++;
       /
    }
    else
    {
        mydata->green = 0;
        set_motion(STOP);
    }
    
}
*/


/*void recv_move(uint8_t *payload)
{
#ifdef SIMULATOR
    //printf("%d Receives move %d %d %d\n", mydata->my_id, payload[MSG], mydata->my_id, payload[RECEIVER]);
#endif
    
    if (mydata->my_id == payload[RECEIVER])
    {
        mydata->token  = 1;
        mydata->blue  = 1;
        mydata->send_token = mydata->now + TOKEN_TIME * 4.0;

    }
   / else if (my_id == payload[SENDER])
    {
        mydata->motion_state = STOP;
    }
    else
    {
        mydata->msg.data[MSG]      = payload[MSG];
        mydata->msg.data[ID]       = mydata->my_id;
        mydata->msg.data[RECEIVER] = payload[RECEIVER];
        mydata->msg.data[SENDER]   = payload[SENDER];
        mydata->msg.type           = NORMAL;
        mydata->msg.crc            = message_crc(&msg);
        mydata->message_sent       = 0;
    } /
}
*/


/*
void update_color(uint8_t *payload)
{
    if (mydata->master == 0)
    {
    	if (payload[SENDER] == mydata->my_right)
    	{
    		if (payload[COLOR] == (RGB(0,3,0)))
    		{
    			mydata->green = 3;
    		}
    		else
    		{
    			mydata->green = 0;
    		}
    	}
    }
}
*/

