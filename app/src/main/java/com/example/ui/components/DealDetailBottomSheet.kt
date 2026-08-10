package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PriceHistory
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailBottomSheet(
    deal: PropertyDeal,
    priceHistory: List<PriceHistory>,
    onDismiss: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onCalculateRoiClick: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    var notesText by remember { mutableStateOf(deal.notes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier.testTag("deal_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = deal.imageUrl.ifBlank { "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800" },
                    contentDescription = deal.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Source Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "Fonte: ${deal.sourceName}",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Title & Location
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = deal.title,
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Text(text = deal.location, color = TextSecondaryDark, fontSize = 14.sp)
                    Text(text = "•", color = TextMutedDark)
                    Text(text = "${deal.surfaceSqm} m²", color = TextSecondaryDark, fontSize = 14.sp)
                }
            }

            HorizontalDivider(color = SurfaceCardBorder)

            // Price Row & ROI CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Offerta Base / Richiesta", color = TextMutedDark, fontSize = 11.sp)
                    Text(currencyFormat.format(deal.askingPrice), color = EmeraldGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Valore Stima: ${currencyFormat.format(deal.estimatedMarketValue)} (-${deal.discountPercent}%)", color = TextSecondaryDark, fontSize = 12.sp)
                }

                Button(
                    onClick = onCalculateRoiClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("detail_calc_roi_btn")
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simula ROI", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Price History Section
            if (priceHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Storico Ribassi Asta / Prezzi", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        priceHistory.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.eventLabel} (${item.dateRecorded})", color = TextSecondaryDark, fontSize = 12.sp)
                                Text(currencyFormat.format(item.price), color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Investor Notes Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Note Investitore & Appunti", color = TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = notesText,
                    onValueChange = {
                        notesText = it
                        onSaveNotes(it)
                    },
                    placeholder = { Text("Aggiungi note su sopralluogo, perizia, vicinanza servizi...", color = TextMutedDark) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("deal_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deal.sourceUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apri Fonte", color = CyanAccent, fontSize = 12.sp)
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .background(RoseRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .size(42.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = RoseRed)
                }
            }
        }
    }
}
