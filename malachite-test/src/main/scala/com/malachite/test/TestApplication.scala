package com.malachite.test

import com.malachite.core.{Application, Environment, Guid}
import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.security.cryptography.{HashProvider, HybridEncryptionProvider}
import com.malachite.core.threading.{AsyncTask, LimitedAsyncTaskExecutor}
import org.apache.logging.log4j.{Level, LogManager}
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.kafka.{KafkaClient, KafkaRecord}
import com.malachite.core.security.authentication.apiclient.{ApiClientAuthenticationServiceImpl, ApiClientValidationRequestImpl}
import com.malachite.core.text.{Encoding, JsonSerializer}
import com.malachite.core.utilities.{FileSystemUtilities, ThreadUtilities}
import com.malachite.core.web.http.server.{WebSocketMessage, WebSocketMessageType}

import java.util.Properties
import scala.jdk.CollectionConverters.*

class TestApplication private extends Application {

    private val logger = LogManager.getLogger(getClass)
    @volatile
    private var stop: Boolean = false

    override def initialize(): Unit = {
        logger.log(Level.INFO, "Initializing Test application.")
        logger.log(Level.INFO, "Test application initialization successful.")
    }

    override def execute(): Unit = {
        val configuration = ConfigurationProvider.getConfiguration.kafkaClient
        val kafkaClient = Environment.kafkaClient
        kafkaClient.subscribe(MyEventListener())

        kafkaClient.publish(KafkaRecord("my-topic", "World"))
        kafkaClient.publish(KafkaRecord("my-topic", "World 2"))
        kafkaClient.publish(KafkaRecord("my-topic", "World 4"))
        kafkaClient.publish(KafkaRecord("my-topic", "World 5"))
        kafkaClient.publish(KafkaRecord("my-topic", "World 6"))

        ThreadUtilities.trySleep(60_000)

        // kafkaClient.close()

       /* new Thread(() => {
            ThreadUtilities.trySleep(10_000)

            stop = true
            kafkaClient.close()

            println("KAFKA CLIENT STOPPED")
        }).start()*/

        /*val s = MyEventListener2()

        new Thread(() => {
            ThreadUtilities.trySleep(5_000)

            kafkaClient.subscribe(MyEventListener())
            kafkaClient.subscribe(s)

            println("SUBSCRIBED TO TOPICS")
        }).start()

        new Thread(() => {
            ThreadUtilities.trySleep(1_000)

            while (!stop) {
                ThreadUtilities.trySleep(1000)

                kafkaClient.publish(KafkaRecord(
                    topic = "my-topic",
                    key = "hello",
                    value = "hello",
                ))
            }
        }).start()

        new Thread(() => {
            ThreadUtilities.trySleep(1_000)

            while (!stop) {
                ThreadUtilities.trySleep(2000)

                kafkaClient.publish(KafkaRecord(
                    topic = "my-topic-2",
                    key = "hello-2",
                    value = "hello-2",
                ))
            }
        }).start()

        new Thread(() => {
            ThreadUtilities.trySleep(15_000)

            kafkaClient.unsubscribe(s)

            println("UNSUBSCRIBED FROM TOPIC")

            ThreadUtilities.trySleep(15_000)

            kafkaClient.subscribe(s)

            println("SUBSCRIBED TO TOPIC +++++++++++++++++++++++++++++++++++++++++++++")
        }).start()

        kafkaClient
            .start
            .await()*/

        /*for (i <- 0 until 10) {
            println(Guid.newGuid(version = 7))
        }*/

        /*val reusableObjectPool = ReusableObjectPool((index, pool, options) => {
            MyReusableObject(index, pool, s"REUSABLE $index")
        }, maximumReusableObjectCountOpt = Some(1000))

        val xList = new java.util.concurrent.ConcurrentLinkedQueue[ReusableObject]

        new Thread(() => {
            while (true) {
                val t = xList.poll()

                if (t != null) {
                    t.release()
                } else {
                    ThreadUtilities.trySleep(1000)
                }
            }


        }).start()

        for (i <- 0 until 100) {
            val x = reusableObjectPool.reusableObject
            xList.add(x)

            println(x)
        }*/





        /*val msg = WebSocketMessage(
            messageType = WebSocketMessageType.PING,
            nodeId = Some(Environment.instanceId),
            owner = "visena",
            senderConnectionUuid = Some(java.util.UUID.randomUUID().toString),
            receiverConnectionUuids = Set(java.util.UUID.randomUUID().toString, java.util.UUID.randomUUID().toString),
            uniqueIds = Set(java.util.UUID.randomUUID().toString, java.util.UUID.randomUUID().toString),
            contexts = Set("XF3Z", "RVAS"),
            topics = Map("XF3Z" -> Set("DOCUMENT_SIGNING", "CUSTOMER_CONTROL")),
            sendToSelf = true,
            payload = None,
            payloadAsObject = Map("hello" -> "world", "age" -> 10),
        )
        val msgAsJson = msg.toString
        val x = WebSocketMessage(msgAsJson)

        println(x)*/

        /*val x = Environment.apiClientSystemCredentials
        val x2 = new ApiClientAuthenticationServiceImpl()
        val t1 = x2.authenticateSystem
        val t2 = x2.authenticateSystem
        val t3 = x2.authenticateSystem
        val t4 = x2.validate(ApiClientValidationRequestImpl(
            apiKey = t3.apiKey,
            token = t3.token,
        ))*/

        // println(t4)



        /*val a = new ApiClientAuthenticationServiceImpl
        val xy = a.authenticateAsSystem

        val passwordMatched = HashProvider.computeHash(
            ("ULlMfOM4owW2-9LkmdhxtU77GoCDXfk-BnlTPV6XdkGAFffSdvUHIkLq_58Eyk0bHul3XfXqr3xMYO0NieBD_Q" +
            "8@#NcdOoxS#+ZWziXGuR0ToVVqqEM6WYDT5bVcKzrx8PjhZhnH4TE6ZcHB8kbRymDTP7HjC9zxcURxDokfNL4w_v").decode(Encoding.UTF_8),
            Environment.secretKey,
        ).encode(Encoding.URL_SAFE_BASE_64)

        println(passwordMatched)*/


        logger.log(Level.INFO, "Executing Test application.")
        logger.log(Level.INFO, "The quick brown fox jumps over the lazy dog.")
        logger.log(Level.INFO, "Test application execution completed.")
    }
}
