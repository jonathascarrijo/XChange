package org.knowm.xchange.coinbase.service.polling;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.coinbase.CoinbaseAdapters;
import org.knowm.xchange.coinbase.dto.marketdata.CoinbaseMoney;
import org.knowm.xchange.coinbase.dto.marketdata.CoinbasePrice;
import org.knowm.xchange.coinbase.dto.marketdata.CoinbaseSpotPriceHistory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.polling.marketdata.PollingMarketDataService;

/**
 * @author jamespedwards42
 */
public class CoinbaseMarketDataService extends CoinbaseMarketDataServiceRaw implements PollingMarketDataService {

	private static final BigDecimal MAX_QUOTE_USD = new BigDecimal(1000);
  /**
   * Constructor
   *
   * @param exchange
   */
  public CoinbaseMarketDataService(Exchange exchange) {

    super(exchange);
  }

  /**
   * @param args Optional Boolean. If true an additional call to retrieve the spot price history will be made and used to populate the 24 hour high
   *        and low values for the Ticker.
   * @return A Ticker with Coinbase's current buy price as the best ask, sell price as the best bid, spot price as the last value, and can optionally
   *         use the spot price history to find the 24 hour high and low.
   */
  @Override
  public Ticker getTicker(CurrencyPair currencyPair, final Object... args) throws IOException {

    final String currency = currencyPair.counter.getCurrencyCode();
    final CoinbasePrice buyPrice = super.getCoinbaseBuyPrice(BigDecimal.ONE, currency);
    final CoinbasePrice sellPrice = super.getCoinbaseSellPrice(BigDecimal.ONE, currency);
    final CoinbaseMoney spotRate = super.getCoinbaseSpotRate(currency);

    final CoinbaseSpotPriceHistory coinbaseSpotPriceHistory = (args != null && args.length > 0 && args[0] != null && args[0] instanceof Boolean
        && (Boolean) args[0]) ? super.getCoinbaseHistoricalSpotRates() : null;

    return CoinbaseAdapters.adaptTicker(currencyPair, buyPrice, sellPrice, spotRate, coinbaseSpotPriceHistory);
  }

	public OrderBook getOrderBook(CurrencyPair currencyPair, Object... args) throws ExchangeException, IOException {
		Ticker ticker = getTicker(currencyPair, args);
		BigDecimal maxquote = getQuoteLimit(currencyPair);

		List<LimitOrder> asks = new ArrayList<LimitOrder>();
		List<LimitOrder> bids = new ArrayList<LimitOrder>();

		BigDecimal askprice = ticker.getAsk();
		BigDecimal bidprice = ticker.getBid();

		LimitOrder limitOrder = new LimitOrder.Builder(OrderType.ASK, currencyPair)
				.tradableAmount(maxquote.divide(askprice, 8, RoundingMode.HALF_EVEN)).limitPrice(askprice).build();
		asks.add(limitOrder);
		limitOrder = new LimitOrder.Builder(OrderType.BID, currencyPair)
				.tradableAmount(maxquote.divide(bidprice, 8, RoundingMode.HALF_EVEN)).limitPrice(bidprice).build();
		bids.add(limitOrder);

		return new OrderBook(new Date(), asks, bids);
	}

	private BigDecimal getQuoteLimit(CurrencyPair currencyPair) {
		if (currencyPair.counter.getCurrencyCode().equals(Currency.USD.getCurrencyCode())) {
			return MAX_QUOTE_USD;
		}
		throw new NotYetImplementedForExchangeException("Not implemented for " + currencyPair.counter);
	}


  @Override
  public Trades getTrades(CurrencyPair currencyPair, final Object... args) {

    throw new NotAvailableFromExchangeException();
  }

}
