package com.example.consumer;

import com.example.JsonUtils;
import com.example.models.Stock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class JsonConsumer {

    private KafkaConsumer<String, String> consumer;
    private AtomicBoolean stopFlag = new AtomicBoolean(false);

    public void init(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "JsonConsumer");
        props.put("auto.offset.reset", "earliest"); // Only applicable new consumer group

        /* Enable following two properties for auto commit */
        props.put("enable.auto.commit", "false");
        props.put("auto.commit.interval.ms", "2000");

        props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");
        props.put("max.poll.records", "1000");
        props.put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
    }

    public void start(){
        this.init();
        String topic = "stocks-json";
        consumer.subscribe(Collections.singleton(topic));
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        Stock stock  = null;
        while (!stopFlag.get()) {
            long startTime = System.currentTimeMillis();
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
            System.out.println("Actual wait for new messages: " + (System.currentTimeMillis() - startTime));
            int count = consumerRecords.count();
            Iterator<ConsumerRecord<String, String>> iterator = consumerRecords.iterator();
            while (iterator.hasNext()){
                ConsumerRecord<String, String> message = iterator.next();
                String topic1 = message.topic();
                String value = message.value();
                String key = message.key();
                Headers headers = message.headers();
                long offset = message.offset();
                long timestamp = message.timestamp();
                try {
                    stock = objectMapper.readValue(value, Stock.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                stock.getClose();
                message.timestampType();
            }
            consumer.commitSync();
        }
    }


    public static void main(String[] args) throws Exception {
        new JsonConsumer().start();
    }
}
