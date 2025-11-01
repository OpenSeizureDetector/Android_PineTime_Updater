package uk.org.openseizuredetector.pinetime.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.org.openseizuredetector.pinetime.R

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    // Read string resources into vals so they can be used from non-composable lambdas
    val title = stringResource(id = R.string.about_title)
    val appName = stringResource(id = R.string.app_name)
    val description = stringResource(id = R.string.about_app_description)
    val creditsIntro = stringResource(id = R.string.about_credits_intro)
    val creditsLibs = stringResource(id = R.string.about_credits_libraries)
    val creditsAuthors = stringResource(id = R.string.about_credits_authors)
    val licenseNote = stringResource(id = R.string.about_license_note)
    val okText = stringResource(id = R.string.action_ok)
    val licenseLinkText = stringResource(id = R.string.about_license_link_text)
    val licenseUrl = stringResource(id = R.string.license_url)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = description)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = creditsIntro, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = creditsLibs)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = creditsAuthors)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = licenseNote)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = okText)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // Open the repository license file in browser
                uriHandler.openUri(licenseUrl)
            }) {
                Text(text = licenseLinkText)
            }
        }
    )
}
