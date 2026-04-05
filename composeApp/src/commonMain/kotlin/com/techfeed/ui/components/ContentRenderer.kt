package com.techfeed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.techfeed.data.model.ContentBlock
import com.techfeed.data.model.ContentBlockType
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedType

/**
 * ContentRenderer — renders a list of [ContentBlock] from the API.
 *
 * Each block type maps to a native Compose widget — zero parsing, zero WebView:
 *
 *  HEADING  → styled Text (h1/h2/h3 from block.style)
 *  TEXT     → body Text (bold/normal from block.style)
 *  IMAGE    → Coil AsyncImage with rounded corners
 *  CODE     → monospaced Text on dark background
 *  QUOTE    → italicized Text with left accent border
 *  DIVIDER  → HorizontalDivider
 */
@Composable
fun ContentRenderer(
    blocks: List<ContentBlock>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier           = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEach { block ->
            when (block.type) {
                ContentBlockType.HEADING  -> HeadingBlock(block)
                ContentBlockType.TEXT     -> TextBlock(block)
                ContentBlockType.IMAGE    -> ImageBlock(block)
                ContentBlockType.CODE     -> CodeBlock(block)
                ContentBlockType.QUOTE    -> QuoteBlock(block)
                ContentBlockType.DIVIDER  -> DividerBlock()
            }
        }
    }
}

// ── Block Renderers ───────────────────────────────────────────────────────────

@Composable
private fun HeadingBlock(block: ContentBlock) {
    val (fontSize, fontWeight) = when (block.style.lowercase()) {
        "h1"  -> 24.sp to FontWeight.Black
        "h2"  -> 20.sp to FontWeight.Bold
        else  -> 17.sp to FontWeight.SemiBold   // h3 default
    }
    Text(
        text       = block.value,
        color      = TechFeedColors.TextPrimary,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        lineHeight = (fontSize.value * 1.3f).sp,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun TextBlock(block: ContentBlock) {
    val fontWeight = if (block.style.lowercase() == "bold") FontWeight.Bold
                     else FontWeight.Normal
    Text(
        text       = block.value,
        style      = TechFeedType.Body,
        fontWeight = fontWeight,
        modifier   = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ImageBlock(block: ContentBlock) {
    if (block.url.isBlank()) return
    AsyncImage(
        model              = block.url,
        contentDescription = block.value.ifBlank { "Article image" },
        contentScale       = ContentScale.Crop,
        modifier           = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TechFeedColors.Surface700)
            .heightIn(min = 120.dp, max = 320.dp),
    )
}

@Composable
private fun CodeBlock(block: ContentBlock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TechFeedColors.Surface700)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text  = block.value,
            style = TechFeedType.Mono,
            color = TechFeedColors.Accent,
        )
    }
}

@Composable
private fun QuoteBlock(block: ContentBlock) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(TechFeedColors.Accent)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text       = block.value,
            style      = TechFeedType.Body.copy(fontStyle = FontStyle.Italic),
            color      = TechFeedColors.TextSecondary,
            modifier   = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DividerBlock() {
    HorizontalDivider(
        color     = TechFeedColors.Surface600,
        modifier  = Modifier.padding(vertical = 4.dp),
    )
}
