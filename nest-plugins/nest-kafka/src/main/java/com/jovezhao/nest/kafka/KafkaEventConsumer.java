package com.jovezhao.nest.kafka;

import com.jovezhao.nest.ddd.event.provider.distribut.EventDataProcessor;
import com.jovezhao.nest.ddd.event.provider.distribut.DistributedEventConsumer;
import com.jovezhao.nest.ddd.event.provider.distribut.EventData;
import com.jovezhao.nest.utils.JsonUtils;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by zhaofujun on 2017/6/22.
 */
public class KafkaEventConsumer extends DistributedEventConsumer<KafkaProviderConfig> {
    private ConsumerConnector createConsumer() {
        Properties properties = new Properties();
        properties.put("zookeeper.connect", this.getProviderConfig().getZk());
        properties.put("group.id", this.getEventHandler().getHandlerName());
        properties.put("zookeeper.session.timeout.ms", 400);
        properties.put("zookeeper.sync.time.ms", 200);
        properties.put("auto.commit.enable", false);

        return Consumer.createJavaConsumerConnector(new ConsumerConfig(properties));
    }

    ConsumerConnector consumer;

    @Override
    protected void init() {
        consumer = createConsumer();
    }

    @Override
    protected void consume() throws Exception {
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(this.getEventHandler().getEventName(), 1); // 一次从主题中获取一个数据
        Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = consumer.createMessageStreams(topicCountMap);

        KafkaStream<byte[], byte[]> stream = messageStreams.get(this.getEventHandler().getEventName()).get(0);// 获取每次接收到的这个数据
        ConsumerIterator<byte[], byte[]> iterator = stream.iterator();
        while (iterator.hasNext()) {
            String json = new String(iterator.next().message());
            EventData eventData = JsonUtils.toObj(json, EventData.class);
            EventDataProcessor processor = new EventDataProcessor(eventData, this.getEventHandler());
            processor.process();
        }
        consumer.commitOffsets();

        Thread.sleep(500);

    }

    @Override
    protected void dispose() {
        consumer.shutdown();
    }


}
