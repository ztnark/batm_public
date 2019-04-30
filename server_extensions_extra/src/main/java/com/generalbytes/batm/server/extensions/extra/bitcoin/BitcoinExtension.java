/*************************************************************************************
 * Copyright (C) 2014-2016 GENERAL BYTES s.r.o. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * GENERAL BYTES s.r.o.
 * Web      :  http://www.generalbytes.com
 *
 ************************************************************************************/
package com.generalbytes.batm.server.extensions.extra.bitcoin;

import com.generalbytes.batm.common.currencies.CryptoCurrency;
import com.generalbytes.batm.common.currencies.FiatCurrency;
import com.generalbytes.batm.server.extensions.*;
import com.generalbytes.batm.server.extensions.FixPriceRateSource;
import com.generalbytes.batm.server.extensions.extra.bitcoin.exchanges.bitfinex.BitfinexExchange;
import com.generalbytes.batm.server.extensions.extra.bitcoin.exchanges.bittrex.BittrexExchange;
import com.generalbytes.batm.server.extensions.extra.bitcoin.exchanges.hitbtc.HitbtcExchange;
import com.generalbytes.batm.server.extensions.extra.bitcoin.exchanges.itbit.ItBitExchange;
import com.generalbytes.batm.server.extensions.extra.bitcoin.paymentprocessors.bitcoinpay.BitcoinPayPP;
import com.generalbytes.batm.server.extensions.extra.bitcoin.paymentprocessors.coinofsale.CoinOfSalePP;
import com.generalbytes.batm.server.extensions.extra.bitcoin.sources.bity.BityRateSource;
import com.generalbytes.batm.server.extensions.extra.bitcoin.sources.mrcoin.MrCoinRateSource;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.bitcoind.BATMBitcoindRPCWallet;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.bitcore.BitcoreWallet;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.bitgo.v2.BitgoWallet;
import com.generalbytes.batm.server.extensions.watchlist.IWatchList;

import java.math.BigDecimal;
import java.util.*;

public class BitcoinExtension extends AbstractExtension{
    private IExtensionContext ctx;

