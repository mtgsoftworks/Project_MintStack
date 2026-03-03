# Tasarim Mimarisi ve Modelleme

## 1. Domain Model Ozeti

Sistemin cekirdek domaini 5 ana grupta toplanir:

- Kimlik ve Yetki: User, Role, ApiConfig
- Portfoy: Portfolio, PortfolioItem, PortfolioTransaction
- Piyasa: Instrument, PriceHistory, CurrencyRate
- Izleme: Watchlist, PriceAlert, UserNotification
- Icerik: News, NewsCategory

## 2. ER Model (Mantiksal)

```mermaid
erDiagram
    USER ||--o{ PORTFOLIO : owns
    PORTFOLIO ||--o{ PORTFOLIO_ITEM : contains
    PORTFOLIO ||--o{ PORTFOLIO_TRANSACTION : has

    INSTRUMENT ||--o{ PORTFOLIO_ITEM : referenced_by
    INSTRUMENT ||--o{ PRICE_HISTORY : has

    USER ||--o{ WATCHLIST : owns
    WATCHLIST ||--o{ WATCHLIST_ITEM : contains

    USER ||--o{ PRICE_ALERT : defines
    USER ||--o{ USER_NOTIFICATION : receives

    USER ||--o{ USER_API_CONFIG : configures
    USER ||--o{ USER_DATA_PREFERENCE : prefers

    NEWS }o--|| NEWS_CATEGORY : belongs_to
```

## 3. Uygulama Modeli (Katmanlar)

```mermaid
flowchart TB
    C[Controller] --> S[Service]
    S --> R[Repository]
    R --> DB[(PostgreSQL)]
    S --> Cache[(Redis)]
    S --> Ext[Dis Servis Saglayicilari]
    S --> WS[WebSocket Publisher]
```

## 4. Kritik Is Akisi Modeli

### 4.1 Portfoy Emir Yasam Dongusu

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> PARTIALLY_FILLED : kismi eslesme
    PENDING --> FILLED : tam eslesme
    PARTIALLY_FILLED --> FILLED : kalan miktar doldu
    PENDING --> CANCELED : iptal
    PENDING --> REJECTED : kural ihlali
```

### 4.2 BIST100 Veri Akisi

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant API as Backend
    participant MD as MarketDataService
    participant Y as Yahoo/Provider
    participant DB as PostgreSQL

    UI->>API: GET /market/indices/XU100.IS
    API->>MD: getMarketIndex("XU100.IS")
    MD->>DB: lokal veri var mi?
    alt lokal ve guncel veri varsa
        DB-->>MD: instrument
    else lokal yoksa/guncel degilse
        MD->>Y: fetchStockPrice(XU100.IS)
        Y-->>MD: fiyat
    end
    MD-->>API: InstrumentResponse
    API-->>UI: JSON response
```

## 5. Veri Dogrulama Kurallari

- Emir olusturmada `instrumentId` veya `instrumentSymbol` zorunlu.
- `LIMIT` emirde `limitPrice`, `STOP` emirde `stopPrice` zorunlu.
- Nakit hareketinde `DEPOSIT/WITHDRAW` disi aksiyon kabul edilmez.
- Veri kaynagi seciminde provider yetenek matrisi kontrol edilir.

## 6. Tasarim Kalitesi ve Iyilestirme Alanlari

- Portfoy alani daha fazla domain service'e bolunebilir.
- Market data provider dispatch katmani ayriklastirilabilir.
- TypeScript strict + daha guclu tip kontratlari ile frontend kalitesi artirilabilir.

## 7. Toplanti Sunumu Icin Vurgu

- Modelin merkezinde "Portfoy + Enstruman + Islem" uclusu var.
- Cekirdek akislarda synchronous API + asynchronous update kombinasyonu kullaniliyor.
- Domain kurallari service katmaninda toplandigi icin testlenebilirlik ve degisiklik yonetimi kolay.
