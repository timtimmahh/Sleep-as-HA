package com.timmahh.sleepasha.net

import com.hivemq.client.mqtt.*
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilderBase
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5WillPublish
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory

fun mqttClient(block: MqttClientDsl.() -> Unit) = MqttClientDsl().apply(block).build()

class MqttClientDsl {

    internal lateinit var identifier: String
    internal lateinit var executorConfig: MqttClientExecutorConfig
    internal lateinit var serverAddress: InetSocketAddress
    internal lateinit var serverHost: String
    internal var serverPort: Int? = null
    private lateinit var _sslConfig: MqttSslConfigDsl.() -> Unit
    private lateinit var _webSocketConfig: MqttWebSocketConfigDsl.() -> Unit
    private lateinit var _transportConfig: MqttTransportConfigDsl.() -> Unit
    private lateinit var _automaticReconnect: MqttAutomaticReconnectDsl.() -> Unit
    private val connectedListeners = mutableListOf<(MqttClientState) -> Unit>()
    private val disconnectedListeners = mutableListOf<(MqttClientState) -> Unit>()

    internal fun sslConfig(block: MqttSslConfigDsl.() -> Unit) {
        _sslConfig = block
    }

    internal fun webSocketConfig(block: MqttWebSocketConfigDsl.() -> Unit) {
        _webSocketConfig = block
    }

    internal fun transportConfig(block: MqttTransportConfigDsl.() -> Unit) {
        _transportConfig = block
    }

    internal fun automaticReconnect(block: MqttAutomaticReconnectDsl.() -> Unit) {
        _automaticReconnect = block
    }

    internal fun onConnected(onConnect: (MqttClientState) -> Unit) {
        connectedListeners += onConnect
    }

    internal fun onDisconnect(onDisconnect: (MqttClientState) -> Unit) {
        disconnectedListeners += onDisconnect
    }

    fun build() = Mqtt5Client.builder()
        .also { builder ->
            if (::identifier.isInitialized)
                builder.identifier(identifier)
            if (::executorConfig.isInitialized)
                builder.executorConfig(executorConfig)
            if (::serverAddress.isInitialized)
                builder.serverAddress(serverAddress)
            if (::serverHost.isInitialized)
                builder.serverHost(serverHost)
            if (serverPort != null)
                builder.serverPort(serverPort!!)
            if (::_sslConfig.isInitialized)
                builder.sslConfig(mqttSslConfig(_sslConfig))
            if (::_webSocketConfig.isInitialized)
                builder.webSocketConfig(mqttWebSocketConfig(_webSocketConfig))
            if (::_transportConfig.isInitialized)
                builder.transportConfig(mqttTransportConfig(_transportConfig))
            if (::_automaticReconnect.isInitialized)
                builder.automaticReconnect(mqttAutomaticReconnect(_automaticReconnect))
            for (listener in connectedListeners)
                builder.addConnectedListener {
                    listener(it.clientConfig.state)
                }
            for (listener in disconnectedListeners)
                builder.addDisconnectedListener {
                    listener(it.clientConfig.state)
                }
        }
        .buildBlocking()

    private fun mqttSslConfig(block: MqttSslConfigDsl.() -> Unit) = MqttSslConfigDsl().apply(block).build()
    private fun mqttWebSocketConfig(block: MqttWebSocketConfigDsl.() -> Unit) =
        MqttWebSocketConfigDsl().apply(block).build()

    private fun mqttTransportConfig(block: MqttTransportConfigDsl.() -> Unit) =
        MqttTransportConfigDsl().apply(block).build()

    private fun mqttAutomaticReconnect(block: MqttAutomaticReconnectDsl.() -> Unit) =
        MqttAutomaticReconnectDsl().apply(block).build()
}

class MqttSslConfigDsl() {
    internal lateinit var keyManagerFactory: KeyManagerFactory
    internal lateinit var trustManagerFactory: TrustManagerFactory
    private val cipherSuites = mutableListOf<String>()
    private val protocols = mutableListOf<String>()
    internal lateinit var handshakeTimeout: Pair<Long, TimeUnit>
    private lateinit var _hostnameVerifier: (hostname: String, session: SSLSession) -> Boolean


    internal fun cipherSuite(value: String) {
        cipherSuites += value
    }

    internal fun protocol(value: String) {
        protocols += value
    }

    internal fun hostnameVerifier(block: (hostname: String, session: SSLSession) -> Boolean) {
        _hostnameVerifier = block
    }

    fun build() = MqttClientSslConfig.builder()
        .also { builder ->
            if (::keyManagerFactory.isInitialized)
                builder.keyManagerFactory(keyManagerFactory)
            if (::trustManagerFactory.isInitialized)
                builder.trustManagerFactory(trustManagerFactory)
            if (cipherSuites.isNotEmpty())
                builder.cipherSuites(cipherSuites)
            if (protocols.isNotEmpty())
                builder.protocols(protocols)
            if (::handshakeTimeout.isInitialized)
                builder.handshakeTimeout(handshakeTimeout.first, handshakeTimeout.second)
            if (::_hostnameVerifier.isInitialized)
                builder.hostnameVerifier(_hostnameVerifier)
        }.build()
}