    @Override
    public void init(IExtensionContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getName() {
        return "BATM Bitcoin extra extension";
    }

    @Override
    public IExchange createExchange(String paramString) //(Bitstamp is in built-in extension)
    {
        if ((paramString != null) && (!paramString.trim().isEmpty()))
        {
            StringTokenizer paramTokenizer = new StringTokenizer(paramString, ":");
            String prefix = paramTokenizer.nextToken();
            if ("bitfinex".equalsIgnoreCase(prefix)) {
                String apiKey = paramTokenizer.nextToken();
                String apiSecret = paramTokenizer.nextToken();
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (paramTokenizer.hasMoreTokens()) {
                    preferredFiatCurrency = paramTokenizer.nextToken().toUpperCase();
                }
                return new BitfinexExchange(apiKey, apiSecret, preferredFiatCurrency);
            }else if ("bittrex".equalsIgnoreCase(prefix)) {
                String apiKey = paramTokenizer.nextToken();
                String apiSecret = paramTokenizer.nextToken();
                return new BittrexExchange(apiKey, apiSecret);
            }else if ("itbit".equalsIgnoreCase(prefix)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                String userId = paramTokenizer.nextToken();
                String accountId = paramTokenizer.nextToken();
                String clientKey = paramTokenizer.nextToken();
                String clientSecret = paramTokenizer.nextToken();
                if (paramTokenizer.hasMoreTokens()) {
                    preferredFiatCurrency = paramTokenizer.nextToken().toUpperCase();
                }
                return new ItBitExchange(userId, accountId, clientKey, clientSecret, preferredFiatCurrency);
            }else if("hitbtc".equalsIgnoreCase(prefix)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                String apiKey = paramTokenizer.nextToken();
                String apiSecret = paramTokenizer.nextToken();
                return new HitbtcExchange(apiKey, apiSecret,preferredFiatCurrency);
            }else if("dvchain".equalsIgnoreCase(prefix)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                String apiSecret = paramTokenizer.nextToken();
                boolean useSandbox = Boolean.parseBoolean(paramTokenizer.nextToken());
                return new DVChainExchange(apiSecret, useSandbox, preferredFiatCurrency);
            }
        }
        return null;
    }

    @Override
    public IPaymentProcessor createPaymentProcessor(String paymentProcessorLogin) {
        if (paymentProcessorLogin !=null && !paymentProcessorLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(paymentProcessorLogin,":");
            String processorType = st.nextToken();
            if ("bitcoinpay".equalsIgnoreCase(processorType)) { //bitcoinpay:msciu823jes
                if (st.hasMoreTokens()) {
                    String apiKey = st.nextToken();
                    return new BitcoinPayPP(apiKey);
                }
            }else if ("coinofsale".equalsIgnoreCase(processorType)) { //coinofsale:token:pin
                String token = st.nextToken();
                String pin = st.nextToken();
                return new CoinOfSalePP(token,pin);
            }
        }
        return null;
    }

    @Override
    public IWallet createWallet(String walletLogin) {
        if (walletLogin !=null && !walletLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(walletLogin,":");
            String walletType = st.nextToken();
            if ("bitcoind".equalsIgnoreCase(walletType)) {
                //"bitcoind:protocol:user:password:ip:port:accountname"

                String protocol = st.nextToken();
                String username = st.nextToken();
                String password = st.nextToken();
                String hostname = st.nextToken();
                String port = st.nextToken();
                String accountName = "";
                if (st.hasMoreTokens()) {
                    accountName = st.nextToken();
                }


                if (protocol != null && username != null && password != null && hostname != null && port != null && accountName != null) {
                    String rpcURL = protocol + "://" + username + ":" + password + "@" + hostname + ":" + port;
                    return new BATMBitcoindRPCWallet(rpcURL, accountName, CryptoCurrency.BTC.getCode());
                }
            }else if ("bitcore".equalsIgnoreCase(walletType)) { //bitcore:apiKey:proxyUrl
                String apiKey = st.nextToken();
                // the next token is a URL, so we can't use : as a delimiter
                // instead use \n and then remove the leading :
                String proxyUrl = st.nextToken("\n").replaceFirst(":", "");
                return new BitcoreWallet(apiKey, proxyUrl);
            }else if ("bitgo".equalsIgnoreCase(walletType)) { //bitgo:host:port:token:wallet_address:wallet_passphrase
                String first = st.nextToken();
                String protocol = "";
                String host = "";
                String fullHost = "";
                if(first != null && first.startsWith("http")) {
                    protocol = first ;
                    host = st.nextToken();
                    fullHost = protocol + ":" + host;
                } else {
                    host = first;
                    fullHost = host;
                }

                String port = "";
                String token = "";
                String next = st.nextToken();
                if(next != null && next.length() > 6) {
                    token = next;
                } else {
                    port = next;
                    token = st.nextToken();
                }
                String walletAddress = st.nextToken();
                String walletPassphrase = st.nextToken();
                return new BitgoWallet(fullHost, port, token, walletAddress, walletPassphrase);
            }
        }
        return null;
    }

    @Override
    public ICryptoAddressValidator createAddressValidator(String cryptoCurrency) {
        return null; //no BTC address validator in open source version so far (It is present in built-in extension)
    }

    @Override
    public IPaperWalletGenerator createPaperWalletGenerator(String cryptoCurrency) {
        return null; //no BTC paper wallet generator in open source version so far (It is present in built-in extension)
    }

    @Override
    public IRateSource createRateSource(String sourceLogin) {
        //NOTE: (Bitstamp is in built-in extension)
        if (sourceLogin != null && !sourceLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(sourceLogin,":");
            String rsType = st.nextToken();

            if ("btcfix".equalsIgnoreCase(rsType)) {
                BigDecimal rate = BigDecimal.ZERO;
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    try {
                        rate = new BigDecimal(st.nextToken());
                    } catch (Throwable e) {
                    }
                }
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken().toUpperCase();
                }
                return new FixPriceRateSource(rate,preferredFiatCurrency);
            } else if ("fixprice".equalsIgnoreCase(rsType)) {
                BigDecimal rate = BigDecimal.ZERO;
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    try {
                        rate = new BigDecimal(st.nextToken());
                    } catch (Throwable e) {
                    }
                }
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken().toUpperCase();
                }
                return new FixPriceRateSource(rate,preferredFiatCurrency);
            }else if ("bitfinex".equalsIgnoreCase(rsType)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken().toUpperCase();
                }
               return new BitfinexExchange("**","**", preferredFiatCurrency);
            }else if ("bittrex".equalsIgnoreCase(rsType)) {
                return new BittrexExchange("**","**");
            }else if ("bity".equalsIgnoreCase(rsType)) {
                String preferredFiatCurrency = FiatCurrency.CHF.getCode();
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken().toUpperCase();
                }
                return new BityRateSource(preferredFiatCurrency);
            }else if ("mrcoin".equalsIgnoreCase(rsType)) {
                return new MrCoinRateSource();
            }else if ("itbit".equalsIgnoreCase(rsType)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken().toUpperCase();
                }
                return new ItBitExchange(preferredFiatCurrency);
            }
        }
        return null;
    }

    @Override
    public Set<String> getSupportedCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CryptoCurrency.BTC.getCode());
        result.add(CryptoCurrency.ETH.getCode());
        result.add(CryptoCurrency.LTC.getCode());
        result.add(CryptoCurrency.BCH.getCode());
        return result;
    }

    @Override
    public Set<String> getSupportedWatchListsNames() {
        return null;
    }

    @Override
    public IWatchList getWatchList(String name) {
        return null;
    }

    @Override
    public Set<ICryptoCurrencyDefinition> getCryptoCurrencyDefinitions() {
        return null;
    }
}
