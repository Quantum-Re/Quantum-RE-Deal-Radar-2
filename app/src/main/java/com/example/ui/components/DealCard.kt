package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DealCard(
    deal: PropertyDeal,
    onCardClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onCalculateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    val bookmarkTint by animateColorAsState(
        targetValue = if (deal.isBookmarked) AmberGold else TextMutedDark,
        label = "bookmarkTint"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(24.dp))
            .clickable { onCardClick() }
            .testTag("deal_card_${deal.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Banner with Badges Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = deal.imageUrl.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" },
                    contentDescription = deal.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Gradient for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 100f
                            )
                        )
                )

                // Top Right: Bookmark Button
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                        .testTag("bookmark_button_${deal.id}")
                ) {
                    Icon(
                        imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Deal",
                        tint = bookmarkTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Top Left: Source Provider Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyanAccent, CircleShape)
                        )
                        Text(
                            text = deal.sourceName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom Left: Discount & Cap Rate Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Discount Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "-${deal.discountPercent}% Stima",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Yield Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberGold
                    ) {
                        Text(
                            text = "${deal.estimatedCapRate}% Yield",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Card Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = deal.title,
                    color = TextPrimaryDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Location & Surface Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = deal.location,
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }

                    Text(text = "•", color = TextMutedDark, fontSize = 12.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${deal.surfaceSqm} m²",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SurfaceCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Price Row: Asking Price vs Market Valuation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (deal.propertyType.contains("Asta", ignoreCase = true)) "Offerta Base Asta" else "Prezzo Richiesta",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currencyFormat.format(deal.askingPrice),
                            color = EmeraldGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valore di Mercato",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currencyFormat.format(deal.estimatedMarketValue),
                            color = TextSecondaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }

                    // Action: Calculate ROI
                    IconButton(
                        onClick = onCalculateClick,
                        modifier = Modifier
                            .background(Color(0xFF0369A1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .size(38.dp)
                            .testTag("calc_roi_button_${deal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calcola ROI",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Auction Date notice if present
                if (!deal.auctionDate.isNull_or_blank_safe()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E1B4B)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = PurpleIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Termine Presentazione Offerta: ${deal.auctionDate}",
                                color = Color(0xFFC7D2FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