class MqttWebSocketConfigDsl {

    internal lateinit var serverPath: String
    internal lateinit var queryString: String
    internal lateinit var subprotocol: String
    internal lateinit var handshakeTimeout: Pair<Long, TimeUnit>

    fun build() = MqttWebSocketConfig.builder()
        .also { builder ->
            if (::serverPath.isInitialized)
                builder.serverPath(serverPath)
            if (::queryString.isInitialized)
                builder.queryString(queryString)
            if (::subprotocol.isInitialized)
                builder.subprotocol(subprotocol)
            if (::handshakeTimeout.isInitialized)
                builder.handshakeTimeout(handshakeTimeout.first, handshakeTimeout.second)
        }.build()
}

class MqttTransportConfigDsl {

    internal lateinit var serverAddress: InetSocketAddress
    internal lateinit var serverHost: String
    internal var serverPort: Int? = null
    internal lateinit var localAddress: InetSocketAddress
    private lateinit var _sslConfig: MqttSslConfigDsl.() -> Unit
    private lateinit var _webSocketConfig: MqttWebSocketConfigDsl.() -> Unit
    private lateinit var _proxyConfig: MqttProxyConfigDsl.() -> Unit
    internal lateinit var socketConnectTimeout: Pair<Long, TimeUnit>
    internal lateinit var mqttConnectTimeout: Pair<Long, TimeUnit>

    internal fun sslConfig(block: MqttSslConfigDsl.() -> Unit) {
        _sslConfig = block
    }

    internal fun webSocketConfig(block: MqttWebSocketConfigDsl.() -> Unit) {
        _webSocketConfig = block
    }

    internal fun proxyConfig(block: MqttProxyConfigDsl.() -> Unit) {
        _proxyConfig = block
    }

    fun build() = MqttClientTransportConfig.builder()
        .also { builder ->
            if (::serverAddress.isInitialized)
                builder.serverAddress(serverAddress)
            if (::serverHost.isInitialized)
                builder.serverHost(serverHost)
            serverPort?.let(builder::serverPort)
            if (::localAddress.isInitialized)
                builder.localAddress(localAddress)
            if (::_sslConfig.isInitialized)
                builder.sslConfig(mqttSslConfig(_sslConfig))
            if (::_webSocketConfig.isInitialized)
                builder.webSocketConfig(mqttWebSocketConfig(_webSocketConfig))
            if (::_proxyConfig.isInitialized)
                builder.proxyConfig(mqttProxyConfig(_proxyConfig))
            if (::socketConnectTimeout.isInitialized)
                builder.socketConnectTimeout(
                    socketConnectTimeout.first,
                    socketConnectTimeout.second
                )
            if (::mqttConnectTimeout.isInitialized)
                builder.mqttConnectTimeout(mqttConnectTimeout.first, mqttConnectTimeout.second)
        }.build()


    private fun mqttSslConfig(block: MqttSslConfigDsl.() -> Unit) = MqttSslConfigDsl().apply(block).build()
    private fun mqttWebSocketConfig(block: MqttWebSocketConfigDsl.() -> Unit) =
        MqttWebSocketConfigDsl().apply(block).build()

    private fun mqttProxyConfig(block: MqttProxyConfigDsl.() -> Unit) =
        MqttProxyConfigDsl().apply(block).build()
}

class MqttAutomaticReconnectDsl {
    internal lateinit var initialDelay: Pair<Long, TimeUnit>
    internal lateinit var maxDelay: Pair<Long, TimeUnit>

    fun build() = MqttClientAutoReconnect.builder()
        .also { builder ->
            if (::initialDelay.isInitialized)
                builder.initialDelay(initialDelay.first, initialDelay.second)
            if (::maxDelay.isInitialized)
                builder.maxDelay(maxDelay.first, maxDelay.second)
        }.build()
}

class MqttProxyConfigDsl {

    internal lateinit var protocol: MqttProxyProtocol
    internal lateinit var address: InetSocketAddress
    internal lateinit var username: String
    internal lateinit var password: String
    internal lateinit var handshakeTimeout: Pair<Long, TimeUnit>

    fun build() = MqttProxyConfig.builder()
        .also { builder ->
            if (::protocol.isInitialized)
                builder.protocol(protocol)
            if (::address.isInitialized)
                builder.address(address)
            if (::username.isInitialized)
                builder.username(username)
            if (::password.isInitialized)
                builder.password(password)
            if (::handshakeTimeout.isInitialized)
                builder.handshakeTimeout(handshakeTimeout.first, handshakeTimeout.second)
        }.build()
}

fun mqttConnect(block: MqttConnectDsl.() -> Unit) = MqttConnectDsl().apply(block).build()

class MqttConnectDsl {

    internal var cleanStart: Boolean? = null
    internal var sessionExpiryInterval: Long? = null
    internal var keepAlive: Int? = null
    internal lateinit var simpleAuth: Pair<String, String>

