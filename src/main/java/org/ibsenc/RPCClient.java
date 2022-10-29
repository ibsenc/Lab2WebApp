package org.ibsenc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

public class RPCClient implements AutoCloseable {

  private Connection connection;
  private Channel channel;
  private String requestQueueName = "rpc_queue";

  public RPCClient(Connection connection, Channel channel) throws IOException, TimeoutException {
    this.connection = connection;
    this.channel = channel;
  }

  public String call(String message) throws IOException, InterruptedException, ExecutionException {
    final String corrId = UUID.randomUUID().toString();

    String replyQueueName = channel.queueDeclare().getQueue();
    AMQP.BasicProperties props = new AMQP.BasicProperties
        .Builder()
        .correlationId(corrId)
        .replyTo(replyQueueName)
        .build();

    channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

//    System.out.println("Adding liftRide to queue with corrId: " + corrId);

    final CompletableFuture<String> response = new CompletableFuture<>();

    String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
      if (delivery.getProperties().getCorrelationId().equals(corrId)) {
        response.complete(new String(delivery.getBody(), "UTF-8"));
      }
    }, consumerTag -> {
    });

    String result = response.get();
    channel.basicCancel(ctag);
    return result;
  }

  public void close() throws IOException {
    connection.close();
  }
}

