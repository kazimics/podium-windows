package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.SubscriptionManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class UnsubscribeTest {

    private lateinit var database: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var testDbFile: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        subscriptionManager = SubscriptionManager(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testUnsubscribeRemovesSubscription() = runBlocking {
        val origin = "https://example.com/feed.xml"

        subscriptionManager.subscribe(origin)
        assertTrue(subscriptionManager.isSubscribed(origin))

        subscriptionManager.unsubscribe(origin)
        assertFalse(subscriptionManager.isSubscribed(origin))
    }

    @Test
    fun testUnsubscribeNonExistentDoesNotThrow() = runBlocking {
        val origin = "https://nonexistent.com/feed.xml"

        subscriptionManager.unsubscribe(origin)
        assertFalse(subscriptionManager.isSubscribed(origin))
    }

    @Test
    fun testUnsubscribeDoesNotAffectOtherSubscriptions() = runBlocking {
        val origin1 = "https://example.com/feed1.xml"
        val origin2 = "https://example.com/feed2.xml"

        subscriptionManager.subscribe(origin1)
        subscriptionManager.subscribe(origin2)

        subscriptionManager.unsubscribe(origin1)

        assertFalse(subscriptionManager.isSubscribed(origin1))
        assertTrue(subscriptionManager.isSubscribed(origin2))
    }

    @Test
    fun testSubscribeAfterUnsubscribe() = runBlocking {
        val origin = "https://example.com/feed.xml"

        subscriptionManager.subscribe(origin)
        assertTrue(subscriptionManager.isSubscribed(origin))

        subscriptionManager.unsubscribe(origin)
        assertFalse(subscriptionManager.isSubscribed(origin))

        subscriptionManager.subscribe(origin)
        assertTrue(subscriptionManager.isSubscribed(origin))
    }
}