    //    internal lateinit var enhancedAuth: MqttEnhancedAuthDsl
    private lateinit var _willPublish: MqttPublishDsl.() -> Unit
    private lateinit var _restrictions: MqttRestrictionsDsl.() -> Unit
    private var userProperties: MutableList<Pair<String, String>> = mutableListOf()

    fun willPublish(block: MqttPublishDsl.() -> Unit) {
        _willPublish = block
    }

    fun restrictions(block: MqttRestrictionsDsl.() -> Unit) {
        _restrictions = block
    }

    fun user(name: String, value: String) {
        userProperties += name to value
    }

    fun user(user: Pair<String, String>) {
        userProperties += user
    }

    fun build() = Mqtt5Connect.builder()
        .also { builder ->
            cleanStart?.let(builder::cleanStart)
            sessionExpiryInterval?.let(builder::sessionExpiryInterval)
            keepAlive?.let(builder::keepAlive)
            if (::simpleAuth.isInitialized)
                builder.simpleAuth(
                    Mqtt5SimpleAuth.builder().username(simpleAuth.first).password
                        (simpleAuth.second.toByteArray()).build()
                )
            if (::_willPublish.isInitialized)
                builder.willPublish(mqttWillPublish(_willPublish))
            if (::_restrictions.isInitialized)
                builder.restrictions(mqttRestrictions(_restrictions))
            userProperties.takeIf { it.isNotEmpty() }?.let { user ->
                builder.userProperties(Mqtt5UserProperties.of(user.map {
                    Mqtt5UserProperty.of(
                        it
                            .first, it.second
                    )
                }))
            }
        }.build()
}

fun mqttRestrictions(block: MqttRestrictionsDsl.() -> Unit) = MqttRestrictionsDsl().apply(block).build()

class MqttRestrictionsDsl {

    internal var receiveMaximum: Int? = null
    internal var sendMaximum: Int? = null
    internal var maximumPacketSize: Int? = null
    internal var sendMaximumPacketSize: Int? = null
    internal var topicAliasMaximum: Int? = null
    internal var sendTopicAliasMaximum: Int? = null
    internal var requestProblemInformation: Boolean? = null
    internal var requestResponseInformation: Boolean? = null

    fun build() = Mqtt5ConnectRestrictions.builder()
        .also { builder ->
            receiveMaximum?.let(builder::receiveMaximum)
            sendMaximum?.let(builder::sendMaximum)
            maximumPacketSize?.let(builder::maximumPacketSize)
            sendMaximumPacketSize?.let(builder::sendMaximumPacketSize)
            topicAliasMaximum?.let(builder::topicAliasMaximum)
            sendTopicAliasMaximum?.let(builder::sendTopicAliasMaximum)
            requestProblemInformation?.let(builder::requestProblemInformation)
            requestResponseInformation?.let(builder::requestResponseInformation)
        }.build()

}

fun mqttPublish(block: MqttPublishDsl.() -> Unit) = MqttPublishDsl().apply(block).build()

fun mqttWillPublish(block: MqttPublishDsl.() -> Unit) = MqttPublishDsl().apply(block).willBuild()

class MqttPublishDsl {

    internal lateinit var topic: String
    internal lateinit var payload: ByteArray
    internal lateinit var qos: MqttQos
    internal var retain: Boolean? = null
    internal var messageExpiryInterval: Long? = null
    internal lateinit var payloadFormatIndicator: Mqtt5PayloadFormatIndicator
    internal lateinit var contentType: String
    internal lateinit var responseTopic: String
    internal lateinit var correlationData: ByteArray
    private val userProperties: MutableList<Pair<String, String>> = mutableListOf()

    fun user(name: String, value: String) {
        userProperties += name to value
    }

    fun user(user: Pair<String, String>) {
        userProperties += user
    }

    private fun <C : Mqtt5PublishBuilderBase.Complete<C>, T : Mqtt5PublishBuilderBase<C>> T.buildInternal() =
        topic(topic)
            .also {
                if (this@MqttPublishDsl::payload.isInitialized)
                    it.payload(payload)
                if (this@MqttPublishDsl::qos.isInitialized)
                    it.qos(qos)
                if (retain != null)
                    it.retain(retain!!)
                if (messageExpiryInterval != null)
                    it.messageExpiryInterval(messageExpiryInterval!!)
                if (this@MqttPublishDsl::payloadFormatIndicator.isInitialized)
                    it.payloadFormatIndicator(payloadFormatIndicator)
                if (this@MqttPublishDsl::contentType.isInitialized)
                    it.contentType(contentType)
                if (this@MqttPublishDsl::responseTopic.isInitialized)
                    it.responseTopic(responseTopic)
                if (this@MqttPublishDsl::correlationData.isInitialized)
                    it.correlationData(correlationData)
                if (userProperties.isNotEmpty())
                    it.userProperties().also { userProp ->
                        userProperties.forEach { user ->
                            userProp.add(user.first, user.second)
                        }
                    }.applyUserProperties()
            }

    fun build() = Mqtt5Publish.builder().buildInternal().build()

    fun willBuild() = Mqtt5WillPublish.builder().buildInternal().build()

}