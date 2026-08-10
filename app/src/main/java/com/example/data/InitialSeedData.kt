package com.example.data

object InitialSeedData {
    val initialSources = listOf(
        ScraperSource(
            id = "reperform",
            name = "1. Reperform",
            url = "https://www.reperform.com/",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".asset-card",
                  "titleSelector": ".asset-title",
                  "priceSelector": ".base-price",
                  "marketValueSelector": ".appraisal-val",
                  "sqmSelector": ".surface-mq",
                  "locationSelector": ".geo-location"
                }
            """.trimIndent(),
            totalDealsFound = 84
        ),
        ScraperSource(
            id = "iresales",
            name = "2. i-Resales",
            url = "https://www.i-resales.it/",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".listing-item",
                  "titleSelector": "h3.title",
                  "priceSelector": ".price-tag",
                  "marketValueSelector": ".market-value",
                  "sqmSelector": ".surface",
                  "locationSelector": ".address-label"
                }
            """.trimIndent(),
            totalDealsFound = 62
        ),
        ScraperSource(
            id = "uclam_reimpresa",
            name = "3. UCLAM / RE-Impresa",
            url = "https://www.re-impresa.it/",
            robotsStatus = "NESSUN_ROBOTS",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".annuncio-box",
                  "titleSelector": ".titolo-annuncio",
                  "priceSelector": ".prezzo-richiesta",
                  "marketValueSelector": ".stima-mercato",
                  "sqmSelector": ".metri-quadri",
                  "locationSelector": ".comune-prov"
                }
            """.trimIndent(),
            totalDealsFound = 45
        ),
        ScraperSource(
            id = "bnl_immobili",
            name = "4. BNL Immobili",
            url = "https://immobili.bnl.it/",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".immobile-card",
                  "titleSelector": ".card-header-title",
                  "priceSelector": ".prezzo-base",
                  "marketValueSelector": ".valore-stima",
                  "sqmSelector": ".superficie-totale",
                  "locationSelector": ".citta-provincia"
                }
            """.trimIndent(),
            totalDealsFound = 38
        ),
        ScraperSource(
            id = "bper_leasing",
            name = "5. BPER Leasing",
            url = "https://www.bperleasing.it/beni-in-vendita/",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".asset-card",
                  "titleSelector": ".asset-name",
                  "priceSelector": ".base-price",
                  "marketValueSelector": ".appraisal-value",
                  "sqmSelector": ".surface",
                  "locationSelector": ".location"
                }
            """.trimIndent(),
            totalDealsFound = 51
        ),
        ScraperSource(
            id = "mps_bandi",
            name = "6. MPS – Bandi e Procedure Competitive",
            url = "https://www.mps.it/comunicazioni-alla-clientela/avvisi/",
            robotsStatus = "CONSENTITO",
            configStatus = "DA_VERIFICARE",
            activeParserRulesJson = """
                {
                  "listSelector": ".avviso-row",
                  "titleSelector": ".descrizione-immobile",
                  "priceSelector": ".prezzo-base",
                  "marketValueSelector": ".valore-perizia",
                  "sqmSelector": ".superficie-commerciale",
                  "locationSelector": ".citta"
                }
            """.trimIndent(),
            totalDealsFound = 29
        ),
        ScraperSource(
            id = "intesa_proprieta",
            name = "7. Intesa Sanpaolo Proprietà",
            url = "https://www.intesasanpaolo.com/it/proprieta.html",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".property-item-box",
                  "titleSelector": ".property-title",
                  "priceSelector": ".asking-price",
                  "marketValueSelector": ".appraisal-val",
                  "sqmSelector": ".property-sqm",
                  "locationSelector": ".city-name"
                }
            """.trimIndent(),
            totalDealsFound = 42
        ),
        ScraperSource(
            id = "pvp_quimmo",
            name = "8. PVP e Quimmo",
            url = "https://portalevenditepubbliche.giustizia.it",
            robotsStatus = "CONSENTITO",
            configStatus = "CONSENTITO",
            activeParserRulesJson = """
                {
                  "listSelector": ".scheda-asta",
                  "titleSelector": ".titolo-lotto",
                  "priceSelector": ".offerta-minima",
                  "marketValueSelector": ".valore-perizia",
                  "sqmSelector": ".mq-lotto",
                  "locationSelector": ".ubicazione"
                }
            """.trimIndent(),
            totalDealsFound = 194
        ),
        ScraperSource(
            id = "bper_real_estate",
            name = "9. BPER Real Estate (Contatto Diretto)",
            url = "https://www.bper.it/real-estate",
            robotsStatus = "NESSUN_ROBOTS",
            configStatus = "SOLO_CONTATTO",
            activeParserRulesJson = """
                {
                  "listSelector": ".direct-asset",
                  "titleSelector": ".asset-title",
                  "priceSelector": ".indicative-price",
                  "marketValueSelector": ".book-value",
                  "sqmSelector": ".total-sqm",
                  "locationSelector": ".address"
                }
            """.trimIndent(),
            totalDealsFound = 15
        ),
        ScraperSource(
            id = "cdp_fintecna_bancaditalia",
            name = "10. CDP / Fintecna e Banca d’Italia",
            url = "https://www.cdp.it/immobiliare",
            robotsStatus = "CONSENTITO",
            configStatus = "DA_VERIFICARE",
            activeParserRulesJson = """
                {
                  "listSelector": ".bando-item",
                  "titleSelector": ".bando-title",
                  "priceSelector": ".importo-base",
                  "marketValueSelector": ".valore-stimato",
                  "sqmSelector": ".superficie",
                  "locationSelector": ".regione-comune"
                }
            """.trimIndent(),
            totalDealsFound = 31
        )
    )

    val initialDeals = listOf(
        PropertyDeal(
            id = 101,
            title = "Palazzina Direzionale ex-NPL - Opp. Riconversione",
            sourceKey = "reperform",
            sourceName = "Reperform",
            sourceUrl = "https://www.reperform.com/",
            location = "Milano - Porta Romana (MI)",
            propertyType = "Commerciale / Value-Add",
            askingPrice = 620000.0,
            estimatedMarketValue = 1100000.0,
            surfaceSqm = 850,
            discountPercent = 43,
            estimatedCapRate = 10.2,
            auctionDate = "18/09/2026",
            status = "PRICE_CUT",
            imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80",
            notes = "Edificio cielo-terra dismesso con potenziale cambio d'uso in residenziale/micro-living.",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 102,
            title = "Complesso Logistico & Commerciale Distressed",
            sourceKey = "iresales",
            sourceName = "i-Resales",
            sourceUrl = "https://www.i-resales.it/",
            location = "Verona - ZAI (VR)",
            propertyType = "Industriale",
            askingPrice = 480000.0,
            estimatedMarketValue = 820000.0,
            surfaceSqm = 1400,
            discountPercent = 41,
            estimatedCapRate = 11.5,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
            notes = "Piattaforma remarketing. Ottimo per operazione di frazionamento logistico o trasformazione last-mile.",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 103,
            title = "Area Industriale 4.500 m² da Riqualificare",
            sourceKey = "uclam_reimpresa",
            sourceName = "UCLAM / RE-Impresa",
            sourceUrl = "https://www.re-impresa.it/",
            location = "Torino - Mirafiori (TO)",
            propertyType = "Riconversione",
            askingPrice = 390000.0,
            estimatedMarketValue = 750000.0,
            surfaceSqm = 4500,
            discountPercent = 48,
            estimatedCapRate = 12.0,
            auctionDate = "10/10/2026",
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
            notes = "Dismissione portafoglio corporate. Pronta masterplan per parco tecnologico o residenze universitarie.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 104,
            title = "Ex Filiale BNL Piano Terra + Interrato Vetrina",
            sourceKey = "bnl_immobili",
            sourceName = "BNL Immobili",
            sourceUrl = "https://immobili.bnl.it/",
            location = "Roma - Quartiere Prati (RM)",
            propertyType = "Commerciale",
            askingPrice = 310000.0,
            estimatedMarketValue = 520000.0,
            surfaceSqm = 280,
            discountPercent = 40,
            estimatedCapRate = 8.9,
            auctionDate = null,
            status = "PRICE_CUT",
            imageUrl = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            notes = "Patrimonio BNL BNP Paribas. Posizione angolare ad altissimo passaggio pedonale.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 105,
            title = "Capannone da Leasing Risolto con Banchina",
            sourceKey = "bper_leasing",
            sourceName = "BPER Leasing",
            sourceUrl = "https://www.bperleasing.it/beni-in-vendita/",
            location = "Bologna - Navile (BO)",
            propertyType = "Industriale",
            askingPrice = 490000.0,
            estimatedMarketValue = 820000.0,
            surfaceSqm = 1200,
            discountPercent = 40,
            estimatedCapRate = 11.2,
            auctionDate = "05/10/2026",
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
            notes = "Remarketing BPER Leasing. Banchina di carico presente, altezza sottomagazzino 8 metri.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 106,
            title = "Edificio Storico Cielo-Terra per Boutique Hotel",
            sourceKey = "mps_bandi",
            sourceName = "MPS – Bandi e Procedure",
            sourceUrl = "https://www.mps.it/comunicazioni-alla-clientela/avvisi/",
            location = "Siena - Centro Storico (SI)",
            propertyType = "Value-Add",
            askingPrice = 750000.0,
            estimatedMarketValue = 1350000.0,
            surfaceSqm = 920,
            discountPercent = 44,
            estimatedCapRate = 9.5,
            auctionDate = "22/10/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80",
            notes = "Bando competitivo MPS. Struttura vincolata con progetto di massima approvato per 18 suite.",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 107,
            title = "Ex Agenzia Intesa Sanpaolo 250 m² Centro",
            sourceKey = "intesa_proprieta",
            sourceName = "Intesa Sanpaolo Proprietà",
            sourceUrl = "https://www.intesasanpaolo.com/it/proprieta.html",
            location = "Firenze - Campo di Marte (FI)",
            propertyType = "Commerciale",
            askingPrice = 280000.0,
            estimatedMarketValue = 460000.0,
            surfaceSqm = 250,
            discountPercent = 39,
            estimatedCapRate = 8.7,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1497215728101-856f4ea42174?auto=format&fit=crop&w=800&q=80",
            notes = "Ex dipendenza bancaria. Canna fumaria installabile, ideale per ristorazione o clinic medicale.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 108,
            title = "Attico Vista Duomo - Offerta Minima Asta -42%",
            sourceKey = "pvp_quimmo",
            sourceName = "PVP e Quimmo",
            sourceUrl = "https://portalevenditepubbliche.giustizia.it",
            location = "Milano - Centro Storico (MI)",
            propertyType = "Asta Giudiziaria",
            askingPrice = 395000.0,
            estimatedMarketValue = 680000.0,
            surfaceSqm = 135,
            discountPercent = 42,
            estimatedCapRate = 8.1,
            auctionDate = "28/09/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
            notes = "Procedura esecutiva tribunale Milano tramite PVP/Quimmo. Terrazzo 40m².",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 109,
            title = "Portafoglio Off-Market 3 Immobili Commerciali Locati",
            sourceKey = "bper_real_estate",
            sourceName = "BPER Real Estate",
            sourceUrl = "https://www.bper.it/real-estate",
            location = "Modena (MO)",
            propertyType = "Off-Market",
            askingPrice = 890000.0,
            estimatedMarketValue = 1400000.0,
            surfaceSqm = 1100,
            discountPercent = 36,
            estimatedCapRate = 9.8,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80",
            notes = "Trattativa diretta riservata con BPER Real Estate desk. Rendita locativa in essere €85k/anno.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 110,
            title = "Edificio Istituzionale Ex-Banca d'Italia da Valorizzare",
            sourceKey = "cdp_fintecna_bancaditalia",
            sourceName = "CDP / Fintecna / B d'I",
            sourceUrl = "https://www.cdp.it/immobiliare",
            location = "Genova - Centro (GE)",
            propertyType = "Riconversione",
            askingPrice = 1200000.0,
            estimatedMarketValue = 2100000.0,
            surfaceSqm = 2200,
            discountPercent = 43,
            estimatedCapRate = 10.5,
            auctionDate = "15/11/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80",
            notes = "Bando CDP/Fintecna. Struttura monumentale adatta per sede executive o luxury residence.",
            isBookmarked = true
        )
    )

    val initialHistories = listOf(
        PriceHistory(dealId = 101, price = 850000.0, dateRecorded = "15/01/2026", eventLabel = "Stima Perizia NPL"),
        PriceHistory(dealId = 101, price = 720000.0, dateRecorded = "20/04/2026", eventLabel = "Primo Ribasso Scraper"),
        PriceHistory(dealId = 101, price = 620000.0, dateRecorded = "01/08/2026", eventLabel = "Attuale Offerta Radar"),

        PriceHistory(dealId = 108, price = 580000.0, dateRecorded = "10/02/2026", eventLabel = "Valore Asta Base"),
        PriceHistory(dealId = 108, price = 395000.0, dateRecorded = "15/06/2026", eventLabel = "Offerta Minima Consentita"),

        PriceHistory(dealId = 105, price = 650000.0, dateRecorded = "01/03/2026", eventLabel = "Stima Perizia Leasing"),
        PriceHistory(dealId = 105, price = 490000.0, dateRecorded = "05/07/2026", eventLabel = "Listino Remarketing BPER")
    )
}

