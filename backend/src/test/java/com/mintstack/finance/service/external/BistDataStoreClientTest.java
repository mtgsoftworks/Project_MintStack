package com.mintstack.finance.service.external;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BistDataStoreClientTest {

    private static final Charset TURKISH_CHARSET = Charset.forName("windows-1254");

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void fetchBondPrices_ShouldParseDailyBulletinAndChooseMostLiquidRow() throws Exception {
        String header = """
            PAZAR ISMI;ISLEM TARIHI;ISLEM KODU;VALOR1;VALOR2;ISIN/KOD;PARA BIRIMI;VKG/REPO SURESI;KKG;BIRIKMIS FAIZ/KIRA;ACILIS;EN DUSUK;EN YUKSEK;KAPANIS;AG.ORT. FIYAT/ORAN/SWAP PUANI;AG. ORT. TAKAS FIYATI;ONCEKI ISLEM TARIHI;ONCEKI AG.ORT. FIYAT;ONCEKI KAPANIS;ACILIS FIYAT/GETIRI;EN DUSUK FIYAT/GETIRI;EN YUKSEK FIYAT/GETIRI;KAPANIS FIYAT/GETIRI;AG. ORT.FIYAT/GETIRI;KAPANIS BILESIK GETIRI;AG. ORT. BILESIK GETIRI;SON FIYAT DEGISIMI;AG. ORT. FIYAT DEGISIMI;KAPANIS BILESIK GETIRI DEGISIMI;AG. ORT. BILESIK GETIRI DEGISIMI;MIKTAR;ISLEM HACMI;ISLEM SAYISI;CAPRAZ ISLEM VE TAKAS DISI ISLEM MIKTAR;CAPRAZ ISLEM VE TAKAS DISI ISLEM HACMI;CAPRAZ ISLEM VE TAKAS DISI ISLEM SAYISI;OZEL ISLEM BILDIRIM MIKTARI;OZEL ISLEM BILDIRIM HACMI;OZEL ISLEM BILDIRIM SAYISI;TOPLAM MIKTAR;TOPLAM ISLEM HACMI;TOPLAM ISLEM SAYISI;BIRIKIMLI MIKTAR;BIRIKIMLI ISLEM HACMI;DENGE FIYATI;TEK FIYAT SEANSI ISLEM MIKTARI;TEK FIYAT SEANSI ISLEM HACMI;TEK FIYAT SEANSI SOZLESME SAYISI
            MARKET NAME;TRADEDATE;INSTRUMENT CODE;VALUEDATE1;VALUEDATE2;ISIN/CODE;CURRENCY;DTM/REPO TERM;DTC;ACCRUED INTEREST/LEASE;OPENING;LOWEST;HIGHEST;CLOSING;WT. AVG. PRICE/RATE/SWAP POINT;WT. AVG. SETTLEMENT PRICE;PREVIOUS TRADE DATE;PREVIOUS WT. AVG. PRICE;PREVIOUS CLOSING PRICE;OPENING PRICE/RETURN;LOWEST PRICE/RETURN;HIGHEST PRICE/RETURN;CLOSING PRICE/RETURN;WT. AVG. PRICE/RETURN;CLOSING COMPOUND RETURN;WT. AVG. COMPOUND RETURN;LAST PRICE CHANGE;WT. AVG. PRICE CHANGE;CLOSING COMPOUND RETURN CHANGE;WT. AVG COMPOUND RETURN CHANGE;QUANTITY;TRADED VALUE;NUMBER OF DEALS;CROSS TRADE AND NON-CLEARED TRADE QUANTITY;CROSS AND NON-CLEARED TRADED VALUE;NUMBER OF CROSS AND NON-CLEARED DEALS;TRADE REPORT TRADE VOLUME;TRADE REPORT TRADED VALUE;NUMBER OF TRADE REPORT DEALS;TOTAL QUANTITY;TOTAL TRADED VALUE;TOTAL NUMBER OF DEALS;ACCUMULATED QUANTITY;ACCUMULATED TRADED VALUE;EQUILIBRIUM PRICE;TRADE QUANTITY AT SINGLE PRICE SESSION;TRADED VALUE AT SINGLE PRICE SESSION;NUMBER OF DEALS AT SINGLE PRICE SESSION
            """;
        String csv = header
            + bondRow("BAP KES NORMAL EMIRLER PZ (OPSN)", "TRT120826T16", "99.39", "98.10", "100.10", "99.50", "99.10", "41000000", "44521098.9")
            + "\n"
            + bondRow("BAP KES NORMAL EMIRLER PZ (OPSN)", "TRT120826T16", "99.35", "98.00", "100.20", "99.80", "99.30", "10000000", "10884450.54")
            + "\n"
            + bondRow("BAP REPO-T. REPO NORMAL (GCREPO)", "S", "39.99", "39.99", "40", "40", "40", "1319996000000", "1371233000000")
            + "\n";

        startServer("/data/ttb/2026/05/ttb202605153.zip", zip("ttb202605153.csv", csv));
        BistDataStoreClient client = createClient();

        List<BistDataStoreClient.BistBondPrice> prices = client.fetchBondPrices(LocalDate.of(2026, 5, 15));

        assertThat(prices).hasSize(1);
        BistDataStoreClient.BistBondPrice price = prices.get(0);
        assertThat(price.symbol()).isEqualTo("TRT120826T16");
        assertThat(price.closePrice()).isEqualByComparingTo("99.50");
        assertThat(price.previousClose()).isEqualByComparingTo("99.10");
        assertThat(price.tradedValue()).isEqualByComparingTo("44521098.9");
    }

    @Test
    void fetchViopPrices_ShouldParseSettlementAndVolume() throws Exception {
        String csv = """
            TARIH;SOZLESME KODU;SOZLESME ADI;PAZAR;PAZAR SEGMENTI;SOZLESME TIPI;SOZLESME SINIFI;DAYANAK VARLIK;VADE TARIHI;UZLASMA FIYATI;ONCEKI UZLASMA FIYATI;UZLASMA FIYATI DEGISIMI (%);ACILIS FIYATI;EN DUSUK FIYAT;EN YUKSEK FIYAT;KAPANIS FIYATI; AGIRLIKLI ORTALAMA FIYAT;ISLEM HACMI;PRIM HACMI;ISLEM MIKTARI;ISLEM SAYISI;ACIK POZISYON;ACIK POZISYON DEGISIMI;ACILIS SEANSI FIYATI;ACILIS SEANSI ISLEM HACMI;ACILIS SEANSI PRIM HACMI;ACILIS SEANSI ISLEM MIKTARI;ACILIS SEANSI ISLEM SAYISI
            TRADE DATE;INSTRUMENT SERIES;INSTRUMENT NAME;MARKET;MARKET SEGMENT;INSTRUMENT TYPE;INSTRUMENT CLASS;UNDERLYING;EXPIRATION DATE;SETTLEMENT PRICE;PREVIOUS SETTLEMENT PRICE;CHANGE OF SETTLEMENT PRICE (%);OPENING PRICE;LOWEST PRICE;HIGHEST PRICE;CLOSING PRICE;VWAP;TRADED VALUE;PREMIUM VALUE;TRADE VOLUME;TRADE COUNT;OPEN POSITION;OPEN POSITION CHANGE;OPENING SESSION PRICE;TRADED VALUE AT OPENING SESSION;PREMIUM VALUE AT OPENING SESSION;TRADED VOLUME AT OPENING SESSION;TRADE COUNT AT OPENING SESSION
            2026-05-15;F_AEFES0526;AEFES_05/2026_VIS;D_EQ;SSF;D_EQ_FPD;DE_AEFES_FPD;AEFES.E;2026-05-25;19.76;20.76;-4.82;20.71;19.69;20.71;19.77;20.06;446537342;0;222619;12438;222253;-14064;20.71;33136;0;16;3
            """;

        startServer("/data/vadeli/viop_20260515.csv", csv.getBytes(TURKISH_CHARSET));
        BistDataStoreClient client = createClient();

        List<BistDataStoreClient.BistViopPrice> prices = client.fetchViopPrices(LocalDate.of(2026, 5, 15));

        assertThat(prices).hasSize(1);
        BistDataStoreClient.BistViopPrice price = prices.get(0);
        assertThat(price.symbol()).isEqualTo("F_AEFES0526");
        assertThat(price.settlementPrice()).isEqualByComparingTo("19.76");
        assertThat(price.previousSettlementPrice()).isEqualByComparingTo("20.76");
        assertThat(price.changePercent()).isEqualByComparingTo("-4.82");
        assertThat(price.tradeVolume()).isEqualByComparingTo("222619");
    }

    private void startServer(String path, byte[] body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> writeBytes(exchange, body));
        server.start();
    }

    private BistDataStoreClient createClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
            .build();
        BistDataStoreClient client = new BistDataStoreClient(webClient);
        ReflectionTestUtils.setField(client, "timeoutMs", 5000);
        ReflectionTestUtils.setField(client, "bondDailyBulletinPathTemplate", "/data/ttb/{yyyy}/{MM}/ttb{yyyyMMdd}3.zip");
        ReflectionTestUtils.setField(client, "viopDailyBulletinPathTemplate", "/data/vadeli/viop_{yyyyMMdd}.csv");
        ReflectionTestUtils.setField(client, "healthPath", "/files/datafilepaths_viop.zip");
        return client;
    }

    private byte[] zip(String fileName, String content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, TURKISH_CHARSET)) {
            zip.putNextEntry(new ZipEntry(fileName));
            zip.write(content.getBytes(TURKISH_CHARSET));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private String bondRow(
        String market,
        String symbol,
        String open,
        String low,
        String high,
        String close,
        String previousClose,
        String quantity,
        String tradedValue
    ) {
        String[] values = new String[48];
        java.util.Arrays.fill(values, "");
        values[0] = market;
        values[1] = "2026-05-15";
        values[2] = symbol + "_KESN_T0";
        values[3] = "2026-05-15";
        values[5] = symbol;
        values[6] = "TRY";
        values[10] = open;
        values[11] = low;
        values[12] = high;
        values[13] = close;
        values[14] = close;
        values[15] = close;
        values[16] = "2026-05-14";
        values[18] = previousClose;
        values[30] = quantity;
        values[31] = tradedValue;
        values[39] = quantity;
        values[40] = tradedValue;
        values[41] = "6";
        return String.join(";", values);
    }

    private void writeBytes(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(body);
        } finally {
            exchange.close();
        }
    }
}
