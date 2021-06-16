# spring-redis-messaging

In Redis, publishers are not programmed to send their messages to specific subscribers. Rather, published messages are characterized into channels, without knowledge of what (if any) subscribers there may be.

Similarly, subscribers express interest in one or more topics and only receive messages that are of interest, without knowledge of what (if any) publishers there are.

This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.

## Redis Configuration
Let's start adding the configuration which is required for the message queues.

First, we'll define a MessageListenerAdapter bean which contains a custom implementation of the MessageListener interface called RedisMessageSubscriber. This bean acts as a subscriber in the pub-sub messaging model:

```javascript
@Bean
public MessageListenerAdapter getMessageListenerAdapter()
{
   return new MessageListenerAdapter(new Receiver());
}
```
RedisMessageListenerContainer is a class provided by Spring Data Redis which provides asynchronous behavior for Redis message listeners. This is called internally and, according to the Spring Data Redis documentation – “handles the low level details of listening, converting and message dispatching.”

```javascript
@Bean
public RedisMessageListenerContainer getRedisMessageListenerContainer()
{
   RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
   redisMessageListenerContainer.setConnectionFactory(getRedisConnectionFactory());
   redisMessageListenerContainer.addMessageListener(getMessageListenerAdapter(),getChannelTopic());
   return redisMessageListenerContainer;
}
```
We will also create a bean using a custom-built MessagePublisher interface and a RedisMessagePublisher implementation. This way, we can have a generic message-publishing API, and have the Redis implementation take a redisTemplate and topic as constructor arguments:

```javascript
@Autowired
private RedisTemplate redisTemplate;

@Autowired
private ChannelTopic channelTopic;

@PostMapping(path = "/publisher")
public String publish(@RequestBody Product product)
{
    redisTemplate.convertAndSend(channelTopic.getTopic(),product.toString());
    return "Event Published !!!";
}
```
Finally, we'll set up a topic to which the publisher will send messages, and the subscriber will receive them:

```javascript
@Bean
public ChannelTopic getChannelTopic()
{
    return new ChannelTopic("manav-verma-publisher");
}
```
## Publishing Messages

Defining the MessagePublisher Interface
Spring Data Redis does not provide a MessagePublisher interface to be used for message distribution. We can define a custom interface which will use redisTemplate in implementation:

```javascript
public interface MessagePublisher {
    void publish(String message);
}
```
RedisMessagePublisher Implementation
Our next step is to provide an implementation of the MessagePublisher interface, adding message publishing details and using the functions in redisTemplate.

The template contains a very rich set of functions for wide range of operations – out of which convertAndSend is capable of sending a message to a queue through a topic:

```javascript
public class RedisMessagePublisher implements MessagePublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ChannelTopic topic;

    public RedisMessagePublisher() {
    }

    public RedisMessagePublisher(
      RedisTemplate<String, Object> redisTemplate, ChannelTopic topic) {
      this.redisTemplate = redisTemplate;
      this.topic = topic;
    }

    public void publish(String message) {
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }
}
```
As you can see, the publisher implementation is straightforward. It uses the convertAndSend() method of the redisTemplate to format and publish the given message to the configured topic.

A topic implements publish and subscribe semantics: when a message is published, it goes to all the subscribers who are registered to listen on that topic.

## Subscribing to Messages
RedisMessageSubscriber implements the Spring Data Redis-provided MessageListener interface:

```javascript
@Service
public class RedisMessageSubscriber implements MessageListener {

    public static List<String> messageList = new ArrayList<String>();

    public void onMessage(Message message, byte[] pattern) {
        messageList.add(message.toString());
        System.out.println("Message received: " + message.toString());
    }
}
```
Note that there is a second parameter called pattern, which we have not used in this example. The Spring Data Redis documentation states that this parameter represents the, “pattern matching the channel (if specified)”, but that it can be null.

## Sending and Receiving Messages
Now we'll put it all together. Let's create a message and then publish it using the RedisMessagePublisher:

```javascript
String message = "Message " + UUID.randomUUID();
redisMessagePublisher.publish(message);
```
When we call publish(message), the content is sent to Redis, where it is routed to the message queue topic defined in our publisher. Then it is distributed to the subscribers of that topic.

You may already have noticed that RedisMessageSubscriber is a listener, which registers itself to the queue for retrieval of messages.

On the arrival of the message, the subscriber's onMessage() method defined triggered.

In our example, we can verify that we've received messages that have been published by checking the messageList in our RedisMessageSubscriber:

```javascript
RedisMessageSubscriber.messageList.get(0).contains(message)
```

## Conclusion
In this article, we examined a pub/sub message queue implementation using Spring Data Redis.
