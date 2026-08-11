package com.martinsterentjevs.cacheit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.martinsterentjevs.cacheit.R

// Requires miranda_sans_regular.ttf, miranda_sans_medium.ttf, geist_mono_regular.ttf under res/font/ — see docs/licenses/ for the
// accompanying OFL license text both fonts require to travel with redistribution.

val MirandaSans = FontFamily(
    Font(R.font.mirandasans_regular, FontWeight.Normal),
    Font(R.font.mirandasans_medium, FontWeight.Medium),
)


// Credentials, hashes, IDs, encrypted-string placeholders — per the design doc, always Geist Mono,
// never the primary sans face, even inline within otherwise-Miranda-Sans text.
val GeistMono = FontFamily(
    Font(R.font.geistmono_regular, FontWeight.Normal),
)

// Semantic type scaleTwo weights only — Normal (400) and Medium (500).
val TypeDisplay = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Medium, fontSize = 72.sp, lineHeight = 79.sp)
val TypeTitle: TextStyle = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 29.sp)
val TypeHeading = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp)
val TypeBody = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
val TypeLabel = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
val TypeCaption = TextStyle(fontFamily = MirandaSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp)
val TypeMono = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)

/**
 * Material3's Typography slots map loosely onto the scale above. The raw Type* values remain
 * directly usable (e.g. TypeMono on a credential/ID display) where Material3 has no matching slot.
 */
val CacheItTypography = Typography(
    displayLarge = TypeDisplay,
    titleLarge = TypeTitle,
    headlineSmall = TypeHeading,
    bodyLarge = TypeBody,
    labelLarge = TypeLabel,
    labelSmall = TypeCaption,
)