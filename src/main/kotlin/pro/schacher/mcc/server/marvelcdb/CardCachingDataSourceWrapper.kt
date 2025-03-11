package pro.schacher.mcc.server.marvelcdb

import io.ktor.client.HttpClient
import pro.schacher.mcc.server.dto.CardDto
import java.util.Locale

class CardCachingDataSourceWrapper(
    urlProvider: UrlProvider,
    httpClient: HttpClient,
    private val expirationRate: Float = 0.1f
) : MarvelCDbDataSource(urlProvider, httpClient) {

    init {
        require(this.expirationRate in 0.0..1.0) {
            "Expiration rate must be between 0 and 1"
        }
    }

    private val cachedCards = mutableMapOf<Locale, Set<CardDto>>()

    override suspend fun getCard(locale: Locale, cardCode: String): Result<CardDto> {
        this.getCardFromCache(locale, cardCode)?.let {
            return Result.success(it)
        }

        return super.getCard(locale, cardCode).also {
            it.onSuccess { card ->
                this.cachedCards[locale] = this.cachedCards[locale]?.plus(card) ?: setOf(card)
            }
        }
    }

    override suspend fun getCards(locale: Locale, cardCodes: List<String>): Result<List<CardDto>> {
        val cardsInCache = cardCodes.mapNotNull {
            this.getCardFromCache(locale, it)
        }

        val missingCardCodes = cardCodes.filter { cardCode ->
            cardsInCache.all { it.code != cardCode }
        }

        val cards = super.getCards(locale, missingCardCodes).getOrThrow()

        val combinedResult = cardsInCache + cards
        this.cachedCards[locale] = this.cachedCards[locale]?.plus(cards) ?: cards.toSet()

        return Result.success(combinedResult)
    }

    private fun getCardFromCache(locale: Locale, cardCode: String): CardDto? {
        val cachedCard = this.cachedCards[locale]?.find { it.code == cardCode }

        if (this.shouldExpireEntry()) {
            println("Expiring card $cardCode")
            return null
        }
        return cachedCard
    }

    private fun shouldExpireEntry(): Boolean = Math.random() < this.expirationRate
}